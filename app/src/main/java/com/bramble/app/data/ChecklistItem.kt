package com.bramble.app.data

import java.time.LocalDate

/**
 * A task. Days are stored as epoch-day numbers (days since 1970-01-01), which makes them
 * easy to compare and save.
 *
 * A normal task appears on [startDay]. If it isn't finished it carries over, appearing on each
 * following day up to today until the day it is completed. A repeating task appears on [startDay]
 * and every day after it. Completion is tracked per day, so a repeating task starts unchecked each day.
 *
 * [spoonCost] is how much energy the task takes: 1 = Easy, 2 = Med, 3 = Hard.
 * A [priority] task is pinned to the top of its list and drawn with a purple border.
 * [skippedDays] are days a repeating task was skipped: it stays on the list for every other day.
 */
data class ChecklistItem(
    val id: Long,
    val text: String,
    val startDay: Long,
    val repeatDaily: Boolean = false,
    val completedDays: Set<Long> = emptySet(),
    val spoonCost: Int = MIN_SPOONS,
    val priority: Boolean = false,
    val skippedDays: Set<Long> = emptySet(),
) {
    /**
     * Whether the task is on the list for [day]. Carry-over only reaches up to [today], so
     * future days stay empty, and it stops after the day the task was completed.
     */
    fun appearsOn(day: Long, today: Long = LocalDate.now().toEpochDay()): Boolean = when {
        day in skippedDays -> false
        repeatDaily -> day >= startDay
        day == startDay -> true
        day < startDay || day > today -> false
        else -> completedDays.none { it < day } // still unfinished before this day
    }

    fun isDoneOn(day: Long): Boolean = day in completedDays

    fun toggledOn(day: Long): ChecklistItem =
        copy(completedDays = if (isDoneOn(day)) completedDays - day else completedDays + day)

    companion object {
        const val MIN_SPOONS = 1
        const val MAX_SPOONS = 3
    }
}
