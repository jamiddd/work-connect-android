package com.jamid.workconnect

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null){
            return null
        }
        val listType: Type = object : TypeToken<List<String>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromBooleanToInt(state: Boolean): Int {
        return if (state) {
            1
        } else {
            0
        }
    }

    @TypeConverter
    fun fromIntToBoolean(state: Int): Boolean {
        return state == 1
    }

    @TypeConverter
    fun fromStringX(value: String?): ArrayList<String>? {
        if (value == null){
            return null
        }
        val listType: Type = object : TypeToken<ArrayList<String>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromIntArray(list: ArrayList<Int>?): String? {
        if (list == null) {
            return null
        }
        val a = arrayListOf<String>()
        for (l in list) {
            a.add(l.toString())
        }
        return fromArrayList(a)
    }

    @TypeConverter
    fun fromStringToNumArray(s: String?): ArrayList<Int>? {
        val list1 = fromStringX(s) ?: return null
        val a = arrayListOf<Int>()

        for (l in list1) {
            a.add(l.toInt())
        }

        return a
    }



}