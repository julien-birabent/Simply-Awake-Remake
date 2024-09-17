package com.example.simplyawakeremake.data.common

import io.reactivex.rxjava3.core.Single

interface DataSaver<T> {

    fun persist(objects: List<T>)

    fun loadAll() : Single<List<T>>

    fun select(id: String) : T?
}