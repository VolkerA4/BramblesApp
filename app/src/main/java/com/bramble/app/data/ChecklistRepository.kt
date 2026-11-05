package com.bramble.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Stores the checklist as a JSON file in the app's private storage.
 * JSON keeps the file human-readable, and the same format is reused for backups.
 */
class ChecklistRepository(context: Context) {
    private val file = File(context.filesDir, "checklist.json")

    fun hasSavedList(): Boolean = file.exists()

    fun load(): List<ChecklistItem> {
        if (!file.exists()) return emptyList()
        return try {
            fromJson(JSONArray(file.readText()))
        } catch (e: Exception) {
            emptyList() // corrupt file: start fresh rather than crash
        }
    }

    fun save(items: List<ChecklistItem>) {
        // Write to a temp file then rename so a crash mid-write can't corrupt the saved list.
        val tmp = File(file.parentFile, "checklist.json.tmp")
        tmp.writeText(toJson(items).toString())
        tmp.renameTo(file)
    }

    companion object {
        fun toJson(items: List<ChecklistItem>): JSONArray {
            val array = JSONArray()
            items.forEach {
                array.put(
                    JSONObject()
                        .put("id", it.id)
                        .put("text", it.text)
                        .put("startDay", it.startDay)
                        .put("repeatDaily", it.repeatDaily)
                        .put("completedDays", JSONArray(it.completedDays.sorted())),
                )
            }
            return array
        }

        /**
         * Throws if an entry is malformed. Repeated ids are dropped since the list UI needs unique ids.
         * Entries from before tasks had dates (just "done") become tasks for today.
         */
        fun fromJson(array: JSONArray): List<ChecklistItem> {
            val today = LocalDate.now().toEpochDay()
            return List(array.length()) { i ->
                val o = array.getJSONObject(i)
                val startDay = o.optLong("startDay", today)
                val completed = buildSet {
                    val days = o.optJSONArray("completedDays")
                    if (days != null) {
                        for (d in 0 until days.length()) add(days.getLong(d))
                    } else if (o.optBoolean("done", false)) {
                        add(startDay)
                    }
                }
                ChecklistItem(
                    id = o.getLong("id"),
                    text = o.getString("text"),
                    startDay = startDay,
                    repeatDaily = o.optBoolean("repeatDaily", false),
                    completedDays = completed,
                )
            }.distinctBy { it.id }
        }
    }
}
