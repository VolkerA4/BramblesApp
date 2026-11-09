package com.bramble.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bramble.app.R
import com.bramble.app.data.ChecklistItem
import com.bramble.app.data.ChecklistRepository
import com.bramble.app.data.CompletionCheck
import com.bramble.app.data.DayRecord
import com.bramble.app.data.MoodEntry
import com.bramble.app.data.SpoonData
import com.bramble.app.data.SpoonLoan
import com.bramble.app.data.SpoonRepository
import com.bramble.app.data.SpoonSummary
import com.bramble.app.data.Spoons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Holds the tasks and the spoon/mood data, shared by the Home and Calendar tabs, and saves to disk
 * after every change. The state is observed by Compose: changing it redraws the UI automatically.
 */
class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ChecklistRepository(app)
    private val spoonRepository = SpoonRepository(app)

    // One thread for disk access so saves happen in order and never overlap.
    private val diskDispatcher = Dispatchers.IO.limitedParallelism(1)

    val items = mutableStateListOf<ChecklistItem>()

    private var spoonState by mutableStateOf(SpoonData())

    /** The saved spoon data, used when making a backup. */
    val spoonData: SpoonData get() = spoonState

    init {
        spoonState = spoonRepository.load()
        if (repository.hasSavedList()) {
            items.addAll(repository.load())
        } else {
            // First launch only: later deletions stay deleted because the saved file now exists.
            items.add(
                ChecklistItem(
                    id = 1L,
                    text = app.getString(R.string.default_task_drink_water),
                    startDay = LocalDate.now().toEpochDay(),
                    repeatDaily = true,
                ),
            )
            persist()
        }
    }

    // ---- Reading spoon state (call these from composables; they update the screen when data changes) ----

    private fun ledger() = Spoons.Ledger(spoonState.usualBudget, spoonState.days.associateBy { it.day }, spoonState.loans, items)

    fun summary(day: Long): SpoonSummary = ledger().summary(day)

    fun record(day: Long): DayRecord = ledger().record(day)

    /** True once the lowest mood has been logged on [day]: no new tasks for that day. */
    fun isTakeItEasy(day: Long): Boolean = ledger().record(day).isTakeItEasy

    /** True if extra spoons were added on [day]; the next day shows a gentle pacing note. */
    fun addedExtraOn(day: Long): Boolean = ledger().addedExtraOn(day)

    fun usualBudget(): Int = spoonState.usualBudget

    /** What should happen when [item] is checked off on [today]. */
    fun checkCompletion(item: ChecklistItem, today: Long): CompletionCheck =
        ledger().checkCompletion(today, item.spoonCost)

    // ---- Tasks ----

    /** Adds a task. On a take-it-easy day the screens ask "are you sure?" first (see AddTaskFlow), but it's always allowed. */
    fun add(
        text: String,
        day: Long,
        repeatDaily: Boolean,
        spoonCost: Int = ChecklistItem.MIN_SPOONS,
        priority: Boolean = false,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        items.add(
            ChecklistItem(
                id = System.currentTimeMillis(),
                text = trimmed,
                startDay = day,
                repeatDaily = repeatDaily,
                spoonCost = spoonCost.coerceIn(ChecklistItem.MIN_SPOONS, ChecklistItem.MAX_SPOONS),
                priority = priority,
            ),
        )
        persist()
    }

    /**
     * Skips a (repeating) task for one [day] only; it stays on every other day. If it was already
     * checked off that day, that is undone, and any energy borrowed for it is given back.
     */
    fun skipDay(item: ChecklistItem, day: Long) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        val old = items[index]
        items[index] = old.copy(skippedDays = old.skippedDays + day, completedDays = old.completedDays - day)
        spoonState = spoonState.copy(loans = spoonState.loans.filterNot { it.taskId == item.id && it.borrowDay == day })
        persist()
    }

    /**
     * Saves edits to a task. Moving it to a different date starts it fresh: its done and skipped
     * days (and any borrowed energy) belonged to the old date, so they're cleared.
     */
    fun updateTask(item: ChecklistItem, text: String, day: Long, repeatDaily: Boolean, spoonCost: Int, priority: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        val old = items[index]
        val dateChanged = day != old.startDay
        items[index] = old.copy(
            text = trimmed,
            startDay = day,
            repeatDaily = repeatDaily,
            spoonCost = spoonCost.coerceIn(ChecklistItem.MIN_SPOONS, ChecklistItem.MAX_SPOONS),
            priority = priority,
            completedDays = if (dateChanged) emptySet() else old.completedDays,
            skippedDays = if (dateChanged) emptySet() else old.skippedDays,
        )
        if (dateChanged) spoonState = spoonState.copy(loans = spoonState.loans.filterNot { it.taskId == item.id })
        persist()
    }

    /** Marks or unmarks [item] as a priority task (it applies to every day a repeating task shows on). */
    fun setPriority(item: ChecklistItem, priority: Boolean) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        items[index] = items[index].copy(priority = priority)
        persist()
    }

    /**
     * Checks or unchecks [item] for one [day]; a repeating task keeps separate state for each day.
     * Unchecking a task that was borrowed for gives the borrowed spoons back. Spoons you added
     * on a good day are kept.
     */
    fun toggle(item: ChecklistItem, day: Long) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) return
        val wasDone = items[index].isDoneOn(day)
        items[index] = items[index].toggledOn(day)
        if (wasDone) {
            spoonState = spoonState.copy(loans = spoonState.loans.filterNot { it.taskId == item.id && it.borrowDay == day })
        }
        persist()
    }

    /** Completes [item] on [day] by borrowing from the days ahead, as planned in [borrow]. */
    fun borrowAndComplete(item: ChecklistItem, day: Long, borrow: CompletionCheck.MustBorrow) {
        spoonState = spoonState.copy(loans = spoonState.loans + SpoonLoan(item.id, day, borrow.shortfall, borrow.parts))
        toggle(item, day)
    }

    fun remove(item: ChecklistItem) {
        items.removeAll { it.id == item.id }
        spoonState = spoonState.copy(loans = spoonState.loans.filterNot { it.taskId == item.id })
        persist()
    }

    /** Replaces everything, used when restoring a backup. Backups from before spoons have none. */
    fun replaceAll(newItems: List<ChecklistItem>, newSpoons: SpoonData?) {
        items.clear()
        items.addAll(newItems)
        spoonState = newSpoons ?: SpoonData()
        persist()
    }

    // ---- Spoons and mood ----

    fun logMood(day: Long, value: Int) {
        updateDay(day) { it.copy(moods = it.moods + MoodEntry(value.coerceIn(1, 5), System.currentTimeMillis())) }
    }

    fun addExtraSpoons(day: Long, amount: Int) {
        if (amount > 0) updateDay(day) { it.copy(extraSpoons = it.extraSpoons + amount) }
    }

    /** Sets the budget for one day. The app never changes it by itself. */
    fun setBudget(day: Long, value: Int) {
        updateDay(day) { it.copy(baseBudget = value.coerceIn(Spoons.MIN_BUDGET, Spoons.MAX_BUDGET)) }
    }

    /** Sets the budget used for every day that hasn't been changed by hand. */
    fun setUsualBudget(value: Int) {
        spoonState = spoonState.copy(usualBudget = value.coerceIn(Spoons.MIN_BUDGET, Spoons.MAX_BUDGET))
        persist()
    }

    fun markPacingNoteSeen(day: Long) {
        updateDay(day) { it.copy(pacingNoteSeen = true) }
    }

    private fun updateDay(day: Long, change: (DayRecord) -> DayRecord) {
        val updated = change(ledger().record(day))
        spoonState = spoonState.copy(days = spoonState.days.filter { it.day != day } + updated)
        persist()
    }

    private fun persist() {
        val itemsSnapshot = items.toList()
        val spoonSnapshot = spoonState
        viewModelScope.launch(diskDispatcher) {
            repository.save(itemsSnapshot)
            spoonRepository.save(spoonSnapshot)
        }
    }
}
