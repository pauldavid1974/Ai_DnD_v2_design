package com.pauldavid74.ai_dnd.core.domain.validation

import com.pauldavid74.ai_dnd.core.domain.model.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioValidatorTest {

    @Test
    fun `validate returns false for empty graph`() {
        val graph = ScenarioGraph(emptyList(), emptyList())
        val result = ScenarioValidator.validate(graph)
        assertFalse(result.isValid)
    }

    @Test
    fun `validate detects unreachable nodes`() {
        val node1 = ScenarioNode("1", "Node 1", "Desc", NodeType.LOCATION)
        val node2 = ScenarioNode("2", "Node 2", "Desc", NodeType.LOCATION)
        val graph = ScenarioGraph(listOf(node1, node2), emptyList())
        
        val result = ScenarioValidator.validate(graph, startNodeId = "1")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Unreachable nodes") })
    }

    @Test
    fun `validate returns true for fully reachable graph via edges`() {
        val node1 = ScenarioNode("1", "Node 1", "Desc", NodeType.LOCATION)
        val finale = ScenarioNode("f", "Finale", "End", NodeType.FINALE)
        val edge = ScenarioEdge("1", "f", "Leads to")
        val graph = ScenarioGraph(listOf(node1, finale), listOf(edge))
        
        val result = ScenarioValidator.validate(graph, startNodeId = "1")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate returns true for fully reachable graph via clues`() {
        val finale = ScenarioNode("f", "Finale", "End", NodeType.FINALE)
        val node1 = ScenarioNode("1", "Node 1", "Desc", NodeType.LOCATION, listOf(Clue("c1", "Find it", "f")))
        val graph = ScenarioGraph(listOf(node1, finale), emptyList())
        
        val result = ScenarioValidator.validate(graph, startNodeId = "1")
        assertTrue(result.isValid)
    }

    @Test
    fun `validate detects trap nodes that cannot reach finale`() {
        val node1 = ScenarioNode("1", "Node 1", "Desc", NodeType.LOCATION)
        val node2 = ScenarioNode("2", "Node 2", "Desc", NodeType.LOCATION)
        val finale = ScenarioNode("f", "Finale", "End", NodeType.FINALE)
        
        // 1 -> f, 2 is isolated
        val edge = ScenarioEdge("1", "f", "Path")
        val graph = ScenarioGraph(listOf(node1, node2, finale), listOf(edge))
        
        val result = ScenarioValidator.validate(graph, startNodeId = "1")
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Trap nodes detected") })
    }

    @Test
    fun `validate returns true when all nodes can reach finale`() {
        val node1 = ScenarioNode("1", "Node 1", "Desc", NodeType.LOCATION)
        val node2 = ScenarioNode("2", "Node 2", "Desc", NodeType.LOCATION)
        val finale = ScenarioNode("f", "Finale", "End", NodeType.FINALE)
        
        // 1 -> 2 -> f
        val edge1 = ScenarioEdge("1", "2", "Path 1")
        val edge2 = ScenarioEdge("2", "f", "Path 2")
        val graph = ScenarioGraph(listOf(node1, node2, finale), listOf(edge1, edge2))
        
        val result = ScenarioValidator.validate(graph, startNodeId = "1")
        assertTrue(result.isValid)
    }
}
