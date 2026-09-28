package me.goga59.planarity

import me.goga59.planarity.algorithm.demoucron
import me.goga59.planarity.algorithm.tutteLayout
import me.goga59.planarity.io.readGraph
import me.goga59.planarity.io.writeResult
import java.io.IOException
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        require(args.size <= 1) { "Usage: planarity [path-to-graph.txt]" }

        val input = Path(args.singleOrNull() ?: "examples/planar.txt")
        val graph = readGraph(input)
        val embedding = demoucron(graph)
        val positions = if (embedding.isPlanar) tutteLayout(graph) else null

        writeResult(input, graph, embedding, positions)
    } catch (ex: IllegalArgumentException) {
        System.err.println("Error: ${ex.message}")
        exitProcess(1)
    } catch (ex: IOException) {
        System.err.println("File error: ${ex.message}")
        exitProcess(1)
    }
}
