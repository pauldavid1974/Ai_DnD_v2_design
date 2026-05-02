package com.pauldavid74.ai_dnd.core.domain.validation

import com.pauldavid74.ai_dnd.core.domain.model.NodeType
import com.pauldavid74.ai_dnd.core.domain.model.ScenarioGraph

object ScenarioValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    )

    /**
     * Traverses the graph to detect disconnected islands or dead-ends.
     * In an Alexandrian Node-Based Scenario, nodes should generally be reachable
     * from a starting point (or reachable from each other in a meaningful way).
     */
    fun validate(graph: ScenarioGraph, startNodeId: String? = null): ValidationResult {
        if (graph.nodes.isEmpty()) {
            return ValidationResult(false, listOf("Graph has no nodes"))
        }

        val errors = mutableListOf<String>()
        val nodeIds = graph.nodes.map { it.id }.toSet()

        // 1. Check for dangling edges
        graph.edges.forEach { edge ->
            if (edge.sourceNodeId !in nodeIds) {
                errors.add("Edge source ${edge.sourceNodeId} not found in nodes")
            }
            if (edge.targetNodeId !in nodeIds) {
                errors.add("Edge target ${edge.targetNodeId} not found in nodes")
            }
        }

        // 2. Reachability from Start
        if (startNodeId != null) {
            if (startNodeId !in nodeIds) {
                errors.add("Start node $startNodeId not found in nodes")
            } else {
                val reachable = findReachable(graph, startNodeId)
                val unreachable = nodeIds - reachable
                if (unreachable.isNotEmpty()) {
                    errors.add("Unreachable nodes (disconnected narrative islands): $unreachable")
                }
            }
        }

        // 3. Trap Cycles / Dead Ends
        // Any node should eventually reach a node marked as 'FINALE' or 'GOAL'
        val goalNodes = graph.nodes.filter { it.type == NodeType.FINALE || it.type == NodeType.GOAL }.map { it.id }.toSet()
        if (goalNodes.isEmpty()) {
            errors.add("No finale/goal node defined in the scenario")
        } else {
            val canReachGoal = mutableSetOf<String>()
            // Reverse reachability from goals
            goalNodes.forEach { goalId ->
                canReachGoal.addAll(findReachableReverse(graph, goalId))
            }
            
            val trapNodes = nodeIds - canReachGoal
            if (trapNodes.isNotEmpty()) {
                errors.add("Trap nodes detected (cannot reach finale): $trapNodes")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun findReachableReverse(graph: ScenarioGraph, targetNodeId: String): Set<String> {
        val reachable = mutableSetOf<String>()
        val queue = mutableListOf(targetNodeId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            if (currentId !in reachable) {
                reachable.add(currentId)
                // Nodes that lead TO currentId
                val sourcesFromEdges = graph.edges
                    .filter { it.targetNodeId == currentId }
                    .map { it.sourceNodeId }
                
                val sourcesFromClues = graph.nodes
                    .filter { node -> node.clues.any { it.targetNodeId == currentId } }
                    .map { it.id }

                queue.addAll(sourcesFromEdges)
                queue.addAll(sourcesFromClues)
            }
        }
        return reachable
    }

    private fun findReachable(graph: ScenarioGraph, startNodeId: String): Set<String> {
        val reachable = mutableSetOf<String>()
        val queue = mutableListOf(startNodeId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)
            if (currentId !in reachable) {
                reachable.add(currentId)
                // Add nodes reachable via explicit edges
                val neighborsFromEdges = graph.edges
                    .filter { it.sourceNodeId == currentId }
                    .map { it.targetNodeId }
                
                // Add nodes reachable via clues
                val neighborsFromClues = graph.nodes
                    .find { it.id == currentId }
                    ?.clues
                    ?.map { it.targetNodeId } ?: emptyList()

                queue.addAll(neighborsFromEdges)
                queue.addAll(neighborsFromClues)
            }
        }
        return reachable
    }
}
