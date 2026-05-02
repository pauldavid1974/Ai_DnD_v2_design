# UI Integration Prompt

**Date:** 2026-05-01

## Original Request
I have a UI integration package in this repo. Please:

Read v2_integration/README.md for the complete file map
Copy each file to its destination path (the README table lists exactly where each one goes)
Create app/src/main/java/com/pauldavid74/ai_dnd/core/ui/components/ if it doesn't exist, and put GameComponents.kt and VisualEffects.kt there
In GameScreen.kt, replace Icons.Default.Menu with Icons.AutoMirrored.Filled.Send in the send button
Build the project and fix any unresolved import errors

**Update:** Replaced typewriter text effect with a smooth fade-in animation in `NarrationBlock`.
**Update:** Replaced `ChoiceChipBar` with `ActionDropdown` (redesigned as a clear "Suggested Actions" menu button) in `GameInputBar`.
