## 1. Git History (Incremental Progress)

Dark mode was implemented in small, reviewable commits.

- e3c1568: Added the dark mode toggle button in the main layout and wired root pane id for styling.
- 2194d80: Added controller-side dark mode state, toggle handler, and preference persistence.
- 38427c2: Expanded dark mode CSS palette across shared controls.
- 04c9ff0: Ran formatting and cleanup on MainViewController after implementation.
- 2fbb5f6: Updated CI workflow with JavaFX native font dependencies.
- 6749e29: Added additional Linux GUI and font dependencies for JavaFX test runtime.
- 88497c7: Increased CI timeout to prevent cancellation after dependency setup.
- 5cf86bc: Created dedicated dark mode test structure and moved coverage into dark-mode-only tests.
- bfa0b89: Refactored dark mode logic into its own class for separation of concerns.
- a1829d2: Polished toggle placement and CSS cleanup for visual consistency.
- 4f04bbe: Fixed dark mode test reliability issues.

This sequence shows: feature scaffold -> behavior -> styling -> CI stabilization -> test hardening -> refactor.

## 2. PR History and Review

- PR #3: https://github.com/neu-cs3100/sp26-hw-cyb12-group-4616/pull/3
	- Review-driven follow-ups included CI fixes for JavaFX native dependencies and timeout stabilization.
- PR #4: https://github.com/neu-cs3100/sp26-hw-cyb12-group-4616/pull/4
	- Continued refinement and integration cleanup on the dark mode branch.
- PR #5: https://github.com/neu-cs3100/sp26-hw-cyb12-group-4616/pull/5
	- Finalized dark mode test fixes and merge readiness.

Meaningful review outcomes captured in commit history:
- Environment-specific CI failures were addressed with explicit Linux package installs.
- Long-running CI setup was addressed by increasing timeout from 5 to 12 minutes.
- Dark mode tests were split into a dedicated file and adjusted for preference persistence behavior.
- Dark mode logic was moved out of MainViewController into its own class for clearer ownership.

## 3. Decision Log

### Decision: Persist dark mode using Java Preferences and manage feature logic in a dedicated DarkMode class

Context:
The feature needed to support theme toggle, survive app restart, and remain explainable and maintainable for TA review.

Option A: Session-only state in MainViewController
- Pros: simplest implementation.
- Cons: theme resets on restart; controller grows in responsibility.

Option B: Persisted state in MainViewController using Preferences
- Pros: restart persistence with low complexity.
- Cons: controller still mixes navigation and theming concerns.

Option C (chosen): Persisted state plus dedicated DarkMode class
- Pros: restart persistence, cleaner separation of concerns, easier testing and explanation.
- Cons: one extra class and small wiring overhead.

Rationale:
Option C balanced user experience and code quality best. Persistence was required for usability, and extracting logic from MainViewController reduced coupling and made dark mode behavior easier to reason about and test.

## 4. Testing and Quality

### Unit tests for the feature
- Added a dedicated dark-mode GUI test file: [src/test/java/app/cookyourbooks/gui/DarkModeGuiTest.java](src/test/java/app/cookyourbooks/gui/DarkModeGuiTest.java).
- Tests cover default light-mode startup, toggling dark mode on, toggling back off, and persistence writes via Java Preferences.
- General GUI navigation tests remain in [src/test/java/app/cookyourbooks/gui/GuiEndToEndExampleTest.java](src/test/java/app/cookyourbooks/gui/GuiEndToEndExampleTest.java) to keep dark mode verification isolated.

### Accessibility check
- Dark mode toggle supports keyboard navigation because it is a standard JavaFX Button.
- It can be focused with Tab and activated with Enter or Space without custom key handlers.
- Sidebar navigation remains keyboard-friendly through standard JavaFX controls.

### Known limitations
- Persistence is tied to local Java Preferences storage for the current user profile/environment.
- Some feature-specific custom-styled controls may need additional dark-mode polishing.
- No automated contrast or screen-reader audit is currently included; accessibility verification is currently manual for keyboard flow.
