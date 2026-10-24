package com.offlineviewer.app.ui

import com.offlineviewer.app.ui.BackNavigation.BackAction
import com.offlineviewer.app.ui.BackNavigation.StateKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [BackNavigation]: the back-button decision logic. No
 * Compose or Android framework is involved; only the state-kind classification
 * and the resulting action are exercised.
 */
class BackNavigationTest {

    // --- actionFor(StateKind) ---------------------------------------------

    @Test
    fun contentState_returnsHome() {
        assertEquals(BackAction.RETURN_HOME, BackNavigation.actionFor(StateKind.CONTENT))
    }

    @Test
    fun errorState_returnsHome() {
        assertEquals(BackAction.RETURN_HOME, BackNavigation.actionFor(StateKind.ERROR))
    }

    @Test
    fun loadingState_returnsHome() {
        // Loading is treated like Content: backing out returns home, not exit.
        assertEquals(BackAction.RETURN_HOME, BackNavigation.actionFor(StateKind.LOADING))
    }

    @Test
    fun emptyState_confirmsExit() {
        assertEquals(BackAction.CONFIRM_EXIT, BackNavigation.actionFor(StateKind.EMPTY))
    }

    // --- kindOf(ViewerUiState) --------------------------------------------

    @Test
    fun kindOf_mapsEachState() {
        assertEquals(StateKind.EMPTY, BackNavigation.kindOf(ViewerUiState.Empty))
        assertEquals(StateKind.LOADING, BackNavigation.kindOf(ViewerUiState.Loading))
        assertEquals(StateKind.ERROR, BackNavigation.kindOf(ViewerUiState.Error("boom")))
    }

    // --- mutual exclusivity of the two BackHandler enabled flags ----------

    @Test
    fun returnHomeAndConfirmExit_areMutuallyExclusive() {
        // Empty -> confirm exit only.
        assertFalse(BackNavigation.shouldReturnHome(ViewerUiState.Empty))
        assertTrue(BackNavigation.shouldConfirmExit(ViewerUiState.Empty))

        // Error -> return home only.
        assertTrue(BackNavigation.shouldReturnHome(ViewerUiState.Error("oops")))
        assertFalse(BackNavigation.shouldConfirmExit(ViewerUiState.Error("oops")))

        // Loading -> return home only.
        assertTrue(BackNavigation.shouldReturnHome(ViewerUiState.Loading))
        assertFalse(BackNavigation.shouldConfirmExit(ViewerUiState.Loading))
    }

    @Test
    fun everyKind_yieldsExactlyOneEnabledHandler() {
        StateKind.entries.forEach { kind ->
            val returnHome = BackNavigation.actionFor(kind) == BackAction.RETURN_HOME
            val confirmExit = BackNavigation.actionFor(kind) == BackAction.CONFIRM_EXIT
            assertTrue("exactly one action must apply for $kind", returnHome xor confirmExit)
        }
    }
}
