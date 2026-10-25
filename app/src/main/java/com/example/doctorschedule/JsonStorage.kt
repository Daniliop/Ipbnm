package com.example.doctorschedule

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object JsonStorage {
    private const val FILE_NAME = "doctors_data.json"
    private const val PREFS = "doctor_app_prefs"
    private const val KEY_AUTO_FETCH = "auto_fetch_turn_info"

    // === تنظیمات سوییچ استخراج خودکار جزئیات نوبت (پیش‌فرض: خاموش) ===
    fun isAutoFetchEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_FETCH, false)
    }

    fun setAutoFetchEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_FETCH, enabled).apply()
    }

    @Synchronized
    fun saveDoctors(context: Context, doctors: List<Doctor>) {
        try {
            val root = readRoot(context)
            root.put("lastUpdate", System.currentTimeMillis())
            val arr = JSONArray()
            for (d in doctors) arr.put(doctorToJson(d))
            root.put("doctors", arr)
            if (!root.has("turnInfos")) root.put("turnInfos", JSONObject())
            writeRoot(context, root)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun saveTurnInfo(context: Context, showId: Int, appoint: Int, telAppoint: Int, webAppoint: Int) {
        try {
            val root = readRoot(context)
            val turnObj = root.optJSONObject("turnInfos") ?: JSONObject()
            val item = JSONObject()
            item.put("appoint", appoint)
            item.put("telAppoint", telAppoint)
            item.put("webAppoint", webAppoint)
            item.put("fetchedAt", System.currentTimeMillis())
            turnObj.put(showId.toString(), item)
            root.put("turnInfos", turnObj)
            writeRoot(context, root)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadJson(context: Context): String {
        return try {
            context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "{}"
        }
    }

    fun getFileSizeBytes(context: Context): Long {
        return try {
            context.getFileStreamPath(FILE_NAME).length()
        } catch (e: Exception) {
            0L
        }
    }

    private fun readRoot(context: Context): JSONObject {
        return try {
            JSONObject(context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(root.toString(2).toByteArray())
        }
    }

    private fun doctorToJson(d: Doctor): JSONObject {
        val obj = JSONObject()
        obj.put("name", d.name)
        obj.put("specialty", d.specialty)
        obj.put("clinic", d.clinic)
        val sArr = JSONArray()
        for (s in d.schedules) {
            val sObj = JSONObject()
            sObj.put("showId", s.showId)
            sObj.put("day", s.day)
            sObj.put("time", s.time)
            sArr.put(sObj)
        }
        obj.put("schedules", sArr)
        return obj
    }
}
