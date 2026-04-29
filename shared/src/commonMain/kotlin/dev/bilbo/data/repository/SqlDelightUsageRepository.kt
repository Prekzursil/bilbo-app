package dev.bilbo.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.bilbo.data.BilboDatabase
import dev.bilbo.data.UsageRepository
import dev.bilbo.data.Usage_sessions
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.UsageSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * SQLDelight-backed implementation of [UsageRepository].
 *
 * Persists [UsageSession] records to the platform's SQLite database so they
 * survive process restart and device reboot — replacing the in-memory
 * [MutableStateFlow] stub previously wired in
 * `androidApp/src/main/kotlin/dev/bilbo/app/di/RepositoryModule.kt`.
 *
 * Reads return [Flow]s backed by SQLDelight's `asFlow()` coroutine extension
 * so collectors observe live updates after every insert / update / delete.
 *
 * Type mapping follows the conventions documented at the top of
 * `BilboDatabase.sq`:
 *   - [Instant]  ↔ ISO-8601 [String]
 *   - [Boolean]  ↔ [Long] (0 = false, 1 = true)
 *   - [AppCategory] ↔ enum [String] name
 *
 * @property database  The shared SQLDelight database instance.
 * @property ioDispatcher  Background dispatcher for blocking SQLite calls.
 */
class SqlDelightUsageRepository(
    private val database: BilboDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : UsageRepository {

    private val queries get() = database.bilboDatabaseQueries

    override fun observeAll(): Flow<List<UsageSession>> =
        queries.selectAllUsageSessions()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map(::toDomain) }

    override suspend fun getAll(): List<UsageSession> = withContext(ioDispatcher) {
        queries.selectAllUsageSessions().executeAsList().map(::toDomain)
    }

    override suspend fun getById(id: Long): UsageSession? = withContext(ioDispatcher) {
        queries.selectUsageSessionById(id).executeAsOneOrNull()?.let(::toDomain)
    }

    override suspend fun getByPackageName(packageName: String): List<UsageSession> =
        withContext(ioDispatcher) {
            queries.selectUsageSessionsByPackageName(packageName).executeAsList().map(::toDomain)
        }

    override suspend fun getByDateRange(from: Instant, to: Instant): List<UsageSession> =
        withContext(ioDispatcher) {
            queries.selectUsageSessionsByDateRange(from.toString(), to.toString())
                .executeAsList()
                .map(::toDomain)
        }

    override suspend fun getByCategory(category: AppCategory): List<UsageSession> =
        withContext(ioDispatcher) {
            queries.selectUsageSessionsByCategory(category.name).executeAsList().map(::toDomain)
        }

    override suspend fun getByDateRangeAndCategory(
        from: Instant,
        to: Instant,
        category: AppCategory,
    ): List<UsageSession> = withContext(ioDispatcher) {
        queries.selectUsageSessionsByDateRangeAndCategory(
            from = from.toString(),
            to = to.toString(),
            category = category.name,
        ).executeAsList().map(::toDomain)
    }

    override suspend fun insert(session: UsageSession): Long = withContext(ioDispatcher) {
        database.transactionWithResult {
            queries.insertUsageSession(
                package_name = session.packageName,
                app_label = session.appLabel,
                category = session.category.name,
                start_time = session.startTime.toString(),
                end_time = session.endTime?.toString(),
                duration_seconds = session.durationSeconds,
                was_tracked = if (session.wasTracked) 1L else 0L,
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun updateEndTime(id: Long, endTime: Instant, durationSeconds: Long) {
        withContext(ioDispatcher) {
            queries.updateUsageSessionEndTime(
                end_time = endTime.toString(),
                duration_seconds = durationSeconds,
                id = id,
            )
        }
    }

    override suspend fun deleteById(id: Long) {
        withContext(ioDispatcher) { queries.deleteUsageSessionById(id) }
    }

    override suspend fun deleteOlderThan(before: Instant) {
        withContext(ioDispatcher) {
            queries.deleteUsageSessionsOlderThan(before.toString())
        }
    }

    override suspend fun countByPackageName(packageName: String): Long = withContext(ioDispatcher) {
        queries.countUsageSessionsByPackageName(packageName).executeAsOne()
    }

    override suspend fun sumDurationByCategory(
        from: Instant,
        to: Instant,
    ): Map<AppCategory, Long> = withContext(ioDispatcher) {
        queries.sumDurationByCategory(from.toString(), to.toString())
            .executeAsList()
            .associate { row ->
                AppCategory.valueOf(row.category) to (row.total_seconds ?: 0L)
            }
    }

    private fun toDomain(row: Usage_sessions): UsageSession = UsageSession(
        id = row.id,
        packageName = row.package_name,
        appLabel = row.app_label,
        category = AppCategory.valueOf(row.category),
        startTime = Instant.parse(row.start_time),
        endTime = row.end_time?.let { Instant.parse(it) },
        durationSeconds = row.duration_seconds,
        wasTracked = row.was_tracked == 1L,
    )
}
