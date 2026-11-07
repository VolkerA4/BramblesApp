package com.bramble.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bramble.app.R
import com.bramble.app.data.ChecklistItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MILLIS_PER_DAY = 86_400_000L

private val MedColor = Color(0xFFC8963E) // warm amber; red is avoided because it reads as an alarm

@StringRes
private fun difficultyLabel(spoonCost: Int): Int = when (spoonCost) {
    1 -> R.string.difficulty_easy
    2 -> R.string.difficulty_med
    else -> R.string.difficulty_hard
}

/** Small pill showing how much energy a task takes: Easy (1 spoon), Med (2), Hard (3). */
@Composable
fun DifficultyTag(spoonCost: Int, modifier: Modifier = Modifier) {
    val color = when (spoonCost) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> MedColor
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        contentColor = color,
    ) {
        Text(
            stringResource(difficultyLabel(spoonCost)),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/**
 * A white rounded card for one task, checked or unchecked for the given [day].
 * The checkbox toggles it; tapping anywhere else on the card opens a menu of actions.
 * The Easy/Med/Hard tag sits at the end of the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistRow(item: ChecklistItem, day: Long, onToggle: () -> Unit, onDelete: () -> Unit) {
    val done = item.isDoneOn(day)
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { menuOpen = true },
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                Checkbox(checked = done, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f)) {
                    Text(
                        item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.repeatDaily) {
                        Text(
                            stringResource(R.string.task_repeats_daily),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DifficultyTag(item.spoonCost, Modifier.padding(start = 8.dp))
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    // Deleting a repeating task removes it from every day, so say so.
                    Text(stringResource(if (item.repeatDaily) R.string.checklist_delete_all_days else R.string.checklist_delete))
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/** "Today", "Tomorrow", "Yesterday", or a date like "Friday, Nov 6". */
@Composable
fun dayLabel(day: Long, today: Long): String = when (day - today) {
    0L -> stringResource(R.string.day_today)
    1L -> stringResource(R.string.day_tomorrow)
    -1L -> stringResource(R.string.day_yesterday)
    else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
}

/**
 * @param lockedDay a day that can't take new tasks (today, after the lowest mood was logged), or null.
 *   Choosing that day shows a kind note and disables Add; other dates still work.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    initialDay: Long,
    onAdd: (text: String, day: Long, repeatDaily: Boolean, spoonCost: Int) -> Unit,
    onDismiss: () -> Unit,
    lockedDay: Long? = null,
) {
    var text by rememberSaveable { mutableStateOf("") }
    // Start on tomorrow instead if the day being looked at is closed to new tasks.
    var day by rememberSaveable { mutableStateOf(if (initialDay == lockedDay) initialDay + 1 else initialDay) }
    var repeatDaily by rememberSaveable { mutableStateOf(false) }
    var spoonCost by rememberSaveable { mutableStateOf(ChecklistItem.MIN_SPOONS) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val blocked = day == lockedDay
    val canAdd = text.isNotBlank() && !blocked

    if (showPicker) {
        // The date picker works in UTC milliseconds, which map straight onto epoch days.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = day * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { day = Math.floorDiv(it, MILLIS_PER_DAY) }
                    showPicker = false
                }) { Text(stringResource(R.string.date_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.checklist_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checklist_add_task)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.checklist_add_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canAdd) onAdd(text, day, repeatDaily, spoonCost) }),
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Text(
                    stringResource(R.string.task_spoon_cost),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (cost in ChecklistItem.MIN_SPOONS..ChecklistItem.MAX_SPOONS) {
                        FilterChip(
                            selected = spoonCost == cost,
                            onClick = { spoonCost = cost },
                            label = { Text(stringResource(difficultyLabel(cost))) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")))
                }
                if (blocked) {
                    Text(
                        stringResource(R.string.take_it_easy_add_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeatDaily, onCheckedChange = { repeatDaily = it })
                    Text(stringResource(R.string.task_repeat_daily))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text, day, repeatDaily, spoonCost) }, enabled = canAdd) {
                Text(stringResource(R.string.checklist_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.checklist_cancel)) }
        },
    )
}
