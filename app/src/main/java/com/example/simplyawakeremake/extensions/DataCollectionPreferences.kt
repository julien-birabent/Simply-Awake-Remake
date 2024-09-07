package com.example.simplyawakeremake.extensions

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DataCollectionPreferences<T>(
    private val sharedPreferences: SharedPreferences,
    private val key: String
) : ReadWriteProperty<Any, List<T>> {

    companion object {
        val gson: Gson = GsonBuilder().create()
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        val sharedPrefStoredValue = sharedPreferences.getString(key, null)
        return sharedPrefStoredValue?.let {
            gson.fromJson(it, type)
        } ?: emptyList()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
        sharedPreferences.edit()
            .putString(key, gson.toJson(value))
            .apply()
    }
}