# Phase 5: Reactions and Interruption Handling

## User Prompt
Proceed to Phase 5

## Plan Context
Phase 5 focus: Reactions and Interruption Handling
Handle the asynchronous interrupt loop for SRD 5.2.1 reaction mechanics.
- ReactionHandler: SharedFlow event bus for broadcasting triggers.
- Suspension logic: Utilizing KStateMachine's ability to suspend transitions and wait for external input events.
- ReactionValidator: Check leveled_spell_cast_this_turn flag and filter reaction abilities that consume a spell slot.
- InterruptEvent/ReactionResolvedEvent: manage the complex, suspendable combat flows.
