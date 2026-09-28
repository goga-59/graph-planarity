package me.goga59.planarity.algorithm

import me.goga59.planarity.model.Edge
import me.goga59.planarity.model.Graph
import me.goga59.planarity.model.edge
import java.util.ArrayDeque

// Делит граф на двусвязные блоки
fun blocks(graph: Graph): List<Set<Edge>> {
    val discoveryTime = mutableMapOf<Int, Int>()

    // lowLink[v] - самое раннее время входа, достижимое из поддерева вершины v
    val lowLink = mutableMapOf<Int, Int>()

    val edgeStack = ArrayDeque<Edge>() // Ребра текущих блоков складываем в стек
    val blocks = mutableListOf<Set<Edge>>()
    var time = 0

    fun dfs(vertex: Int, parent: Int?) {
        discoveryTime[vertex] = ++time
        lowLink[vertex] = time

        for (neighbor in graph.neighbors(vertex)) {
            val currentEdge = edge(vertex, neighbor)

            if (neighbor !in discoveryTime) {
                edgeStack.addLast(currentEdge)
                dfs(neighbor, vertex)
                lowLink[vertex] = minOf(lowLink.getValue(vertex), lowLink.getValue(neighbor))

                // Если из поддерева нельзя попасть к предкам, блок найден
                // Снимаем его ребра со стека
                if (lowLink.getValue(neighbor) >= discoveryTime.getValue(vertex)) {
                    val block = linkedSetOf<Edge>()

                    do {
                        val last = edgeStack.removeLast()
                        block += last
                    } while (last != currentEdge)

                    blocks += block
                }
            } else if (neighbor != parent && discoveryTime.getValue(neighbor) < discoveryTime.getValue(vertex)) {
                // Обратное ребро позволяет вернуться к предку
                edgeStack.addLast(currentEdge)
                lowLink[vertex] = minOf(lowLink.getValue(vertex), discoveryTime.getValue(neighbor))
            }
        }
    }

    graph.vertices.sorted().forEach { if (it !in discoveryTime) dfs(it, null) }
    return blocks
}
