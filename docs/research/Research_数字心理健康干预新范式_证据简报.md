# 数字心理健康干预新范式：生成式体验（Generative Experience）
> 昕动智能 · Health Tech Researcher · Standing Task 交付物
> 版本: v1.0 · 2026-04-12
> 用途: 产品/算法团队参考；可能影响干预引擎设计方向

---

## 核心发现

**GUIDE系统**（arXiv:2604.07558, Bhattacharjee et al., Stanford/Georgia Tech, 2026-04-08）是首个提出"生成式体验"范式的数字心理健康干预系统。与传统数字心理健康工具仅个性化"提供什么内容"不同，GUIDE在运行时动态生成"如何提供干预"的体验结构。

---

## 研究概述

| 维度 | 详情 |
|------|------|
| 研究设计 | 预注册RCT（随机对照试验） |
| 样本量 | N = 237 |
| 实验组 | GUIDE（生成式体验系统）|
| 对照组 | LLM-based认知重构（基于大模型的传统个性化）|
| 主要结果 | GUIDE组压力显著降低（p = .02），用户体验显著改善（p = .04）|

---

## 核心创新：生成式体验 vs 传统个性化

| 传统DMH个性化 | 生成式体验（GUIDE）|
|--------------|-------------------|
| 个性化"内容"（说什么）| 个性化"体验结构"（怎么说、何时说、用什么形式）|
| 静态推荐（基于用户画像匹配）| 运行时动态组合（模块化组件生成）|
| 交互格式固定 | 多模态交互结构按需生成 |

GUIDE通过 rubric-guided generation 生成模块化组件，动态编排干预体验的交互结构。这是干预引擎设计中值得关注的新方向。

---

## 产品启示

### 对干预引擎架构的影响

当前干预引擎主要关注"处方内容生成"（运动类型/强度、睡眠时间建议等）。GUIDE提示了一个进阶方向：**体验编排层（Experience Orchestration）**——不仅生成干预内容，还动态决定：

- 推送时机（用户当前状态最合适的时候）
- 交互形式（文字/语音/视觉反馈/游戏化）
- 响应深度（用户情绪状态动态调整对话长度和深度）

### 对压力管理模块的直接影响

- XIN-52（压力管理文献综述）已交付，但GUIDE提供了干预递送层面的新证据
- GUIDE在压力干预场景（p = .02）显著优于LLM控制组
- **关键差异化建议**：昕动干预引擎的压力管理模块应关注"交互体验的实时适配"，而非仅"推送个性化内容"

### BD引用建议

> "数字心理健康工具的下一代范式不是'更个性化的内容'，而是'更智能的体验编排'——在对的时机、以对的形式传递对的支持。这是我们干预引擎的设计方向。"

---

## 技术关键词

Generative Experience, DMH, rubric-guided generation, multimodal interaction structure, intervention experience composition, digital mental health, personalized intervention delivery

---

## 文献信息

```
@article{bhattacharjee2026guide,
  title     = {Generative Experiences for Digital Mental Health Interventions: Evidence from a Randomized Study},
  author    = {Ananya Bhattacharjee, Michael Liut, Matthew Jörke, Diyi Yang, Emma Brunskill},
  year      = {2026},
  eprint    = {2604.07558},
  archivePrefix = {arXiv},
  primaryClass  = {cs.HC},
  url       = {https://arxiv.org/abs/2604.07558}
}
```

---

*昕动智能 Health Tech Researcher · 2026-04-12 · Standing Task产出*
