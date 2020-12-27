package main.setting

import main.puzzle.Chip
import main.puzzle.Stat
import main.puzzle.Tag
import java.util.*
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
class Filter {
    private val stars: BooleanArray = BooleanArray(NUM_STAR)
    private val colors: BooleanArray = BooleanArray(NUM_COLOR)
    private val types: BooleanArray = BooleanArray(NUM_TYPE)
    private val marks: BooleanArray = BooleanArray(NUM_MARK)
    var levelMin: Int = 0
    var levelMax: Int = Chip.LEVEL_MAX
    var ptMin: Stat = Stat()
    var ptMax: Stat = Stat(Chip.PT_MAX)
    val includedTags: MutableSet<Tag?> = HashSet()
    val excludedTags: MutableSet<Tag?> = HashSet()

    // get
    fun getStar(i: Int): Boolean {
        return stars[i]
    }

    fun getColor(i: Int): Boolean {
        return colors[i]
    }

    fun getType(i: Int): Boolean {
        return types[i]
    }

    fun getMark(i: Int): Boolean {
        return marks[i]
    }

    // set
    fun setStar(i: Int, b: Boolean) {
        set(stars, i, b)
    }

    fun setColor(i: Int, b: Boolean) {
        set(colors, i, b)
    }

    fun setType(i: Int, b: Boolean) {
        set(types, i, b)
    }

    fun setMark(i: Int, b: Boolean) {
        set(marks, i, b)
    }

    // setAll
    fun setStars(vararg bools: Boolean) {
        setAll(stars, bools)
    }

    fun setColors(vararg bools: Boolean) {
        setAll(colors, bools)
    }

    fun setTypes(vararg bools: Boolean) {
        setAll(types, bools)
    }

    // anyTrue
    fun anySCTMTrue(): Boolean {
        return anyStarTrue() || anyColorTrue() || anyTypeTrue() || anyMarkTrue()
    }

    fun anyStarTrue(): Boolean {
        return anyTrue(stars)
    }

    fun anyColorTrue(): Boolean {
        return anyTrue(colors)
    }

    fun anyTypeTrue(): Boolean {
        return anyTrue(types)
    }

    fun anyMarkTrue(): Boolean {
        return anyTrue(marks)
    }

    // reset
    fun reset() {
        setAll(stars, BooleanArray(NUM_STAR))
        setAll(colors, BooleanArray(NUM_COLOR))
        setAll(types, BooleanArray(NUM_TYPE))
        setAll(marks, BooleanArray(NUM_MARK))
        levelMin = 0
        levelMax = Chip.LEVEL_MAX
        ptMin = Stat()
        ptMax = Stat(Chip.PT_MAX)
        includedTags.clear()
        excludedTags.clear()
    }

    fun equals(stars: BooleanArray?, types: BooleanArray?, ptMin: Stat?, ptMax: Stat?): Boolean {
        return (Arrays.equals(this.stars, stars)
                && Arrays.equals(this.types, types)
                && (this.ptMin == ptMin) && (this.ptMax == ptMax))
    }

    companion object {
        const val NUM_STAR: Int = 4
        const val NUM_COLOR: Int = 2
        const val NUM_TYPE: Int = 7
        const val NUM_MARK: Int = 2
        private operator fun set(data: BooleanArray, i: Int, b: Boolean) {
            if (i < data.size) {
                data[i] = b
            }
        }

        private fun setAll(data: BooleanArray, bools: BooleanArray) {
            System.arraycopy(bools, 0, data, 0, min(data.size, bools.size))
        }

        private fun anyTrue(list: BooleanArray): Boolean {
            for (b: Boolean in list) {
                if (b) {
                    return true
                }
            }
            return false
        }
    }
}