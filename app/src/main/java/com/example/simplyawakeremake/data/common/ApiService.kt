package com.example.simplyawakeremake.data.common

import io.reactivex.rxjava3.core.Single

interface ApiService<T> {

    fun fetchAll() : Single<List<T>>
}