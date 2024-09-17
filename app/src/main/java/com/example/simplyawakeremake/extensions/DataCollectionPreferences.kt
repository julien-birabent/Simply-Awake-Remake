package com.example.simplyawakeremake.extensions

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DataCollectionPreferences<T>(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val objectClass: Class<T>
) : ReadWriteProperty<Any, List<T>> {

    companion object {
        val gson: Gson = GsonBuilder().create()
        val jsonParser = JsonParser()
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): List<T> {
        val sharedPrefStoredValue = sharedPreferences.getString(key, null)
        return if (sharedPrefStoredValue != null) {
            jsonParser.parse(sharedPrefStoredValue).asJsonArray
                .map { gson.fromJson(it, objectClass) }
        } else {
            emptyList()
        }
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<T>) {
        sharedPreferences.edit()
            .putString(key, gson.toJson(value))
            .apply()
    }
}