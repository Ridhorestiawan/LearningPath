package com.ridho.learningpath.ui.common

/**
 * State UI standar untuk setiap layar: memisahkan Loading, Empty, Error, dan Content.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}