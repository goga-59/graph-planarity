package me.goga59.planarity.algorithm

import me.goga59.planarity.model.Graph
import me.goga59.planarity.model.Point
import me.goga59.planarity.model.edge
import kotlin.math.abs

// Строит укладку каждой компоненты и размещает компоненты рядом
fun tutteLayout(graph: Graph): Map<Int, Point> {
    val positions = mutableMapOf<Int, Point>()
    var offset = 0.0

    for (component in graph.components()) {
        val vertices = component.sorted()
        // Для каждой компоненты находим свои координаты, затем переносим ее вправо
        val connected = graph.induced(component)

        val local = when (vertices.size) {
            1 -> mapOf(vertices[0] to Point(0.0, 0.0))
            2 -> mapOf(vertices[0] to Point(0.0, 0.0), vertices[1] to Point(4.0, 0.0))
            else -> drawConnected(connected)
        }

        // Сдвиг по горизонтали отделяет компоненты друг от друга
        local.forEach { (vertex, point) -> positions[vertex] = Point(point.x + offset, point.y) }
        offset += 6.0
    }

    return positions
}

private fun drawConnected(graph: Graph): Map<Int, Point> {
    // Дополняем компоненту до графа с треугольными гранями
    completeToMaximalPlanar(graph)

    val outerFace = demoucron(graph).faces.firstOrNull { it.size == 3 }
        ?: error("Triangulation did not produce a triangular face")

    // Выбранную треугольную грань принимаем за внешнюю
    return barycentricLayout(graph, outerFace)
}

private fun completeToMaximalPlanar(graph: Graph) {
    val vertices = graph.vertices.sorted()

    // При четырех и более вершинах максимальный планарный граф 3-связен
    for (i in vertices.indices) {
        for (j in i + 1 until vertices.size) {
            val candidate = edge(vertices[i], vertices[j])
            if (candidate in graph.edges) continue

            // Оставляем новое ребро, только если граф остается планарным
            // Проверка нужна после каждого добавления, потому что подходящие ребра заранее неизвестны
            graph.edges += candidate
            if (!demoucron(graph).isPlanar) graph.edges -= candidate
        }
    }
}

private fun barycentricLayout(graph: Graph, outerFace: List<Int>): Map<Int, Point> {
    // Вершины внешней грани закрепляем в углах треугольника
    val boundaryPositions = mapOf(
        outerFace[0] to Point(0.0, 0.0),
        outerFace[1] to Point(4.0, 0.0),
        outerFace[2] to Point(2.0, 3.5),
    )

    val interiorVertices = graph.vertices.sorted().filterNot(boundaryPositions::containsKey)
    val interiorIndex = interiorVertices.withIndex().associate { (i, vertex) -> vertex to i }
    // Каждая строка задает одну вершину, два последних столбца хранят правые части для координат
    val system = Array(interiorVertices.size) { DoubleArray(interiorVertices.size + 2) }

    // Каждая внутренняя вершина должна быть средним координат своих соседей
    // Координаты трех углов известны заранее, поэтому ставим их справа в уравнениях
    interiorVertices.forEachIndexed { row, vertex ->
        val neighbors = graph.neighbors(vertex)
        system[row][row] = neighbors.size.toDouble()

        neighbors.forEach { neighbor ->
            val point = boundaryPositions[neighbor]

            if (point != null) {
                // Известные координаты внешней грани сразу переносим в правую часть
                system[row][interiorVertices.size] += point.x
                system[row][interiorVertices.size + 1] += point.y
            } else {
                system[row][interiorIndex.getValue(neighbor)] -= 1.0
            }
        }
    }

    // Одним прямым ходом Гаусса обрабатываем обе координаты
    for (column in interiorVertices.indices) {
        // Берем строку с наибольшим по модулю коэффициентом, чтобы уменьшить ошибку округления
        val pivot = (column until interiorVertices.size).maxBy { abs(system[it][column]) }
        check(abs(system[pivot][column]) > 1e-12) { "Singular barycentric system" }

        val rowToSwap = system[column]
        system[column] = system[pivot]
        system[pivot] = rowToSwap

        // Убираем текущую неизвестную из строк ниже
        for (row in column + 1 until interiorVertices.size) {
            val factor = system[row][column] / system[column][column]
            for (cell in column until interiorVertices.size + 2) system[row][cell] -= factor * system[column][cell]
        }
    }

    // Обратным ходом находим координаты внутренних вершин
    val positions = boundaryPositions.toMutableMap()
    for (row in interiorVertices.indices.reversed()) {
        var x = system[row][interiorVertices.size]
        var y = system[row][interiorVertices.size + 1]

        // Подставляем координаты вершин, найденные на предыдущих шагах обратного хода
        for (column in row + 1 until interiorVertices.size) {
            val point = positions.getValue(interiorVertices[column])
            x -= system[row][column] * point.x
            y -= system[row][column] * point.y
        }

        positions[interiorVertices[row]] = Point(x / system[row][row], y / system[row][row])
    }

    return positions
}
