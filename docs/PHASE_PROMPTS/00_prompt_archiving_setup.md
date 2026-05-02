Execute the following file management and documentation updates to establish a prompt version control system:

1. Create a new directory named PHASE_PROMPTS inside the existing /docs directory.
2. Inside /docs/PHASE_PROMPTS/, create a new file named 00_prompt_archiving_setup.md. Copy the exact text of this current prompt and save it inside that new file.
3. Open AGENT_DIRECTIVES.md in the root directory. Under the "6. Execution Protocol" section, append the following bullet point:
   * Prompt Archiving: Whenever the user provides a major multi-step prompt (such as initiating a new Phase), you must first save the raw text of their prompt into a new markdown file within /docs/PHASE_PROMPTS/ using a sequential naming convention (e.g., 01_phase_1_foundation.md). This ensures all architectural instructions are version-controlled alongside the code.

Confirm when the directory is created, the setup file is saved, and the directives are updated.
