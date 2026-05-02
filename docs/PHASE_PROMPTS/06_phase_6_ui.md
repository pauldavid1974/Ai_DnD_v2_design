# Phase 6: UI Layer (Jetpack Compose)

## User Prompt
Proceed to Phase 6

## Plan Context
Phase 6 focus: UI Integration
Bind the state machine to Jetpack Compose via Hilt ViewModels.
- GameViewModel.kt: Expose StateFlow<IState> and SharedFlow for UI events. Observe current state using collectAsStateWithLifecycle().
- Render combat interface, hit points, and dialog overlays in real-time.
- Handle "Undo" button to dispatch RollbackEvent.
- Integrated Dagger Hilt @HiltViewModel.
