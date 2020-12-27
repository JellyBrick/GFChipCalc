/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle.assembly

import main.puzzle.Chip
import main.util.IO

/**
 *
 * @author Bunnyspa
 */
class ProgressFile(val cs: CalcSetting?, val ces: CalcExtraSetting?, val p: Progress?) {
    fun toData(): String {
        val lines: MutableList<String?> = mutableListOf()
        lines.add(ces!!.calcMode.toString())
        lines.add(cs!!.boardName)
        lines.add(cs.boardStar.toString())
        lines.add(IO.data(cs.maxLevel))
        lines.add(IO.data(ces.matchColor))
        lines.add(IO.data(cs.rotation))
        lines.add(IO.data(cs.symmetry))
        lines.add(ces.markMin.toString())
        lines.add(ces.markMax.toString())
        lines.add(ces.markType.toString())
        lines.add(ces.sortType.toString())
        lines.add(cs.stat!!.toData())
        lines.add(cs.pt!!.toData())
        lines.add(p!!.nComb.toString())
        lines.add(p.nDone.toString())
        lines.add(p.nTotal.toString())
        lines.add(ces.calcModeTag.toString())

        // Chips
        lines.add(ces.chips.size.toString())
        ces.chips.forEach { c -> lines.add(c.toData()) }

        // Boards
        p.getBoards().forEach { b ->
            lines.add(b.getChipCount().toString())
            b.forEachChip { c: Chip ->
                lines.add(
                    ces.chips.indexOf(c).toString() + ","
                            + c.rotation + ","
                            + IO.data(b.getLocation(c)!!)
                )
            }
        }
        // Done
        return java.lang.String.join(System.lineSeparator(), lines)
    }
}