// BypassManagerTest.kt
// Spark — Unit Tests: BypassManager
//
// Tests: default bypass list, add/remove, persistence contract

package dev.spark.tracking

import kotlin.test.*

// MARK: - BypassManager under test

class BypassManager(
    initialBypassList: List<String> = defaultBypassList
) {
    private val _list: MutableList<String> = initialBypassList.toMutableList()

    val bypassList: List<String> get() = _list.toList()

    fun add(packageName: String): Boolean {
        if (_list.contains(packageName)) return false
        _list.add(packageName)
        return true
    }

    fun remove(packageName: String): Boolean = _list.remove(packageName)

    fun isBypassed(packageName: String): Boolean = _list.contains(packageName)

    fun reset() {
        _list.clear()
        _list.addAll(defaultBypassList)
    }

    fun clear() = _list.clear()

    companion object {
        // Default set of apps that should never be gated.
        val defaultBypassList = listOf(
            "com.android.phone",           // Phone app
            "com.android.contacts",        // Contacts
            "com.google.android.dialer",   // Google Phone
            "com.android.mms",             // Messages
            "com.google.android.apps.messaging", // Google Messages
            "com.samsung.android.messaging",
            "com.oneplus.camera",
            "com.android.camera2",
            "com.android.settings",        // Settings
            "com.google.android.gms"       // Google Play Services
        )
    }
}

// MARK: - Tests

class BypassManagerTest {

    private lateinit var manager: BypassManager

    @BeforeTest
    fun setUp() {
        manager = BypassManager()
    }

    // ── Default bypass list ───────────────────────────────────────────────

    @Test
    fun testDefaultBypassListIsNotEmpty() {
        assertTrue(manager.bypassList.isNotEmpty())
    }

    @Test
    fun testPhoneAppIsInDefaultList() {
        assertTrue(manager.isBypassed("com.android.phone"))
    }

    @Test
    fun testContactsIsInDefaultList() {
        assertTrue(manager.isBypassed("com.android.contacts"))
    }

    @Test
    fun testSettingsIsInDefaultList() {
        assertTrue(manager.isBypassed("com.android.settings"))
    }

    @Test
    fun testGooglePlayServicesIsInDefaultList() {
        assertTrue(manager.isBypassed("com.google.android.gms"))
    }

    @Test
    fun testMessagingAppIsInDefaultList() {
        assertTrue(manager.isBypassed("com.google.android.apps.messaging"))
    }

    @Test
    fun testInstagramIsNotInDefaultList() {
        assertFalse(manager.isBypassed("com.instagram.android"))
    }

    @Test
    fun testTikTokIsNotInDefaultList() {
        assertFalse(manager.isBypassed("com.zhiliaoapp.musically"))
    }

    // ── Add ───────────────────────────────────────────────────────────────

    @Test
    fun testAddNewPackageReturnsTrue() {
        val added = manager.add("com.spotify.music")
        assertTrue(added)
    }

    @Test
    fun testAddNewPackageAppearsInList() {
        manager.add("com.spotify.music")
        assertTrue(manager.isBypassed("com.spotify.music"))
    }

    @Test
    fun testAddDuplicateReturnsFalse() {
        manager.add("com.spotify.music")
        val addedAgain = manager.add("com.spotify.music")
        assertFalse(addedAgain)
    }

    @Test
    fun testAddDuplicateDoesNotDuplicate() {
        manager.add("com.spotify.music")
        manager.add("com.spotify.music")
        val count = manager.bypassList.count { it == "com.spotify.music" }
        assertEquals(1, count)
    }

    @Test
    fun testAddDefaultAlreadyPresentReturnsFalse() {
        val added = manager.add("com.android.phone")
        assertFalse(added)
    }

    @Test
    fun testListGrowsByOneAfterAdd() {
        val before = manager.bypassList.size
        manager.add("com.spotify.music")
        assertEquals(before + 1, manager.bypassList.size)
    }

    // ── Remove ────────────────────────────────────────────────────────────

    @Test
    fun testRemoveExistingReturnsTrue() {
        val removed = manager.remove("com.android.phone")
        assertTrue(removed)
    }

    @Test
    fun testRemoveExistingDisappearsFromList() {
        manager.remove("com.android.phone")
        assertFalse(manager.isBypassed("com.android.phone"))
    }

    @Test
    fun testRemoveNonExistentReturnsFalse() {
        val removed = manager.remove("com.not.installed")
        assertFalse(removed)
    }

    @Test
    fun testListShrinksAfterRemove() {
        val before = manager.bypassList.size
        manager.remove("com.android.phone")
        assertEquals(before - 1, manager.bypassList.size)
    }

    @Test
    fun testRemoveAddedItem() {
        manager.add("com.spotify.music")
        val removed = manager.remove("com.spotify.music")
        assertTrue(removed)
        assertFalse(manager.isBypassed("com.spotify.music"))
    }

    // ── isBypassed ────────────────────────────────────────────────────────

    @Test
    fun testIsBypassedReturnsTrueForExistingItem() {
        assertTrue(manager.isBypassed("com.android.contacts"))
    }

    @Test
    fun testIsBypassedReturnsFalseForUnknownApp() {
        assertFalse(manager.isBypassed("com.random.unknown.app"))
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    @Test
    fun testResetRestoresDefaultList() {
        manager.add("com.spotify.music")
        manager.remove("com.android.phone")
        manager.reset()
        assertEquals(BypassManager.defaultBypassList.sorted(), manager.bypassList.sorted())
    }

    @Test
    fun testResetRemovesCustomAdditions() {
        manager.add("com.spotify.music")
        manager.reset()
        assertFalse(manager.isBypassed("com.spotify.music"))
    }

    // ── Clear ─────────────────────────────────────────────────────────────

    @Test
    fun testClearEmptiesAllEntries() {
        manager.clear()
        assertTrue(manager.bypassList.isEmpty())
    }

    @Test
    fun testClearThenAddWorks() {
        manager.clear()
        manager.add("com.spotify.music")
        assertEquals(listOf("com.spotify.music"), manager.bypassList)
    }

    // ── bypassList returns immutable copy ─────────────────────────────────

    @Test
    fun testBypassListReturnsCopy() {
        val snapshot = manager.bypassList
        manager.add("com.new.app")
        assertNotEquals(snapshot.size, manager.bypassList.size)
    }
}
