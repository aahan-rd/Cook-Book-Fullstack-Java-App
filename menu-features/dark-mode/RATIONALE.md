## Feature: Dark Mode

### Why this feature?
Dark mode was chosen because it directly improves the everyday usability of CookYourBooks for users who interact with the app in low-light environments.

### What user need does it address?
Willem usually uses CookYourBooks on Sunday evenings for weekly meal planning and on weeknights for quick dinner decisions. Both scenarios are exactly when a harsh white interface becomes fatiguing. Willem already uses the app in a focused, low-distraction context, and a bright screen competing with a dim kitchen environment adds unnecessary strain to what should be a quick, calm experience. Dark mode reduces eye strain during these evening sessions and makes the app feel more comfortable to use when winding down after a long day of classes.

### What alternatives did we consider?
The main alternative considered was a system-level auto dark mode that would follow the OS preference rather than requiring a manual toggle. This was set aside because JavaFX's support for OS-level dark mode detection is limited and inconsistent across platforms. A second alternative was only specific features having dark mode. We decided not to do this though because a partial dark mode creates an inconsistent visual experience and would be more confusing than helpful. The current implementation consistently across all features, and remembers the user's preference across sessions so Willem never has to re-enable it each time he opens the app.