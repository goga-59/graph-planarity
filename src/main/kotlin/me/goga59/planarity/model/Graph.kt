package me.goga59.planarity.model

import java.util.ArrayDeque

data class Edge(val u: Int, val v: Int)

// Меньший номер идет первым, чтобы одинаковые ребра совпадали
fun edge(u: Int, v: Int) = Edge(minOf(u, v), maxOf(u, v))

// Простой неориентированный граф
class Graph(val vertices: Set<Int>, val edges: MutableSet<Edge> = linkedSetOf()) {

    // Возвращает соседей по возрастанию номеров
    fun neighbors(vertex: Int): List<Int> =
        edges.mapNotNull {
            when (vertex) {
                it.u -> it.v
                it.v -> it.u
                else -> null
            }
        }.sorted()

    // Ищет компоненты связности обходом в ширину
    fun components(): List<Set<Int>> {
        val unseen = vertices.toMutableSet()
        val result = mutableListOf<Set<Int>>()

        while (unseen.isNotEmpty()) {
            val component = mutableSetOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(unseen.first())

            while (queue.isNotEmpty()) {
                val vertex = queue.removeFirst()
                if (!unseen.remove(vertex)) continue
                component += vertex
                neighbors(vertex).forEach { if (it in unseen) queue.add(it) }
            }

            result += component
        }

        return result.sortedBy { it.min() }
    }

    // Оставляет только выбранные вершины и ребра между ними
    fun induced(vertices: Set<Int>): Graph {
        val selectedEdges = edges.filterTo(linkedSetOf()) { it.u in vertices && it.v in vertices }
        return Graph(vertices, selectedEdges)
    }

}
