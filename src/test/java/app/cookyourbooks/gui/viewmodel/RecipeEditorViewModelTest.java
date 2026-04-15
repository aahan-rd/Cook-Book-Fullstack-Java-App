package app.cookyourbooks.gui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import app.cookyourbooks.gui.RecipeFixtures;
import app.cookyourbooks.gui.ViewModelTestBase;
import app.cookyourbooks.gui.editor.RecipeEditorViewModelImpl;
import app.cookyourbooks.model.Recipe;
import app.cookyourbooks.repository.RecipeRepository;
import app.cookyourbooks.repository.RepositoryException;

/**
 * Tests for RecipeEditorViewModelImpl covering requirements E1–E10.
 *
 * <p>All tests written against the {@link RecipeEditorViewModel} interface — not the
 * implementation. This ensures the tests run correctly against the course staff's reference
 * implementation during mutation testing.
 *
 * <p>After the submission deadline, map each test method to its requirement in the Pawtograder test
 * mapping window.
 */
@SuppressWarnings("NullAway.Init")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeEditorViewModelTest extends ViewModelTestBase {

  @Mock private RecipeRepository mockRepo;

  private RecipeEditorViewModel vm;

  private Recipe testRecipe;

  @BeforeEach
  void setUp() {
    vm = new RecipeEditorViewModelImpl(mockRepo);

    testRecipe =
        RecipeFixtures.makeRecipeWithIngredients(
            "r1",
            "Pancakes",
            RecipeFixtures.measuredCups("flour", 2),
            RecipeFixtures.vague("salt", "to taste"));
    when(mockRepo.findById("r1")).thenReturn(Optional.of(testRecipe));
  }

  // ── E1: loadRecipe populates state ────────────────────────────────────

  @Test
  void e1_loadRecipe_populatesTitle() {
    vm.loadRecipe("r1");
    assertEquals("Pancakes", vm.getTitle());
  }

  @Test
  void e1_loadRecipe_populatesIngredients() {
    vm.loadRecipe("r1");
    assertEquals(2, vm.getIngredientCount());
    assertTrue(vm.getIngredientNames().contains("flour"));
    assertTrue(vm.getIngredientNames().contains("salt"));
  }

  @Test
  void e1_loadRecipe_storesRecipeId() {
    vm.loadRecipe("r1");
    assertEquals("r1", vm.getRecipeId());
  }

  @Test
  void e1_loadRecipe_clearsDirtyAndEditMode() {
    vm.loadRecipe("r1");
    assertFalse(vm.isDirty());
    assertFalse(vm.isEditing());
  }

  @Test
  void e1_loadRecipe_unknownId_isNoOp() {
    when(mockRepo.findById("unknown")).thenReturn(Optional.empty());
    vm.loadRecipe("unknown");
    assertNull(vm.getRecipeId());
  }

  // ── E2: toggleEditMode ────────────────────────────────────────────────

  @Test
  void e2_toggleEditMode_entersEditMode() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    assertTrue(vm.isEditing());
  }

  @Test
  void e2_toggleEditMode_whenInEditMode_exits() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.toggleEditMode();
    assertFalse(vm.isEditing());
  }

  @Test
  void e2_toggleEditMode_whenExiting_clearsDirty() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Changed");
    assertTrue(vm.isDirty());
    vm.toggleEditMode(); // exit should discard
    assertFalse(vm.isDirty());
  }

  // ── E3: dirty tracking ────────────────────────────────────────────────

  @Test
  void e3_changingTitle_setsDirty() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("New Title");
    assertTrue(vm.isDirty());
  }

  @Test
  void e3_addingIngredient_setsDirty() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.addIngredient();
    assertTrue(vm.isDirty());
  }

  @Test
  void e3_removingIngredient_setsDirty() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.removeIngredient(0);
    assertTrue(vm.isDirty());
  }

  @Test
  void e3_loadRecipe_doesNotSetDirty() {
    // Loading a recipe must NOT trigger dirty even though it
    // programmatically changes title and ingredient properties
    vm.loadRecipe("r1");
    assertFalse(vm.isDirty());
  }

  // ── E4: discardChanges ────────────────────────────────────────────────

  @Test
  void e4_discardChanges_revertsTitle() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Changed Title");
    vm.discardChanges();
    assertEquals("Pancakes", vm.getTitle());
  }

  @Test
  void e4_discardChanges_revertsIngredientCount() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.addIngredient();
    assertEquals(3, vm.getIngredientCount());
    vm.discardChanges();
    assertEquals(2, vm.getIngredientCount());
  }

  @Test
  void e4_discardChanges_clearsDirty() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Changed");
    vm.discardChanges();
    assertFalse(vm.isDirty());
  }

  @Test
  void e4_discardChanges_exitsEditMode() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.discardChanges();
    assertFalse(vm.isEditing());
  }

  // ── E5: isValid ───────────────────────────────────────────────────────

  @Test
  void e5_blankTitle_isInvalid() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("  ");
    assertFalse(vm.isValid());
  }

  @Test
  void e5_emptyTitle_isInvalid() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("");
    assertFalse(vm.isValid());
  }

  @Test
  void e5_nonBlankTitle_isValid() {
    vm.loadRecipe("r1");
    assertTrue(vm.isValid());
  }

  // ── E6: addIngredient / removeIngredient ──────────────────────────────

  @Test
  void e6_addIngredient_appendsToList() {
    vm.loadRecipe("r1");
    int before = vm.getIngredientCount();
    vm.addIngredient();
    assertEquals(before + 1, vm.getIngredientCount());
  }

  @Test
  void e6_removeIngredient_removesFromList() {
    vm.loadRecipe("r1");
    int before = vm.getIngredientCount();
    vm.removeIngredient(0);
    assertEquals(before - 1, vm.getIngredientCount());
  }

  @Test
  void e6_removeIngredient_outOfBounds_isNoOp() {
    vm.loadRecipe("r1");
    int before = vm.getIngredientCount();
    vm.removeIngredient(99);
    assertEquals(before, vm.getIngredientCount());
  }

  // ── E7: save persists to repository ──────────────────────────────────

  @Test
  void e7_save_callsRepositoryWithUpdatedTitle() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated Pancakes");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              return null;
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    assertTrue(done.await(2, TimeUnit.SECONDS));

    verify(mockRepo).save(argThat(r -> r.getTitle().equals("Updated Pancakes")));
  }

  @Test
  void e7_save_preservesRecipeId() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              return null;
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    done.await(2, TimeUnit.SECONDS);

    verify(mockRepo).save(argThat(r -> r.getId().equals("r1")));
  }

  // ── E8: save is async, isSaving is true while in progress ────────────

  @Test
  void e8_save_isSavingWhileInProgress() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(
            inv -> {
              started.countDown();
              release.await();
              return null;
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    assertTrue(started.await(2, TimeUnit.SECONDS));
    assertTrue(vm.isSaving());

    release.countDown();
    waitForFxEvents();
    waitForFxEvents();
    assertFalse(vm.isSaving());
  }

  @Test
  void e8_save_exitsEditModeOnSuccess() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              return null;
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    done.await(2, TimeUnit.SECONDS);
    waitForFxEvents();

    assertFalse(vm.isEditing());
    assertFalse(vm.isSaving());
    assertFalse(vm.isDirty());
  }

  @Test
  void e8_save_showsSuccessMessage() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              return null;
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    done.await(2, TimeUnit.SECONDS);
    waitForFxEvents();

    assertTrue(vm.getStatusMessage().contains("Saved"));
  }

  // ── E9: save failure preserves state ─────────────────────────────────

  @Test
  void e9_saveFailure_preservesDirtyState() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              throw new RepositoryException("disk full");
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    done.await(2, TimeUnit.SECONDS);
    waitForFxEvents();

    assertTrue(vm.isDirty());
  }

  @Test
  void e9_saveFailure_staysInEditMode() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              throw new RepositoryException("disk full");
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    done.await(2, TimeUnit.SECONDS);
    waitForFxEvents();

    assertTrue(vm.isEditing());
  }

  @Test
  void e9_saveFailure_showsErrorMessage() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch done = new CountDownLatch(1);
    doAnswer(
            inv -> {
              done.countDown();
              throw new RepositoryException("disk full");
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    assertTrue(done.await(2, TimeUnit.SECONDS));

    // Poll until status message is populated on the FX thread
    long deadline = System.currentTimeMillis() + 3000;
    while (vm.getStatusMessage().isEmpty() && System.currentTimeMillis() < deadline) {
      waitForFxEvents();
      Thread.sleep(20);
    }

    assertTrue(
        vm.getStatusMessage().contains("failed") || vm.getStatusMessage().contains("disk full"),
        "Expected error message but got: '" + vm.getStatusMessage() + "'");
  }

  @Test
  void e9_saveFailure_clearsSavingFlag() throws Exception {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set("Updated");

    CountDownLatch taskStarted = new CountDownLatch(1);
    CountDownLatch taskRelease = new CountDownLatch(1);

    doAnswer(
            inv -> {
              taskStarted.countDown();
              taskRelease.await();
              throw new RepositoryException("disk full");
            })
        .when(mockRepo)
        .save(any());

    vm.save();
    assertTrue(taskStarted.await(2, TimeUnit.SECONDS));
    assertTrue(vm.isSaving()); // confirm it is saving mid-task

    taskRelease.countDown();

    // Poll until isSaving becomes false, with a timeout
    long deadline = System.currentTimeMillis() + 3000;
    while (vm.isSaving() && System.currentTimeMillis() < deadline) {
      waitForFxEvents();
      Thread.sleep(20);
    }

    assertFalse(vm.isSaving());
  }

  // ── E10: save is no-op when not dirty or not valid ───────────────────

  @Test
  void e10_save_whenNotDirty_isNoOp() {
    vm.loadRecipe("r1");
    // not in edit mode, not dirty
    vm.save();
    verify(mockRepo, never()).save(any());
  }

  @Test
  void e10_save_whenInvalid_isNoOp() {
    vm.loadRecipe("r1");
    vm.toggleEditMode();
    vm.titleProperty().set(""); // invalid
    vm.save();
    // save() is called but should be a no-op since isValid = false
    verify(mockRepo, never()).save(any());
  }
}
