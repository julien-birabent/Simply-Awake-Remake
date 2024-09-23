package com.simplyawakeremake.data.common

sealed class ResultState<out T> {
    data class Loading<T>(val data: T?) : ResultState<T>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error<T>(val throwable: Throwable, val lastData: T?) : ResultState<T>()
}

val <T>ResultState<T>.data: T?
    get() = when (this) {
        is ResultState.Loading -> data
        is ResultState.Success -> data
        is ResultState.Error -> lastData
    }