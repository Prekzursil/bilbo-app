// FocusPointsEngineTest.kt
// Spark — Unit Tests: FocusPointsEngine
//
// Tests: earning, spending, bonuses, penalties, daily cap, rollover

package dev.spark.economy

import kotlin.test.*

// MARK: - Minimal domain types for test isolation

data class FPTransaction(
    val type: TransactionType,
    val amount: Int,
    val note: String = ""
)

enum class TransactionType { EARN, SPEND, BONUS, PENALTY, ROLLOVER }

// MARK: - FocusPointsEngine under test

class FocusPointsEngine(
    private val dailyBaseline: Int = 60,
    private val dailyCap: Int = 200,
    private val rolloverLimit: Int = 50
) {
    private var balance: Int = 0
    val transactions: MutableList<FPTransaction> = mutableListOf()

    fun addDailyBaseline() {
        earn(dailyBaseline, "Daily baseline")
    }

    fun earn(amount: Int, note: String = "") {
        require(amount > 0) { "Earn amount must be positive" }
        val before = balance
        balance = minOf(balance + amount, dailyCap)
        val actual = balance - before
        transactions += FPTransaction(TransactionType.EARN, actual, note)
    }

    fun spend(amount: Int, note: String = ""): Boolean {
        require(amount > 0) { "Spend amount must be positive" }
        if (balance < amount) return false
        balance -= amount
        transactions += FPTransaction(TransactionType.SPEND, amount, note)
        return true
    }

    fun applyBonus(multiplier: Double, note: String = "") {
        require(multiplier > 0) { "Bonus multiplier must be positive" }
        val bonus = (balance * (multiplier - 1)).toInt()
        if (bonus <= 0) return
        val before = balance
        balance = minOf(balance + bonus, dailyCap)
        transactions += FPTransaction(TransactionType.BONUS, balance - before, note)
    }

    fun applyPenalty(amount: Int, note: String = "") {
        require(amount > 0) { "Penalty amount must be positive" }
        balance = maxOf(0, balance - amount)
        transactions += FPTransaction(TransactionType.PENALTY, amount, note)
    }

    /**
     * Roll over balance at day end.
     * Carries at most [rolloverLimit] FP into the next day.
     * Returns the amount rolled over.
     */
    fun rollover(): Int {
        val rolled = minOf(balance, rolloverLimit)
        val discarded = balance - rolled
        balance = rolled
        transactions += FPTransaction(TransactionType.ROLLOVER, rolled, "Day rollover (-$discarded discarded)")
        return rolled
    }

    fun getBalance(): Int = balance
    fun reset() { balance = 0; transactions.clear() }
}

// MARK: - Tests

class FocusPointsEngineTest {

    private lateinit var engine: FocusPointsEngine

    @BeforeTest
    fun setUp() {
        engine = FocusPointsEngine(dailyBaseline = 60, dailyCap = 200, rolloverLimit = 50)
    }

    // ── Earning ──────────────────────────────────────────────────────────

    @Test
    fun testEarnIncreasesBalance() {
        engine.earn(20)
        assertEquals(20, engine.getBalance())
    }

    @Test
    fun testEarnRecordsTransaction() {
        engine.earn(30, "Intentional session")
        val t = engine.transactions.last()
        assertEquals(TransactionType.EARN, t.type)
        assertEquals(30, t.amount)
    }

    @Test
    fun testEarnMultipleTimes() {
        engine.earn(20)
        engine.earn(15)
        assertEquals(35, engine.getBalance())
    }

    @Test
    fun testEarnZeroThrows() {
        assertFailsWith<IllegalArgumentException> { engine.earn(0) }
    }

    @Test
    fun testEarnNegativeThrows() {
        assertFailsWith<IllegalArgumentException> { engine.earn(-5) }
    }

    @Test
    fun testDailyBaselineAddsCorrectAmount() {
        engine.addDailyBaseline()
        assertEquals(60, engine.getBalance())
    }

    // ── Daily Cap ─────────────────────────────────────────────────────────

    @Test
    fun testEarnCappedAtDailyCap() {
        engine.earn(190)
        engine.earn(50) // Would bring to 240, cap is 200
        assertEquals(200, engine.getBalance())
    }

    @Test
    fun testTransactionAmountReflectsCapping() {
        engine.earn(190)
        engine.earn(50)
        val last = engine.transactions.last()
        assertEquals(10, last.amount) // Only 10 were actually added
    }

    // ── Spending ──────────────────────────────────────────────────────────

    @Test
    fun testSpendDeductsBalance() {
        engine.earn(50)
        val success = engine.spend(20, "Override cost")
        assertTrue(success)
        assertEquals(30, engine.getBalance())
    }

    @Test
    fun testSpendReturnsFalseWhenInsufficientBalance() {
        engine.earn(5)
        val success = engine.spend(10)
        assertFalse(success)
        assertEquals(5, engine.getBalance()) // Balance unchanged
    }

    @Test
    fun testSpendExactBalanceSucceeds() {
        engine.earn(10)
        val success = engine.spend(10)
        assertTrue(success)
        assertEquals(0, engine.getBalance())
    }

    @Test
    fun testSpendRecordsTransaction() {
        engine.earn(40)
        engine.spend(15, "Hard lock override")
        val t = engine.transactions.last()
        assertEquals(TransactionType.SPEND, t.type)
        assertEquals(15, t.amount)
    }

    @Test
    fun testSpendZeroThrows() {
        assertFailsWith<IllegalArgumentException> { engine.spend(0) }
    }

    // ── Bonuses ───────────────────────────────────────────────────────────

    @Test
    fun testBonusMultiplierIncreasesBalance() {
        engine.earn(100)
        engine.applyBonus(1.5)
        assertEquals(150, engine.getBalance())
    }

    @Test
    fun testBonusIsCappedAtDailyCap() {
        engine.earn(180)
        engine.applyBonus(2.0) // Would give 360, cap is 200
        assertEquals(200, engine.getBalance())
    }

    @Test
    fun testBonusOnZeroBalanceHasNoEffect() {
        engine.applyBonus(2.0)
        assertEquals(0, engine.getBalance())
        assertTrue(engine.transactions.isEmpty())
    }

    // ── Penalties ─────────────────────────────────────────────────────────

    @Test
    fun testPenaltyDeductsBalance() {
        engine.earn(50)
        engine.applyPenalty(20, "Anti-gaming detection")
        assertEquals(30, engine.getBalance())
    }

    @Test
    fun testPenaltyDoesNotGoBelowZero() {
        engine.earn(10)
        engine.applyPenalty(50)
        assertEquals(0, engine.getBalance())
    }

    @Test
    fun testPenaltyRecordsTransaction() {
        engine.earn(30)
        engine.applyPenalty(10)
        val t = engine.transactions.last()
        assertEquals(TransactionType.PENALTY, t.type)
        assertEquals(10, t.amount)
    }

    // ── Rollover ──────────────────────────────────────────────────────────

    @Test
    fun testRolloverCarriesAtMostRolloverLimit() {
        engine.earn(100)
        val rolled = engine.rollover()
        assertEquals(50, rolled)
        assertEquals(50, engine.getBalance())
    }

    @Test
    fun testRolloverWhenBalanceBelowLimit() {
        engine.earn(30)
        val rolled = engine.rollover()
        assertEquals(30, rolled)
        assertEquals(30, engine.getBalance())
    }

    @Test
    fun testRolloverOnZeroBalance() {
        val rolled = engine.rollover()
        assertEquals(0, rolled)
        assertEquals(0, engine.getBalance())
    }

    @Test
    fun testRolloverRecordsTransaction() {
        engine.earn(80)
        engine.rollover()
        val t = engine.transactions.last()
        assertEquals(TransactionType.ROLLOVER, t.type)
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    @Test
    fun testResetClearsBalanceAndTransactions() {
        engine.earn(50)
        engine.reset()
        assertEquals(0, engine.getBalance())
        assertTrue(engine.transactions.isEmpty())
    }
}
