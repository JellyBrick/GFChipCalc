package main.puzzle

import main.ui.resource.AppText
import main.util.Fn
import main.util.IO
import main.util.Rational
import main.util.Version3
import java.io.Serializable
import java.util.*
import kotlin.math.max

/**
 *
 * @author Bunnyspa
 */
class Chip : Serializable {
    val id: String?
    val shape: Shape
    var pt: Stat? = null
    var initRotation = 0
    var rotation = 0
    var initLevel = 0
    var level = 0
    var star = 0
    var color = 0
    var displayType = 0
    var boardIndex = -1
    var isMarked = false
    val tags: MutableSet<Tag>

    // Pool init
    constructor(shape: Shape) {
        id = null
        this.shape = shape
        pt = null
        tags = HashSet()
    }

    // Pool to inventory init
    constructor(c: Chip, star: Int, color: Int) {
        id = UUID.randomUUID().toString()
        shape = c.shape
        rotation = c.rotation
        initRotation = c.initRotation
        pt = Stat()
        this.star = star
        this.color = color
        tags = hashSetOf()
    }

    // Chip deep copy
    constructor(c: Chip) {
        id = c.id
        shape = c.shape
        star = c.star
        color = c.color
        pt = c.pt
        initLevel = c.initLevel
        level = c.level
        initRotation = c.initRotation
        rotation = c.rotation
        displayType = c.displayType
        isMarked = c.isMarked
        tags = HashSet(c.tags)
    }

    // Pre 5.3.0
    constructor(version: Version3, data: Array<String>, type: Int) {
        if (version.isCurrent(4, 0, 0)) {
            // 4.0.0+
            var i = 0
            id = if (data.size > i) data[i] else UUID.randomUUID().toString()
            i++
            shape = if (data.size > i) Shape.byName(data[i]) else Shape.NONE
            i++
            val dmgPt = if (data.size > i) Fn.limit(data[i].toInt(), 0, getMaxPt()) else 0
            i++
            val brkPt = if (data.size > i) Fn.limit(data[i].toInt(), 0, getMaxPt()) else 0
            i++
            val hitPt = if (data.size > i) Fn.limit(data[i].toInt(), 0, getMaxPt()) else 0
            i++
            val rldPt = if (data.size > i) Fn.limit(data[i].toInt(), 0, getMaxPt()) else 0
            i++
            pt = Stat(dmgPt, brkPt, hitPt, rldPt)
            rotation = if (data.size > i) data[i].toInt() % shape.maxRotation else 0
            i++
            if (type == INVENTORY) {
                initRotation = rotation
                isMarked = data.size > i && "1" == data[i]
                boardIndex = -1
            } else {
                initRotation = if (data.size > i) data[i].toInt() % shape.maxRotation else 0
            }
            i++
            star = if (data.size > i) Fn.limit(data[i].toInt(), 2, 5) else 5
            i++
            level = if (data.size > i) Fn.limit(data[i].toInt(), 0, LEVEL_MAX) else 0
            i++
            if (version.isCurrent(4, 7, 0) && type == COMBINATION) {
                initLevel = if (data.size > i) Fn.limit(data[i].toInt(), 0, LEVEL_MAX) else level
                i++
            } else {
                initLevel = level
            }
            color = if (data.size > i) Fn.limit(data[i].toInt(), 0, AppText.TEXT_MAP_COLOR.size) else 0
            i++
            tags = HashSet()
            if (type == INVENTORY && data.size > i) {
                for (tagStr in data[i].split(",").toTypedArray()) {
                    tags.add(IO.parseTag(tagStr))
                }
            }
        } else {
            // 1.0.0 - 3.0.0
            shape = if (data.isNotEmpty()) Shape.byName(data[0]) else Shape.NONE
            rotation = if (data.size > 1) data[1].toInt() % shape.maxRotation else 0
            if (type == INVENTORY) {
                initRotation = rotation
                val dmgPt = if (data.size > 2) Fn.limit(data[2].toInt(), 0, getMaxPt()) else 0
                val brkPt = if (data.size > 3) Fn.limit(data[3].toInt(), 0, getMaxPt()) else 0
                val hitPt = if (data.size > 4) Fn.limit(data[4].toInt(), 0, getMaxPt()) else 0
                val rldPt = if (data.size > 5) Fn.limit(data[5].toInt(), 0, getMaxPt()) else 0
                pt = Stat(dmgPt, brkPt, hitPt, rldPt)
                star = if (data.size > 6) Fn.limit(data[6].toInt(), 2, 5) else 5
                level = if (data.size > 7) Fn.limit(data[7].toInt(), 0, LEVEL_MAX) else 0
                color = if (data.size > 8) Fn.limit(data[8].toInt(), 0, AppText.TEXT_MAP_COLOR.size) else 0
                isMarked = data.size > 9 && "1" == data[9]
                boardIndex = -1
            } else {
                initRotation = if (data.size > 2) data[2].toInt() % shape.maxRotation else 0
                star = if (data.size > 3) Fn.limit(data[3].toInt(), 2, 5) else 5
                level = if (data.size > 4) Fn.limit(data[4].toInt(), 0, LEVEL_MAX) else 0
                color = if (data.size > 5) Fn.limit(data[5].toInt(), 0, AppText.TEXT_MAP_COLOR.size) else 0
                val dmgPt = if (data.size > 6) Fn.limit(data[6].toInt(), 0, getMaxPt()) else 0
                val brkPt = if (data.size > 7) Fn.limit(data[7].toInt(), 0, getMaxPt()) else 0
                val hitPt = if (data.size > 8) Fn.limit(data[8].toInt(), 0, getMaxPt()) else 0
                val rldPt = if (data.size > 9) Fn.limit(data[9].toInt(), 0, getMaxPt()) else 0
                pt = Stat(dmgPt, brkPt, hitPt, rldPt)
            }
            id = if (data.size > 10) data[10] else UUID.randomUUID().toString()
            tags = HashSet()
        }
    }

    // ImageProcessor
    constructor(
        shape: Shape, star: Int, color: Int, pt: Stat?,
        level: Int, rotation: Int
    ) {
        id = UUID.randomUUID().toString()
        this.shape = shape
        this.star = star
        this.color = color
        this.pt = pt
        initLevel = level
        this.level = level
        this.rotation = rotation % shape.maxRotation
        initRotation = rotation % shape.maxRotation
        tags = HashSet()
    }

    // json (Inventory)
    constructor(
        id: String?, shape: Shape, star: Int, color: Int, pt: Stat?,
        level: Int, rotation: Int
    ) {
        this.id = id
        this.shape = shape
        this.star = star
        this.color = color
        this.pt = pt
        initLevel = level
        this.level = level
        this.rotation = rotation % shape.maxRotation
        initRotation = rotation % shape.maxRotation
        tags = HashSet()
    }

    constructor(
        id: String?,
        shape: Shape, star: Int, color: Int, pt: Stat?,
        level: Int, rotation: Int,
        isMarked: Boolean, tags: MutableSet<Tag>
    ) {
        this.id = id
        this.shape = shape
        this.star = star
        this.color = color
        this.pt = pt
        initLevel = level
        this.level = level
        this.rotation = rotation % shape.maxRotation
        initRotation = rotation % shape.maxRotation
        this.isMarked = isMarked
        this.tags = tags
    }

    // <editor-fold defaultstate="collapsed" desc="Size and Type">
    fun getSize(): Int {
        return shape.getSize()
    }

    fun getType(): Shape.Type {
        return shape.getType()
    }

    fun typeGeq(type: Shape.Type): Boolean {
        return shape.getType() >= type
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Rotation and Ticket">
    //    public final int getMaxRotation() {
    //        return getMaxRotation(shape);
    //    }
    //
    //    public static int getMaxRotation(Shape shape) {
    //        return CHIP_ROTATION_MAP.get(shape);
    //    }

    fun resetRotation() {
        rotation = initRotation
    }

    fun initRotate(i: Int) {
        initRotation = rotation + i
    }

    fun initRotate(direction: Boolean) {
        initRotate(if (direction) 3 else 1)
    }

    fun rotate(i: Int) {
        rotation += i
    }

    fun getNumTicket(): Int {
        return if (rotation != initRotation) star * 10 else 0
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="PT and Stat">
    fun getMaxPt(): Int {
        return getMaxPt(getSize())
    }

    private fun getTotalPts(): Int {
        return pt!!.sum()
    }

    fun isPtValid(): Boolean {
        return getTotalPts() == getSize()
    }

    fun getStat(): Stat {
        return Stat(
            getStat(RATE_DMG, this, pt!!.dmg),
            getStat(RATE_BRK, this, pt!!.brk),
            getStat(RATE_HIT, this, pt!!.hit),
            getStat(RATE_RLD, this, pt!!.rld)
        )
    }

    fun getOldStat(): Stat {
        val dmg = getOldStat(RATE_DMG, this, pt!!.dmg)
        val brk = getOldStat(RATE_BRK, this, pt!!.brk)
        val hit = getOldStat(RATE_HIT, this, pt!!.hit)
        val rld = getOldStat(RATE_RLD, this, pt!!.rld)
        return Stat(dmg, brk, hit, rld)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Level and XP">
    fun resetLevel() {
        level = initLevel
    }

    fun setMinInitLevel() {
        initLevel = 0
    }

    fun setMaxLevel() {
        level = LEVEL_MAX
    }

    fun setMaxInitLevel() {
        initLevel = LEVEL_MAX
    }

    fun getCumulXP(): Int {
        var xp = 0
        for (l in initLevel + 1..LEVEL_MAX) {
            xp += getXP(star, l)
        }
        return xp
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Tag">
    fun containsTag(tag: Tag?): Boolean {
        return tags.stream().anyMatch { t -> t == tag }
    }

    fun setTag(t: Tag, enabled: Boolean) {
        if (enabled) {
            addTag(t)
        } else {
            removeTag(t)
        }
    }

    private fun addTag(t: Tag) {
        tags.add(t)
    }

    private fun removeTag(t: Tag) {
        tags.remove(t)
    }

    fun containsHOCTagName(): Boolean {
        for (hoc in Board.NAMES) {
            for (t in tags) {
                if (hoc == t.name) {
                    return true
                }
            }
        }
        return false
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Matrix">
    fun generateMatrix(shape: Shape = this.shape, rotation: Int = this.rotation): PuzzleMatrix<Boolean> {
        return Companion.generateMatrix(shape, rotation)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Image">

    fun statExists(): Boolean {
        return null != pt
    }

    // </editor-fold>
    fun toData(): String {
        val s = arrayOf(
            id, shape.id.toString(), star.toString(), color.toString(),
            pt!!.toData(), initLevel.toString(), initRotation.toString(),
            IO.data(isMarked),
            IO.data(tags.map { obj -> obj.toData() }, ",")
        )
        return java.lang.String.join(";", *s)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this.javaClass != other.javaClass) {
            return false
        }
        val chip = other as Chip
        return id == chip.id
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 59 * hash + id.hashCode()
        return hash
    }

    override fun toString(): String {
        return id ?: "null"
    }

    companion object {
        val RATE_DMG = Rational(44, 10)
        val RATE_BRK = Rational(127, 10)
        val RATE_HIT = Rational(71, 10)
        val RATE_RLD = Rational(57, 10)
        val RATES = arrayOf(RATE_DMG, RATE_BRK, RATE_HIT, RATE_RLD)
        const val COLOR_ORANGE = 0
        const val COLOR_BLUE = 1
        const val COUNTERCLOCKWISE = true
        const val CLOCKWISE = false
        const val SIZE_MAX = 6
        const val LEVEL_MAX = 20
        const val PT_MAX = 5
        const val STAR_MIN = 2
        const val STAR_MAX = 5
        const val INVENTORY = 0
        const val COMBINATION = 1
        fun getTypeMult(type: Shape.Type, star: Int): Rational {
            val a = if (type.getSize() < 5) 16 else 20
            val b = if (type.getSize() < 3 || type == Shape.Type._5A) 4 else 0
            val c = if (type.getSize() < 4 || type == Shape.Type._5A) 4 else 0
            return Rational(star * a - b - if (3 < star) c else 0, 100)
        }

        fun getMaxPt(size: Int): Int {
            return if (size > 4) size - 1 else size
        }

        fun getPt(rate: Rational, type: Shape.Type, star: Int, level: Int, stat: Int): Int {
            for (pt in 0 until PT_MAX) {
                if (getStat(rate, type, star, level, pt) == stat) {
                    return pt
                }
            }
            return -1
        }

        fun getStat(rate: Rational, c: Chip, pt: Int): Int {
            return getStat(rate, c.getType(), c.star, c.level, pt)
        }

        fun getStat(rate: Rational, type: Shape.Type, star: Int, level: Int, pt: Int): Int {
            val base = Rational(pt).mult(rate).mult(getTypeMult(type, star)).intCeil
            return getLevelMult(level).mult(base).intCeil
        }

        private fun getOldStat(rate: Rational, c: Chip, pt: Int): Int {
            return getOldStat(rate, c.shape, c.star, c.level, pt)
        }

        private fun getOldStat(rate: Rational, shape: Shape, star: Int, level: Int, pt: Int): Int {
            val base = Rational(pt).mult(rate).mult(getTypeMult(shape.getType(), star))
            return getLevelMult(level).mult(base).intCeil
        }

        fun getMaxEffStat(rate: Rational, pt: Int): Int {
            val base = Rational(pt).mult(rate).intCeil
            return getLevelMult(LEVEL_MAX).mult(base).intCeil
        }

        fun getMaxEffStat(rate: Rational, pt: Int, level: Int): Int {
            val base = Rational(pt).mult(rate).intCeil
            return getLevelMult(level).mult(base).intCeil
        }

        fun getLevelMult(level: Int): Rational {
            return if (level < 10) Rational(level).mult(8, 100).add(1) else Rational(level).mult(7, 100).add(11, 10)
        }

        private fun getXP(star: Int, level: Int): Int {
            val xp =
                150 + (level - 1) * 75 + (if (6 <= level) (level - 5) * 75 else 0) + (if (17 <= level) 150 else 0) + if (20 == level) 150 else 0
            return xp * max(0, star - 2) / 3
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Compare">
        fun compare(c1: Chip, c2: Chip): Int {
            return Shape.compare(c1.shape, c2.shape)
        }

        fun compareStar(c1: Chip, c2: Chip): Int {
            return c1.star - c2.star
        }

        fun compareLevel(c1: Chip, c2: Chip): Int {
            return c1.level - c2.level
        }

        fun generateMatrix(shape: Shape, rotation: Int): PuzzleMatrix<Boolean> {
            val matrix = PuzzleMatrix(Shape.MATRIX_MAP[shape])
            matrix.rotate(rotation)
            return matrix
        }
    }
}