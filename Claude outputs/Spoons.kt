package com.bramble.app.data

/**
 * Spoon and mood rules. Everything here is plain Kotlin with no Android code, so it's easy to test.
 *
 * Mood scale: 1 = >:((, 2 = :(, 3 = :/, 4 = :), 5 = :))
 */

/** One mood check-in. */
data class MoodEntry(val value: Int, val atMillis: Long)

/**
 * What the user set for one day. Days without a record use the usual budget and have no check-ins.
 *
 * @property extraSpoons spoons added on a good day. They're kept even if mood drops later.
 * @property moodPrompted the automatic "How are you feeling?" pop-up was already shown today.
 * @property pacingNoteSeen the "you did a lot yesterday" note was already shown today.
 */
data class DayRecord(
    val day: Long,
    val baseBudget: Int,
    val extraSpoons: Int = 0,
    val moods: List<MoodEntry> = emptyList(),
    val moodPrompted: Boolean = false,
    val pacingNoteSeen: Boolean = false,
) {
    /** The latest check-in decides the day's mood. */
    val latestMood: Int? get() = moods.maxByOrNull { it.atMillis }?.value

    /** Logging the lowest mood makes the rest of the day a take-it-easy day. */
    val isTakeItEasy: Boolean get() = moods.any { it.value == Spoons.LOWEST_MOOD }
}

/** Spoons taken from one later day to pay for a borrow. */
data class LoanPart(val lendDay: Long, val spoons: Int)

/**
 * A task finished on [borrowDay] with spoons the day didn't have. The day gets [shortfall] spoons;
 * later days pay back [shortfall] + 1, spread over [parts] (normally just tomorrow).
 */
data class SpoonLoan(val taskId: Long, val borrowDay: Long, val shortfall: Int, val parts: List<LoanPart>) {
    val payback: Int get() = parts.sumOf { it.spoons }
}

/** Everything spoon-related that gets saved, apart from the tasks themselves. */
data class SpoonData(
    val usualBudget: Int = Spoons.DEFAULT_BUDGET,
    val days: List<DayRecord> = emptyList(),
    val loans: List<SpoonLoan> = emptyList(),
)

/** What should happen when a task is checked off today. */
sealed interface CompletionCheck {
    /** Enough spoons: just complete it. */
    data object Fits : CompletionCheck

    /** No check-in yet today, so ask how they're feeling before deciding. */
    data class NeedsMood(val shortfall: Int) : CompletionCheck

    /** Feeling okay or better: offer to add [step] extra spoons. */
    data class CanAddSpoons(val shortfall: Int, val step: Int) : CompletionCheck

    /** Low mood, or the day's extra spoons are used up: offer to borrow. */
    data class MustBorrow(val shortfall: Int, val parts: List<LoanPart>) : CompletionCheck {
        val payback: Int get() = parts.sumOf { it.spoons }
    }
}

/** A day's spoon numbers, ready to display. */
data class SpoonSummary(
    val budget: Int, // base + extra - lent to earlier days
    val spent: Int,
    val borrowed: Int, // spoons borrowed by this day
    val lent: Int, // spoons this day is paying back
    val extra: Int,
) {
    val left: Int get() = budget - spent + borrowed
}

object Spoons {
    const val DEFAULT_BUDGET = 12
    const val MIN_BUDGET = 1
    const val MAX_BUDGET = 40
    const val LOWEST_MOOD = 1

    /** The +1 for pushing past what you had. Added once per borrowed task. */
    const val BORROW_EXTRA = 1

    /** How far ahead a payback can spread if the next days are already low. */
    private const val MAX_LEND_DAYS = 60

    /** Total extra spoons a day can add, based on mood. */
    fun extraAllowance(mood: Int?): Int = when (mood) {
        4, 5 -> 8
        3 -> 2
        else -> 0
    }

    /** How many extra spoons one "add" adds: sets of 4 on a good day, 2 on a :/ day. */
    fun extraStep(mood: Int?): Int = when (mood) {
        4, 5 -> 4
        3 -> 2
        else -> 0
    }

    /** Read-only view of the saved spoon state plus tasks, with the calculations on top. */
    class Ledger(
        private val usualBudget: Int,
        private val days: Map<Long, DayRecord>,
        private val loans: List<SpoonLoan>,
        private val items: List<ChecklistItem>,
    ) {
        fun record(day: Long): DayRecord = days[day] ?: DayRecord(day, usualBudget)

        fun lentBy(day: Long): Int = loans.sumOf { loan -> loan.parts.filter { it.lendDay == day }.sumOf { it.spoons } }

        fun borrowedBy(day: Long): Int = loans.filter { it.borrowDay == day }.sumOf { it.shortfall }

        fun spentOn(day: Long): Int = items.filter { it.appearsOn(day) && it.isDoneOn(day) }.sumOf { it.spoonCost }

        fun summary(day: Long): SpoonSummary {
            val record = record(day)
            val lent = lentBy(day)
            return SpoonSummary(
                budget = record.baseBudget + record.extraSpoons - lent,
                spent = spentOn(day),
                borrowed = borrowedBy(day),
                lent = lent,
                extra = record.extraSpoons,
            )
        }

        /** Decides what happens when a task costing [cost] is checked off on [today]. */
        fun checkCompletion(today: Long, cost: Int): CompletionCheck {
            val left = summary(today).left.coerceAtLeast(0)
            // Use what's left first; only the rest is a shortfall.
            val shortfall = cost - left
            if (shortfall <= 0) return CompletionCheck.Fits

            val record = record(today)
            val mood = record.latestMood ?: return CompletionCheck.NeedsMood(shortfall)
            val allowanceLeft = extraAllowance(mood) - record.extraSpoons
            if (allowanceLeft > 0) {
                return CompletionCheck.CanAddSpoons(shortfall, minOf(extraStep(mood), allowanceLeft))
            }
            return CompletionCheck.MustBorrow(shortfall, planPayback(today, shortfall + BORROW_EXTRA))
        }

        /**
         * Spreads [amount] over the days after [today], nearest first, never taking a day below zero.
         * The +1 is already in [amount], so it's never charged twice when the payback spills over.
         */
        fun planPayback(today: Long, amount: Int): List<LoanPart> {
            val parts = mutableListOf<LoanPart>()
            var remaining = amount
            var day = today + 1
            while (remaining > 0 && day <= today + MAX_LEND_DAYS) {
                val available = summary(day).left.coerceAtLeast(0)
                val take = minOf(available, remaining)
                if (take > 0) {
                    parts += LoanPart(day, take)
                    remaining -= take
                }
                day++
            }
            // Only reachable if every day for two months is empty; put the rest on tomorrow.
            if (remaining > 0) parts += LoanPart(today + 1, remaining)
            return parts.groupBy { it.lendDay }.map { (d, p) -> LoanPart(d, p.sumOf { it.spoons }) }
        }

        /** True if extra spoons were added on [day]. The next day uses this to show the pacing note. */
        fun addedExtraOn(day: Long): Boolean = record(day).extraSpoons > 0
    }
}
