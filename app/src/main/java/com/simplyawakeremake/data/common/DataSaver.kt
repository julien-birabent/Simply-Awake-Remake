package com.simplyawakeremake.data.common

interface DataSaver<T> {

    fun persist(objects: List<T>)

    suspend fun loadAll(): List<T>

    fun select(id: String): T?
}