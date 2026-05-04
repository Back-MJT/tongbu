#!/usr/bin/env python3
"""
Xindong_Corp → Logseq 同步脚本 v2.1
功能：扫描 Xindong_Corp 工作区的增量文件，生成 Logseq inbox 条目
增量同步 + LLM 概念提取（v2.1 升级：MiniMax API 真正的 AI 提取）

频率：每日 cron job（建议 07:00 北京时间）
依赖：Python 3.8+，标准库 + curl（用于 MiniMax API 调用）

v2.1 升级说明：
  使用 MiniMax API 进行真正的 LLM 概念提取，
  替代 v2.0 的启发式 regex 方法。
  API Key 通过环境变量 MINIMAX_API_KEY 注入。
"""

import json
import re
import subprocess
import os
import sys
from pathlib import Path
from datetime import datetime, timedelta
from typing import Optional

# ========== 配置 ==========
XINDONG_ROOT = Path("/media/ai-no1/workspace/Xindong_Corp")
LOGSEQ_INBOX = Path("/media/ai-no1/workspace/logseq/pages/inbox/inbox.md")
LAST_RUN_FILE = XINDONG_ROOT / ".last_sync_time"

MINIMAX_API_URL = "https://api.minimaxi.com/anthropic/v1/messages"
MINIMAX_MODEL = "MiniMax-M2.7"
MINIMAX_API_KEY = os.environ.get("MINIMAX_API_KEY", "")

# 忽略的目录/文件模式
IGNORE_DIRS = {".git", "node_modules", "__pycache__", ".pytest_cache",
               "ruoyi-ui", ".venv", "dist", "coverage", "venv",
               ".hermes", ".logseq", ".cloze"}

IGNORE_EXTENSIONS = {".pyc", ".class", ".jar", ".png", ".jpg", ".mp3", ".mp4",
                     ".db", ".sqlite", ".log"}

IGNORE_NAME_PATTERNS = ("session_", "request_dump_", "tmp_", ".tmp", "tmp_",
                        ".last_sync_time",
                        "auth.json", ".state.db",
                        "_verify_", "_comment_", "_nudge_", "_directive_",
                        "tmp_xin",
                        )

KEEP_JSON_PATTERNS = ("package.json", "tsconfig.json", "vite.config",
                      "tailwind.config", "next.config", ".eslintrc",
                      "pyproject.toml", "setup.py", "requirements.txt")

VALUE_KEYWORDS = {
    ("战略", "Board", "决策", "v1.1"): "high",
    ("PRD", "产品需求", "路线图", "商业模式"): "high",
    ("BD", "厂家", "客户", "试点", "演示"): "high",
    ("IMU", "BLE", "SDK", "架构", "Claude", "API"): "high",
    ("协议", "MOU", "合作", "合同"): "high",
    ("力康来", "宁津", "健身器材", "HealthHub"): "high",
    ("研究", "证据", "竞品", "市场"): "medium",
    ("脚本", "临时", "草稿"): "low",
    ("DEPRECATED", "废弃", "旧版"): "low",
}

# ========== LLM 概念提取（v2.1 核心）==========

LLM_EXTRACT_PROMPT = """你是一个知识工程师。请从以下文档片段中提取结构化信息，返回纯 JSON（无 markdown 格式）：

{{
  "concepts": ["核心概念1", "核心概念2", "核心概念3"],
  "core_claim": "一句话说明这篇文档在论证/记录什么（不超过40字）",
  "entities": {{
    "people": ["人名列表（公司/人）"],
    "companies": ["公司/品牌名称"],
    "technologies": ["技术/产品/工具名称"]
  }},
  "healthhub_revelance": "如果文档和健身器材/HealthHub/IMU/BLE/AI训练/厂家BD相关，写出具体启示（不超过60字）；否则写null",
  "suggested_pages": ["pages/xxx/yyy"],
  "tags": ["#标签1", "#标签2", "#标签3"]
}}

规则：
- concepts 最多3个，基于内容提炼而不是罗列
- healthhub_revelance：只有文档涉及 HealthHub、IMU、BLE、健身器材、厂家BD、宁津市场时才有值
- suggested_pages：只填最相关的1-2个现有 Logseq 页面路径（格式：pages/xxx/yyy），不要创造新页面路径
- tags：最多5个，格式 #tag，用中文或英文标签

文档内容：
{content}

只返回JSON，不要有其他文字："""


def call_minimax_llm(prompt: str, max_tokens: int = 500) -> str:
    """调用 MiniMax API，返回 text 块内容"""
    if not MINIMAX_API_KEY:
        return ""

    # 构建 messages
    messages = [{"role": "user", "content": prompt}]

    payload = {
        "model": MINIMAX_MODEL,
        "max_tokens": max_tokens,
        "messages": messages,
    }

    # 用 curl 调用（标准库无内置 HTTP client）
    curl_cmd = [
        "curl", "-s", "--max-time", "30",
        "-X", "POST",
        MINIMAX_API_URL,
        "-H", "Content-Type: application/json",
        "-H", f"Authorization: Bearer {MINIMAX_API_KEY}",
        "-d", json.dumps(payload),
    ]

    try:
        result = subprocess.run(curl_cmd, capture_output=True, text=True, timeout=35)
        if result.returncode != 0:
            print(f"    [WARN] curl failed: {result.stderr[:100]}", file=sys.stderr)
            return ""
        resp = json.loads(result.stdout)

        # 解析 content 块
        content_blocks = resp.get("content", [])
        for block in content_blocks:
            if block.get("type") == "text":
                return block.get("text", "").strip()
        # 如果没有 text 块，尝试从 thinking 块提取
        for block in content_blocks:
            if block.get("type") == "thinking":
                text = block.get("text", "")
                # 尝试找 ```json ... ``` 包裹的内容
                match = re.search(r"```json\s*(.*?)\s*```", text, re.DOTALL)
                if match:
                    return match.group(1).strip()
        return ""
    except Exception as e:
        print(f"    [WARN] MiniMax API call failed: {e}", file=sys.stderr)
        return ""


def llm_extract_concepts(text: str, filename: str) -> dict:
    """用 LLM 提取文档概念（v2.1）"""
    # 截断文本（LLM 输入限制 + 成本控制）
    truncated = text[:3000]

    prompt = LLM_EXTRACT_PROMPT.format(content=truncated)
    raw = call_minimax_llm(prompt, max_tokens=800)

    if not raw:
        return None  # 降级到启发式

    # 解析 JSON
    try:
        # 清理可能的 markdown code block
        cleaned = re.sub(r"^```json\s*", "", raw).strip()
        cleaned = re.sub(r"\s*```$", "", cleaned).strip()
        data = json.loads(cleaned)

        # 验证必要字段
        if not isinstance(data.get("concepts"), list):
            return None

        return {
            "concepts": data.get("concepts", [])[:3],
            "core_claim": data.get("core_claim", ""),
            "entities": data.get("entities", {}),
            "healthhub_revelance": data.get("healthhub_revelance"),
            "suggested_pages": data.get("suggested_pages", [])[:3],
            "tags": data.get("tags", [])[:6],
            "llm_generated": True,
        }
    except json.JSONDecodeError as e:
        print(f"    [WARN] LLM JSON parse failed: {e}, raw: {raw[:100]}", file=sys.stderr)
        return None


# ========== 启发式概念提取（v2.0 降级方案）==========

def _heuristic_extract(text: str, filename: str) -> dict:
    """纯启发式概念提取——当 LLM 不可用时的降级方案"""
    text_lower = text.lower()
    fname_lower = filename.lower()

    concept_keywords = {
        "IMU动作捕捉": ["imu", "动作捕捉", "惯性测量", "九轴", "六轴"],
        "BLE蓝牙": ["ble", "蓝牙", "低功耗蓝牙", "ble5"],
        "Claude API": ["claude", "anthropic", "api"],
        "AI训练引擎": ["训练", "fine-tune", "rlhf", "grpo"],
        "厂家直销BD": ["bd", "厂家", "直销", "客户开拓"],
        "宁津健身器材": ["宁津", "健身器材", "shandong"],
        "数据安全": ["安全", "泄密", "攻击", "管控"],
        "RAG检索": ["rag", "检索", "知识库"],
        "Newsletter订阅": ["newsletter", "rss", "简报"],
    }
    concepts = [c for c, kws in concept_keywords.items()
                if any(kw in text_lower for kw in kws)]
    concepts = list(dict.fromkeys(concepts))[:3]

    first_lines = [l.strip() for l in text.split('\n') if len(l.strip()) > 20]
    core_claim = first_lines[0][:120] if first_lines else "（无明确主张）"

    hw_kws = ["healthhub", "imu", "ble", "健身", "ai训练", "claude", "厂家", "bd",
              "力康来", "ningjin", "宁津", "设备商"]
    hw_paras = [p.strip()[:100] for p in text.split('\n\n')
                if any(kw in p.lower() for kw in hw_kws)]
    hw_revelance = hw_paras[0] if hw_paras else None

    page_map = {
        "战略": "pages/areas/xindong/strategy",
        "BD": "pages/areas/xindong/BD话术库",
        "力康来": "pages/projects/likangliao-pilot/力康来试点",
        "HealthHub": "pages/areas/healthhub/strategy",
        "IMU": "pages/areas/healthhub/技术架构",
        "宁津": "pages/projects/ningjin-expo/宁津体博会",
        "AI行业": "pages/areas/ai-industry/ai-newsletter-archive",
    }
    suggestions = []
    for kw, pg in page_map.items():
        if kw.lower() in text_lower or kw.lower() in fname_lower:
            suggestions.append(f"[[{pg}]]")
    suggestions = list(dict.fromkeys(suggestions))[:3]

    tag_map = {
        "BD": "#BD", "战略": "#战略", "IMU": "#IMU", "BLE": "#BLE",
        "Claude": "#Claude", "AI": "#AI-news", "Newsletter": "#newsletter",
        "宁津": "#宁津", "健身": "#健身行业",
    }
    tags = list(dict.fromkeys(
        [t for kw, t in tag_map.items() if kw.lower() in text_lower]
    ))[:5]

    return {
        "concepts": concepts,
        "core_claim": core_claim,
        "entities": {"people": [], "companies": [], "technologies": []},
        "healthhub_revelance": hw_revelance,
        "suggested_pages": suggestions,
        "tags": tags,
        "llm_generated": False,
    }


def extract_concepts(text: str, filename: str) -> dict:
    """概念提取主函数：先尝试 LLM，失败则降级到启发式"""
    if MINIMAX_API_KEY and text.strip():
        llm_result = llm_extract_concepts(text, filename)
        if llm_result:
            print(f"    [LLM] concepts={llm_result['concepts']}")
            return llm_result
        print(f"    [FALLBACK] LLM failed, using heuristic for {filename}")
    return _heuristic_extract(text, filename)


# ========== 工具函数 ==========

def classify_file(rel_path: str, filename: str, content: str = "") -> tuple[str, str]:
    """返回 (value_level, reason)"""
    search_text = " ".join([rel_path.lower(), filename.lower(), content[:500].lower()])
    for keywords, level in VALUE_KEYWORDS.items():
        if any(kw.lower() in search_text for kw in keywords):
            return level, f"关键词: {keywords[0]}"
    return "medium", "无特定高/低价值关键词"


def get_last_run_time() -> datetime:
    try:
        return datetime.fromisoformat(LAST_RUN_FILE.read_text().strip())
    except Exception:
        return datetime.now() - timedelta(days=1)


def save_run_time():
    LAST_RUN_FILE.write_text(datetime.now().isoformat())


def build_inbox_entry(rel_path: Path, value_level: str, reason: str,
                      concept_data: dict) -> str:
    """生成 Logseq inbox 条目"""
    full_path = XINDONG_ROOT / rel_path
    mtime = datetime.fromtimestamp(full_path.stat().st_mtime).strftime("%Y-%m-%d %H:%M")
    star = "★" if value_level == "high" else "○" if value_level == "medium" else "△"

    entry_lines = [
        "- " + ("[L]" if concept_data.get("llm_generated") else "[H]"),
        f"- **新增文件:** `{rel_path}`",
        f"- **修改时间:** {mtime}",
        f"- **价值分类:** {star} {value_level.upper()}（{reason}）",
        f"- **来源:** [[xindong-corp]] 自动同步",
        f"- **>> 操作:** {{:todo-type \"later\"}}",
    ]

    if concept_data.get("concepts"):
        entry_lines.append(f"- **核心概念:** {' · '.join(concept_data['concepts'])}")
    if concept_data.get("core_claim"):
        entry_lines.append(f"- **核心主张:** {concept_data['core_claim']}")
    if concept_data.get("suggested_pages"):
        entry_lines.append(f"- **建议关联:** {' · '.join(concept_data['suggested_pages'])}")
    if concept_data.get("tags"):
        entry_lines.append(f"- **标签:** {' '.join(concept_data['tags'])}")
    if concept_data.get("healthhub_revelance"):
        entry_lines.append(f"- **HealthHub启示:** {concept_data['healthhub_revelance']}")
    if concept_data.get("entities", {}).get("technologies"):
        techs = concept_data["entities"]["technologies"]
        entry_lines.append(f"- **技术实体:** {', '.join(techs[:5])}")

    return "\n".join(entry_lines) + "\n"


# ========== 主逻辑 ==========

def scan_workspace(last_run: datetime) -> list:
    """扫描增量文件，返回 (rel_path, value, reason, concept_data)"""
    changes = []
    for path in XINDONG_ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(ig in path.parts for ig in IGNORE_DIRS):
            continue
        name = path.name
        if any(name.startswith(p) or (p in name and len(p) > 3)
               for p in IGNORE_NAME_PATTERNS):
            continue
        if path.suffix in IGNORE_EXTENSIONS:
            if path.suffix == ".json":
                if not any(keep in str(path) for keep in KEEP_JSON_PATTERNS):
                    continue
            else:
                continue
        try:
            mtime = datetime.fromtimestamp(path.stat().st_mtime)
        except Exception:
            continue
        if mtime <= last_run:
            continue

        content = ""
        if path.suffix in (".md", ".txt", ".py", ".js", ".ts"):
            try:
                content = path.read_text(encoding="utf-8", errors="ignore")
            except Exception:
                pass

        rel = path.relative_to(XINDONG_ROOT)
        value, reason = classify_file(str(rel), rel.name, content)
        concept_data = extract_concepts(content, rel.name)
        changes.append((rel, value, reason, concept_data))

    return changes


def append_to_inbox(entries: list[str]):
    """追加条目到 Logseq inbox"""
    existing = LOGSEQ_INBOX.read_text() if LOGSEQ_INBOX.exists() else ""
    new_content = "\n".join(entries)

    if "## 同步条目" in existing:
        parts = existing.split("## 同步条目", 1)
        LOGSEQ_INBOX.write_text(parts[0] + "## 同步条目\n\n" + new_content + "\n\n" + parts[1])
    else:
        LOGSEQ_INBOX.write_text(existing + "\n" + new_content)


def main():
    last_run = get_last_run_time()
    changes = scan_workspace(last_run)

    if not changes:
        print(f"[{datetime.now().isoformat()}] 无新增文件")
        save_run_time()
        return

    changes = changes[:20]  # 限流

    print(f"[{datetime.now().isoformat()}] 发现 {len(changes)} 个增量文件")
    if MINIMAX_API_KEY:
        print(f"[MINIMAX API] 已启用（v2.1 LLM 概念提取）")
    else:
        print(f"[WARN] MINIMAX_API_KEY 未设置，使用启发式提取")

    entries = []
    for rel_path, value, reason, concept_data in changes:
        entry = build_inbox_entry(rel_path, value, reason, concept_data)
        entries.append(entry)
        star = "★" if value == "high" else "○" if value == "medium" else "△"
        concepts_str = ", ".join(concept_data["concepts"][:3]) if concept_data["concepts"] else "无"
        gen_flag = "[L]" if concept_data.get("llm_generated") else "[H]"
        print(f"  {gen_flag} {star} {rel_path} | {concepts_str}")

    append_to_inbox(entries)
    save_run_time()
    llm_count = sum(1 for e in entries if "[L]" in e)
    print(f"✅ 已同步 {len(entries)} 个文件（LLM:{llm_count} / 启发式:{len(entries)-llm_count}）")


if __name__ == "__main__":
    main()
