# Document Governance Policy
**Effective:** 2026-04-22 | **Owner:** CEO | **Review:** On-demand

---

## 1. Document Layers

Every document must live in exactly one layer. No exceptions.

### Layer A — Company Reference (`/Xindong_Corp/`)
**What:** Top-level strategic and product documents that define what we are building.
**Criteria:** Board-relevant, cross-functional, version-controlled by whole company.
**Examples:**
- `ROADMAP.md` — phased product plan
- `PRD_MVP_v1.0.md` — current product requirements
- `PROJECT_BACKGROUND.md` — company context and goals
- `BUSINESS_MODEL.md` — revenue model
- `TECHNICAL_ARCHITECTURE.md` — system design
- `B2B_BACKEND_MVP_SPEC.md` / `B2B_DASHBOARD_MVP_SPEC.md` — B2B specs
- `USER_STORIES_MVP.md` — user journeys
- `PHASE2_PLAN.md` — next phase

**Rule:** Documents in Layer A may be referenced by any agent. Changes require PM→CEO approval.

---

### Layer B — Shared Library (`/Xindong_Corp/docs/`)
**What:** Operational documents produced by agents that are company assets.
**Criteria:** Cross-agent reference material, external-facing deliverables, contracts, technical specs.
**Subfolders:**
- `docs/contracts/` — all partnership/MOU contracts
- `docs/board/` — board feedback and resolutions
- `docs/tech/` — technical specifications and architecture (FE + algorithm deliverables)
- `docs/bd/` — sales materials, pilot plans, customer research, BD deliverables
- `docs/research/` — research reports, evidence briefs (from researcher + research_deliverables/)
- `docs/content/` — content team deliverables (articles, SEO, BP materials)
- `docs/daily_notes/` — daily standup/operation notes (per-agent subdirs)
- `docs/fe/` — founding engineer deliverables (tech docs from intervention-engine, etc.)

**Examples already here:**
- `三方技术开发合作框架协议_正式版.md` → `docs/contracts/`
- `github_sync_strategy.md` → `docs/tech/`
- `Board反馈回应_v1.1修订.md` → `docs/board/`
- `sales_one_pager.md` (from agents/bd/) → `docs/bd/`

**Rule:** PM owns Layer B taxonomy. BD owns `docs/bd/`, FE owns `docs/tech/`, etc.

---

### Layer C — Agent Homes (`/Xindong_Corp/agents/<role>/`)
**What:** Agent self-management files only.
**Allowed files:**
- `AGENTS.md` — agent definition
- `SOUL.md` — persona and voice
- `HEARTBEAT.md` — operational checklist
- `TOOLS.md` — available tools
- `*.md` task work files (tmp_ prefix, auto-cleaned)

**NOT allowed:** Deliverables. A deliverable produced by an agent belongs in Layer A or B.

**Rule:** Agent folders are for agent infrastructure. Deliverables go to docs/.

---

### Layer D — Temporary Work Files (`/Xindong_Corp/agents/<role>/tmp_*`)
**What:** Per-task working files used during issue execution.
**Naming:** `tmp_<issue-id>_<descriptor>.<ext>` (e.g., `tmp_xin130_plan.md`)
**Lifecycle:** Delete within 24h of issue close. Archive if historically valuable.

---

## 2. Naming Conventions

| Document Type | Convention | Example |
|---------------|-----------|---------|
| Company spec | `UPPER_SNAKE_CASE.md` | `MVP_TECHNICAL_ARCHITECTURE.md` |
| Contract | `<counterparty>_<type>_<version>.md` | `力康来_技术开发合作框架协议_v1.md` |
| Board document | `Board_<topic>_<date>.md` | `Board反馈回应_v1.1.md` |
| Agent deliverable | `<short-topic>.md` | `github_sync_strategy.md` |
| Work log | `tmp_<issue>_<descriptor>.<ext>` | `tmp_xin130_plan.md` |
| Issue completion | `<issue-id>_done.md` | `xin100_done.md` |

**Rules:**
- No spaces in filenames
- Use `_` not `-` as primary separator (reserve `-` for version-like suffixes)
- Include issue ID in deliverable filename when it originates from an issue
- Chinese acceptable for contracts and domestic-facing docs; English for technical specs

---

## 3. Document Lifecycle

```
Draft → Review → Published → Archived
```

- **Draft:** Agent working copy (Layer D or agent folder)
- **Review:** Shared with stakeholders via Paperclip comment or meeting
- **Published:** Moved to appropriate Layer A or B location
- **Archived:** Moved to `docs/archive/` with date prefix (`archive/2026-04_xxx.md`)

**Key rule:** A document is "done" when it is in its permanent home (Layer A or B). Not before.

---

## 4. QA Checklist (Before Publishing)

Before moving a document to Layer A or B, confirm:

- [ ] File name follows naming conventions
- [ ] No placeholder text (`TODO`, `FILL IN`, `XXX`)
- [ ] Version number updated if applicable
- [ ] Front-matter includes: title, date, author (agent name), status
- [ ] Cross-references to other docs use relative paths
- [ ] No secrets, credentials, or private client data
- [ ] Linked to relevant Paperclip issue (if applicable)

---

## 5. Immediate Actions (XIN-130)

| # | Action | Owner | Status |
|---|--------|-------|--------|
| 1 | Move PM task completions (xin100_done, xin104_done, xin104_final_comment) → `docs/` | PM | Done |
| 2 | Move BD docs to `docs/bd/` (sales_one_pager, pilot_onboarding_plan, etc.) | BD | Done |
| 3 | Create `docs/contracts/` and move all contract files there | PM | Done |
| 4 | Create `docs/board/` and move board feedback docs there | PM | Done |
| 5 | Create `docs/tech/` and move tech specs (nRF52840, ble-scanner, etc.) there | FE | Done |
| 6 | Delete tmp_* files older than 7 days in root `/Xindong_Corp/` | CEO | Done |
| 7 | Delete tmp_* files in agent folders older than 7 days | Each agent | Done |
| 8 | Create `docs/research/` and move researcher substantive reports there | Researcher | Done |
| 9 | Confirm all Layer A docs have proper names (upper snake case) | PM | Pending |

---

## 6. Enforcement

- CEO reviews document organization on each heartbeat (HEARTBEAT.md checklist item)
- Agents are expected to maintain these rules without being reminded
- New agents: AGENTS.md template includes reference to this policy

---

*Document Control: XIN-130 — CEO — 2026-04-22 (updated 2026-04-22 comment)*
