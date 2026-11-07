package com.bramble.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stores budgets, moods and borrowed spoons as JSON in the app's private storage,
 * next to the checklist. The same format is reused inside backups.
 */
class SpoonRepository(context: Context) {
    private val file = File(context.filesDir, "spoons.json")

    fun load(): SpoonData {
        if (!file.exists()) return SpoonData()
        return try {
            fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            SpoonData() // corrupt file: start fresh rather than crash
        }
    }

    fun save(data: SpoonData) {
        // Write to a temp file then rename so a crash mid-write can't corrupt the saved data.
        val tmp = File(file.parentFile, "spoons.json.tmp")
        tmp.writeText(toJson(data).toString())
        tmp.renameTo(file)
    }

    companion object {
        fun toJson(data: SpoonData): JSONObject {
            val days = JSONArray()
            data.days.forEach { d ->
                val moods = JSONArray()
                d.moods.forEach { moods.put(JSONObject().put("value", it.value).put("at", it.atMillis)) }
                days.put(
                    JSONObject()
                        .put("day", d.day)
                        .put("baseBudget", d.baseBudget)
                        .put("extraSpoons", d.extraSpoons)
                        .put("moods", moods)
                        .put("moodPrompted", d.moodPrompted)
                        .put("pacingNoteSeen", d.pacingNoteSeen),
                )
            }
            val loans = JSONArray()
            data.loans.forEach { loan ->
                val parts = JSONArray()
                loan.parts.forEach { parts.put(JSONObject().put("lendDay", it.lendDay).put("spoons", it.spoons)) }
                loans.put(
                    JSONObject()
                        .put("taskId", loan.taskId)
                        .put("borrowDay", loan.borrowDay)
                        .put("shortfall", loan.shortfall)
                        .put("parts", parts),
                )
            }
            return JSONObject()
                .put("usualBudget", data.usualBudget)
                .put("days", days)
                .put("loans", loans)
        }

        /** Throws if the JSON is malformed. */
        fun fromJson(o: JSONObject): SpoonData {
            val usual = o.optInt("usualBudget", Spoons.DEFAULT_BUDGET).coerceIn(Spoons.MIN_BUDGET, Spoons.MAX_BUDGET)
            val daysJson = o.optJSONArray("days") ?: JSONArray()
            val days = List(daysJson.length()) { i ->
                val d = daysJson.getJSONObject(i)
                val moodsJson = d.optJSONArray("moods") ?: JSONArray()
                DayRecord(
                    day = d.getLong("day"),
                    baseBudget = d.optInt("baseBudget", usual).coerceIn(Spoons.MIN_BUDGET, Spoons.MAX_BUDGET),
                    extraSpoons = d.optInt("extraSpoons", 0).coerceAtLeast(0),
                    moods = List(moodsJson.length()) { j ->
                        val m = moodsJson.getJSONObject(j)
                        MoodEntry(m.getInt("value").coerceIn(1, 5), m.getLong("at"))
                    },
                    moodPrompted = d.optBoolean("moodPrompted", false),
                    pacingNoteSeen = d.optBoolean("pacingNoteSeen", false),
                )
            }.distinctBy { it.day }
            val loansJson = o.optJSONArray("loans") ?: JSONArray()
            val loans = List(loansJson.length()) { i ->
                val l = loansJson.getJSONObject(i)
                val partsJson = l.getJSONArray("parts")
                SpoonLoan(
                    taskId = l.getLong("taskId"),
                    borrowDay = l.getLong("borrowDay"),
                    shortfall = l.getInt("shortfall"),
                    parts = List(partsJson.length()) { j ->
                        val p = partsJson.getJSONObject(j)
                        LoanPart(p.getLong("lendDay"), p.getInt("spoons"))
                    },
                )
            }
            return SpoonData(usual, days, loans)
        }
    }
}
