package com.antigravity.substance.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigravity.substance.data.db.entities.CustomFieldDefEntity
import com.antigravity.substance.data.db.entities.RouteOfAdminEntity
import com.antigravity.substance.data.db.entities.SubstanceEntity
import com.antigravity.substance.data.db.entities.SubstanceSubtypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubstanceDao {
    @Query("SELECT * FROM substances WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveSubstances(): Flow<List<SubstanceEntity>>

    @Query("SELECT * FROM substances ORDER BY isArchived ASC, sortOrder ASC, id ASC")
    fun observeAllSubstances(): Flow<List<SubstanceEntity>>

    @Query("SELECT * FROM substances ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSubstancesOnce(): List<SubstanceEntity>

    @Query("SELECT * FROM substances WHERE id = :id LIMIT 1")
    suspend fun getSubstanceById(id: Long): SubstanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubstance(substance: SubstanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubstances(substances: List<SubstanceEntity>): List<Long>

    @Update
    suspend fun updateSubstance(substance: SubstanceEntity)

    @Delete
    suspend fun deleteSubstance(substance: SubstanceEntity)

    @Query("DELETE FROM substances WHERE id = :id")
    suspend fun deleteSubstanceById(id: Long)

    @Query("DELETE FROM substances")
    suspend fun deleteAllSubstances()

    // Substance Subtypes (Variants / Strains / Sources)
    @Query("SELECT * FROM substance_subtypes ORDER BY substanceId ASC, id ASC")
    fun observeAllSubtypes(): Flow<List<SubstanceSubtypeEntity>>

    @Query("SELECT * FROM substance_subtypes WHERE substanceId = :substanceId ORDER BY id ASC")
    fun observeSubtypesForSubstance(substanceId: Long): Flow<List<SubstanceSubtypeEntity>>

    @Query("SELECT * FROM substance_subtypes ORDER BY substanceId ASC, id ASC")
    suspend fun getAllSubtypesOnce(): List<SubstanceSubtypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtype(subtype: SubstanceSubtypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtypes(subtypes: List<SubstanceSubtypeEntity>)

    @Query("DELETE FROM substance_subtypes WHERE id = :id")
    suspend fun deleteSubtypeById(id: Long)

    @Query("DELETE FROM substance_subtypes WHERE substanceId = :substanceId")
    suspend fun deleteSubtypesForSubstance(substanceId: Long)

    @Query("DELETE FROM substance_subtypes")
    suspend fun deleteAllSubtypes()

    // Custom Field Definitions
    @Query("SELECT * FROM custom_field_definitions ORDER BY sortOrder ASC, id ASC")
    fun observeAllCustomFieldDefs(): Flow<List<CustomFieldDefEntity>>

    @Query("SELECT * FROM custom_field_definitions WHERE substanceId IS NULL OR substanceId = :substanceId ORDER BY sortOrder ASC, id ASC")
    fun observeCustomFieldsForSubstance(substanceId: Long): Flow<List<CustomFieldDefEntity>>

    @Query("SELECT * FROM custom_field_definitions ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCustomFieldDefsOnce(): List<CustomFieldDefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomFieldDef(def: CustomFieldDefEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFieldDefs(defs: List<CustomFieldDefEntity>)

    @Query("DELETE FROM custom_field_definitions WHERE id = :id")
    suspend fun deleteCustomFieldDefById(id: Long)

    @Query("DELETE FROM custom_field_definitions")
    suspend fun deleteAllCustomFieldDefs()

    // Routes of Administration
    @Query("SELECT * FROM routes_of_admin ORDER BY id ASC")
    fun observeAllRoutesOfAdmin(): Flow<List<RouteOfAdminEntity>>

    @Query("SELECT * FROM routes_of_admin ORDER BY id ASC")
    suspend fun getAllRoutesOfAdminOnce(): List<RouteOfAdminEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRouteOfAdmin(roa: RouteOfAdminEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutesOfAdmin(routes: List<RouteOfAdminEntity>)

    @Query("DELETE FROM routes_of_admin WHERE id = :id")
    suspend fun deleteRouteOfAdminById(id: Long)

    @Query("DELETE FROM routes_of_admin")
    suspend fun deleteAllRoutesOfAdmin()
}
