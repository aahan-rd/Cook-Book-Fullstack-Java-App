/**
 * ViewModel interfaces for CookYourBooks GUI features.
 *
 * <p>Each interface defines the <b>grading contract</b> for one core feature. Implement exactly one
 * of these per team member. The interface specifies:
 *
 * <ul>
 *   <li><b>Observable properties</b> for JavaFX binding in the View
 *   <li><b>Commands</b> (void methods) for user actions
 *   <li><b>Non-JavaFX accessors</b> (plain Java getters) for grading tests
 * </ul>
 *
 * <p>Observable list methods use wildcard types ({@code ObservableList<?>}) — you choose your own
 * entry type (record, domain object, etc.). Grading tests use the non-JavaFX accessors, which
 * return plain Java types like {@code List<String>}.
 */
@NullMarked
package app.cookyourbooks.gui.viewmodel;

import org.jspecify.annotations.NullMarked;
