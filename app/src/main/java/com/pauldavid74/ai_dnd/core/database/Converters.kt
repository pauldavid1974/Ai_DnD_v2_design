package com.pauldavid74.ai_dnd.core.database

import androidx.room.TypeConverter
import com.pauldavid74.ai_dnd.core.domain.model.Clue
import com.pauldavid74.ai_dnd.core.domain.model.GrimPortent
import com.pauldavid74.ai_dnd.core.domain.model.NodeType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun fromNodeType(value: NodeType): String = value.name

    @TypeConverter
    fun toNodeType(value: String): NodeType = NodeType.valueOf(value)

    @TypeConverter
    fun fromClueList(value: List<Clue>): String = Json.encodeToString(value)

    @TypeConverter
    fun toClueList(value: String): List<Clue> = try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }

    @TypeConverter
    fun fromGrimPortentList(value: List<GrimPortent>): String = Json.encodeToString(value)

    @TypeConverter
    fun toGrimPortentList(value: String): List<GrimPortent> = try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        emptyList()
    }
}
