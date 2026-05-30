package dev.bilbo.app.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.bilbo.app.enforcement.SharedPrefsCooldownPersistence
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.economy.AppClassifier
import dev.bilbo.economy.BudgetEnforcer
import dev.bilbo.economy.FocusPointsEngine
import dev.bilbo.enforcement.CooldownManager
import dev.bilbo.intelligence.DecisionEngine
import dev.bilbo.intelligence.tier1.RuleEngine
import dev.bilbo.intelligence.tier2.HeuristicEngine
import dev.bilbo.intelligence.tier3.CloudInsightClient
import dev.bilbo.intelligence.tier3.InsightPromptBuilder
import dev.bilbo.tracking.SessionTracker
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [EngineModule].
 *
 * Calls each @Provides function directly with real or mock arguments so we
 * cover the factory code paths without starting a Hilt component.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EngineModuleTest {

    private lateinit var context: Context

    // In-memory repository mocks
    private val appProfileRepository = mockk<AppProfileRepository>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val usageRepository = mockk<UsageRepository>(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        coEvery { budgetRepository.getByDate(any()) } returns null
    }

    // ── Economy layer ─────────────────────────────────────────────────────────

    @Test
    fun `provideFocusPointsEngine returns non-null FocusPointsEngine`() {
        val engine = EngineModule.provideFocusPointsEngine()
        assertNotNull(engine)
        assertIs<FocusPointsEngine>(engine)
    }

    @Test
    fun `provideBudgetEnforcer returns non-null BudgetEnforcer`() {
        val fpEngine = EngineModule.provideFocusPointsEngine()
        val enforcer = EngineModule.provideBudgetEnforcer(fpEngine)
        assertNotNull(enforcer)
        assertIs<BudgetEnforcer>(enforcer)
    }

    @Test
    fun `provideAppClassifier returns non-null AppClassifier`() {
        val classifier = EngineModule.provideAppClassifier()
        assertNotNull(classifier)
        assertIs<AppClassifier>(classifier)
    }

    // ── Intelligence layers ───────────────────────────────────────────────────

    @Test
    fun `provideHeuristicEngine returns non-null HeuristicEngine`() {
        val engine = EngineModule.provideHeuristicEngine()
        assertNotNull(engine)
        assertIs<HeuristicEngine>(engine)
    }

    @Test
    fun `provideInsightPromptBuilder returns non-null InsightPromptBuilder`() {
        val builder = EngineModule.provideInsightPromptBuilder()
        assertNotNull(builder)
        assertIs<InsightPromptBuilder>(builder)
    }

    @Test
    fun `provideHttpClient returns non-null HttpClient`() {
        val client = EngineModule.provideHttpClient()
        assertNotNull(client)
        assertIs<HttpClient>(client)
        client.close()
    }

    @Test
    fun `provideCloudInsightClient returns non-null CloudInsightClient`() {
        val client = EngineModule.provideHttpClient()
        val promptBuilder = EngineModule.provideInsightPromptBuilder()
        val cloudClient = EngineModule.provideCloudInsightClient(client, promptBuilder)
        assertNotNull(cloudClient)
        assertIs<CloudInsightClient>(cloudClient)
        client.close()
    }

    // ── Enforcement / Tracking ────────────────────────────────────────────────

    @Test
    fun `provideCooldownManager returns non-null CooldownManager`() {
        val persistence = SharedPrefsCooldownPersistence(context)
        val manager = EngineModule.provideCooldownManager(persistence)
        assertNotNull(manager)
        assertIs<CooldownManager>(manager)
    }

    @Test
    fun `provideSessionTracker returns non-null SessionTracker`() {
        val tracker = EngineModule.provideSessionTracker(usageRepository, appProfileRepository)
        assertNotNull(tracker)
        assertIs<SessionTracker>(tracker)
    }

    // ── RuleEngine ────────────────────────────────────────────────────────────

    @Test
    fun `provideRuleEngine returns non-null RuleEngine`() {
        val persistence = SharedPrefsCooldownPersistence(context)
        val cooldownManager = EngineModule.provideCooldownManager(persistence)
        val ruleEngine = EngineModule.provideRuleEngine(
            appProfileRepository = appProfileRepository,
            budgetRepository = budgetRepository,
            cooldownManager = cooldownManager,
        )
        assertNotNull(ruleEngine)
        assertIs<RuleEngine>(ruleEngine)
    }

    // ── IntelligenceEngines bundle ────────────────────────────────────────────

    @Test
    fun `provideIntelligenceEngines bundles all three engine references`() {
        val ruleEngine = EngineModule.provideRuleEngine(
            appProfileRepository = appProfileRepository,
            budgetRepository = budgetRepository,
            cooldownManager = EngineModule.provideCooldownManager(SharedPrefsCooldownPersistence(context)),
        )
        val heuristicEngine = EngineModule.provideHeuristicEngine()
        val promptBuilder = EngineModule.provideInsightPromptBuilder()

        val bundle = EngineModule.provideIntelligenceEngines(ruleEngine, heuristicEngine, promptBuilder)
        assertNotNull(bundle)
        assertIs<EngineModule.IntelligenceEngines>(bundle)
    }

    // ── DecisionEngine ────────────────────────────────────────────────────────

    @Test
    fun `provideDecisionEngine returns non-null DecisionEngine`() {
        val persistence = SharedPrefsCooldownPersistence(context)
        val cooldownManager = EngineModule.provideCooldownManager(persistence)
        val httpClient = EngineModule.provideHttpClient()
        val promptBuilder = EngineModule.provideInsightPromptBuilder()
        val cloudClient = EngineModule.provideCloudInsightClient(httpClient, promptBuilder)
        val heuristicEngine = EngineModule.provideHeuristicEngine()
        val ruleEngine = EngineModule.provideRuleEngine(
            appProfileRepository = appProfileRepository,
            budgetRepository = budgetRepository,
            cooldownManager = cooldownManager,
        )
        val bundle = EngineModule.provideIntelligenceEngines(ruleEngine, heuristicEngine, promptBuilder)

        val decisionEngine = EngineModule.provideDecisionEngine(
            appProfileRepository = appProfileRepository,
            budgetRepository = budgetRepository,
            cooldownManager = cooldownManager,
            cloudInsightClient = cloudClient,
            engines = bundle,
        )
        assertNotNull(decisionEngine)
        assertIs<DecisionEngine>(decisionEngine)
        httpClient.close()
    }
}
