package com.bramble.app.data

/**
 * A task. Days are stored as epoch-day numbers (days since 1970-01-01), which makes them
 * easy to compare and save.
 *
 * A normal task appears only on [startDay]. A repeating task appears on [startDay] and every
 * day after it. Completion is tracked per day, so a repeating task starts unchecked each day.
 *
 * [spoonCost] is how much energy the task takes: 1 = Easy, 2 = Med, 3 = Hard.
 */
data class ChecklistItem(
    val id: Long,
    val text: String,
    val startDay: Long,
    val repeatDaily: Boolean = false,
    val completedDays: Set<Long> = emptySet(),
    val spoonCost: Int = MIN_SPOONS,
) {
    fun appearsOn(day: Long): Boolean = if (repeatDaily) day >= startDay else day == startDay

    fun isDoneOn(day: Long): Boolean = day in completedDays

    fun toggledOn(day: Long): ChecklistItem =
        copy(completedDays = if (isDoneOn(day)) completedDays - day else completedDays + day)

    companion object {
        const val MIN_SPOONS = 1
        const val MAX_SPOONS = 3
    }
}
