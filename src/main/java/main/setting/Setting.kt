package main.setting

import main.App
import main.puzzle.Chip
import main.util.IO
import main.util.Version2
import main.util.Version3
import java.util.*
import java.util.function.Consumer

/**
 *
 * @author Bunnyspa
 */
class Setting {
    //// Variables ////
    var updateVersion: Version2 = Version2(1, 0)

    // Display
    var locale: Locale = Locale.getDefault()
    var fontSize: Int = DEFAULT_FONTSIZE
    var colorPreset: Int = COLOR_NORMAL

    // Pool
    var poolOrder: Boolean = DESCENDING
    var poolColor: Int = Chip.COLOR_ORANGE
    var poolStar: Int = 5
    var poolPanelVisible: Boolean = true

    // Inventory Display
    var displayType: Int = DISPLAY_STAT

    // Chip
    var maxLevel: Boolean = true
    var colorMatch: Boolean = true
    var rotation: Boolean = true
    var symmetry: Boolean = false

    // Board
    var boardMarkMin: Int = 0
    var boardMarkMax: Int = 64
    var boardMarkType: Int = BOARD_MARKTYPE_CELL
    var boardSortType: Int = BOARD_SORTTYPE_TICKET

    // Combinator
    var advancedSetting: Boolean = false
    var showProgImage: Boolean = true

    // Board
    var board: BoardSetting = BoardSetting()

    constructor()
    constructor(generalLines: List<String>, boardStatLines: List<String>) {
        try {
            val v: Version3 = Version3(generalLines[0])
            if (v.isCurrent(4, 2, 0)) {
                generalLines.forEach(Consumer { line: String ->
                    // Display
                    if (line.startsWith("UPDATE_VERSION=")) {
                        updateVersion = Version2(afterEqual(line))
                    } else if (line.startsWith("APPEARANCE_LANG=") || line.startsWith("DISPLAY_LANG=")) {
                        locale = Locale.forLanguageTag(afterEqual(line).replace("_", "-"))
                    } else if (line.startsWith("APPEARANCE_FONT=") || line.startsWith("DISPLAY_FONT=")) {
                        val parts: Array<String> = afterEqual(line).split(",").toTypedArray()
                        fontSize = parts[1].toInt()
                    } else if (line.startsWith("DISPLAY_FONTSIZE=")) {
                        fontSize = afterEqual(line).toInt()
                    } else if (line.startsWith("APPEARANCE_COLOR=") || line.startsWith("DISPLAY_COLOR=")) {
                        colorPreset = afterEqual(line).toInt()
                    } //
                    else if (line.startsWith("POOL_ORDER=")) {
                        poolOrder = IO.parseBoolean(afterEqual(line))
                    } else if (line.startsWith("POOL_STAR=")) {
                        poolStar = afterEqual(line).toInt()
                    } else if (line.startsWith("POOL_COLOR=")) {
                        poolColor = afterEqual(line).toInt()
                    } else if (line.startsWith("POOL_VISIBLE=")) {
                        poolPanelVisible = IO.parseBoolean(afterEqual(line))
                    } //
                    else if (line.startsWith("DISPLAY_TYPE=")) {
                        displayType = afterEqual(line).toInt()
                    } //
                    else if (line.startsWith("CHIP_MAXLEVEL=")) {
                        maxLevel = IO.parseBoolean(afterEqual(line))
                    } else if (line.startsWith("CHIP_MATCHCOLOR=")) {
                        colorMatch = IO.parseBoolean(afterEqual(line))
                    } else if (line.startsWith("CHIP_ROTATABLE=")) {
                        rotation = IO.parseBoolean(afterEqual(line))
                    } else if (line.startsWith("CHIP_SYMMETRY=")) {
                        symmetry = IO.parseBoolean(afterEqual(line))
                    } //
                    else if (line.startsWith("BOARD_SORT=")) {
                        boardSortType = afterEqual(line).toInt()
                    } //
                    else if (line.startsWith("ADVANCED_SETTING=")) {
                        advancedSetting = IO.parseBoolean(afterEqual(line))
                    } else if (line.startsWith("COMB_HIDEPROG=") || line.startsWith("COMB_SHOWPROG=")) {
                        showProgImage = IO.parseBoolean(afterEqual(line))
                    }
                })
                if (advancedSetting) {
                    colorMatch = true
                }
                // Board
                board = IO.parseBS(boardStatLines, advancedSetting)
            } else {
                fontSize = generalLines[1].toInt()
                poolOrder = IO.parseBoolean(generalLines[2])
                poolStar = 5 - generalLines[3].toInt()
                poolColor = generalLines[4].toInt()
                poolPanelVisible = IO.parseBoolean(generalLines[5])
                displayType = generalLines[6].toInt()
                maxLevel = IO.parseBoolean(generalLines[7])
                colorMatch = IO.parseBoolean(generalLines[8])
                rotation = IO.parseBoolean(generalLines[9])
                colorPreset = generalLines[11].toInt()
            }
        } catch (ignored: Exception) {
        }
    }

    fun toData(): String {
        val lines: MutableList<String> = mutableListOf()
        lines.add(App.VERSION.toData())
        lines.add("[$SECTION_GENERAL]")
        lines.add("UPDATE_VERSION=" + updateVersion.toData())
        lines.add("DISPLAY_LANG=" + locale.language + "-" + locale.country)
        lines.add("DISPLAY_FONTSIZE=$fontSize")
        lines.add("DISPLAY_COLOR=$colorPreset")
        lines.add("POOL_VISIBLE=" + IO.data(poolPanelVisible))
        lines.add("POOL_ORDER=" + IO.data(poolOrder))
        lines.add("POOL_STAR=$poolStar")
        lines.add("POOL_COLOR=$poolColor")
        lines.add("DISPLAY_TYPE=$displayType")
        lines.add("CHIP_MAXLEVEL=" + IO.data(maxLevel))
        lines.add("CHIP_MATCHCOLOR=" + IO.data(colorMatch))
        lines.add("CHIP_ROTATABLE=" + IO.data(rotation))
        lines.add("CHIP_SYMMETRY=" + IO.data(symmetry))
        lines.add("BOARD_SORT=$boardSortType")
        lines.add("ADVANCED_SETTING=" + IO.data(advancedSetting))
        lines.add("COMB_SHOWPROG=" + IO.data(showProgImage))
        lines.add("[$SECTION_BOARD]")
        lines.add(board.toData())
        return java.lang.String.join(System.lineSeparator(), lines)
    }

    companion object {
        const val SECTION_GENERAL: String = "General"
        const val SECTION_BOARD: String = "Board"
        const val ASCENDING: Boolean = true
        const val DESCENDING: Boolean = false
        const val DISPLAY_STAT: Int = 0
        const val DISPLAY_PT: Int = 1
        const val NUM_DISPLAY: Int = 2
        const val COLOR_NORMAL: Int = 0
        const val COLOR_COLORBLIND: Int = 1
        const val NUM_COLOR: Int = 2
        const val BOARD_MARKTYPE_CELL: Int = 0
        const val BOARD_MARKTYPE_CHIP: Int = 1
        const val NUM_BOARD_MARKTYPE: Int = 2
        const val BOARD_SORTTYPE_TICKET: Int = 0
        const val BOARD_SORTTYPE_XP: Int = 1
        const val DEFAULT_FONTSIZE: Int = 12
        private fun afterEqual(line: String): String {
            return line.split("=").toTypedArray()[1].trim { it <= ' ' }
        }
    }
}