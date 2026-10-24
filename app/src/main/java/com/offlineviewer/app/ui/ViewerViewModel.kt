package com.offlineviewer.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineviewer.app.data.FileLoader
import com.offlineviewer.app.render.FileRenderer
import com.offlineviewer.app.render.LoadedFile
import com.offlineviewer.app.render.RendererRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state for the viewer screen. */
sealed interface ViewerUiState {
    /** No file loaded yet; show the home / empty prompt. */
    data object Empty : ViewerUiState

    /** A file is being read. */
    data object Loading : ViewerUiState

    /** A file is loaded and ready to render with [renderer]. */
    data class Content(val file: LoadedFile, val renderer: FileRenderer) : ViewerUiState

    /** Loading failed. */
    data class Error(val message: String) : ViewerUiState
}

/**
 * Drives the viewer screen: given a content [Uri], loads it via [FileLoader]
 * (off the main thread) and resolves a renderer via [RendererRegistry],
 * publishing [ViewerUiState] transitions.
 */
class ViewerViewModel(
    private val registry: RendererRegistry = RendererRegistry.createDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ViewerUiState>(ViewerUiState.Empty)
    val state: StateFlow<ViewerUiState> = _state.asStateFlow()

    /** The Uri of the file currently loaded, for share / re-open actions. */
    var currentUri: Uri? = null
        private set

    /** Load and render the given [uri] using the supplied [loader]. */
    fun open(uri: Uri, loader: FileLoader) {
        currentUri = uri
        _state.value = ViewerUiState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { loader.load(uri) }
            _state.value = when (result) {
                is FileLoader.Result.Success -> {
                    val renderer = registry.resolve(result.file)
                    ViewerUiState.Content(result.file, renderer)
                }

                is FileLoader.Result.Failure -> ViewerUiState.Error(result.message)
            }
        }
    }

    /** Reset back to the empty / home state. */
    fun clear() {
        currentUri = null
        _state.value = ViewerUiState.Empty
    }
}
