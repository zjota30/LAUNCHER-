package com.diamondrp.launcher.utils

/**
 * Wrapper padrão para resultados assíncronos (chamadas de rede, downloads, etc.)
 * Evita expor exceptions cruas para a UI e centraliza o tratamento de erro.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
