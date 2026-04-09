package dev.spark.data

import dev.spark.domain.DopamineBudget
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository for persisting and querying daily [DopamineBudget] records.
 */
interface BudgetRepository {

    /**
     * Observe all budget records, newest date first.
     * Emits whenever the underlying data changes.
     */
    fun observeAll(): Flow<List<DopamineBudget>>

    /**
     * Return all stored budget records.
     */
    suspend fun getAll(): List<DopamineBudget>

    /**
     * Return the budget for [date], or null if no record exists yet.
     */
    suspend fun getByDate(date: LocalDate): DopamineBudget?

    /**
     * Observe the budget for a specific [date] in real-time.
     */
    fun observeByDate(date: LocalDate): Flow<DopamineBudget?>

    /**
     * Return budgets for dates within [[from], [to]] inclusive, ordered ascending.
     */
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<DopamineBudget>

    /**
     * Return the [limit] most recent budget records.
     */
    suspend fun getRecent(limit: Long): List<DopamineBudget>

    /**
     * Persist a new budget record for a date that does not yet exist.
     */
    suspend fun insert(budget: DopamineBudget)

    /**
     * Replace all fields of an existing budget record identified by its date.
     */
    suspend fun update(budget: DopamineBudget)

    /**
     * Insert or replace the budget for its date (upsert semantics).
     */
    suspend fun upsert(budget: DopamineBudget)

    /**
     * Atomically add [amount] to [fpEarned] for the given [date].
     * If no record exists for [date], a new one is created with defaults.
     */
    suspend fun incrementFpEarned(date: LocalDate, amount: Int)

    /**
     * Atomically add [amount] to [fpSpent] for the given [date].
     * If no record exists for [date], a new one is created with defaults.
     */
    suspend fun incrementFpSpent(date: LocalDate, amount: Int)

    /**
     * Atomically add [amount] to [fpBonus] for the given [date].
     * If no record exists for [date], a new one is created with defaults.
     */
    suspend fun incrementFpBonus(date: LocalDate, amount: Int)

    /**
     * Remove the budget record for [date].
     */
    suspend fun deleteByDate(date: LocalDate)

    /**
     * Return the sum of [DopamineBudget.fpEarned] over all dates in [[from], [to]].
     */
    suspend fun sumFpEarned(from: LocalDate, to: LocalDate): Long
}
