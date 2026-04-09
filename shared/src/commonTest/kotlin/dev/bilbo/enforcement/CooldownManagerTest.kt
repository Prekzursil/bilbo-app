package dev.bilbo.enforcement

import kotlin.test.*

// =============================================================================
//  CooldownManager Tests
// =============================================================================

private class FakeCooldownPersistence : CooldownPersistence {
    val saved = mutableMapOf<String, Long>()
    var clearCalls = mutableListOf<String>()

    override fun save(packageName: String, expiryEpochSeconds: Long) {
        saved[packageName] = expiryEpochSeconds
    }

    override fun clear(packageName: String) {
        saved.remove(packageName)
        clearCalls.add(packageName)
    }

    override fun loadAll(): Map<String, Long> = saved.toMap()
}

class CooldownManagerLockTest {

    @Test fun lockAppMakesItLocked() {
        val manager = CooldownManager()
        manager.lockApp("com.test", 10)
        assertTrue(manager.isLocked("com.test"))
    }

    @Test fun lockAppExtendsExistingLock() {
        val persistence = FakeCooldownPersistence()
        val manager = CooldownManager(persistence)
        manager.lockApp("com.test", 5)
        val firstExpiry = persistence.saved["com.test"]!!
        manager.lockApp("com.test", 60) // longer lock
        val secondExpiry = persistence.saved["com.test"]!!
        assertTrue(secondExpiry >= firstExpiry)
    }

    @Test fun lockAppKeepsLongerExisting() {
        val persistence = FakeCooldownPersistence()
        val manager = CooldownManager(persistence)
        manager.lockApp("com.test", 60) // 60 min
        val longExpiry = persistence.saved["com.test"]!!
        manager.lockApp("com.test", 1) // 1 min - shorter
        val afterExpiry = persistence.saved["com.test"]!!
        assertEquals(longExpiry, afterExpiry) // kept the longer one
    }
}

class CooldownManagerIsLockedTest {

    @Test fun isLockedReturnsFalseForUnknown() {
        val manager = CooldownManager()
        assertFalse(manager.isLocked("com.nonexistent"))
    }

    @Test fun isLockedReturnsTrueForActive() {
        val manager = CooldownManager()
        manager.lockApp("com.test", 60)
        assertTrue(manager.isLocked("com.test"))
    }
}

class CooldownManagerRemainingTest {

    @Test fun getRemainingMinutesNull() {
        val manager = CooldownManager()
        assertNull(manager.getRemainingMinutes("com.nonexistent"))
    }

    @Test fun getRemainingMinutesPositive() {
        val manager = CooldownManager()
        manager.lockApp("com.test", 60)
        val remaining = manager.getRemainingMinutes("com.test")
        assertNotNull(remaining)
        assertTrue(remaining > 0)
    }

    @Test fun getRemainingSecondsNull() {
        val manager = CooldownManager()
        assertNull(manager.getRemainingSeconds("com.nonexistent"))
    }

    @Test fun getRemainingSecondsPositive() {
        val manager = CooldownManager()
        manager.lockApp("com.test", 60)
        val remaining = manager.getRemainingSeconds("com.test")
        assertNotNull(remaining)
        assertTrue(remaining > 0)
    }

    @Test fun getExpiryEpochNull() {
        val manager = CooldownManager()
        assertNull(manager.getExpiryEpoch("com.nonexistent"))
    }

    @Test fun getExpiryEpochReturnsValue() {
        val manager = CooldownManager()
        manager.lockApp("com.test", 60)
        val expiry = manager.getExpiryEpoch("com.test")
        assertNotNull(expiry)
    }
}

class CooldownManagerUnlockTest {

    @Test fun unlockAppRemoves() {
        val persistence = FakeCooldownPersistence()
        val manager = CooldownManager(persistence)
        manager.lockApp("com.test", 60)
        assertTrue(manager.isLocked("com.test"))
        manager.unlockApp("com.test")
        assertFalse(manager.isLocked("com.test"))
    }

    @Test fun unlockAppClearsPersistence() {
        val persistence = FakeCooldownPersistence()
        val manager = CooldownManager(persistence)
        manager.lockApp("com.test", 60)
        manager.unlockApp("com.test")
        assertTrue(persistence.clearCalls.contains("com.test"))
    }
}

class CooldownManagerGetAllTest {

    @Test fun getAllLockedAppsEmpty() {
        val manager = CooldownManager()
        assertTrue(manager.getAllLockedApps().isEmpty())
    }

    @Test fun getAllLockedAppsReturnsActive() {
        val manager = CooldownManager()
        manager.lockApp("com.a", 60)
        manager.lockApp("com.b", 60)
        val locked = manager.getAllLockedApps()
        assertEquals(2, locked.size)
        assertTrue(locked.contains("com.a"))
        assertTrue(locked.contains("com.b"))
    }
}

class CooldownManagerPersistenceTest {

    @Test fun restoreFromPersistenceLoadsActive() {
        val persistence = FakeCooldownPersistence()
        val futureExpiry = kotlinx.datetime.Clock.System.now().epochSeconds + 3600
        persistence.saved["com.persisted"] = futureExpiry

        val manager = CooldownManager(persistence)
        manager.restoreFromPersistence()
        assertTrue(manager.isLocked("com.persisted"))
    }

    @Test fun restoreFromPersistenceIgnoresExpired() {
        val persistence = FakeCooldownPersistence()
        val pastExpiry = kotlinx.datetime.Clock.System.now().epochSeconds - 3600
        persistence.saved["com.expired"] = pastExpiry

        val manager = CooldownManager(persistence)
        manager.restoreFromPersistence()
        assertFalse(manager.isLocked("com.expired"))
    }

    @Test fun restoreFromPersistenceClearsExpired() {
        val persistence = FakeCooldownPersistence()
        val pastExpiry = kotlinx.datetime.Clock.System.now().epochSeconds - 3600
        persistence.saved["com.expired"] = pastExpiry

        val manager = CooldownManager(persistence)
        manager.restoreFromPersistence()
        assertTrue(persistence.clearCalls.contains("com.expired"))
    }
}

class NoOpCooldownPersistenceTest {

    @Test fun saveIsNoOp() {
        NoOpCooldownPersistence.save("com.test", 12345L)
        // No exception
    }

    @Test fun clearIsNoOp() {
        NoOpCooldownPersistence.clear("com.test")
        // No exception
    }

    @Test fun loadAllReturnsEmpty() {
        val result = NoOpCooldownPersistence.loadAll()
        assertTrue(result.isEmpty())
    }
}
