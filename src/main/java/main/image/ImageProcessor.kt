package main.image

import main.puzzle.Chip
import main.puzzle.Shape
import main.puzzle.Stat
import main.ui.resource.AppColor
import main.ui.resource.AppImage
import main.util.DoubleKeyHashMap
import main.util.Fn
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
object ImageProcessor {
    private val STAR = AppColor.YELLOW_STAR
    private val LEVEL = AppColor.LEVEL
    private val GRAY = Color(66, 66, 66)
    private val WHITE = Color.WHITE
    private val BLACK = Color.BLACK
    private const val FACTOR_USED = 0.6
    private const val THRESHOLD_STAR = 0.05
    private const val THRESHOLD_COLOR = 0.01
    fun detectChips(image: BufferedImage): List<Rectangle> {
        return detectChips(ColorMatrix(image))
    }

    private fun detectChips(matrix: ColorMatrix): List<Rectangle> {
        val rects_ratio = mutableListOf<Rectangle>()

        // Ratio
        matrix.monochrome(0.5).findRects()
            .filter { r -> 90 <= r.width }
            .filter { r ->
                val ratio = r.height.toDouble() / r.width
                1.3 < ratio && ratio < 1.7
            }.forEach { e -> rects_ratio.add(e) }

        // Remove outliers using width median
        rects_ratio.sortWith(Comparator.comparingInt { o -> o.width })
        val medianWidth = rects_ratio[rects_ratio.size / 2].width
        return rects_ratio.filter { r ->
            abs(
                r.width - medianWidth
            ) < 10
        }
    }

    fun idChip(image: BufferedImage, rect: Rectangle): Chip {
        return idChip(ColorMatrix(image).crop(rect), null)
    }

    private fun idChip(matrix: ColorMatrix, debug: DebugInfo?): Chip {
        //System.out.println("====================");
        val factor = matrix.getWidth().toFloat() / 100

        // System.out.println("Factor: " + factor);
        // Used
        val preStarRect = Rectangle(0, 0, (56 * factor).toInt(), (18 * factor).toInt())
        val usedCM = matrix.crop(preStarRect)
        val colorNUnused = usedCM.monochromeCount(STAR, THRESHOLD_STAR)
        val colorNUsed = usedCM.monochromeCount(used(STAR), THRESHOLD_STAR)
        val used = colorNUsed > colorNUnused
        if (debug != null) {
            debug.used = used
        }
        // System.out.println("Used: " + used);

        // Color
        val orange =
            if (used) used(AppColor.CHIPS[Chip.COLOR_ORANGE]!!) else AppColor.CHIPS[Chip.COLOR_ORANGE]!!
        val blue =
            if (used) used(AppColor.CHIPS[Chip.COLOR_BLUE]!!) else AppColor.CHIPS[Chip.COLOR_BLUE]!!
        val colorNOrange = matrix.monochromeCount(orange, THRESHOLD_COLOR)
        val colorNBlue = matrix.monochromeCount(blue, THRESHOLD_COLOR)
        val color: Int = if (colorNBlue > colorNOrange) Chip.COLOR_BLUE else Chip.COLOR_ORANGE
        if (debug != null) {
            debug.color = color
        }
        // System.out.println("Color: " + color + "=" + Chip.COLORSTRS.get(color));

        // Simplify
        val cm = simplify(matrix, used, color)

        // Star
        val starCM = cm.crop(preStarRect)
        val starRects = filterRects_star(starCM.monochrome(STAR).findRects(), factor)
        val star =
            Fn.limit(starRects.size, Chip.STAR_MIN, Chip.STAR_MAX) //idStar(starRect, factor);
        // System.out.println("Star: " + star);

        // Level
        val levelRects = cm.monochrome(LEVEL).findRects()
        val levelRect = filterRect_level(levelRects, factor)
        var level = 0
        if (levelRect != null) {
            val levelCM = cm.crop(levelRect).monochrome(LEVEL).invert()
            val levelDigitRects = filterRects_levelDigit(levelCM.findRects(), factor)
            level = Fn.limit(idDigits(levelCM, levelDigitRects), 0, 20)

            // idChip_drawRect(debug, used(LEVEL), levelRect);
            // idChip_drawRect(debug, LEVEL, levelDigitRects, levelRect.x, levelRect.y);
        }
        // System.out.println("Level: " + level);

        // Stat Icon
        val statIconAreaRects = filterRects_statIconArea(cm.monochrome(WHITE).findRects(), factor)

        // Color and Shape
        val shapeY1 = (getMaxY(starRects, (10 * factor).toInt()) + 8 * factor).toInt()
        val shapeY2 = (getMinY(statIconAreaRects, (56 * factor).toInt()) + 4 * factor).toInt()
        val shapeAreaRect = Rectangle(0, shapeY1, cm.getWidth(), shapeY2 - shapeY1)
        val shapeAreaCM = cm.crop(shapeAreaRect).monochrome(AppColor.CHIPS[color])
        val shapeRect = filterRect_shape(shapeAreaCM.findRects(), factor.toDouble())
        var shapeRot = ShapeRot(Shape.DEFAULT, 0)
        if (shapeRect != null) {
            val shapeCM = shapeAreaCM.crop(shapeRect)
            shapeRot = idShape(shapeCM)
        }
        val shape = shapeRot.shape!!
        val rotation = shapeRot.rotation
        // System.out.println("Name: " + name);
        // System.out.println("Rotation: " + rotation);

        // Stat
        val leveled = level != 0
        if (debug != null) {
            debug.leveled = leveled
        }
        var dmg = 0
        var brk = 0
        var hit = 0
        var rld = 0
        for (r in statIconAreaRects) {
            val statIconRect = merge(
                cm.crop(Rectangle(r.x + 1, r.y + 1, r.width - 2, r.height - 2)).monochrome(WHITE).invert().findRects()
            )
            //  testImage(cm.crop(r).monochrome(WHITE).invert());
            // System.out.println("SIR: " + statIconRect);
            // idChip_drawRect(debug, Color.MAGENTA.darker(), statIconRect, r.x, r.y);
            val statType = idStatType(cm.crop(r).crop(statIconRect!!))
            if (statType != -1) {
                val statRect = Rectangle(r.x + r.width, (r.y + 2 * factor).toInt(), (28 * factor).toInt(), r.height)
                val statCM = simplify_statDigits(matrix.crop(statRect), used, leveled).monochrome(GRAY).invert()
                val statDigitRects = filterRects_statDigit(statCM.findRects(), factor)
                val stat = idDigits(statCM, statDigitRects)
                when (statType) {
                    Stat.DMG -> dmg =
                        Chip.getPt(Chip.RATE_DMG, shape.getType(), star, level, stat)
                    Stat.BRK -> brk =
                        Chip.getPt(Chip.RATE_BRK, shape.getType(), star, level, stat)
                    Stat.HIT -> hit =
                        Chip.getPt(Chip.RATE_HIT, shape.getType(), star, level, stat)
                    else -> rld = Chip.getPt(Chip.RATE_RLD, shape.getType(), star, level, stat)
                }

                // idChip_drawRect(debug, Color.MAGENTA, statRect);
                // idChip_drawRect(debug, Color.MAGENTA.darker(), statDigitRects, statRect.x, statRect.y);
            }
        }
        val stats = intArrayOf(dmg, brk, hit, rld)
        for (i in 0..3) {
            if (stats[i % 4] < 0 && stats[(i + 1) % 4] >= 0 && stats[(i + 2) % 4] >= 0 && stats[(i + 3) % 4] >= 0) {
                stats[i % 4] = shape.getSize() - stats[(i + 1) % 4] - stats[(i + 2) % 4] - stats[(i + 3) % 4]
                break
            }
        }
        val pt = Stat(
            Fn.limit(stats[0], 0, Chip.PT_MAX),
            Fn.limit(stats[1], 0, Chip.PT_MAX),
            Fn.limit(stats[2], 0, Chip.PT_MAX),
            Fn.limit(stats[3], 0, Chip.PT_MAX)
        )
        // System.out.println("PT: " + pt.toString());

        // System.out.println("-----");
        // idChip_drawRect(debug, WHITE, new Rectangle(0, 0, cm.getWidth(), cm.getHeight()));
        // Star
        // idChip_drawRect(debug, used(STAR), preStarRect);
        // idChip_drawRect(debug, STAR, starRects);
        // Stat Icon
        // statIconRects.forEach((r) -> System.out.println("StatIcon: " + r));
        // idChip_drawRect(debug, Color.MAGENTA, statIconAreaRects);
        //idChip_drawHorizontalLine(image, Color.RED, shapeY1);
        //idChip_drawHorizontalLine(image, Color.RED, shapeY2);
        // Shape
        // idChip_drawRect(debug, Color.GREEN, shapeAreaRect);
        // System.out.println("Name: " + nameRect);
        // idChip_drawRect(debug, Color.GREEN.darker(), shapeRect, shapeAreaRect.x, shapeAreaRect.y);
        // System.out.println("PT: " + pt.toString());
        return Chip(shape, star, color, pt, level, rotation)
    }

    //    private static void idChip_drawRect(DebugInfo debug, Color color, Rectangle rect) {
    //        idChip_drawRect(debug, color, rect, 0, 0);
    //    }
    //
    //    private static void idChip_drawRect(DebugInfo debug, Color color, Collection<Rectangle> rects) {
    //        rects.forEach((rect) -> idChip_drawRect(debug, color, rect));
    //    }
    //
    //    private static void idChip_drawRect(DebugInfo debug, Color color, Collection<Rectangle> rects, int xOffset, int yOffset) {
    //        rects.forEach((rect) -> idChip_drawRect(debug, color, rect, xOffset, yOffset));
    //    }
    //
    //    private static void idChip_drawRect(DebugInfo debug, Color color, Rectangle rect, int xOffset, int yOffset) {
    //        if (debug != null && debug.image != null && rect != null) {
    //            debug.image.drawRect(new Rectangle(
    //                    rect.x + xOffset,
    //                    rect.y + yOffset,
    //                    rect.width + 1,
    //                    rect.height + 1
    //            ), color.getRed(), color.getGreen(), color.getBlue());
    //        }
    //    }
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

    private fun simplify(matrix: ColorMatrix, used: Boolean, color: Int): ColorMatrix {
        return matrix.simplify(used, STAR, LEVEL, WHITE, GRAY, BLACK, AppColor.CHIPS.getValue(color))
    }

    private fun simplify_statDigits(matrix: ColorMatrix, used: Boolean, leveled: Boolean): ColorMatrix {
        val textColor = if (leveled) LEVEL else WHITE
        return matrix.simplify(used, textColor!!, GRAY, Fn.percColor(textColor, GRAY, 0.5))
    }

    private fun filterRects_star(rects: Set<Rectangle>, factor: Float): Set<Rectangle> {
        val out: MutableSet<Rectangle> = hashSetOf()
        rects
            .filter { r -> 3 * factor < r.width }
            .filter { r -> 9 * factor > r.width }
            .filter { r -> 3 * factor < r.height }
            .filter { r -> 9 * factor > r.height }
            .forEach { e -> out.add(e) }
        return out
    }

    private fun filterRect_level(rects: Set<Rectangle>, factor: Float): Rectangle? {
        for (r in rects) {
            if (15 * factor < r.width && 70 * factor < r.x && 90 * factor > r.x) {
                return r
            }
        }
        return null
    }

    private fun filterRects_levelDigit(rects: Set<Rectangle>, factor: Float): Set<Rectangle> {
        val out: MutableSet<Rectangle> = hashSetOf()
        rects
            .filter { r -> 6 * factor > r.width }
            .filter { r -> 8 * factor < r.height }
            .filter { r -> 12 * factor > r.height }
            .forEach { e -> out.add(e) }
        return out
    }

    private fun filterRects_statIconArea(rects: Set<Rectangle>, factor: Float): Set<Rectangle> {
        val out: MutableSet<Rectangle> = hashSetOf()
        rects
            .filter { r -> 18 * factor < r.width }
            .filter { r -> 22 * factor > r.width }
            .filter { r ->
                (0 <= r.x && r.x <= 5 * factor
                        || 45 * factor <= r.x && r.x <= 55 * factor)
            }
            .forEach { e -> out.add(e) }
        return out
    }

    private fun filterRects_statDigit(rects: Set<Rectangle>, factor: Float): Set<Rectangle> {
        val out: MutableSet<Rectangle> = hashSetOf()
        rects
            .filter { r -> 9 * factor > r.width }
            .filter { r -> 12 * factor < r.height }
            .filter { r -> 18 * factor > r.height }
            .forEach { e -> out.add(e) }
        return out
    }

    private val CM_STATS = arrayOf(
        ColorMatrix(AppImage.IP_DMG),
        ColorMatrix(AppImage.IP_BRK),
        ColorMatrix(AppImage.IP_HIT),
        ColorMatrix(AppImage.IP_RLD)
    )

    private fun idStatType(matrix: ColorMatrix): Int {
        val sims = DoubleArray(4)
        for (i in 0..3) {
            sims[i] = matrix.similarity(CM_STATS[i])
        }
        // System.out.println("sim: " + Arrays.toString(sims));
        if (sims[1] < sims[0] && sims[2] < sims[0] && sims[3] < sims[0]) {
            return Stat.DMG
        }
        if (sims[2] < sims[1] && sims[3] < sims[1]) {
            return Stat.BRK
        }
        return if (sims[3] < sims[2]) {
            Stat.HIT
        } else Stat.RLD
    }

    private val CM_DIGITS = arrayOf(
        ColorMatrix(AppImage.IP_DIGITS[0]),
        ColorMatrix(AppImage.IP_DIGITS[1]),
        ColorMatrix(AppImage.IP_DIGITS[2]),
        ColorMatrix(AppImage.IP_DIGITS[3]),
        ColorMatrix(AppImage.IP_DIGITS[4]),
        ColorMatrix(AppImage.IP_DIGITS[5]),
        ColorMatrix(AppImage.IP_DIGITS[6]),
        ColorMatrix(AppImage.IP_DIGITS[7]),
        ColorMatrix(AppImage.IP_DIGITS[8]),
        ColorMatrix(AppImage.IP_DIGITS[9])
    )

    private fun idDigits(monochromed: ColorMatrix, rects: Set<Rectangle>): Int {
        var out = 0
        val rectList = ArrayList(rects)
        rectList.sortWith(Comparator.comparingInt { o: Rectangle -> o.x })
        for (r in rectList) {
            val cropped = monochromed.crop(r)
            val digit = idDigit(cropped)
            out = out * 10 + digit
        }
        return out
    }

    private fun idDigit(monochromed: ColorMatrix): Int {
        var digit = -1
        var maxSim = 0.0
        // double[] sims = new double[10];
        for (i in 0..9) {
            val resource = CM_DIGITS[i]
            val sim = monochromed.similarity(resource)
            // sims[i] = sim;
            if (maxSim < sim) {
                digit = i
                maxSim = sim
            }
        }
        // System.out.println(Arrays.toString(sims));
        return digit
    }

    private val TYPES = arrayOf(Shape.Type._6, Shape.Type._5B, Shape.Type._5A, Shape.Type._4, Shape.Type._3)
    private val SHAPES: DoubleKeyHashMap<Shape, Int, ColorMatrix> =
        object : DoubleKeyHashMap<Shape, Int, ColorMatrix>( // <editor-fold defaultstate="collapsed">
        ) {
            init {
                for (type in TYPES) {
                    for (shape in Shape.getShapes(type)) {
                        for (rotation in 0 until shape.maxRotation) {
                            val resource = genShapeResource(shape, rotation)
                            put(shape, rotation, resource)
                        }
                    }
                }
            }
        }
    private const val SHAPE_EDGE = 1
    private const val SHAPE_SQUARE = 8
    private fun genShapeResource(shape: Shape, rotation: Int): ColorMatrix {
        val pm = Chip.generateMatrix(shape, rotation)
        val out = ColorMatrix(SHAPE_SQUARE * pm.numCol + SHAPE_EDGE * 2, SHAPE_SQUARE * pm.numRow + SHAPE_EDGE * 2)
        for (row in 0 until pm.numRow) {
            for (col in 0 until pm.numCol) {
                if (pm[row, col]!!) {
                    genShape_rect(
                        out,
                        col * SHAPE_SQUARE,
                        row * SHAPE_SQUARE,
                        (col + 1) * SHAPE_SQUARE,
                        (row + 1) * SHAPE_SQUARE
                    )
                } else {
                    genShape_rect(
                        out,
                        col * SHAPE_SQUARE + SHAPE_EDGE,
                        row * SHAPE_SQUARE + SHAPE_EDGE,
                        (col + 1) * SHAPE_SQUARE - SHAPE_EDGE,
                        (row + 1) * SHAPE_SQUARE - SHAPE_EDGE
                    )
                    // Up
                    if (pm[row - 1, col] == null || !pm[row - 1, col]!!) {
                        genShape_rect(
                            out,
                            col * SHAPE_SQUARE + SHAPE_EDGE,
                            row * SHAPE_SQUARE - SHAPE_EDGE,
                            (col + 1) * SHAPE_SQUARE - SHAPE_EDGE,
                            row * SHAPE_SQUARE + SHAPE_EDGE
                        )
                    }
                    // Down
                    if (pm[row + 1, col] == null || !pm[row + 1, col]!!) {
                        genShape_rect(
                            out,
                            col * SHAPE_SQUARE + SHAPE_EDGE,
                            (row + 1) * SHAPE_SQUARE - SHAPE_EDGE,
                            (col + 1) * SHAPE_SQUARE - SHAPE_EDGE,
                            (row + 1) * SHAPE_SQUARE + SHAPE_EDGE
                        )
                    }
                    // Left
                    if (pm[row, col - 1] == null || !pm[row, col - 1]!!) {
                        genShape_rect(
                            out,
                            col * SHAPE_SQUARE - SHAPE_EDGE,
                            row * SHAPE_SQUARE + SHAPE_EDGE,
                            col * SHAPE_SQUARE + SHAPE_EDGE,
                            (row + 1) * SHAPE_SQUARE - SHAPE_EDGE
                        )
                    }
                    // Right
                    if (pm[row, col + 1] == null || !pm[row, col + 1]!!) {
                        genShape_rect(
                            out,
                            (col + 1) * SHAPE_SQUARE - SHAPE_EDGE,
                            row * SHAPE_SQUARE + SHAPE_EDGE,
                            (col + 1) * SHAPE_SQUARE + SHAPE_EDGE,
                            (row + 1) * SHAPE_SQUARE - SHAPE_EDGE
                        )
                    }
                }
            }
        }
        return out
    }

    private fun genShape_rect(out: ColorMatrix, x1: Int, y1: Int, x2: Int, y2: Int) {
        out.fillWhiteRect(x1 + SHAPE_EDGE, y1 + SHAPE_EDGE, x2 + SHAPE_EDGE, y2 + SHAPE_EDGE)
    }

    // </editor-fold>
    private fun getShapeResource(shape: Shape?, rotation: Int): ColorMatrix? {
        return if (!SHAPES.containsKey(shape!!, rotation)) {
            null
        } else SHAPES[shape, rotation]
    }

    private fun idShape(monochromed: ColorMatrix): ShapeRot {
        var s: Shape? = Shape.DEFAULT
        var r = 0
        var maxSim = 0.0
        val monoRatio = getRatio(monochromed)
        for (type in TYPES) {
            for (shape in Shape.getShapes(type)) {
                for (rotation in 0 until shape.maxRotation) {
                    val resource = getShapeResource(shape, rotation)
                    val resourceRatio = getRatio(resource!!)
                    if (abs(monoRatio - resourceRatio) < 0.5) {
                        val sim = monochromed.similarity(resource)
                        if (maxSim < sim) {
                            maxSim = sim
                            s = shape
                            r = rotation
                        }
                    }
                }
            }
        }
        return ShapeRot(s, r)
    }

    private fun getRatio(matrix: ColorMatrix): Double {
        return max(matrix.getWidth(), matrix.getHeight()).toDouble() / min(matrix.getWidth(), matrix.getHeight())
    }

    private fun filterRect_shape(rects: Set<Rectangle?>, factor: Double): Rectangle? {
        val filtered: MutableSet<Rectangle?> = HashSet()
        rects.stream()
            .filter { r: Rectangle? -> 10 * factor < r!!.width }
            .filter { r: Rectangle? -> 10 * factor < r!!.height }
            .forEach { e: Rectangle? -> filtered.add(e) }
        return merge(filtered)
    }

    private fun getMinY(rects: Set<Rectangle>, defaultValue: Int): Int {
        return if (rects.isEmpty()) {
            defaultValue
        } else rects.stream().map { r: Rectangle -> r.y }
            .min { x: Int?, y: Int? -> (x!!).compareTo(y!!) }.get()
    }

    private fun getMaxY(rects: Set<Rectangle?>, defaultValue: Int): Int {
        return if (rects.isEmpty()) {
            defaultValue
        } else rects.stream().map { r: Rectangle? -> r!!.y + r.height }
            .max { x: Int?, y: Int? -> (x!!).compareTo(y!!) }.get()
    }

    private fun merge(rects: Collection<Rectangle?>): Rectangle? {
        var xMin = -1
        var xMax = -1
        var yMin = -1
        var yMax = -1
        for (r in rects) {
            val x1 = r!!.x
            val x2 = r.x + r.width
            val y1 = r.y
            val y2 = r.y + r.height
            if (-1 == xMin) {
                xMin = x1
                xMax = x2
                yMin = y1
                yMax = y2
            }
            if (xMin > x1) {
                xMin = x1
            }
            if (xMax < x2) {
                xMax = x2
            }
            if (yMin > y1) {
                yMin = y1
            }
            if (yMax < y2) {
                yMax = y2
            }
        }
        return if (-1 != xMin) {
            Rectangle(xMin, yMin, xMax - xMin, yMax - yMin)
        } else null
    }

    private class DebugInfo {
        var used = false
        var leveled = false
        var color = 0
    }

    private class ShapeRot(val shape: Shape?, val rotation: Int)
}