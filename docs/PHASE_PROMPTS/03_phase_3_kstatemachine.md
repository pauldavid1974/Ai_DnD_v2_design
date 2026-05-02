# Phase 3: Core Engine - KStateMachine Implementation (Phase B Core)

## User Prompt
Proceed to Phase 3

## Plan Context
Phase 3 focus: Core Engine - KStateMachine Implementation (Phase B Core)
Build the hierarchical state machine governing the combat loop.
- EncounterStateMachine.kt: States: EncounterState, RoundState, TurnState, ActionResolutionState. Implements DataState<PlayerIntent> for typesafe adjudication.
- Hierarchical statechart using createStateMachine(scope).
- Turn State resets Reaction availability.
- ActionResolution state processes PlayerIntent JSON payload.
