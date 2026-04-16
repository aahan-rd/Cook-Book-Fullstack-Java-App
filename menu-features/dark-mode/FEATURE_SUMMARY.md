## Feature Summary

![alt text](<final library2.png>)
![alt text](<design/final search.png>)
![alt text](<Screenshot 2026-04-16 at 3.36.09 PM.png>)

### Integration Notes
Dark mode integrates with the rest of the app through a single CSS class (dark-mode) applied to the root BorderPane in MainView.fxml. Because all feature views (Library, Recipe Editor, Import, Search) stem from this root pane, the dark mode styles cascade automatically to every feature without any feature-specific code changes.
The theme toggle button is in the sidebar managed by MainViewController. The DarkMode class handles state management, applies the CSS class, updates the button label/icon, and keeps the user's preference across sessions using Java's Preferences API so the chosen theme is restored on next run.
The only feature-specific integration point is the Recipe Editor, which required removing hardcoded inline style= color values from RecipeEditorView.fxml (such as white backgrounds on scroll panes and hardcoded #444 text fills) so that the CSS dark mode rules could take effect properly.

### Status
Complete:

Toggle button in sidebar with sun/moon icons
Dark mode CSS applied to all views — sidebar, content area, Library, Recipe Editor, Import, Search
Persistent preference saved and restored across app launches
Context menu (Library dropdown) styled in dark mode
Ingredient list cell buttons styled correctly in dark mode
All three DarkModeGuiTest tests passing

Known issues:

Some inline-styled elements in other features (Import, Library) may not fully respect dark mode if teammates have hardcoded colors in their FXML -> scroll bar is grey

