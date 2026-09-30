package me.goga59.planarity.algorithm

import me.goga59.planarity.model.Edge
import me.goga59.planarity.model.Graph
import me.goga59.planarity.model.edge
import java.util.ArrayDeque

// Порядок ребер и грани при успехе, точки примыкания при конфликте
data class Embedding(
    val rotation: Map<Int, List<Int>>,
    val faces: List<List<Int>>,
    val conflict: Set<Int> = emptySet(),
) {
    val isPlanar get() = conflict.isEmpty()
}

// Здесь Bridge - фрагмент относительно уже уложенной части
// Это отдельное неуложенное ребро или связная группа новых вершин
// Такой фрагмент не обязательно является ребром-разрезом графа
private data class Bridge(
    val contacts: Set<Int>,
    val unembeddedVertices: Set<Int> = emptySet(),
    val singleEdge: Edge? = null,
)

private data class BridgePlacement(
    val bridge: Bridge,
    val admissibleFaces: List<Int>,
)

// Укладывает блоки по отдельности и объединяет порядки ребер в общих вершинах
fun demoucron(graph: Graph): Embedding {
    val rotation = graph.vertices.associateWith { mutableListOf<Int>() }
    var faces = emptyList<List<Int>>()

    for (blockEdges in blocks(graph)) {
        if (blockEdges.size == 1) {
            val (u, v) = blockEdges.first()
            rotation.getValue(u) += v
            rotation.getValue(v) += u
            continue
        }

        val blockVertices = blockEdges.flatMapTo(mutableSetOf()) { listOf(it.u, it.v) }
        val block = Graph(blockVertices, blockEdges.toMutableSet())

        val result = embedBlock(block)
        if (!result.isPlanar) return result

        // В точке сочленения порядки соседей разных блоков идут подряд
        result.rotation.forEach { (vertex, neighbors) ->
            rotation.getValue(vertex).addAll(neighbors)
        }

        faces = result.faces
    }

    return Embedding(rotation, faces)
}

// Начинаем укладку с цикла, найденного поиском в глубину
private fun initialCycle(graph: Graph): List<Int> {
    val parent = mutableMapOf<Int, Int>()
    val depth = mutableMapOf<Int, Int>()

    fun visit(vertex: Int, parentVertex: Int?): List<Int>? {
        for (neighbor in graph.neighbors(vertex)) {
            if (neighbor == parentVertex) continue

            if (neighbor !in depth) {
                parent[neighbor] = vertex
                depth[neighbor] = depth.getValue(vertex) + 1
                visit(neighbor, vertex)?.let { return it }
            } else if (depth.getValue(neighbor) < depth.getValue(vertex)) {
                // Обратное ребро замыкает цикл, который восстанавливаем по родителям
                val path = mutableListOf(vertex)

                while (path.last() != neighbor) {
                    path += parent.getValue(path.last())
                }

                return path.reversed()
            }
        }

        return null
    }

    val start = graph.vertices.min()
    depth[start] = 0

    return visit(start, null) ?: error("A nontrivial biconnected block must contain a cycle")
}

// Находит мосты и вершины, которыми они связаны с уложенной частью
private fun bridges(graph: Graph, embeddedVertices: Set<Int>, embeddedEdges: Set<Edge>): List<Bridge> {
    val result = mutableListOf<Bridge>()

    // Неуложенное ребро между уложенными вершинами это отдельный фрагмент
    for (edge in graph.edges) {
        if (edge !in embeddedEdges && edge.u in embeddedVertices && edge.v in embeddedVertices)
            result += Bridge(setOf(edge.u, edge.v), singleEdge = edge)
    }

    val unseen = (graph.vertices - embeddedVertices).toMutableSet()
    while (unseen.isNotEmpty()) {
        val outside = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.add(unseen.first())

        while (queue.isNotEmpty()) {
            val vertex = queue.removeFirst()
            if (!unseen.remove(vertex)) continue

            outside += vertex
            graph.neighbors(vertex).forEach { if (it in unseen) queue.add(it) }
        }

        val contacts = outside.flatMapTo(mutableSetOf()) { vertex ->
            graph.neighbors(vertex).filter { it in embeddedVertices }
        }

        // Вершины примыкания определяют, в какие грани можно поместить эту часть
        result += Bridge(contacts, unembeddedVertices = outside)
    }

    return result
}

// Ищет цепь между двумя точками примыкания через еще не уложенные вершины
private fun bridgePath(graph: Graph, bridge: Bridge): List<Int> {
    bridge.singleEdge?.let { return listOf(it.u, it.v) }

    val start = bridge.contacts.min()
    val parent = mutableMapOf<Int, Int>()
    val queue = ArrayDeque<Int>()

    graph.neighbors(start).filter { it in bridge.unembeddedVertices }.forEach {
        parent[it] = start
        queue.add(it)
    }

    while (queue.isNotEmpty()) {
        val vertex = queue.removeFirst()

        for (neighbor in graph.neighbors(vertex)) {
            if (neighbor in bridge.contacts && neighbor != start) {
                val path = mutableListOf(neighbor, vertex)
                while (path.last() != start) path += parent.getValue(path.last())
                return path.reversed()
            }

            if (neighbor in bridge.unembeddedVertices && neighbor !in parent) {
                parent[neighbor] = vertex
                queue.add(neighbor)
            }
        }
    }

    error("A bridge of a biconnected graph needs two attachments")
}

private fun embedBlock(graph: Graph): Embedding {
    val cycle = initialCycle(graph)
    val embeddedVertices = cycle.toMutableSet()
    val embeddedEdges = cycle.indices.mapTo(mutableSetOf()) { edge(cycle[it], cycle[(it + 1) % cycle.size]) }

    // У цикла две стороны, поэтому сначала есть две грани
    val faces = mutableListOf(cycle, cycle.reversed())

    while (embeddedEdges.size < graph.edges.size) {
        // После новой цепи заново ищем фрагменты и грани, содержащие все их точки примыкания
        val placements = bridges(graph, embeddedVertices, embeddedEdges).map { bridge ->
            val admissibleFaces = faces.indices.filter { index ->
                bridge.contacts.all { contact -> contact in faces[index] }
            }

            BridgePlacement(bridge, admissibleFaces)
        }

        // Фрагмент с меньшим выбором граней нужно разместить первым
        val (bridge, admissibleFaces) = placements.minBy { it.admissibleFaces.size }

        if (admissibleFaces.isEmpty()) return Embedding(emptyMap(), emptyList(), bridge.contacts)

        val path = bridgePath(graph, bridge)
        val faceIndex = admissibleFaces.first()
        val (firstFace, secondFace) = splitFace(faces[faceIndex], path)

        faces[faceIndex] = firstFace
        faces += secondFace
        embeddedVertices.addAll(path)

        for (index in 0 until path.lastIndex) {
            embeddedEdges += edge(path[index], path[index + 1])
        }
    }

    return Embedding(rotationFromFaces(graph, faces), faces)
}

private fun splitFace(face: List<Int>, path: List<Int>): Pair<List<Int>, List<Int>> {
    val startIndex = face.indexOf(path.first())
    val boundary = face.drop(startIndex) + face.take(startIndex)
    val endIndex = boundary.indexOf(path.last())
    // Новая цепь входит в границы двух новых граней в противоположных направлениях
    val innerPath = path.drop(1).dropLast(1)
    val firstFace = boundary.take(endIndex + 1) + innerPath.asReversed()
    val secondFace = path + boundary.drop(endIndex + 1)
    return firstFace to secondFace
}

// По обходу граней восстанавливаем порядок соседей каждой вершины
private fun rotationFromFaces(graph: Graph, faces: List<List<Int>>): Map<Int, List<Int>> {
    val successors = graph.vertices.associateWith { mutableMapOf<Int, Int>() }

    for (face in faces) for (index in face.indices) {
        val vertex = face[index]
        successors.getValue(vertex)[face[(index + face.size - 1) % face.size]] = face[(index + 1) % face.size]
    }

    return graph.vertices.associateWith { vertex ->
        val next = successors.getValue(vertex)
        val neighbors = mutableListOf<Int>()
        var current = next.keys.min()

        do {
            neighbors += current
            current = next.getValue(current)
        } while (current != neighbors.first())

        check(neighbors.size == graph.neighbors(vertex).size) { "Invalid face rotation" }
        neighbors
    }
}
