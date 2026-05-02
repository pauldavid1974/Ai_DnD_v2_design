# Alignment Audit & Missing Components Implementation Prompt

**Date:** 2026-04-29
**Agent:** Android Studio Agent

## Raw User Request
"Compare the current state of the project with the projects alignment documents. then find the things that are missing. make a list of these things and then create a plan to include the missing components."
"thank you. next. i want you to take one more really good look around. see if you missed anything else."
"okay, good work. this all needs to be done. but i do not want to neglect the ui. Please take one more look at that and insure everything that is discussed in the prd is in the app."
"next, i want you to align yourself strictly to the github workflow document. your plan should be done in phases. be sure you comply strictly with the github workflow markdown file"

## Summary of Planned Actions
1. **Audit:** Identified gaps in NSAI Boundary, Two-Call Cycle, SSE Streaming, DAG Patching, JIT Context, Wiki Lookups, and UI Kinetics.
2. **Strategy:** Restructured the implementation into 5 isolated feature branches matching `GITHUB_WORKFLOW.md` naming conventions.
3. **Phases:**
    - Branch 1: Core Engine Adjudication (`The Bones`)
    - Branch 2: AI Streaming & JIT Context (`The Brain`)
    - Branch 3: Campaign Integrity (`DAG Patching`)
    - Branch 4: UI Kinetics & Aesthetic Law (`The Feel`)
    - Branch 5: Living World & Security (`Fronts`)
4. **Constraints:** Enforced atomic micro-commits, conventional commits, and a mandatory "Test Before Commit" gate for each branch.
