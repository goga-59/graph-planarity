package me.goga59.planarity.io

import me.goga59.planarity.model.Graph
import me.goga59.planarity.model.Point
import java.io.IOException
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo
import kotlin.io.path.pathString
import kotlin.math.hypot

private const val BASE_SCALE = 144.0
private const val MIN_VERTEX_DISTANCE_POINTS = 45.0
private const val MAX_IMAGE_SIZE_PIXELS = 6000.0
private const val POINTS_PER_INCH = 72.0
private const val PNG_DPI = 96.0

fun writePng(graph: Graph, positions: Map<Int, Point>, path: Path) {
    val scale = drawingScale(positions.values.toList())

    val dot = buildString {
        appendLine("graph Planarity {")
        appendLine("  graph [overlap=true, splines=false, outputorder=edgesfirst, pad=0.3, dpi=${PNG_DPI.toInt()}];")
        appendLine("  node [shape=circle, fixedsize=true, width=0.4, style=filled, fillcolor=\"#bfdbfe\", color=\"#1d4ed8\", fontcolor=\"#0f172a\"];")
        appendLine("  edge [color=\"#334155\", penwidth=2];")
        positions.toSortedMap().forEach { (vertex, point) ->
            val x = "%.6f".format(Locale.US, point.x * scale)
            val y = "%.6f".format(Locale.US, point.y * scale)
            appendLine("  $vertex [pos=\"$x,$y!\"];")
        }
        graph.edges.forEach { (u, v) -> appendLine("  $u -- $v;") }
        appendLine("}")
    }

    val temporary = createTempFile(path.parent, "planarity-", ".png")

    try {
        val process = try {
            ProcessBuilder("neato", "-n2", "-Tpng", "-o", temporary.pathString)
                .redirectErrorStream(true)
                .start()
        } catch (error: IOException) {
            throw IOException("Graphviz 'neato' was not found in PATH", error)
        }

        process.outputStream.bufferedWriter().use { it.write(dot) }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) throw IOException("Graphviz failed: $output")

        temporary.moveTo(path, overwrite = true)
    } finally {
        temporary.deleteIfExists()
    }
}

private fun drawingScale(points: List<Point>): Double {
    var closest = Double.POSITIVE_INFINITY

    for (i in points.indices) for (j in i + 1 until points.size) {
        closest = minOf(closest, hypot(points[i].x - points[j].x, points[i].y - points[j].y))
    }

    val width = points.maxOf(Point::x) - points.minOf(Point::x)
    val height = points.maxOf(Point::y) - points.minOf(Point::y)
    val span = maxOf(width, height)
    val requested = if (closest > 0 && closest.isFinite()) maxOf(BASE_SCALE, MIN_VERTEX_DISTANCE_POINTS / closest) else BASE_SCALE

    // Увеличиваем расстояния между вершинами, но ограничиваем размер PNG
    val limit = if (span > 0) MAX_IMAGE_SIZE_PIXELS * POINTS_PER_INCH / (PNG_DPI * span) else BASE_SCALE
    return minOf(requested, limit)
}
