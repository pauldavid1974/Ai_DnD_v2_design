# Git & Repository Management Protocol

**CRITICAL DIRECTIVE:** As an AI agent operating in this workspace, you are strictly bound by this Git protocol. You do not have permission to push untested code, make monolithic commits, or push directly to the `main` branch. 

## 1. Branching Strategy (Feature Isolation)
All work must be isolated to targeted feature branches. You must never commit directly to `main` or `master`.
* **Format:** `<type>/<phase-number>-<short-description>`
* **Types:** `feat` (new features), `fix` (bug fixes), `chore` (maintenance/setup), `refactor` (restructuring).
* **Example:** `feat/phase-1-room-schema` or `fix/phase-2-dice-math`.

## 2. Atomic Micro-Commits (Zero-Tolerance for Monster Commits)
You must commit your work incrementally. A single commit should do exactly ONE thing. 
* Do NOT build the UI, the ViewModel, and the Database in a single commit.
* **Rule of Thumb:** If your commit message requires the word "and", the commit is too large. Break it into two commits.
* **Gate:** Before committing, the code MUST compile. Do not commit broken build states.

## 3. Conventional Commit Formatting
All commit messages must follow the Conventional Commits specification to ensure a clean, machine-readable history.
* `feat: add CharacterEntity room schema`
* `fix: correct index out of bounds in dice parser`
* `docs: update architecture blueprint`
* `test: add unit tests for combat modifier math`

## 4. The "Test Before Commit" Mandate
Before staging files for a commit in the `core:engine` or data layers, you must run the relevant JUnit tests. If the tests fail, you must fix the logic before executing the `git commit` command.

## 5. End of Session Protocol
When completing a task or pausing work, ensure the branch is pushed to origin to maintain remote backups: `git push -u origin <branch-name>`.