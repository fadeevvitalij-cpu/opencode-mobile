package com.opencode.mobile.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.Instant
import java.util.*

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? = value?.let {
        val type: Type = object : TypeToken<Map<String, String>>() {}.type
        gson.fromJson(it, type)
    }

    @TypeConverter
    fun fromList(value: List<String>?): String? = value?.let { gson.toJson(it) }

    @TypeConverter
    fun toList(value: String?): List<String>? = value?.let {
        val type: Type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(it, type)
    }
}