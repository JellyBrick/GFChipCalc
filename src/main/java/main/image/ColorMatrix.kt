package main.image

import main.util.Fn
import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 *
 * @author Bunnyspa
 */
class ColorMatrix {
    private lateinit var red: Array<IntArray>
    private lateinit var green: Array<IntArray>
    private lateinit var blue: Array<IntArray>

    constructor(width: Int, height: Int) {
        setDimension(Integer.max(0, width), Integer.max(0, height))
    }

    constructor(image: BufferedImage) {
        setData(image)
    }

    constructor(matrix: ColorMatrix) {
        val height = matrix.getHeight()
        val width = matrix.getWidth()
        setDimension(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                red[x][y] = matrix.red[x][y]
                green[x][y] = matrix.green[x][y]
                blue[x][y] = matrix.blue[x][y]
            }
        }
    }

    private fun setDimension(width: Int, height: Int) {
        red = Array(width) { IntArray(height) }
        green = Array(width) { IntArray(height) }
        blue = Array(width) { IntArray(height) }
    }

    private fun setData(image: BufferedImage) {
        setDimension(image.width, image.height)
        for (y in 0 until getHeight()) {
            for (x in 0 until getWidth()) {
                val rgb = image.getRGB(x, y)
                val c = Color(rgb)
                red[x][y] = c.red
                green[x][y] = c.green
                blue[x][y] = c.blue
            }
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    fun getColor(x: Int, y: Int): Color {
        val xx = Fn.limit(x, 0, getWidth() - 1)
        val yy = Fn.limit(y, 0, getHeight() - 1)
        return Color(getRed(xx, yy), getGreen(xx, yy), getBlue(xx, yy))
    }

    fun getColorArray(x: Int, y: Int): IntArray {
        val xx = Fn.limit(x, 0, getWidth() - 1)
        val yy = Fn.limit(y, 0, getHeight() - 1)
        return intArrayOf(getRed(xx, yy), getGreen(xx, yy), getBlue(xx, yy))
    }

    fun getRed(x: Int, y: Int): Int {
        val xx = Fn.limit(x, 0, getWidth() - 1)
        val yy = Fn.limit(y, 0, getHeight() - 1)
        return red[xx][yy]
    }

    fun getGreen(x: Int, y: Int): Int {
        val xx = Fn.limit(x, 0, getWidth() - 1)
        val yy = Fn.limit(y, 0, getHeight() - 1)
        return green[xx][yy]
    }

    fun getBlue(x: Int, y: Int): Int {
        val xx = Fn.limit(x, 0, getWidth() - 1)
        val yy = Fn.limit(y, 0, getHeight() - 1)
        return blue[xx][yy]
    }

    fun getWidth(): Int {
        return red.size
    }

    fun getHeight(): Int {
        return if (red.isEmpty()) {
            0
        } else red[0].size
    }

    fun getImage(): BufferedImage {
        val out = BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB)
        for (y in 0 until getHeight()) {
            for (x in 0 until getWidth()) {
                val c = Color(getRed(x, y), getGreen(x, y), getBlue(x, y))
                out.setRGB(x, y, c.rgb)
            }
        }
        return out
    }

    fun setColor(x: Int, y: Int, red: Int, green: Int, blue: Int) {
        if (!isOB(x, y)) {
            setRed(x, y, red)
            setGreen(x, y, green)
            setBlue(x, y, blue)
        }
    }

    fun setColor(x: Int, y: Int, color: Color) {
        if (!isOB(x, y)) {
            setRed(x, y, color.red)
            setGreen(x, y, color.green)
            setBlue(x, y, color.blue)
        }
    }

    fun setColor(x: Int, y: Int, color: IntArray) {
        if (!isOB(x, y)) {
            setRed(x, y, color[0])
            setGreen(x, y, color[1])
            setBlue(x, y, color[2])
        }
    }

    private fun isOB(x: Int, y: Int): Boolean {
        return isOB(x, y, getWidth(), getHeight())
    }

    fun setRed(x: Int, y: Int, value: Int) {
        red[x][y] = Fn.limit(value, 0, 255)
    }

    fun setGreen(x: Int, y: Int, value: Int) {
        green[x][y] = Fn.limit(value, 0, 255)
    }

    fun setBlue(x: Int, y: Int, value: Int) {
        blue[x][y] = Fn.limit(value, 0, 255)
    }

    fun fillWhiteRect(x1: Int, y1: Int, x2: Int, y2: Int) {
        for (y in y1 until y2) {
            for (x in x1 until x2) {
                setColor(x, y, MAX, MAX, MAX)
            }
        }
    }

    fun crop(rect: Rectangle): ColorMatrix {
        val out = ColorMatrix(rect.width + 1, rect.height + 1)
        for (y in 0 until rect.height + 1) {
            for (x in 0 until rect.width + 1) {
                out.setColor(x, y, getColorArray(rect.x + x, rect.y + y))
            }
        }
        return out
    }

    private fun resize(newWidth: Int, newHeight: Int, smooth: Boolean): ColorMatrix {
        val out = ColorMatrix(newWidth, newHeight)
        if (smooth) {
            for (y in 0 until newHeight) {
                for (x in 0 until newWidth) {
                    val newX = x.toDouble() * getWidth() / newWidth
                    val newY = y.toDouble() * getHeight() / newHeight
                    val xInt = newX.toInt()
                    val yInt = newY.toInt()
                    val xDec = newX - xInt
                    val yDec = newY - yInt
                    val factor = arrayOf(
                        doubleArrayOf((1 - xDec) * (1 - yDec), xDec * (1 - yDec)),
                        doubleArrayOf((1 - xDec) * yDec, xDec * yDec)
                    )
                    val r = convR(xInt, yInt, factor)
                    val g = convG(xInt, yInt, factor)
                    val b = convB(xInt, yInt, factor)
                    out.setColor(x, y, r, g, b)
                }
            }
            return out
        }
        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                val x2 = (x.toFloat() * getWidth() / newWidth).roundToInt()
                val y2 = (y.toFloat() * getHeight() / newHeight).roundToInt()
                out.setColor(x, y, getColorArray(x2, y2))
            }
        }
        return out
    }

    fun monochrome(threshold: Double): ColorMatrix {
        val width = getWidth()
        val height = getHeight()
        val out = ColorMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = getColorArray(x, y)
                var avg = (c[0] + c[1] + c[2]) / 3
                avg = if (avg > 255 * threshold) 255 else 0
                out.setColor(x, y, avg, avg, avg)
            }
        }
        return out
    }

    fun monochrome(color: Color?): ColorMatrix {
        val width = getWidth()
        val height = getHeight()
        val out = ColorMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = if (isSameColor(x, y, color)) 0 else 255
                out.setColor(x, y, c, c, c)
            }
        }
        return out
    }

    fun monochrome(color: Color, threshold: Double): ColorMatrix {
        val width = getWidth()
        val height = getHeight()
        val out = ColorMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = if (isSimColor(x, y, color, threshold)) 0 else 255
                out.setColor(x, y, c, c, c)
            }
        }
        return out
    }

    //
    //    public ColorMatrix xor(ColorMatrix matrix) {
    //        int width = getWidth();
    //        int height = getHeight();
    //        ColorMatrix resized = matrix.resize(width, height, false);
    //        ColorMatrix out = new ColorMatrix(width, height);
    //
    //        for (int y = 0; y < height; y++) {
    //            for (int x = 0; x < width; x++) {
    //                if (isSimColor(x, y, resized.getColor(x, y), THRESHOLD_SIMILARITY)) {
    //                    out.setWhite(x, y);
    //                } else {
    //                    out.setBlack(x, y);
    //                }
    //            }
    //        }
    //        return out;
    //    }
    fun invert(): ColorMatrix {
        val width = getWidth()
        val height = getHeight()
        val out = ColorMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out.setColor(x, y, MAX - getRed(x, y), MAX - getGreen(x, y), MAX - getBlue(x, y))
            }
        }
        return out
    }

    fun simplify(used: Boolean, vararg colors: Color): ColorMatrix {
        val width = getWidth()
        val height = getHeight()
        val out = ColorMatrix(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var mc: Color? = null
                var mv = 1.0
                for (c in colors) {
                    val v = variance(x, y, if (used) used(c) else c)
                    if (v < mv) {
                        mc = c
                        mv = v
                    }
                }
                out.setColor(x, y, mc!!)
            }
        }
        return out
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Convolution Methods">
    fun convR(x: Int, y: Int, factor: Array<DoubleArray>): Int {
        if (factor.isEmpty()) {
            return 0
        }
        val height = factor.size
        val width: Int = factor[0].size
        var out = 0.0
        for (yd in 0 until height) {
            for (xd in 0 until width) {
                val r = getRed(x + xd, y + yd).toDouble()
                val f = factor[yd][xd]
                out += r * f
            }
        }
        return out.toInt()
    }

    fun convG(x: Int, y: Int, factor: Array<DoubleArray>): Int {
        if (factor.isEmpty()) {
            return 0
        }
        val height = factor.size
        val width: Int = factor[0].size
        var out = 0.0
        for (yd in 0 until height) {
            for (xd in 0 until width) {
                val r = getGreen(x + xd, y + yd).toDouble()
                val f = factor[yd][xd]
                out += r * f
            }
        }
        return out.toInt()
    }

    fun convB(x: Int, y: Int, factor: Array<DoubleArray>): Int {
        if (factor.isEmpty()) {
            return 0
        }
        val height = factor.size
        val width: Int = factor[0].size
        var out = 0.0
        for (yd in 0 until height) {
            for (xd in 0 until width) {
                val r = getBlue(x + xd, y + yd).toDouble()
                val f = factor[yd][xd]
                out += r * f
            }
        }
        return out.toInt()
    }

    // </editor-fold>
    fun monochromeCount(color: Color, threshold: Double): Int {
        var out = 0
        val cm = monochrome(color, threshold)
        for (y in 0 until getHeight()) {
            for (x in 0 until getWidth()) {
                if (cm.getRed(x, y) == 0) {
                    out++
                }
            }
        }
        return out
    }

    fun findRects(): Set<Rectangle> {
        val width = getWidth()
        val height = getHeight()
        val temp = ColorMatrix(this)
        val rects: MutableSet<Rectangle> = hashSetOf()
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (temp.getRed(x, y) == 0) {
                    var xMin = x
                    var xMax = x
                    var yMin = y
                    var yMax = y
                    val pts: MutableList<Point> = LinkedList()
                    pts.add(Point(x, y))
                    while (pts.isNotEmpty()) {
                        val pt = pts[0]
                        pts.removeAt(0)
                        temp.setRed(pt.x, pt.y, 127)
                        if (xMin > pt.x) {
                            xMin = pt.x
                        }
                        if (xMax < pt.x) {
                            xMax = pt.x
                        }
                        if (yMin > pt.y) {
                            yMin = pt.y
                        }
                        if (yMax < pt.y) {
                            yMax = pt.y
                        }
                        get8Neighbors(pt, width, height)
                            .filter { p -> temp.getRed(p.x, p.y) == 0 }
                            .filter { p -> !pts.contains(p) }
                            .forEach { e -> pts.add(e) }
                    }
                    rects.add(Rectangle(xMin, yMin, xMax - xMin, yMax - yMin))
                }
            }
        }
        return rects
    }

    fun similarity(matrix: ColorMatrix): Double {
        val width = getWidth()
        val height = getHeight()
        val resized = matrix.resize(width, height, false)
        var count = 0.0
        val total = width * height
        if (total == 0) {
            return 0.0
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (isSimColor(x, y, resized.getColor(x, y), THRESHOLD_SIMILARITY)) {
                    count++
                }
            }
        }
        return count / total
    }

    private fun variance(x: Int, y: Int, c: Color): Double {
        val dR = abs(getRed(x, y) - c.red)
        val dG = abs(getGreen(x, y) - c.green)
        val dB = abs(getBlue(x, y) - c.blue)
        val sqSum = (dR * dR + dG * dG + dB * dB).toDouble()
        return sqSum / SQSUMMAX
    }

    private fun isSimColor(x: Int, y: Int, c: Color, threshold: Double): Boolean {
        return variance(x, y, c) < threshold
    }

    private fun isSameColor(x: Int, y: Int, c: Color?): Boolean {
        return getColor(x, y) == c
    }

    companion object {
        private const val MIN = 0
        private const val MAX = 255
        private const val SQSUMMAX = MAX * MAX * 3
        private fun isOB(x: Int, y: Int, width: Int, height: Int): Boolean {
            return x < 0 || width - 1 < x || y < 0 || height - 1 < y
        }

        private const val FACTOR_USED = 0.6
        private fun used(c: Color): Color {
            val red = c.red
            val green = c.green
            val blue = c.blue
            return Color(
                Fn.limit((red * FACTOR_USED).toInt(), 0, 255),
                Fn.limit((green * FACTOR_USED).toInt(), 0, 255),
                Fn.limit((blue * FACTOR_USED).toInt(), 0, 255)
            )
        }

        private const val THRESHOLD_SIMILARITY = 0.01

        //    private static Set<Point> get4Neighbors(Point p, int width, int height) {
        //        Point[] pts = new Point[]{
        //            new Point(p.x, p.y - 1),
        //            new Point(p.x, p.y + 1),
        //            new Point(p.x - 1, p.y),
        //            new Point(p.x + 1, p.y)
        //        };
        //        Set<Point> out = new HashSet<>();
        //        for (Point pt : pts) {
        //            if (0 <= pt.x && pt.x < width && 0 <= pt.y && pt.y < height) {
        //                out.add(pt);
        //            }
        //        }
        //        return out;
        //    }
        private fun get8Neighbors(p: Point, width: Int, height: Int): Set<Point> {
            val pts = arrayOf(
                Point(p.x, p.y - 1),
                Point(p.x, p.y + 1),
                Point(p.x - 1, p.y),
                Point(p.x + 1, p.y),
                Point(p.x - 1, p.y - 1),
                Point(p.x - 1, p.y + 1),
                Point(p.x + 1, p.y - 1),
                Point(p.x + 1, p.y + 1)
            )
            val out: MutableSet<Point> = HashSet()
            for (pt in pts) {
                if (pt.x in 0 until width && 0 <= pt.y && pt.y < height) {
                    out.add(pt)
                }
            }
            return out
        }
    }
}