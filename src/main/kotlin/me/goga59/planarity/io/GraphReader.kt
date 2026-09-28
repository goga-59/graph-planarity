package me.goga59.planarity.io

import me.goga59.planarity.model.Graph
import me.goga59.planarity.model.edge
import java.nio.file.Path
import kotlin.io.path.readLines

fun readGraph(path: Path): Graph {
    val whitespace = Regex("\\s+")
    val lines = path.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    val header = lines.firstOrNull()?.split(whitespace)
    require(header?.size == 2) { "First line must contain n and m" }

    val n = header[0].toIntOrNull()
    val m = header[1].toIntOrNull()
    require(n != null && n > 0 && m != null && m >= 0) { "n must be positive and m nonnegative" }
    require(lines.size - 1 == m) { "Expected $m edges, got ${lines.size - 1}" }

    val graph = Graph((1..n).toSet())
    lines.drop(1).forEachIndexed { index, line ->
        val pair = line.split(whitespace)
        require(pair.size == 2) { "Edge ${index + 1}: expected two vertex numbers" }

        val u = pair[0].toIntOrNull()
        val v = pair[1].toIntOrNull()
        require(u != null && v != null && u in 1..n && v in 1..n && u != v) {
            "Edge ${index + 1}: vertices must be distinct numbers from 1 to $n"
        }

        require(graph.edges.add(edge(u, v))) { "Edge ${index + 1}: duplicate $u-$v" }
    }

    return graph
}
