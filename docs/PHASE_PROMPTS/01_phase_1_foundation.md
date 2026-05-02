Execute Phase 1: Foundation & Data Layer.

1. Update the app and project-level build.gradle.kts files to include the necessary dependencies for Dagger Hilt, Room Persistence Library, kotlinx.serialization, KStateMachine, and Ktor Client.
2. Create the core:database package structure.
3. Define the Room Database schema, including:
   - CharacterEntity for player stats and HP.
   - MemoryEntity for the Chronicler's narrative updates.
   - SrdReferenceEntity to house machine-readable D&D rules.
4. Set up Dagger Hilt modules to provide the Database and local Repository instances.

Ensure CharacterEntity only stores raw integers for stats (1-30). Modifiers must be implemented as computed getters as specified in wiki/srd_character_creation.md. Keep your generated files modular and atomic. Stop and confirm when the project compiles and the Room schema is verified.