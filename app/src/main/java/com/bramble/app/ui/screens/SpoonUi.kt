package com.bramble.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bramble.app.ui.theme.AmethystSmoke
import com.bramble.app.ui.theme.BrambleDialog
import com.bramble.app.ui.theme.LavenderVeil
import com.bramble.app.ui.theme.MidnightViolet
import com.bramble.app.ui.theme.SageGreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bramble.app.data.ChecklistItem
import com.bramble.app.data.CompletionCheck
import com.bramble.app.data.Spoons

internal val MoodFaces = listOf(">:((", ":(", ":/", ":)", ":))") // moods 1..5, left to right

// Colors behind the faces. Purple is Amethyst Smoke, lightened a little so the dark face text stays
// readable; yellow is the one color outside the palette.
private val MoodPurple = lerp(AmethystSmoke, LavenderVeil, 0.25f)
private val MoodYellow = Color(0xFFF2D98A)
private val MoodGreen = SageGreen

// The five box colors, one per mood: the three colors plus the midpoint between each neighboring pair.
internal val MoodColors = listOf(
    MoodPurple,
    lerp(MoodPurple, MoodYellow, 0.25f),
    MoodYellow,
    lerp(MoodYellow, MoodGreen, 0.5f),
    MoodGreen,
)

/** The color for an average mood between 1 and 5, blended smoothly between the five box colors. */
internal fun moodColorFor(average: Float): Color {
    val position = (average - 1f).coerceIn(0f, 4f)
    val lower = position.toInt().coerceAtMost(MoodColors.lastIndex - 1)
    return lerp(MoodColors[lower], MoodColors[lower + 1], position - lower)
}

/** One-tap mood picker. Calls [onPick] with 1 (>:(() to 5 (:))). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDialog(
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    @StringRes subtitle: Int = R.string.mood_subtitle,
) {
    BrambleDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mood_title)) },
        text = {
            Column {
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodyMedium)
                // One colored box per face, stepping from purple (low mood) through yellow to green (good mood).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    MoodFaces.forEachIndexed { index, face ->
                        Surface(
                            onClick = { onPick(index + 1) },
                            shape = RoundedCornerShape(12.dp),
                            color = MoodColors[index],
                            contentColor = MidnightViolet, // dark text on the pastel colors, in light and dark mode
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
    BrambleDialog(
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

        is Pending.AddSpoons -> BrambleDialog(
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

        is Pending.Borrow -> BrambleDialog(
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
