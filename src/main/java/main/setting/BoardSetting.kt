package main.setting

import main.puzzle.Board
import main.puzzle.Stat
import main.util.DoubleKeyHashMap

/**
 *
 * @author Bunnyspa
 */
class BoardSetting {
    private val statMap: DoubleKeyHashMap<String, Int, Stat> = DoubleKeyHashMap()
    private val ptMap: DoubleKeyHashMap<String, Int, Stat>
    private val modeMap: DoubleKeyHashMap<String, Int, Int>
    private val presetIndexMap: DoubleKeyHashMap<String, Int, Int>
    fun toData(): String {
        val lines: MutableList<String> = mutableListOf()
        for (name: String? in Board.NAMES) {
            for (star in 1..5) {
                val ss: String = java.lang.String.join(
                    ";",
                    name, star.toString(), getStatMode(name, star).toString(),
                    getStat(name, star)!!.toData(),
                    getPt(name, star)!!.toData(), getPresetIndex(name, star).toString()
                )
                lines.add(ss)
            }
        }
        return java.lang.String.join(System.lineSeparator(), lines)
    }

    fun setStat(name: String?, star: Int, stat: Stat?) {
        statMap.put((name)!!, star, (stat)!!)
    }

    fun setPt(name: String?, star: Int, pt: Stat?) {
        ptMap.put((name)!!, star, (pt)!!)
    }

    fun setMode(name: String?, star: Int, mode: Int) {
        modeMap.put((name)!!, star, mode)
    }

    fun setPresetIndex(name: String?, star: Int, index: Int) {
        presetIndexMap.put((name)!!, star, index)
    }

    fun getStat(name: String?, star: Int): Stat? {
        if (statMap.containsKey((name)!!, star)) {
            return statMap[(name), star]
        }
        return Stat()
    }

    fun getPt(name: String?, star: Int): Stat? {
        if (ptMap.containsKey((name)!!, star)) {
            return ptMap[(name), star]
        }
        return Stat()
    }

    fun getStatMode(name: String?, star: Int): Int {
        if (modeMap.containsKey((name)!!, star)) {
            return (modeMap[(name), star])!!
        }
        return MAX_DEFAULT
    }

    fun getPresetIndex(name: String?, star: Int): Int {
        if (presetIndexMap.containsKey((name)!!, star)) {
            return (presetIndexMap[(name), star])!!
        }
        return -1
    }

    fun hasDefaultPreset(name: String, star: Int): Boolean {
        if (!StatPresetMap.PRESET.containsKey(name, star)) {
            return false
        }
        if (StatPresetMap.PRESET.size(name, star) != 1) {
            return false
        }
        return (StatPresetMap.PRESET[name, star, 0]!!.stat == Board.getMaxStat(name, star))
    }

    companion object {
        const val MAX_DEFAULT: Int = 0
        const val MAX_STAT: Int = 1
        const val MAX_PT: Int = 2
        const val MAX_PRESET: Int = 3
        fun getPreset(name: String?, star: Int, index: Int): StatPreset? {
            return StatPresetMap.PRESET[name, star, index]
        }
    }

    init {
        ptMap = DoubleKeyHashMap()
        modeMap = DoubleKeyHashMap()
        presetIndexMap = DoubleKeyHashMap()
        for (name: String? in Board.NAMES) {
            for (star in 1..5) {
                statMap.put((name)!!, star, Board.getMaxStat(name, star))
                ptMap.put((name), star, Board.getMaxPt(name, star))
                modeMap.put((name), star, if (star == 5) MAX_PRESET else MAX_DEFAULT)
                presetIndexMap.put((name), star, 0)
            }
        }
    }
}