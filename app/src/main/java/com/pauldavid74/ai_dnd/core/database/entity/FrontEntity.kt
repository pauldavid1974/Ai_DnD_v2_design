package com.pauldavid74.ai_dnd.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pauldavid74.ai_dnd.core.domain.model.GrimPortent

@Entity(tableName = "fronts")
data class FrontEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val description: String,
    val doom: String,
    val portents: List<GrimPortent>
)
