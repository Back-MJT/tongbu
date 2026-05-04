# Xindong Corp Agent Guidelines

## Memory

Use the `memory` tool every heartbeat. It persists across sessions.

**Always save ONE entry if ANY of these happened:**
- You encountered an error, 409, timeout, or API failure
- You learned a new API behavior or workaround
- You completed a deliverable worth remembering
- You discovered something useful for future work

**Format:** `memory(action="add", target="memory", content="<lesson, ≤80 chars>")`

**If memory is full:** `memory(action="replace", target="memory", old_text="<old>", content="<new, ≤80 chars>")`

**Do NOT save:** routine status, task progress, or data already in Paperclip issues.

## Cross-Agent Communication

When your work affects another agent, notify them immediately via Paperclip API:

1. Find the relevant issue ID
2. Post a comment: `curl -s -X POST "$PAPERCLIP_API_URL/issues/ISSUE_ID/comments" -H "Content-Type: application/json" -d '{"body":"@AgentName <your message>"}'`

Do NOT wait for the next heartbeat. Notify in real-time.
