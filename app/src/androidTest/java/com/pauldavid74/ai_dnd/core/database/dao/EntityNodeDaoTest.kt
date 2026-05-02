package com.pauldavid74.ai_dnd.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pauldavid74.ai_dnd.core.database.AppDatabase
import com.pauldavid74.ai_dnd.core.database.entity.EntityNode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntityNodeDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: EntityNodeDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.entityNodeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getEntitiesWithinRadius_returnsOnlyCorrectEntities() = runBlocking {
        // Arrange: 5ft grid, origin at (10, 10)
        // 20ft radius = 4 units in 5ft grid? 
        // No, the query uses absolute coordinates. Let's assume units are feet.
        // 20ft radius, radiusSq = 400
        val originX = 10f
        val originY = 10f
        val radiusSq = 400f // 20ft radius

        val inside1 = EntityNode("1", "Inside 1", 15, 15, 10, 10, 15, entityType = "MONSTER") // DistSq = 5^2 + 5^2 = 50
        val inside2 = EntityNode("2", "Inside 2", 25, 10, 10, 10, 15, entityType = "MONSTER") // DistSq = 15^2 + 0^2 = 225
        val edge = EntityNode("3", "On Edge", 30, 10, 10, 10, 15, entityType = "MONSTER") // DistSq = 20^2 + 0^2 = 400
        val outside = EntityNode("4", "Outside", 31, 10, 10, 10, 15, entityType = "MONSTER") // DistSq = 21^2 = 441

        dao.upsertEntity(inside1)
        dao.upsertEntity(inside2)
        dao.upsertEntity(edge)
        dao.upsertEntity(outside)

        // Act
        val results = dao.getEntitiesWithinRadius(originX, originY, radiusSq)

        // Assert
        assertEquals(3, results.size)
        assertTrue(results.any { it.id == "1" })
        assertTrue(results.any { it.id == "2" })
        assertTrue(results.any { it.id == "3" })
        assertTrue(results.none { it.id == "4" })
    }
}
