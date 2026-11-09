package com.bramble.app.data

/**
 * A ready-made task offered on the Add task screen. [spoonCost] uses the same 1..3 scale as
 * [ChecklistItem]. Plug the suggestions database in by passing a category -> list map to
 * AddTaskDialog; nothing else needs to change.
 */
data class TaskSuggestion(
    val text: String,
    val spoonCost: Int = ChecklistItem.MIN_SPOONS,
)
