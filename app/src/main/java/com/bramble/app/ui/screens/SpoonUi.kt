package com.bramble.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bramble.app.R
import com.bramble.app.data.ChecklistItem
import com.bramble.app.data.CompletionCheck
import com.bramble.app.data.Spoons

private val MoodFaces = listOf(">:((", ":(", ":/", ":)", ":))") // moods 1..5

/** One-tap mood picker. Calls [onPick] with 1 (>:(() to 5 (:))). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDialog(
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    @StringRes subtitle: Int = R.string.mood_subtitle,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mood_title)) },
        text = {
            Column {
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    MoodFaces.forEachIndexed { index, face ->
                        Surface(
                            onClick = { onPick(index + 1) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                face,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_not_now)) } },
    )
}

/** Lets the user set today's spoon budget, or the budget they usually start each day with. */
@Composable
fun BudgetDialog(
    current: Int,
    onSaveToday: (Int) -> Unit,
    onSaveEveryDay: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { value = (value - 1).coerceAtLeast(Spoons.MIN_BUDGET) },
                    ) { Text("−") }
                    Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
                    OutlinedButton(
                        onClick = { value = (value + 1).coerceAtMost(Spoons.MAX_BUDGET) },
                    ) { Text("+") }
                }
                TextButton(onClick = { onSaveEveryDay(value) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.budget_every_day))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSaveToday(value) }) { Text(stringResource(R.string.budget_today)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.checklist_cancel)) } },
    )
}

private sealed interface Pending {
    data class AskMood(val item: ChecklistItem) : Pending
    data class AddSpoons(val item: ChecklistItem, val step: Int) : Pending
    data class Borrow(val item: ChecklistItem, val borrow: CompletionCheck.MustBorrow) : Pending
}

/**
 * Returns the function to call when a task's checkbox is tapped, and shows whatever dialog it needs.
 *
 * Unchecking, or checking a task on a day other than [today], just toggles it. Checking a task today
 * that costs more than the spoons left goes through the spoon rules:
 * no mood yet, ask; feeling okay or better, offer to add spoons; otherwise offer to borrow.
 * Saying "not now" to any prompt leaves the task unchecked. Nothing here ever blocks or scolds.
 */
@Composable
fun rememberToggleHandler(viewModel: TasksViewModel, today: Long): (ChecklistItem, Long) -> Unit {
    var pending by remember { mutableStateOf<Pending?>(null) }

    // Runs the rules for an unchecked task. May set `pending` to ask something first.
    fun process(item: ChecklistItem) {
        when (val check = viewModel.checkCompletion(item, today)) {
            CompletionCheck.Fits -> viewModel.toggle(item, today)
            is CompletionCheck.NeedsMood -> pending = Pending.AskMood(item)
            is CompletionCheck.CanAddSpoons -> pending = Pending.AddSpoons(item, check.step)
            is CompletionCheck.MustBorrow -> pending = Pending.Borrow(item, check)
        }
    }

    when (val p = pending) {
        is Pending.AskMood -> MoodDialog(
            subtitle = R.string.mood_subtitle_over,
            onPick = { mood ->
                viewModel.logMood(today, mood)
                pending = null
                process(p.item) // now that there's a mood, decide again
            },
            onDismiss = { pending = null },
        )

        is Pending.AddSpoons -> AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.add_spoons_title)) },
            text = { Text(stringResource(R.string.add_spoons_message, p.step)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addExtraSpoons(today, p.step)
                    pending = null
                    process(p.item) // may still be short, e.g. after a small add on a :/ day
                }) { Text(stringResource(R.string.add_spoons_confirm, p.step)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.dialog_not_now)) } },
        )

        is Pending.Borrow -> AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.borrow_title)) },
            text = { Text(stringResource(R.string.borrow_message, p.borrow.payback)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrowAndComplete(p.item, today, p.borrow)
                    pending = null
                }) { Text(stringResource(R.string.borrow_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.dialog_not_now)) } },
        )

        null -> Unit
    }

    return { item, day ->
        if (day != today || item.isDoneOn(day)) viewModel.toggle(item, day) else process(item)
    }
}
