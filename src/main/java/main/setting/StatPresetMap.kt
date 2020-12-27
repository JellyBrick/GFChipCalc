package main.setting

import main.App
import main.puzzle.Board
import main.puzzle.Shape
import main.puzzle.Stat
import main.ui.resource.AppText
import main.util.DoubleKeyHashMap
import java.util.*

/**
 *
 * @author Bunnyspa
 */
open class StatPresetMap {
    private val data: DoubleKeyHashMap<String, Int, MutableList<StatPreset>> = DoubleKeyHashMap()
    fun put(name: String, star: Int, typeMin: Shape.Type, stat: Stat, pt: Stat, ptMin: Stat, ptMax: Stat) {
        init(name, star)
        data[name, star]!!.add(StatPreset(stat, pt, ptMin, ptMax, typeMin))
    }

    private fun init(name: String, star: Int) {
        if (!data.containsKey(name, star)) {
            data.put(name, star, mutableListOf())
        }
    }

    fun containsKey(name: String?, star: Int): Boolean {
        return data.containsKey((name)!!, star)
    }

    private operator fun get(name: String?, star: Int): List<StatPreset>? {
        if (containsKey(name, star)) {
            return data[(name)!!, star]
        }
        return null
    }

    operator fun get(name: String?, star: Int, index: Int): StatPreset? {
        if (containsKey(name, star)) {
            return data[(name)!!, star]!![index]
        }
        return null
    }

    fun size(name: String?, star: Int): Int {
        if (containsKey(name, star)) {
            return data[(name)!!, star]!!.size
        }
        return 0
    }

    fun getStrings(app: App, name: String?, star: Int): List<String?> {
        val bps: List<StatPreset>? = get(name, star)
        val out: MutableList<String?> = ArrayList(bps!!.size)
        for (i in bps.indices) {
            val pt: Stat = bps[i].pt
            var item: String?
            item = if (pt == null) {
                app.getText(AppText.CSET_PRESET_OPTION, (i + 1).toString())
            } else {
                pt.toStringSlash()
            }
            val type: Shape.Type = data[(name)!!, star]!![i].typeMin
            item += " (" + type.toString() + (if (type == Shape.Type._6) "" else "-6") + ")"
            out.add(item)
        }
        return out
    }

    fun getTypeFilter(name: String?, star: Int, index: Int): BooleanArray {
        if (!containsKey(name, star)) {
            return BooleanArray(Filter.NUM_TYPE)
        }
        val type: Shape.Type = data[(name)!!, star]!![index].typeMin
        val out = BooleanArray(Filter.NUM_TYPE)
        for (i in 0 until Filter.NUM_TYPE - type.id + 1) {
            out[i] = true
        }
        return out
    }

    companion object {
        val PRESET: StatPresetMap = object : StatPresetMap( // <editor-fold defaultstate="collapsed">
        ) {
            init {
                // BGM71
                put(
                    Board.NAME_BGM71, 5, Shape.Type._5B,
                    Stat(157, 328, 191, 45), Stat(13, 10, 10, 3),
                    Stat(1, 0, 0, 0), Stat(3, 3, 3, 3)
                )
                put(
                    Board.NAME_BGM71, 5, Shape.Type._5B,
                    Stat(189, 328, 140, 45), Stat(16, 10, 7, 3),
                    Stat(1, 0, 1, 0), Stat(4, 3, 1, 3)
                )

                // AGS30
                put(
                    Board.NAME_AGS30,
                    5,
                    Shape.Type._5A,
                    Board.getMaxStat(Board.NAME_AGS30, 5),
                    Board.getMaxPt(Board.NAME_AGS30, 5),
                    Stat(),
                    Stat(3, 5, 5, 5)
                )

                // 2B14
                put(
                    Board.NAME_2B14, 5, Shape.Type._5A,
                    Stat(227, 33, 90, 90), Stat(20, 1, 5, 6),
                    Stat(2, 0, 0, 0), Stat(5, 1, 5, 3)
                )
                put(
                    Board.NAME_2B14, 5, Shape.Type._5A,
                    Stat(227, 58, 80, 90), Stat(20, 2, 4, 6),
                    Stat(2, 0, 0, 0), Stat(5, 2, 1, 3)
                )
                put(
                    Board.NAME_2B14, 5, Shape.Type._5B,
                    Stat(220, 58, 90, 90), Stat(19, 2, 5, 6),
                    Stat(2, 0, 0, 0), Stat(4, 2, 3, 3)
                )

                // M2
                put(
                    Board.NAME_M2,
                    5,
                    Shape.Type._4,
                    Board.getMaxStat(Board.NAME_M2, 5),
                    Board.getMaxPt(Board.NAME_M2, 5),
                    Stat(),
                    Stat(5, 2, 5, 5)
                )

                // AT4
                put(
                    Board.NAME_AT4, 5, Shape.Type._5A,
                    Stat(167, 261, 174, 65), Stat(14, 8, 9, 5),
                    Stat(1, 0, 0, 0), Stat(4, 3, 2, 4)
                )
                put(
                    Board.NAME_AT4, 5, Shape.Type._6,
                    Stat(166, 261, 174, 65), Stat(14, 8, 9, 5),
                    Stat(1, 0, 1, 0), Stat(3, 3, 2, 4)
                )

                // QLZ04
                put(
                    Board.NAME_QLZ04,
                    5,
                    Shape.Type._5B,
                    Board.getMaxStat(Board.NAME_QLZ04, 5),
                    Board.getMaxPt(Board.NAME_QLZ04, 5),
                    Stat(),
                    Stat(3, 4, 3, 4)
                )

                // Mk 153
                put(
                    Board.NAME_MK153, 5, Shape.Type._5B,
                    Stat(195, 273, 140, 75), Stat(17, 9, 7, 5),
                    Stat(0, 0, 1, 0), Stat(4, 5, 2, 3)
                )
                put(
                    Board.NAME_MK153, 5, Shape.Type._5B,
                    Stat(189, 263, 176, 75), Stat(16, 8, 9, 5),
                    Stat(1, 0, 1, 0), Stat(4, 3, 2, 3)
                )
            }
        } // </editor-fold>
    }
}