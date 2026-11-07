package com.bramble.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bramble.app.R
import com.bramble.app.data.ChecklistItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: TasksViewModel = viewModel()) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showMoodDialog by rememberSaveable { mutableStateOf(false) }
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val todayDay = today.toEpochDay()
    val handleToggle = rememberToggleHandler(viewModel, todayDay)

    val summary = viewModel.summary(todayDay)
    val takeItEasy = viewModel.isTakeItEasy(todayDay)

    if (showAddDialog) {
        AddTaskDialog(
            initialDay = todayDay,
            lockedDay = todayDay.takeIf { takeItEasy },
            onAdd = { text, day, repeatDaily, spoonCost ->
                viewModel.add(text, day, repeatDaily, spoonCost)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showMoodDialog) {
        MoodDialog(
            onPick = { mood ->
                viewModel.logMood(todayDay, mood)
                showMoodDialog = false
            },
            onDismiss = { showMoodDialog = false },
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            current = viewModel.record(todayDay).baseBudget,
            onSaveToday = {
                viewModel.setBudget(todayDay, it)
                showBudgetDialog = false
            },
            onSaveEveryDay = {
                viewModel.setUsualBudget(it)
                viewModel.setBudget(todayDay, it)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false },
        )
    }

    // The day after extra spoons were added: a gentle, optional note, shown once.
    val showPacingNote = viewModel.addedExtraOn(todayDay - 1) && !viewModel.record(todayDay).pacingNoteSeen
    if (showPacingNote) {
        AlertDialog(
            onDismissRequest = { viewModel.markPacingNoteSeen(todayDay) },
            title = { Text(stringResource(R.string.pacing_title)) },
            text = { Text(stringResource(R.string.pacing_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.markPacingNoteSeen(todayDay) }) { Text(stringResource(R.string.pacing_ok)) }
            },
        )
    }

    val groups = groupByDay(viewModel.items, todayDay)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Top half of the screen: the mascot. Fit scales it to fill the space without cropping.
            Image(
                painter = painterResource(R.drawable.raccoon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            )

            // Bottom half: the checklist.
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f), // pushes the counter to the right edge
                    )
                    SpoonCounter(left = summary.left, budget = summary.budget, onClick = { showBudgetDialog = true })
                }
                if (takeItEasy) {
                    Text(
                        stringResource(R.string.take_it_easy_banner),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // The list takes all spare room so the button stays pinned under it.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { (day, tasks) ->
                        // Today's tasks sit right under the date, so only other days need a header.
                        if (day != todayDay) {
                            item(key = "day-$day") {
                                Text(
                                    dayLabel(day, todayDay),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        items(tasks, key = { it.id }) { task ->
                            ChecklistRow(
                                item = task,
                                day = day,
                                onToggle = { handleToggle(task, day) },
                                onDelete = { viewModel.remove(task) },
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.checklist_add_task))
                }
            }
        }

        // Top-right corner, mirroring the menu button on the left. Opens the mood check-in.
        IconButton(
            onClick = { showMoodDialog = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        ) {
            Icon(painterResource(R.drawable.ic_smiley), contentDescription = stringResource(R.string.mood_button))
        }
    }
}

/** "9 / 12 spoons": what's left today out of today's budget. Tap to change the budget. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoonCounter(left: Int, budget: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("${left.coerceAtLeast(0)} / $budget", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.spoons_label), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Groups tasks by the day they belong to: today first, then upcoming days soonest-first,
 * then earlier days most-recent-first. A repeating task that started in the past is listed
 * under today only, since it would otherwise appear on every day.
 */
private fun groupByDay(items: List<ChecklistItem>, today: Long): List<Pair<Long, List<ChecklistItem>>> {
    val groups = items.groupBy { if (it.repeatDaily) maxOf(it.startDay, today) else it.startDay }
    val upcoming = groups.keys.filter { it > today }.sorted()
    val earlier = groups.keys.filter { it < today }.sortedDescending()
    return (listOfNotNull(today.takeIf { it in groups }) + upcoming + earlier).map { it to groups.getValue(it) }
}