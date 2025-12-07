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

inline fun <T, R> ResultState<T>.mapData(transform: (T) -> R): ResultState<R> =
    when (this) {
        is ResultState.Success ->
            ResultState.Success(transform(data))

        is ResultState.Error ->
            data?.let { current ->
                ResultState.Error(throwable, transform(current))
            } ?: ResultState.Error(throwable, null)

        is ResultState.Loading ->
            ResultState.Loading(data?.let { transform(it) })
    }