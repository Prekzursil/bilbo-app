package dev.spark.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.spark.app.BuildConfig
import dev.spark.data.AppProfileRepository
import dev.spark.data.BudgetRepository
import dev.spark.data.UsageRepository
import dev.spark.economy.AppClassifier
import dev.spark.economy.BudgetEnforcer
import dev.spark.economy.FocusPointsEngine
import dev.spark.enforcement.CooldownManager
import dev.spark.enforcement.CooldownPersistence
import dev.spark.intelligence.DecisionEngine
import dev.spark.intelligence.tier1.RuleEngine
import dev.spark.intelligence.tier2.HeuristicEngine
import dev.spark.intelligence.tier3.CloudInsightClient
import dev.spark.intelligence.tier3.InsightPromptBuilder
import dev.spark.tracking.SessionTracker
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Singleton

/**
 * Provides engine, manager, and tracker singletons that depend on the
 * repositories and platform types bound in [RepositoryModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    // ── Economy layer ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFocusPointsEngine(): FocusPointsEngine = FocusPointsEngine()

    @Provides
    @Singleton
    fun provideBudgetEnforcer(
        fpEngine: FocusPointsEngine,
    ): BudgetEnforcer = BudgetEnforcer(fpEngine)

    @Provides
    @Singleton
    fun provideAppClassifier(): AppClassifier = AppClassifier.fromDefaults(emptyList())

    // ── Intelligence — Tier 2 ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideHeuristicEngine(): HeuristicEngine = HeuristicEngine()

    // ── Intelligence — Tier 3 ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideInsightPromptBuilder(): InsightPromptBuilder = InsightPromptBuilder()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android)

    @Provides
    @Singleton
    fun provideCloudInsightClient(
        httpClient: HttpClient,
        promptBuilder: InsightPromptBuilder,
    ): CloudInsightClient = CloudInsightClient(
        httpClient = httpClient,
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        promptBuilder = promptBuilder,
    )

    // ── Intelligence — Tier 1 ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideRuleEngine(
        appProfileRepository: AppProfileRepository,
        budgetRepository: BudgetRepository,
        cooldownManager: CooldownManager,
    ): RuleEngine = RuleEngine(
        appProfileProvider = { packageName ->
            runBlocking { appProfileRepository.getByPackageName(packageName) }
        },
        budgetProvider = {
            runBlocking {
                budgetRepository.getByDate(
                    Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                )
            } ?: dev.spark.domain.DopamineBudget(
                date = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date,
                fpEarned = 0,
                fpSpent = 0,
                fpBonus = 0,
                fpRolloverIn = 0,
                fpRolloverOut = 0,
                nutritiveMinutes = 0,
                emptyCalorieMinutes = 0,
                neutralMinutes = 0,
            )
        },
        cooldownChecker = { packageName ->
            cooldownManager.getRemainingMinutes(packageName)
        },
    )

    // ── Intelligence — Orchestrator ──────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDecisionEngine(
        appProfileRepository: AppProfileRepository,
        budgetRepository: BudgetRepository,
        cooldownManager: CooldownManager,
        cloudInsightClient: CloudInsightClient,
        ruleEngine: RuleEngine,
        heuristicEngine: HeuristicEngine,
        promptBuilder: InsightPromptBuilder,
    ): DecisionEngine {
        val appProfileProvider: (String) -> dev.spark.domain.AppProfile? = { packageName ->
            runBlocking { appProfileRepository.getByPackageName(packageName) }
        }
        val budgetProvider: () -> dev.spark.domain.DopamineBudget = {
            runBlocking {
                budgetRepository.getByDate(
                    Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                )
            } ?: dev.spark.domain.DopamineBudget(
                date = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date,
                fpEarned = 0,
                fpSpent = 0,
                fpBonus = 0,
                fpRolloverIn = 0,
                fpRolloverOut = 0,
                nutritiveMinutes = 0,
                emptyCalorieMinutes = 0,
                neutralMinutes = 0,
            )
        }
        val cooldownChecker: (String) -> Int? = { packageName ->
            cooldownManager.getRemainingMinutes(packageName)
        }

        return DecisionEngine(
            appProfileProvider = appProfileProvider,
            budgetProvider = budgetProvider,
            cooldownChecker = cooldownChecker,
            cloudInsightClient = cloudInsightClient,
            anonymousUserId = UUID.randomUUID().toString(),
            ruleEngine = ruleEngine,
            heuristicEngine = heuristicEngine,
            promptBuilder = promptBuilder,
        )
    }

    // ── Enforcement ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideCooldownManager(
        persistence: CooldownPersistence,
    ): CooldownManager = CooldownManager(persistence)

    // ── Tracking ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSessionTracker(
        usageRepository: UsageRepository,
        appProfileRepository: AppProfileRepository,
    ): SessionTracker = SessionTracker(usageRepository, appProfileRepository)
}
