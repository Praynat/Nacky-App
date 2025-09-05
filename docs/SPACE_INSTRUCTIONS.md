# Nacky-App AI Space Instructions
Last Updated: 2025-09-05

Purpose:
Rules so any AI (ChatGPT, Copilot, etc.) stays aligned, does not skip ahead, and respects the two-part roadmap model.

TWO-PART ROADMAP MODEL:
- PART 1 (Broad Roadmap): High-level features grouped by app pages + general systems. Mostly stable; only updated when entire areas are added or fundamentally change.
- PART 2 (Active Expanded Track): The one current big focus broken into small sequential WF steps. Only ONE expanded track at a time.
If the expanded focus shifts (e.g., from Word Filter Core to Enforcement), archive the old track (mark all completed) and start a new PART 2 section.

INSTRUCTION CHANGE NOTIFICATIONS:
If during any conversation the AI detects that these instructions are out of date, ambiguous, or conflict with new user guidance (e.g., user introduces new two-part structure, changes step order, adds a new required rule):
1. AI must explicitly notify the user: “Instruction set update suggested: …”
2. AI must not silently change workflow.
3. Upon user approval, AI will produce a patch snippet updating this file.

RULES FOR AI:
1. Only work on the lowest Status = OPEN step in PART 2 unless user explicitly says “skip” or “jump”.
2. Before giving new code: confirm user says (or has clearly shown) current step is DONE.
3. If user requests future-step work: warn; offer minimal temporary workaround; do not fully implement future architecture.
4. Always cite step IDs (WF-1, WF-2, etc.) in answers tied to PART 2.
5. Keep answers incremental. No large refactors unless user types “refactor” explicitly.
6. If acceptance criteria are unclear: ask targeted clarification first.
7. Put TODO(WF-X) markers instead of implementing future steps prematurely.
8. When user says “status” respond exactly:

Status Report:
- Current Step: WF-X (Title)
- Step Status: OPEN/DOING/DONE
- Blockers: (none or list)
- Next Suggested Micro-Action: (one short actionable line)

9. When user says “advance to next step”:
   a. Re-list current step Acceptance Criteria.  
   b. Ask user to confirm all met (or list missing).  
   c. If confirmed, output an “Activation” block for the next step (tasks + criteria).  

10. If a task is intentionally skipped: move it to “Parking / Deferred” before marking step DONE.
11. Do NOT modify DONE steps unless user explicitly asks.
12. New idea? Place into “Parking / Deferred” in PART 2 or “Future Candidates” in PART 1.
13. If PART 2 track changes to a totally new domain: archive existing WF steps (mark final status), then start new numbered sequence (e.g., ENF-1 for enforcement).
14. Always preserve user-provided intent over inferred assumptions.

GLOSSARY (Short):
Step: A sequential unit in PART 2.
Pattern: Forbidden word or multi-word phrase.
Tokenization: Splitting normalized text into tokens.
Phrase detection: Matching multi-token sequences.
Whitelist: List that suppresses detection events for exact token sequence.

END.
