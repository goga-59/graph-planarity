package me.goga59.planarity.io

import me.goga59.planarity.algorithm.Embedding
import me.goga59.planarity.model.Graph
import me.goga59.planarity.model.Point
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.Path as pathOf
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

fun writeResult(input: Path, graph: Graph, embedding: Embedding, positions: Map<Int, Point>?) {
    val name = input.nameWithoutExtension
    val textPath = pathOf("results") / "txt" / "$name.txt"
    val pngPath = pathOf("results") / "png" / "$name.png"

    textPath.createParentDirectories()

    val result = report(input, graph, embedding, positions)
    textPath.writeText(result)
    print(result)

    println("Report: ${textPath.absolutePathString()}")
    if (positions != null) {
        pngPath.createParentDirectories()
        writePng(graph, positions, pngPath)
        println("Drawing: ${pngPath.absolutePathString()}")
    } else {
        pngPath.deleteIfExists()
    }
}

private fun report(input: Path, graph: Graph, embedding: Embedding, positions: Map<Int, Point>?): String =
    buildString {
        appendLine("Input: $input")
        appendLine()
        appendLine("Vertices: ${graph.vertices.size}")
        appendLine("Edges: ${graph.edges.size}")
        appendLine()
        appendLine("Planar: ${if (positions == null) "no" else "yes"}")
        appendLine()

        if (positions == null) {
            appendLine("No admissible face for a bridge attached at: ${embedding.conflict.sorted().joinToString(" ")}")
        } else {
            appendLine("Cyclic edge order:")
            graph.vertices.sorted().forEach { vertex ->
                val neighbors = embedding.rotation.getValue(vertex)
                appendLine("$vertex: ${neighbors.joinToString(" ")}".trimEnd())
            }
            appendLine()
            appendLine("Coordinates:")
            positions.toSortedMap().forEach { (vertex, point) ->
                appendLine("$vertex: (${"%.6f".format(Locale.US, point.x)}, ${"%.6f".format(Locale.US, point.y)})")
            }
        }
    }
