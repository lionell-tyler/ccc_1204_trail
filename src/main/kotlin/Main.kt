import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class STPair (
    var s: String,
    var t: Int
)

data class Vector2 (
    var x: Float, // up or down
    var y: Float // left or right
)

data class PolygonAreaTrace (
    var position: Vector2,
    var directionRight: Vector2,
    var directionLeft: Vector2
)

class Line (
    val start: Vector2,
    val end: Vector2,
) {
    val centerpoint = Vector2(
        (start.x + end.x) / 2F,
        (start.y + end.y) / 2F
    )
    val direction = Vector2(
        Math.signum(end.x - start.x),
        Math.signum(end.y - start.y)
    )
    val casts = listOf(
        turnLeft(direction),
        turnRight(direction),
    )
    val isHorizontal = direction.x == 0F
    val isVertical = direction.y == 0F
}

fun main(args: Array<String>) {

    // read input from input.txt from classpath
    val input = ClassLoader.getSystemClassLoader().getResource("input.txt")!!.readText()
    var parts = input.split(" ")

    var l = parts[0].toInt()

    // read l STPairs from parts
    var pairs = mutableListOf<STPair>()
    for (i in 0 until l) {
        pairs.add(STPair(parts[1 + i * 2], parts[2 + i * 2].toInt()))
    }

    var distance = 0
    var positions = mutableListOf<Vector2>(Vector2(0f, 0f))
    var direction = Vector2(1f, 0f)
    var functionalRotation = 0
    for(pair in pairs) {
        for(i in 0 until pair.t) {
            var s = pair.s
            for (move in s) {
                if (move == 'F') {
                    distance++
                    positions.add(Vector2(positions.last().x + direction.x, positions.last().y + direction.y))
                } else if (move == 'R') {
                    // turn direction right
                    direction = turnRight(direction)
                    functionalRotation ++
                } else if (move == 'L') {
                    // turn direction left
                    direction = turnLeft(direction)
                    functionalRotation --
                }
            }
        }
    }

    // calculate bounding box of positions
    var xmin = positions.minByOrNull { it.x }!!.x
    var xmax = positions.maxByOrNull { it.x }!!.x
    var ymin = positions.minByOrNull { it.y }!!.y
    var ymax = positions.maxByOrNull { it.y }!!.y

    var minPosition = Vector2(xmin, ymin)
    var maxPosition = Vector2(xmax, ymax)

    // calculate all possible positions between minPosition and maxPosition
    var allPositionsInSquare = mutableListOf<Vector2>()
    for(x in xmin.toInt()..xmax.toInt()) {
        for(y in ymin.toInt()..ymax.toInt()) {
            allPositionsInSquare.add(Vector2(x.toFloat(), y.toFloat()))
        }
    }

    // calculate all fields in square
    val fieldsInSquare = mutableSetOf<Vector2>()
    for(x in xmin.toInt()..xmax.toInt()-1) {
        for(y in ymin.toInt()..ymax.toInt()-1) {
            fieldsInSquare.add(Vector2(x + 0.5F, y + 0.5F))
        }
    }

    // create lines from positions
    var lines = mutableListOf<Line>()
    for(i in 0 until positions.lastIndex) {
        lines.add(
            Line(
                positions[i],
                if (i == positions.lastIndex) positions[0] else positions[i+1]
            )
        )
    }

    var linesPerCenterpoint = lines.groupBy { it.centerpoint }

    var potentialCasts = mutableSetOf<Line>()
    lines@ for(line in lines) {
        var castIndex = if (functionalRotation > 0) 1 else 0
        var cast = line.casts[castIndex]
        var castTestPosition = addVectors(line.centerpoint, cast)
        bounds@ while (isWithinBounds(castTestPosition, minPosition, maxPosition)) {

            val linesOnTestPosition = linesPerCenterpoint[castTestPosition] ?: listOf()
            for(lineOnTestPosition in linesOnTestPosition) {
                if (lineOnTestPosition.casts[castIndex] == oppositeVector(cast)) {
                    potentialCasts.add(Line(line.centerpoint, castTestPosition))
                    continue@lines
                }
            }
            castTestPosition = addVectors(castTestPosition, cast)
        }
    }
    var horizontalCasts = potentialCasts.filter { it.isHorizontal }
    var verticalCasts = potentialCasts.filter { it.isVertical }

    // calculate the inside area of the polygon
    var fieldsInPolygon = mutableSetOf<Vector2>()
    for(horizontalCast in horizontalCasts) {
        for (verticalCast in verticalCasts) {
            var potentialPoint = Vector2(
                horizontalCast.start.x,
                verticalCast.start.y
            )
            var xmin = min(verticalCast.start.x, verticalCast.end.x)
            var xmax = max(verticalCast.start.x, verticalCast.end.x)
            var ymin = min(horizontalCast.start.y, horizontalCast.end.y)
            var ymax = max(horizontalCast.start.y, horizontalCast.end.y)
            if (potentialPoint.x in xmin..xmax && potentialPoint.y in ymin..ymax) {
                fieldsInPolygon.add(potentialPoint)
            }

        }
    }

    // calculate the pockets
    var pockets = mutableSetOf<Vector2>()
    var fieldsNotInThePolygon = fieldsInSquare.filterNot { it in fieldsInPolygon }.toSet()
    for (fieldNotInPolygon in fieldsNotInThePolygon) {
        var northPoint = addVectors(fieldNotInPolygon, Vector2(0.5f, 0f))
        var eastPoint = addVectors(fieldNotInPolygon, Vector2(0f, 0.5f))
        var southPoint = addVectors(fieldNotInPolygon, Vector2(-0.5f, 0f))
        var westPoint = addVectors(fieldNotInPolygon, Vector2(0f, -0.5f))

        var northDirection = Vector2(1f, 0f)
        var eastDirection = Vector2(0f, 1f)
        var southDirection = Vector2(-1f, 0f)
        var westDirection = Vector2(0f, -1f)

        var cast = cast@ { direction: Vector2, start: Vector2 ->
            var currentPoint = start
            while (isWithinBounds(currentPoint, minPosition, maxPosition)) {
                if (linesPerCenterpoint[currentPoint] != null ) {
                    return@cast currentPoint
                }
                currentPoint = addVectors(currentPoint, direction)
            }
            return@cast null
        }

        var isPocket = (
            cast(northDirection, northPoint) != null &&
            cast(southDirection, southPoint) != null
        ) || (
            cast(eastDirection, eastPoint) != null &&
            cast(westDirection, westPoint) != null
        )

        if (isPocket) {
            pockets.add(fieldNotInPolygon)
        }
    }

    var distanceX = xmax - xmin
    var distanceY = ymax - ymin
    var areaOfRectangle = distanceX * distanceY


    var result = "$distance ${areaOfRectangle.toInt()} ${fieldsInPolygon.size} ${pockets.size}"
    println(result)
    // copy distance to clipboard
    var clipboard = Toolkit.getDefaultToolkit().systemClipboard
    var str = StringSelection(result)
    clipboard.setContents(str, null)

    // draw lines as graphics
    val image = BufferedImage(distanceX.toInt() * 10, distanceY.toInt() * 10, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    graphics.color = Color.WHITE
    graphics.fillRect(0, 0, distanceX.toInt() * 10, distanceY.toInt() * 10)


    //draw fields in polygon as fields
    graphics.color = Color.green
    for(field in fieldsInPolygon) {
        var position = substractVectors(field, minPosition)
        graphics.fillRect(
            (position.x * 10).toInt() - 5,
            (position.y * 10).toInt() - 5,
            10,
            10
        )
    }

    // draw pockets as grey field
    graphics.color = Color.gray
    for(pocket in pockets) {
        var position = substractVectors(pocket, minPosition)
        graphics.fillRect(
            (position.x * 10).toInt() - 5,
            (position.y * 10).toInt() - 5,
            10,
            10
        )
    }
    /*
    graphics.color = Color.RED
    for(cast in verticalCasts) {
        var start = substractVectors(cast.start, minPosition)
        var end = substractVectors(cast.end, minPosition)
        graphics.drawLine(
            (start.x * 10).toInt(),
            (start.y * 10).toInt(),
            (end.x * 10).toInt(),
            (end.y * 10).toInt()
        )
    }

    graphics.color = Color.BLUE
    for(cast in horizontalCasts) {
        var start = substractVectors(cast.start, minPosition)
        var end = substractVectors(cast.end, minPosition)
        graphics.drawLine(
            (start.x * 10).toInt(),
            (start.y * 10).toInt(),
            (end.x * 10).toInt(),
            (end.y * 10).toInt()
        )
    }
    */

    graphics.color = Color.BLACK
    // shift all lines to 0,0
    for(line in lines) {
        var start = substractVectors(line.start, minPosition)
        var end = substractVectors(line.end, minPosition)
        graphics.drawLine(
            (start.x * 10).toInt(),
            (start.y * 10).toInt(),
            (end.x * 10).toInt(),
            (end.y * 10).toInt()
        )
    }

    /*
    //draw fields in polygon as points
    graphics.color = Color.magenta
    for(field in fieldsInPolygon) {
        var position = substractVectors(field, minPosition)
        graphics.fillRect(
            (position.x * 10).toInt(),
            (position.y * 10).toInt(),
            1,
            1
        )
    }
    */


    /**for(field in fieldsInSquare) {
        if(fieldsInPolygon.contains(field)) {
            graphics.fillRect(field.x.toInt(), field.y.toInt(), 1, 1)
        }
    }**/

    ImageIO.write(image, "png", File("output.png"))

}

private fun turnLeft(direction: Vector2): Vector2 {
    var direction1 = direction
    if (direction1.x == 1f) {
        direction1 = Vector2(0f, -1f)
    } else if (direction1.y == 1f) {
        direction1 = Vector2(1f, 0f)
    } else if (direction1.x == -1f) {
        direction1 = Vector2(0f, 1f)
    } else if (direction1.y == -1f) {
        direction1 = Vector2(-1f, 0f)
    }
    return direction1
}

private fun turnRight(direction: Vector2): Vector2 {
    var direction1 = direction
    if (direction1.x == 1f) {
        direction1 = Vector2(0f, 1f)
    } else if (direction1.y == 1f) {
        direction1 = Vector2(-1f, 0f)
    } else if (direction1.x == -1f) {
        direction1 = Vector2(0f, -1f)
    } else if (direction1.y == -1f) {
        direction1 = Vector2(1f, 0f)
    }
    return direction1
}

private fun isWithinBounds(position: Vector2, minPosition: Vector2, maxPosition: Vector2): Boolean {
    if (position.x < minPosition.x || position.x > maxPosition.x) {
        return false
    }
    if (position.y < minPosition.y || position.y > maxPosition.y) {
        return false
    }
    return true
}

private fun isVectorPositive(vector: Vector2): Boolean {
    return vector.x >= 0 && vector.y >= 0
}

private fun distance(vector1: Vector2, vector2: Vector2): Float {
    return Math.sqrt(
        (vector1.x.toDouble() - vector2.x.toDouble()).pow(2) +
        (vector1.y.toDouble() - vector2.y.toDouble()).pow(2)
    ).toFloat()
}

private fun addVectors(v1: Vector2, v2: Vector2): Vector2 {
    return Vector2(v1.x + v2.x, v1.y + v2.y)
}

private fun substractVectors(v1: Vector2, v2: Vector2): Vector2 {
    return Vector2(v1.x - v2.x, v1.y - v2.y)
}

private fun oppositeVector(vector: Vector2): Vector2 {
    return Vector2(
        if (vector.x != 0f) -vector.x else 0f,
        if (vector.y != 0f) -vector.y else 0f
    )
}

private fun isHorizontal(vector: Vector2): Boolean {
    return vector.y != 0f && vector.x == 0f
}

private fun isVertical(vector: Vector2): Boolean {
    return vector.x != 0f && vector.y == 0f
}