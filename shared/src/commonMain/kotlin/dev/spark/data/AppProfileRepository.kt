package dev.spark.data

import dev.spark.domain.AppCategory
import dev.spark.domain.AppProfile
import dev.spark.domain.EnforcementMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository for persisting and querying [AppProfile] records.
 * Package name is the natural primary key.
 */
interface AppProfileRepository {

    /**
     * Observe all app profiles, ordered by app label ascending.
     * Emits whenever the underlying data changes.
     */
    fun observeAll(): Flow<List<AppProfile>>

    /**
     * Return all stored app profiles.
     */
    suspend fun getAll(): List<AppProfile>

    /**
     * Return the profile for [packageName], or null if not found.
     */
    suspend fun getByPackageName(packageName: String): AppProfile?

    /**
     * Observe a specific app profile by package name.
     */
    fun observeByPackageName(packageName: String): Flow<AppProfile?>

    /**
     * Return all profiles belonging to [category].
     */
    suspend fun getByCategory(category: AppCategory): List<AppProfile>

    /**
     * Return all profiles configured with [enforcementMode].
     */
    suspend fun getByEnforcementMode(enforcementMode: EnforcementMode): List<AppProfile>

    /**
     * Return all profiles where [AppProfile.isBypassed] is true.
     */
    suspend fun getBypassed(): List<AppProfile>

    /**
     * Return all profiles that were manually classified by the user.
     */
    suspend fun getCustomClassified(): List<AppProfile>

    /**
     * Persist a new profile. If a profile with the same package name exists,
     * an exception is thrown — use [upsert] for insert-or-replace semantics.
     */
    suspend fun insert(profile: AppProfile)

    /**
     * Replace all mutable fields of an existing profile identified by
     * [AppProfile.packageName].
     */
    suspend fun update(profile: AppProfile)

    /**
     * Insert or replace the profile (upsert semantics).
     */
    suspend fun upsert(profile: AppProfile)

    /**
     * Override the category for a specific app, marking it as a custom
     * classification.
     */
    suspend fun updateCategory(packageName: String, category: AppCategory)

    /**
     * Enable or disable the bypass flag for an app.
     */
    suspend fun updateBypass(packageName: String, isBypassed: Boolean)

    /**
     * Remove the profile for [packageName].
     */
    suspend fun deleteByPackageName(packageName: String)
}
