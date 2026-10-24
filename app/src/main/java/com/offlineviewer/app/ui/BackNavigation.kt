package com.offlineviewer.app.ui

/**
 * Pure, framework-free decision logic for the system back button.
 *
 * The viewer is a single-activity Compose app with no navigation library, so
 * back behaviour is derived directly from the current [ViewerUiState]. Keeping
 * the mapping here (rather than inline in the Compose tree) lets it be covered
 * by plain JVM unit tests; the Compose `BackHandler`s simply read the result.
 */
object BackNavigation {

    /** What the back button should do for a given screen state. */
    enum class BackAction {
        /** Return to the home / empty state (e.g. close the open file). */
        RETURN_HOME,

        /** At the root already: ask the user to confirm leaving the app. */
        CONFIRM_EXIT,
    }

    /**
     * Coarse classification of [ViewerUiState] used for back handling. Mirrors
     * the sealed state without dragging Android types into the decision.
     */
    enum class StateKind { EMPTY, LOADING, CONTENT, ERROR }

    /** Classifies a [ViewerUiState] into its [StateKind]. */
    fun kindOf(state: ViewerUiState): StateKind = when (state) {
        is ViewerUiState.Empty -> StateKind.EMPTY
        is ViewerUiState.Loading -> StateKind.LOADING
        is ViewerUiState.Content -> StateKind.CONTENT
        is ViewerUiState.Error -> StateKind.ERROR
    }

    /**
     * Resolves the back action for a state kind.
     *
     * - [StateKind.CONTENT] / [StateKind.ERROR] / [StateKind.LOADING] -> go
     *   back to home: a file is (being) shown, so back should dismiss it rather
     *   than leave the app. Loading is treated like Content so that backing out
     *   of an in-progress load returns home instead of abruptly exiting.
     * - [StateKind.EMPTY] -> we are at the root, so back leads toward exit and
     *   the caller should surface the confirmation dialog.
     */
    fun actionFor(kind: StateKind): BackAction = when (kind) {
        StateKind.CONTENT, StateKind.ERROR, StateKind.LOADING -> BackAction.RETURN_HOME
        StateKind.EMPTY -> BackAction.CONFIRM_EXIT
    }

    /** Convenience: resolve the back action directly from a [ViewerUiState]. */
    fun actionFor(state: ViewerUiState): BackAction = actionFor(kindOf(state))

    /**
     * True when back should return to home (not exit). Used as the `enabled`
     * flag for the "close file" `BackHandler`.
     */
    fun shouldReturnHome(state: ViewerUiState): Boolean =
        actionFor(state) == BackAction.RETURN_HOME

    /**
     * True when back should trigger the exit-confirmation dialog. The exact
     * complement of [shouldReturnHome], so the two `BackHandler`s are mutually
     * exclusive and exactly one intercepts back for any given state.
     */
    fun shouldConfirmExit(state: ViewerUiState): Boolean =
        actionFor(state) == BackAction.CONFIRM_EXIT
}
