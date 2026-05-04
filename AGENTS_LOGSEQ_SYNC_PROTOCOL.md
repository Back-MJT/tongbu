# Xindong Corp × 第二大脑 同步协议

> **版本：** v1.0
> **日期：** 2026-04-24
> **目标：** Xindong_Corp 7个Agent的内容持续流入第二大脑，不遗漏、不堆积

---

## 核心原则

**每个 Agent 完成重要工作后，必须向 Logseq 写一条 summary。**

这不是可选项，是工作完成的最后一步。

---

## Agent 写回格式

完成任何 `done` 级别的任务后，在 Logseq inbox 写入：

```markdown
-
- **任务:** [XIN-XX] 任务名称
- **Agent:** [你的Agent角色]
- **完成时间:** [[YYYY-MM-DD]]
- **产出文件:** `path/to/file.md`（Xindong_Corp内的路径）
- **一句话结论:** 完成了什么，有哪些关键结论
- **价值分类:** ★高 / ○中 / △低
- **待跟进:** [谁需要做什么]
- **>> 处理:** {{:todo-type "later"}}
```

**写入路径：** `pages/inbox/inbox.md`

---

## 各 Agent 的写回触发规则

| Agent | 触发时机 | 写回内容重点 |
|-------|---------|------------|
| **Founding Engineer** | PR合并 / 测试通过 / 架构文档完成 | 技术决策、API变更、代码里程碑 |
| **BD Manager** | 客户接触 / 演示完成 / MOU签署 | 客户反馈、BD进展、竞品信号 |
| **Content Creator** | 内容发布 / 文章完成 | 产出标题、核心观点、目标受众 |
| **Researcher** | 研究报告完成 / 文献扫描完成 | 关键发现、数据来源、可信度 |
| **PM** | PRD更新 / 里程碑完成 | 产品决策、需求变更、路线图调整 |
| **Algorithm Engineer** | 引擎模块完成 / 提示词更新 | 技术方案、参数变更、效果数据 |
| **CEO** | Board决议 / 战略决策 / 合同签署 | 决策记录、风险更新、团队调整 |

---

## 每日 CEO 审视流程（约5分钟）

1. 打开 `pages/inbox/inbox.md`
2. 扫一遍今天的新条目
3. 每条快速决定：
   - **★高价值** → 直接创建/更新对应 `areas/xindong/` 或 `projects/` 页面
   - **○中价值** → 追加到对应 area 页面底部作为补充内容
   - **△低价值** → 保留但标记为 archive，未来有需要时再处理
4. 处理完后删除已整合的条目（保持 inbox 干净）

---

## 每周 Brain Maintenance（约30分钟，周一早上）

1. **批量归档** — inbox 中积累的中低价值内容批量处理
2. **更新战略/项目页面** — Board 新决策、BD 新进展、技术新突破
3. **补充双链** — 新内容有没有关联到现有知识网络
4. **检查过时引用** — 废弃方案、过期状态及时更新

---

## 自动化同步（cron job，补充人工）

- **脚本：** `scripts/logseq_sync.py`
- **频率：** 每天 07:00 北京时间
- **功能：** 自动扫描 Xindong_Corp 新增/修改的文件，生成 Logseq inbox 条目
- **覆盖：** 所有 Agent 的工作文件（代码/文档/报告）
- **局限：** 只做文件发现，分类和整合仍需 CEO 人工判断

---

## 禁止事项

- ❌ 不要把所有文件都标记为"高价值"——失去筛选意义
- ❌ 不要让 inbox 堆积超过 20 条——超过就批量处理
- ❌ 不要删除历史归档——只移走，不删除（Logseq 的 page 历史可以追溯）

---

## 关键页面索引

| 页面 | 路径 | 用途 |
|------|------|------|
| 第二大脑入口 | `pages/xindong-corp.md` | 整体导航 |
| inbox 收集箱 | `pages/inbox/inbox.md` | 新内容入口 |
| 战略主页 | `pages/areas/xindong/strategy.md` | 战略决策主页面 |
| 力康来试点 | `pages/projects/likangliao-pilot/pilot.md` | P0项目追踪 |
| Phase1执行 | `pages/projects/phase1/phase1.md` | 当前工作追踪 |
