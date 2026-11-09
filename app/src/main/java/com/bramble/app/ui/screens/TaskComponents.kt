package com.bramble.app.ui.screens

import androidx.annotation.StringRes
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.bramble.app.data.TaskSuggestion
import com.bramble.app.R
import com.bramble.app.data.ChecklistItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MILLIS_PER_DAY = 86_400_000L

@StringRes
private fun difficultyLabel(spoonCost: Int): Int = when (spoonCost) {
    1 -> R.string.difficulty_easy
    2 -> R.string.difficulty_med
    else -> R.string.difficulty_hard
}

/** Shows how much energy a task takes: one fish (easy), two (medium) or three (hard). */
@Composable
fun DifficultyTag(spoonCost: Int, modifier: Modifier = Modifier) {
    val label = stringResource(difficultyLabel(spoonCost))
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
    ) {
        repeat(spoonCost.coerceIn(ChecklistItem.MIN_SPOONS, ChecklistItem.MAX_SPOONS)) {
            Icon(
                painter = painterResource(R.drawable.ic_sardine),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * A white rounded card for one task, checked or unchecked for the given [day].
 * The checkbox toggles it; tapping anywhere else on the card opens a menu of actions.
 * The fish difficulty tag sits at the end of the card.
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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                TaskCheckbox(checked = done, onToggle = onToggle)
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

/**
 * A checkbox that is filled green with a checkmark only when the task is done, and an empty
 * outlined box otherwise. The 48dp box is the touch target; the visible box inside is 22dp.
 */
@Composable
private fun TaskCheckbox(checked: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(shape)
                .background(if (checked) colors.primary else Color.Transparent, shape)
                .border(2.dp, if (checked) colors.primary else colors.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
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
 * Full-screen "new task" sheet. A bordered card holds the task name, an energy picker (one to
 * three sardines) and date / repeat chips; the Add button sits below it. Suggested tasks follow,
 * with category chips on top and the tasks as a wrapping cloud of small pills.
 *
 * @param lockedDay a day that can't take new tasks (today, after the lowest mood was logged), or null.
 *   Choosing that day shows a kind note and disables Add; other dates still work.
 * @param suggestions category name -> suggested tasks. Empty until the suggestions database exists;
 *   the Suggestions section only appears when there is something to show.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    initialDay: Long,
    onAdd: (text: String, day: Long, repeatDaily: Boolean, spoonCost: Int) -> Unit,
    onDismiss: () -> Unit,
    lockedDay: Long? = null,
    suggestions: Map<String, List<TaskSuggestion>> = emptyMap(),
) {
    var text by rememberSaveable { mutableStateOf("") }
    // Start on tomorrow instead if the day being looked at is closed to new tasks.
    var day by rememberSaveable { mutableStateOf(if (initialDay == lockedDay) initialDay + 1 else initialDay) }
    var repeatDaily by rememberSaveable { mutableStateOf(false) }
    var spoonCost by rememberSaveable { mutableStateOf(ChecklistItem.MIN_SPOONS) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val todayDay = remember { LocalDate.now().toEpochDay() }
    val blocked = day == lockedDay
    val canAdd = text.isNotBlank() && !blocked
    val shownCategory = category?.takeIf { it in suggestions } ?: suggestions.keys.firstOrNull()
    val colors = MaterialTheme.colorScheme

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
            ) {
                // Title on the left, close on the right.
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.checklist_add_task),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.add_task_close))
                    }
                }

                Surface(
                    color = colors.surfaceContainerLowest,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, colors.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text(stringResource(R.string.checklist_add_hint), style = MaterialTheme.typography.titleLarge) },
                            textStyle = MaterialTheme.typography.titleLarge,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (canAdd) onAdd(text, day, repeatDaily, spoonCost) }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = colors.primary,
                                unfocusedIndicatorColor = colors.outlineVariant,
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        )

                        Text(
                            stringResource(R.string.task_spoon_cost),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                        // Energy picker: one, two or three fish.
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            for (cost in ChecklistItem.MIN_SPOONS..ChecklistItem.MAX_SPOONS) {
                                val selected = spoonCost == cost
                                val label = stringResource(difficultyLabel(cost))
                                Surface(
                                    onClick = { spoonCost = cost },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
                                    border = BorderStroke(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) colors.primary else colors.outlineVariant,
                                    ),
                                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                    ) {
                                        repeat(cost) {
                                            Icon(
                                                painterResource(R.drawable.ic_sardine),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Date and repeat as chips.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        ) {
                            AssistChip(
                                onClick = { showPicker = true },
                                label = { Text(dayLabel(day, todayDay)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                            FilterChip(
                                selected = repeatDaily,
                                onClick = { repeatDaily = !repeatDaily },
                                label = { Text(stringResource(if (repeatDaily) R.string.task_repeat_daily else R.string.task_does_not_repeat)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_repeat), contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                        }
                        if (blocked) {
                            Text(
                                stringResource(R.string.take_it_easy_add_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                Button(
                    onClick = { onAdd(text, day, repeatDaily, spoonCost) },
                    enabled = canAdd,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                ) {
                    Text(stringResource(R.string.checklist_add))
                }

                // Suggestions: pick a category, then tap a task to fill in the card above.
                val shown = shownCategory?.let { suggestions[it] }.orEmpty()
                if (shown.isNotEmpty()) {
                    Text(
                        stringResource(R.string.suggestions_title).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (name in suggestions.keys) {
                            FilterChip(
                                selected = name == shownCategory,
                                onClick = { category = name },
                                label = { Text(name) },
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
                    ) {
                        for (suggestion in shown) {
                            Surface(
                                onClick = {
                                    text = suggestion.text
                                    spoonCost = suggestion.spoonCost.coerceIn(ChecklistItem.MIN_SPOONS, ChecklistItem.MAX_SPOONS)
                                    scope.launch { scrollState.animateScrollTo(0) }
                                },
                                shape = RoundedCornerShape(50),
                                color = colors.surfaceContainerLowest.copy(alpha = 0.6f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                ) {
                                    Text(suggestion.text, style = MaterialTheme.typography.bodyMedium)
                                    DifficultyTag(suggestion.spoonCost, Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
