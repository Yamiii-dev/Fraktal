import org.openrndr.KEY_ENTER
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix33
import org.openrndr.math.Vector2
import java.io.File
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.random.nextULong

fun RandomMatrix(): Matrix33 {
    val angle = Math.toRadians(Random.nextDouble(0.0, 360.0))
    val scale = Random.nextDouble(0.2, 1.0)
    val tx = Random.nextDouble(-0.5, 0.5)
    val ty = Random.nextDouble(-0.5, 0.5)

    val c = cos(angle) * scale
    val s = sin(angle) * scale

    // 2D affine matrix in homogeneous coordinates:
    return Matrix33(
        c, -s, tx,
        s,  c, ty,
        0.0, 0.0, 1.0
    )
}

const val iterations = 15
const val SCREEN_WIDTH = 1280
const val SCREEN_HEIGHT = 720
const val CENTER_WIDTH = SCREEN_WIDTH / 2
const val CENTER_HEIGHT = SCREEN_HEIGHT / 2

var transform1 = RandomMatrix()
var transform2 = RandomMatrix()
var transform3 = RandomMatrix()

data class Point(
    val position: Vector2,
    val lightness: Double
)

fun main() = application {
    configure {
        width = SCREEN_WIDTH
        height = SCREEN_HEIGHT
    }


    program {
        val style = ShadeStyle()
        style.fragmentTransform = "x_fill = va_color;"
        val center = Vector2(CENTER_WIDTH.toDouble(), CENTER_HEIGHT.toDouble())

        var zoom = 1.0

        mouse.scrolled.listen {
            zoom += (it.rotation.y / 10) * zoom
        }

        var dragging = false

        mouse.buttonDown.listen {
            dragging = true
        }
        mouse.buttonUp.listen {
            dragging = false
        }
        var offset = Vector2.ZERO
        var lastPosition = mouse.position
        var mousePosition = mouse.position
        val startingPoint = Vector2(0.0, 0.0)
        var points = mutableListOf<Point>()
        GetPoints(iterations, startingPoint, points)
        var vb = vertexBuffer(vertexFormat {
            position(3)
            color(4)
        }, points.size)
        vb.put {
            for(p in points){
                write(p.position.xy0)
                val c = ColorRGBa.WHITE.copy(alpha = 0.01)
                write(c)
            }
        }
        points = mutableListOf()
        keyboard.keyDown.listen {
            if(it.key == KEY_SPACEBAR){
                transform1 = RandomMatrix()
                transform2 = RandomMatrix()
                transform3 = RandomMatrix()
                GetPoints(iterations, startingPoint, points)
                vb.put {
                    for(p in points){
                        write(p.position.xy0)
                        val c = ColorRGBa.WHITE.copy(alpha = 0.1)
                        write(c)
                    }
                }
                points = mutableListOf()
            }
            else if(it.key == KEY_ENTER){
                val imageWidth = width * 6
                val imageHeight = height * 6
                val imageCenter = Vector2(imageWidth.toDouble() / 2, imageHeight.toDouble() / 2)
                val rt = renderTarget(imageWidth, imageHeight) {
                    colorBuffer()
                }
                drawer.isolatedWithTarget(rt){
                    drawer.clear(ColorRGBa.BLACK)
                    drawer.shadeStyle = style
                    drawer.stroke = null
                    drawer.translate(center)
                    drawer.scale(center.y)
                    drawer.vertexBuffer(vb, DrawPrimitive.POINTS)
                }
                rt.colorBuffer(0).saveToFile(File("${Random.nextULong()}.png"))
            }
        }

        extend {
            lastPosition = mousePosition
            mousePosition = mouse.position
            if(dragging){
                offset -= (lastPosition - mousePosition) / zoom
            }
            drawer.clear(ColorRGBa.BLACK)
            drawer.shadeStyle = style
            drawer.stroke = null
            drawer.translate(center)
            drawer.translate(-offset)
            drawer.scale(zoom)
            drawer.translate(offset)
            drawer.scale(center.y)
            drawer.vertexBuffer(vb, DrawPrimitive.POINTS)
        }
    }
}


fun GetPoints(iteration: Int, point: Vector2, points: MutableList<Point>) {
    if(iteration == 0) return
    val lightness = ((iterations - iteration).toDouble() / iterations).pow(0.7)
    val pointV3 = point.xy1
    val point1 = (transform1 * pointV3).xy
    val point2 = (transform2 * pointV3).xy
    val point3 = (transform3 * pointV3).xy
    points += listOf(Point(point1, lightness), Point(point2, lightness), Point(point3, lightness))

    GetPoints(iteration - 1, point1, points)
    GetPoints(iteration - 1, point2, points)
    GetPoints(iteration - 1, point3, points)
}