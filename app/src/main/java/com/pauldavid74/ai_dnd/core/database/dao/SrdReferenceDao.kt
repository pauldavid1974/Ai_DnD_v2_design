package com.pauldavid74.ai_dnd.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pauldavid74.ai_dnd.core.database.entity.SrdReferenceEntity

@Dao
interface SrdReferenceDao {
    @Query("SELECT * FROM srd_references WHERE id = :id")
    suspend fun getReferenceById(id: String): SrdReferenceEntity?

    @Query("SELECT * FROM srd_references WHERE category = :category")
    suspend fun getReferencesByCategory(category: String): List<SrdReferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferences(references: List<SrdReferenceEntity>)

    @Query("SELECT COUNT(*) FROM srd_references")
    suspend fun getCount(): Int
}
