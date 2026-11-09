package com.bramble.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bramble.app.R
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.ExperimentalMaterial3Api
import com.bramble.app.ui.theme.BrambleCorner
import java.time.format.FormatStyle
import java.time.ZoneId
import java.time.Instant
import com.bramble.app.ui.theme.MidnightViolet
import com.bramble.app.data.MoodEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.offset
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val DaysOfWeek = List(7) { DayOfWeek.SUNDAY.plus(it.toLong()) }
private val SundayOnOrBefore = TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)

// The pager has a fixed number of pages with "today" in the middle, which lets you
// swipe about 19 years in either direction (weeks) or 83 years (months).
private const val PAGE_COUNT = 2001
private const val TODAY_PAGE = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: TasksViewModel = viewModel()) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val thisSunday = remember { today.with(SundayOnOrBefore) }
    val thisMonth = remember { YearMonth.from(today) }
    // Checking off a task today can ask about spoons first (mood, adding spoons, borrowing).
    val handleToggle = rememberToggleHandler(viewModel, today.toEpochDay())

    // Dates are saved as epoch-day numbers because rememberSaveable can store a Long directly.
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    // A date inside the page being shown; lets us stay on the same period when toggling views.
    var focusEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    // When on, the list under the calendar is the day's mood log instead of its tasks.
    var showMoods by rememberSaveable { mutableStateOf(false) }
    val selected = LocalDate.ofEpochDay(selectedEpochDay)
    // Average mood (1 to 5) for each day that has check-ins; the days are colored with it while the mood log is on.
    val averageMoods: Map<Long, Float> = if (showMoods) {
        viewModel.spoonData.days
            .filter { it.moods.isNotEmpty() }
            .associate { record -> record.day to record.moods.map { it.value }.average().toFloat() }
    } else {
        emptyMap()
    }

    fun pageFor(date: LocalDate, month: Boolean): Int {
        val offset = if (month) {
            ChronoUnit.MONTHS.between(thisMonth, YearMonth.from(date))
        } else {
            ChronoUnit.WEEKS.between(thisSunday, date.with(SundayOnOrBefore))
        }
        return (TODAY_PAGE + offset).toInt().coerceIn(0, PAGE_COUNT - 1)
    }

    // The date that represents a page: the 15th for a month, the Wednesday for a week
    // (the middle of the week decides which month a week belongs to).
    fun focusFor(page: Int, month: Boolean): LocalDate {
        val offset = (page - TODAY_PAGE).toLong()
        return if (month) thisMonth.plusMonths(offset).atDay(15) else thisSunday.plusWeeks(offset).plusDays(3)
    }

    if (showAddDialog) {
        // New tasks start on the day you're looking at.
        AddTaskFlow(viewModel, initialDay = selected.toEpochDay(), onClose = { showAddDialog = false })
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // The strip beside the menu button (which BrambleApp draws in the top-left corner). The mood
        // log toggle sits at the right end, lined up with the mood button on Home.
        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            // A toggle with a rounded-square highlight (the stock icon toggle can only be a circle).
            Surface(
                checked = showMoods,
                onCheckedChange = { showMoods = it },
                shape = BrambleCorner,
                color = if (showMoods) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (showMoods) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.offset(x = 12.dp).size(48.dp), // the column has 16dp side padding; Home's icon sits 4dp from the edge
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_smiley),
                        contentDescription = stringResource(if (showMoods) R.string.calendar_show_tasks else R.string.calendar_show_moods),
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().animateContentSize(),
        ) {
            // Switching between week and month builds a fresh pager starting on the same period.
            key(expanded) {
                val pagerState = rememberPagerState(initialPage = pageFor(LocalDate.ofEpochDay(focusEpochDay), expanded)) {
                    PAGE_COUNT
                }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.collect { page ->
                        focusEpochDay = focusFor(page, expanded).toEpochDay()
                    }
                }

                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            // currentPage updates live while swiping, so the title follows your finger.
                            focusFor(pagerState.currentPage, expanded).format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                        )
                        IconButton(onClick = {
                            if (expanded) {
                                // Collapsing: go to the selected week if it's in the month being viewed.
                                val viewed = focusFor(pagerState.currentPage, true)
                                focusEpochDay = (if (YearMonth.from(selected) == YearMonth.from(viewed)) selected else viewed.withDayOfMonth(1)).toEpochDay()
                            } else {
                                focusEpochDay = focusFor(pagerState.currentPage, false).toEpochDay()
                            }
                            expanded = !expanded
                        }) {
                            Icon(
                                painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                                stringResource(if (expanded) R.string.calendar_collapse else R.string.calendar_expand),
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        DaysOfWeek.forEach { day ->
                            Text(
                                day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            )
                        }
                    }

                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                        val offset = (page - TODAY_PAGE).toLong()
                        val month = thisMonth.plusMonths(offset)
                        // Month pages always show 6 rows so the card doesn't change height while swiping.
                        val firstShown = if (expanded) month.atDay(1).with(SundayOnOrBefore) else thisSunday.plusWeeks(offset)
                        val weekCount = if (expanded) 6 else 1

                        Column {
                            repeat(weekCount) { week ->
                                Row(Modifier.fillMaxWidth()) {
                                    repeat(7) { dayIndex ->
                                        val date = firstShown.plusDays((week * 7 + dayIndex).toLong())
                                        // Dots only count one-off tasks; daily tasks are on every day so they'd mark them all.
                                        val dotTasks = viewModel.items.filter { it.appearsOn(date.toEpochDay()) && !it.repeatDaily }
                                        DayCell(
                                            date = date,
                                            isSelected = date == selected,
                                            isToday = date == today,
                                            inCurrentMonth = !expanded || YearMonth.from(date) == month,
                                            hasTasks = !showMoods && dotTasks.isNotEmpty(), // no task dots in mood view
                                            allDone = dotTasks.all { it.isDoneOn(date.toEpochDay()) },
                                            moodAverage = averageMoods[date.toEpochDay()],
                                            onClick = { selectedEpochDay = date.toEpochDay() },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            selected.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )

        val selectedDay = selected.toEpochDay()
        if (showMoods) {
            // Mood log: every check-in logged on this day, earliest first.
            val moods = viewModel.record(selectedDay).moods.sortedBy { it.atMillis }
            if (moods.isEmpty()) {
                Text(
                    stringResource(R.string.calendar_no_moods),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(moods) { entry -> MoodLogRow(entry) }
                }
            }
            return@Column
        }

        val dayTasks = viewModel.items.filter { it.appearsOn(selectedDay) }.sortedForDay(selectedDay) // priority, then daily, then standard; done tasks at the bottom
        if (dayTasks.isEmpty()) {
            Text(
                stringResource(R.string.calendar_no_tasks),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(top = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(dayTasks, key = { it.id }) { task ->
                    TaskItem(item = task, day = selectedDay, viewModel = viewModel, onToggle = { handleToggle(task, selectedDay) })
                }
            }
        }

        Button(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(12.dp), // matches the task cards
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Text(stringResource(R.string.checklist_add_task))
        }
    }
}

/** One mood check-in in the log: a task-style card with the face in its mood color and the time it was logged. */
@Composable
private fun MoodLogRow(entry: MoodEntry) {
    val time = remember(entry.atMillis) {
        Instant.ofEpochMilli(entry.atMillis).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) // follows the phone's 12/24-hour setting
    }
    val index = (entry.value - 1).coerceIn(0, MoodFaces.lastIndex)
    // The emoticons are punctuation, so give screen readers a plain description instead.
    val description = stringResource(R.string.mood_log_description, entry.value, time)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MoodColors[index], contentColor = MidnightViolet) {
                Text(
                    MoodFaces[index],
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Text(
                time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    inCurrentMonth: Boolean,
    hasTasks: Boolean,
    allDone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    moodAverage: Float? = null, // set only while the mood log is on, for days that have check-ins
) {
    val colors = MaterialTheme.colorScheme
    val moodColor = moodAverage?.let { moodColorFor(it) }
    val moodDescription = moodAverage?.let { stringResource(R.string.mood_average_description, it) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(BrambleCorner)
            .background(
                when {
                    moodColor != null -> moodColor.copy(alpha = if (inCurrentMonth) 1f else 0.5f)
                    isSelected -> colors.primary
                    else -> Color.Transparent
                },
            )
            .then(
                when {
                    moodColor != null && isSelected -> Modifier.border(2.dp, colors.primary, BrambleCorner) // keep the selection visible on a colored day
                    isToday && !isSelected -> Modifier.border(1.5.dp, colors.primary, BrambleCorner)
                    else -> Modifier
                },
            )
            .semantics { if (moodDescription != null) stateDescription = moodDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                moodColor != null -> MidnightViolet.copy(alpha = if (inCurrentMonth) 1f else 0.6f) // dark text on the pastel colors
                isSelected -> colors.onPrimary
                !inCurrentMonth -> colors.onSurface.copy(alpha = 0.38f)
                else -> colors.onSurface
            },
        )
        // Dot under the number: green when everything that day is done, gray otherwise.
        if (hasTasks) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .background(
                        when {
                            isSelected -> colors.onPrimary
                            allDone -> colors.primary
                            else -> colors.onSurface.copy(alpha = 0.4f)
                        },
                        CircleShape,
                    ),
            )
        }
    }
}
