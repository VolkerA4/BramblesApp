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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.stateDescription
import com.bramble.app.ui.theme.AmethystSmoke
import com.bramble.app.ui.theme.BrambleCorner
import com.bramble.app.ui.theme.BrambleDialog
import com.bramble.app.ui.theme.popupBorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
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
import com.bramble.app.data.TaskSuggestions
import com.bramble.app.R
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.Popup
import androidx.annotation.DrawableRes
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
 * One task on a list, with everything that can happen to it: tapping the card opens a small popup
 * (centered across the screen, just under the card) with Priority, Edit task and Remove.
 * Remove on a daily task asks whether to skip just this day or delete it from every day.
 */
@Composable
fun TaskItem(item: ChecklistItem, day: Long, viewModel: TasksViewModel, onToggle: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) } // daily task: skip this day or delete?
    var confirmDelete by remember { mutableStateOf(false) } // standard task: really delete?

    // The Box is what the popup is positioned against.
    Box {
        ChecklistRow(item = item, day = day, onToggle = onToggle, onClick = { menuOpen = true })
        if (menuOpen) {
            TaskActionsPopup(
                item = item,
                onDismiss = { menuOpen = false },
                onPriority = {
                    menuOpen = false
                    viewModel.setPriority(item, !item.priority)
                },
                onEdit = {
                    menuOpen = false
                    editing = true
                },
                onRemove = {
                    menuOpen = false
                    if (item.repeatDaily) confirmRemove = true else confirmDelete = true
                },
            )
        }
    }

    if (editing) {
        AddTaskDialog(
            initialDay = item.startDay,
            initial = item,
            onAdd = { text, newDay, repeatDaily, spoonCost, priority ->
                viewModel.updateTask(item, text, newDay, repeatDaily, spoonCost, priority)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }

    if (confirmDelete) {
        BrambleDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_task_title)) },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.checklist_cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.remove(item)
                }) { Text(stringResource(R.string.checklist_delete)) }
            },
        )
    }

    if (confirmRemove) {
        val skipLabel = if (day == LocalDate.now().toEpochDay()) R.string.task_skip_today else R.string.task_skip_day
        // Delete on the left, skip on the right. Tapping outside the box cancels without doing either.
        BrambleDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.remove_daily_title)) },
            text = { Text(stringResource(R.string.remove_daily_body)) },
            dismissButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.remove(item)
                }) { Text(stringResource(R.string.checklist_delete)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.skipDay(item, day)
                }) { Text(stringResource(skipLabel)) }
            },
        )
    }
}

/**
 * A white rounded card for one task, checked or unchecked for the given [day].
 * The checkbox toggles it; tapping anywhere else on the card calls [onClick].
 * The fish difficulty tag sits at the end of the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    day: Long,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val done = item.isDoneOn(day)
    val priorityLabel = stringResource(R.string.task_priority)

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        // Priority tasks get an Amethyst Smoke border; screen readers hear it too, not just see it.
        border = if (item.priority) BorderStroke(2.dp, AmethystSmoke) else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (item.priority) stateDescription = priorityLabel },
    ) {
        // Name on the left; difficulty tag, then the checkbox, on the right. The checkbox's 48dp
        // touch area already has some empty margin, so the end padding is small.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
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
            TaskCheckbox(checked = done, onToggle = onToggle)
        }
    }
}

/** Puts a popup in the middle of the screen horizontally, just below the card it belongs to (above it if there's no room). */
private class CenteredBelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (windowSize.width - popupContentSize.width) / 2
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) below
        else (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/** The three actions for a task, side by side in a small rounded box. Tapping outside closes it. */
@Composable
private fun TaskActionsPopup(
    item: ChecklistItem,
    onDismiss: () -> Unit,
    onPriority: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
    Popup(
        popupPositionProvider = remember(gap) { CenteredBelowAnchor(gap) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true), // lets a tap outside, or Back, close it
    ) {
        Surface(
            shape = BrambleCorner,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
            border = popupBorderStroke(),
        ) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PopupOption(R.drawable.ic_priority, R.string.task_priority, onClick = onPriority, isSelected = item.priority)
                PopupOption(R.drawable.ic_edit, R.string.task_edit, onClick = onEdit)
                PopupOption(R.drawable.ic_delete, R.string.task_remove, onClick = onRemove)
            }
        }
    }
}

/** One action in the popup: an icon over a label. [isSelected] shades it, used for Priority while it's on. */
@Composable
private fun PopupOption(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    isSelected: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clip(BrambleCorner)
            .background(if (isSelected) AmethystSmoke.copy(alpha = 0.25f) else Color.Transparent)
            .semantics { selected = isSelected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (isSelected) AmethystSmoke else LocalContentColor.current,
        )
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}


/**
 * Orders one day's tasks: unfinished before finished; within each of those, priority tasks first,
 * then daily tasks, then standard tasks. Ties keep the order the tasks were added in.
 */
fun List<ChecklistItem>.sortedForDay(day: Long): List<ChecklistItem> =
    sortedWith(
        compareBy<ChecklistItem> { it.isDoneOn(day) }
            .thenBy { !it.priority }
            .thenBy { !it.repeatDaily },
    )

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

private data class NewTask(val text: String, val day: Long, val repeatDaily: Boolean, val spoonCost: Int, val priority: Boolean)

/**
 * The whole "add a task" flow, shared by Home and Calendar. On a take-it-easy day (the lowest mood
 * was logged today) a task for today is still allowed, but asks "are you sure?" first.
 * "Not today" goes back to the form with everything still filled in.
 */
@Composable
fun AddTaskFlow(viewModel: TasksViewModel, initialDay: Long, onClose: () -> Unit) {
    var pending by remember { mutableStateOf<NewTask?>(null) }

    AddTaskDialog(
        initialDay = initialDay,
        onAdd = { text, day, repeatDaily, spoonCost, priority ->
            val today = LocalDate.now().toEpochDay()
            if (day == today && viewModel.isTakeItEasy(today)) {
                pending = NewTask(text, day, repeatDaily, spoonCost, priority)
            } else {
                viewModel.add(text, day, repeatDaily, spoonCost, priority)
                onClose()
            }
        },
        onDismiss = onClose,
        takeItEasyDay = LocalDate.now().toEpochDay().takeIf { viewModel.isTakeItEasy(it) },
        suggestions = TaskSuggestions,
    )

    pending?.let { task ->
        BrambleDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.take_it_easy_confirm_title)) },
            text = { Text(stringResource(R.string.take_it_easy_confirm_body)) },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.take_it_easy_confirm_no)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.add(task.text, task.day, task.repeatDaily, task.spoonCost, task.priority)
                    pending = null
                    onClose()
                }) { Text(stringResource(R.string.take_it_easy_confirm_yes)) }
            },
        )
    }
}

/**
 * Full-screen "new task" sheet. A bordered card holds the task name, an energy picker (one to
 * three sardines) and date / repeat chips; the Add button sits below it. Suggested tasks follow,
 * with category chips on top and the tasks as a wrapping cloud of small pills.
 *
 * @param takeItEasyDay a day to show a gentle "take-it-easy day" note for while it's the chosen date, or null.
 *   It's only a note: Add stays enabled.
 * @param suggestions category name -> suggested tasks. Empty until the suggestions database exists;
 *   the Suggestions section only appears when there is something to show.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    initialDay: Long,
    onAdd: (text: String, day: Long, repeatDaily: Boolean, spoonCost: Int, priority: Boolean) -> Unit,
    onDismiss: () -> Unit,
    takeItEasyDay: Long? = null,
    suggestions: Map<String, List<TaskSuggestion>> = emptyMap(),
    initial: ChecklistItem? = null,
) {
    // With [initial] set this edits that task instead of adding a new one: the form starts filled in,
    // and the title and button say "Edit task" and "Save".
    var text by rememberSaveable { mutableStateOf(initial?.text ?: "") }
    var day by rememberSaveable { mutableStateOf(initialDay) }
    var repeatDaily by rememberSaveable { mutableStateOf(initial?.repeatDaily ?: false) }
    var priority by rememberSaveable { mutableStateOf(initial?.priority ?: false) }
    var spoonCost by rememberSaveable { mutableStateOf(initial?.spoonCost ?: ChecklistItem.MIN_SPOONS) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val todayDay = remember { LocalDate.now().toEpochDay() }
    val canAdd = text.isNotBlank()
    val shownCategory = category?.takeIf { it in suggestions } ?: suggestions.keys.firstOrNull()
    val colors = MaterialTheme.colorScheme

    if (showPicker) {
        // The date picker works in UTC milliseconds, which map straight onto epoch days.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = day * MILLIS_PER_DAY)
        BrambleDialog(
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
            text = { DatePicker(state = pickerState) },
            fullWidthText = true, // the date picker has its own side padding
            properties = DialogProperties(usePlatformDefaultWidth = false), // it's a fixed 360dp wide
        )
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
                        stringResource(if (initial != null) R.string.task_edit else R.string.checklist_add_task),
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
                            keyboardActions = KeyboardActions(onDone = { if (canAdd) onAdd(text, day, repeatDaily, spoonCost, priority) }),
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
                                label = { Text(stringResource(R.string.task_daily_task)) }, // same label either way; the chip's fill shows if it's on
                                leadingIcon = { Icon(painterResource(R.drawable.ic_repeat), contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                            FilterChip(
                                selected = priority,
                                onClick = { priority = !priority },
                                label = { Text(stringResource(R.string.task_priority)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_priority), contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                        }
                        // A gentle note when the chosen day is a take-it-easy day. Adding is still allowed (with a warning).
                        if (day == takeItEasyDay) {
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
                    onClick = { onAdd(text, day, repeatDaily, spoonCost, priority) },
                    enabled = canAdd,
                    shape = RoundedCornerShape(50),
                    // On a take-it-easy day the button is toned down: a faint fill with green text, so it's still readable.
                    colors = if (day == takeItEasyDay) {
                        ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.35f), contentColor = colors.primary)
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
                ) {
                    Text(stringResource(if (initial != null) R.string.task_save else R.string.checklist_add))
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
