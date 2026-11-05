package com.bramble.app.data

import org.json.JSONObject

/** Everything a backup file holds. [name] is null when the file has no usable name. */
data class BackupData(
    val items: List<ChecklistItem>,
    val name: String?,
    val pronouns: String?,
)

object Backup {
    // 2: tasks have dates and per-day completion. Version 1 files still load (see ChecklistRepository.fromJson).
    private const val VERSION = 2

    fun toJson(items: List<ChecklistItem>, name: String, pronouns: String): String =
        JSONObject()
            .put("version", VERSION)
            .put("profile", JSONObject().put("name", name).put("pronouns", pronouns))
            .put("checklist", ChecklistRepository.toJson(items))
            .toString(2)

    /** Returns null if [json] isn't a valid Bramble backup. */
    fun parse(json: String): BackupData? = try {
        val root = JSONObject(json)
        require(root.getInt("version") <= VERSION) // made by a newer app version
        val profile = root.optJSONObject("profile")
        BackupData(
            items = ChecklistRepository.fromJson(root.getJSONArray("checklist")),
            name = profile?.optString("name")?.trim()?.takeIf { it.isNotEmpty() },
            pronouns = profile?.optString("pronouns")?.trim(),
        )
    } catch (e: Exception) {
        null
    }
}
