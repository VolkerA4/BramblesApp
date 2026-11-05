package com.bramble.app.ui.screens

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bramble.app.R
import com.bramble.app.data.ChecklistItem
import com.bramble.app.data.ChecklistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Holds the tasks, shared by the Home and Calendar tabs, and saves to disk after every change.
 * The state list is observed by Compose: changing it redraws the UI automatically.
 */
class TasksViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ChecklistRepository(app)

    // One thread for disk access so saves happen in order and never overlap.
    private val diskDispatcher = Dispatchers.IO.limitedParallelism(1)

    val items = mutableStateListOf<ChecklistItem>()

    init {
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

    fun add(text: String, day: Long, repeatDaily: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        items.add(ChecklistItem(id = System.currentTimeMillis(), text = trimmed, startDay = day, repeatDaily = repeatDaily))
        persist()
    }

    /** Checks or unchecks [item] for one [day]; a repeating task keeps separate state for each day. */
    fun toggle(item: ChecklistItem, day: Long) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            items[index] = items[index].toggledOn(day)
            persist()
        }
    }

    fun remove(item: ChecklistItem) {
        items.removeAll { it.id == item.id }
        persist()
    }

    /** Replaces the whole list, used when restoring a backup. */
    fun replaceAll(newItems: List<ChecklistItem>) {
        items.clear()
        items.addAll(newItems)
        persist()
    }

    private fun persist() {
        val snapshot = items.toList()
        viewModelScope.launch(diskDispatcher) { repository.save(snapshot) }
    }
}
