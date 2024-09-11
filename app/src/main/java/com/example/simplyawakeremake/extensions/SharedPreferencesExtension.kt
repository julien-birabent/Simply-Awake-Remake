package com.example.simplyawakeremake.extensions

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty


inline fun <reified T> SharedPreferences.dataCollection(key: String): ReadWriteProperty<Any, List<T>> =
    DataCollectionPreferences(this, key, T::class.java)