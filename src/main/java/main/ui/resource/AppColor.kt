/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.ui.resource

import main.App
import main.puzzle.Chip
import main.puzzle.Shape
import main.setting.Setting
import java.awt.Color

/**
 *
 * @author Bunnyspa
 */
object AppColor {
    val CHIPS: Map<Int, Color> = mapOf(
        Chip.COLOR_ORANGE to Color(240, 107, 65),
        Chip.COLOR_BLUE to Color(111, 137, 218)
    ) // </editor-fold>
    val LEVEL = Color(10, 205, 171)
    val YELLOW_STAR = Color(255, 170, 0)
    val RED_STAR: Color = Color.RED
    fun getPoolColor(app: App?, chip: Chip): Color? {
        if (app == null) {
            return Color.GRAY
        }
        if (chip.getSize() < 5) {
            val i = (chip.getSize() + 1) % 3
            return if (i == 0) app.orange() else if (i == 1) app.green() else app.blue()
        }
        if (chip.shape.getType() == Shape.Type._5A) {
            return app.orange()
        }
        return if (chip.shape.getType() == Shape.Type._5B) {
            app.green()
        } else app.blue()
    }

    object Index {
        private val DEFAULT = arrayOf<Color?>( // <editor-fold defaultstate="collapsed">
            Color(15079755),
            Color(3978315),
            Color(16769305),
            Color(4416472),
            Color(16089649),
            Color(9510580),
            Color(4649200),
            Color(15741670),
            Color(12383756),
            Color(16432830),
            Color(32896),
            Color(15122175),
            Color(10117924),
            Color(16775880),
            Color(8388608),
            Color(11206595),
            Color(8421376),
            Color(16767153),
            Color(117),
            Color(8421504)
        ) // </editor-fold>
        val CB = arrayOf<Color?>( // <editor-fold defaultstate="collapsed">
            Color(15113984),
            Color(5682409),
            Color(40563),
            Color(15787074),
            Color(29362),
            Color(13983232),
            Color(13400487)
        ) // </editor-fold>

        fun colors(alt: Int): Array<Color?> {
            return if (alt % Setting.NUM_COLOR == 1) {
                CB
            } else DEFAULT
        }
    }

    object Three {
        private val DEFAULT = arrayOf(
            Color(14928556), Color(12901541), Color(10335956)
        )
        private val CB = arrayOf(
            Index.CB[0], Index.CB[2], Index.CB[1]
        )

        fun blue(alt: Int): Color? {
            return if (alt % Setting.NUM_COLOR == 1) {
                CB[2]
            } else DEFAULT[2]
        }

        fun green(alt: Int): Color? {
            return if (alt % Setting.NUM_COLOR == 1) {
                CB[1]
            } else DEFAULT[1]
        }

        fun orange(alt: Int): Color? {
            return if (alt % Setting.NUM_COLOR == 1) {
                CB[0]
            } else DEFAULT[0]
        }
    }
}