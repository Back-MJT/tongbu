# GitHub 定时推送与拉取策略

**文档版本**: v1.0
**制定人**: CEO
**生效日期**: 2026-04-20
**关联**: XIN-129

---

## 背景

XIN-127 已建立 GitHub 仓库 (zdjyn/Xindong_Platform)，首次推送包含 258 个文件。
当前状态：本地仓库与 origin/main 同步，无未同步内容。

本策略旨在明确何时、由谁、将什么内容推送到 GitHub，以及何时拉取。

---

## 核心原则

1. **GitHub 是唯一真相源** — 所有代码、文档、配置最终都落在 GitHub
2. **频繁推送，小步提交** — 避免大量未同步的本地修改堆积
3. **拉取优先，推送在后** — 每次推送前先拉取，避免覆盖他人工作
4. **文档与代码同等重要** — docs/ 目录与代码同步更新

---

## 推送策略

### 触发条件（满足任一即推送）

| 优先级 | 触发条件 | 说明 |
|--------|---------|------|
| P0 | 单个任务完成（issue closed） | issue 对应的代码、文档、测试全部推送 |
| P1 | 每日结束前（~18:00 UTC） | 当天工作的代码汇总推送 |
| P2 | 阶段性里程碑 | Sprint 完成、Phase 切换、重大功能发布 |
| P3 | 重要文档更新 | 合同、协议、PRD 等文档修订 |

### 提交规范

```
<type>(<scope>): <简短描述>

type: feat | fix | docs | refactor | test | chore
scope: fe | be | bd | cc | ae | docs | infra
示例:
  docs(bd): 更新力康来合同 v2.1
  feat(fe): 小程序 BLE 连接优化
  fix(ae): 运动处方算法边界条件修复
```

### 分支策略

- **main** — 稳定分支，所有 Agent 推送至此
- **不推荐** 使用 feature branches — MVP 阶段保持简单，优先速度
- 如需并行开发，由对应 Agent 创建短期分支，完成后合并到 main

---

## 拉取策略

### 触发条件

| 优先级 | 触发条件 | 说明 |
|--------|---------|------|
| P0 | Agent 启动时 | 每次唤醒/启动必须先拉取 |
| P1 | 开始处理新 issue 前 | 确保拿到最新代码 |
| P2 | 推送冲突时 | 先拉取解决冲突再推送 |

---

## 冲突处理

1. **自动合并**（无冲突）：直接推送
2. **手动冲突**：拉取后，Agent 自行判断保留内容，优先保留：
   - 更多功能的版本
   - 更完整的测试覆盖
   - 更新的文档
3. **重大冲突**（涉及多人同时修改同一文件）：通知 CEO，由 Board 决策

---

## 推送内容过滤

**禁止推送**：
- `.env`（包含密钥）、`node_modules/`、`.venv/`
- 编译产物 `dist/`、`build/`、`__pycache__/`
- IDE 配置 `.vscode/`、`*.swp`
- 大于 10MB 的二进制文件

**.gitignore** 已覆盖上述内容，确保不会误推送。

**必须推送**：
- 所有源代码（Python、Java、JavaScript/TypeScript）
- 文档（docs/、specs/）
- 配置文件（docker-compose.yml、Dockerfile、nginx.conf）
- 脚手架代码（RuoYi 若依相关）

---

## 各 Agent 职责

| Agent | 负责推送内容 |
|-------|------------|
| Founding Engineer | intervention-engine、b2b-frontend、device-gateway、mini-program |
| Algorithm Engineer | 算法相关代码（如有独立仓库） |
| BD Manager | 演示材料、合同文档（docs/ 下） |
| Content Creator | 内容资产（图片、文案，若在 docs/ 下） |
| PM | PRD、 roadmap、计划文档 |
| CEO | 策略文档、跨 Agent 协调文档 |

---

## 实施方式

### Agent 配置固化（FE + AE 必须配置）

每个 Agent 的 `HEARTBEAT.md` 必须包含以下心跳检查项：

**开始工作时（所有 Agent）：**
```bash
cd $WORKSPACE && git pull origin main
```

**结束时（FE + AE）：**
```bash
cd $WORKSPACE && git status
# 若有未提交源码 → git add . && git commit -m "..." && git push origin main
```

禁止提交：`target/`、`node_modules/`、`__pycache__/`、`*.class`、`.venv/`

### 自动推送（推荐）

在 `agents/<role>/` 下创建 `git_sync.sh` 脚本，结合 cron 或 Agent 启动钩子：

```bash
#!/bin/bash
# git_sync.sh — 在 AGENT_HOME 下创建
cd /media/ai-no1/workspace/Xindong_Corp
git add -A
git diff --cached --quiet || {
    git commit -m "$(date -u +%Y-%m-%dT%H:%M:%SZ) sync"
    git push origin main
}
```

### 手动推送

```bash
cd /media/ai-no1/workspace/Xindong_Corp
git pull origin main
git add <changed_files>
git commit -m "<commit message>"
git push origin main
```

---

## 验证机制

- 每次推送后验证 GitHub Actions（若有）是否通过
- 每月一次 Board 审查 GitHub 提交日志，检查遗漏
- 发现未同步的重大 work in progress（WIP）超过 24 小时，触发提醒

---

## 当前状态

| 项目 | 状态 |
|------|------|
| GitHub 仓库 | https://github.com/zdjyn/Xindong_Platform |
| main 分支 | ✅ 与本地同步 |
| SSH Deploy Key | ✅ 已配置 (~/.ssh/id_ed25519) |
| 首次推送 | ✅ 7c1b49f (258 files) |

---

*CEO 制定 — 2026-04-20*
