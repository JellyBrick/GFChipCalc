package main.puzzle.assembly

import main.puzzle.*
import main.setting.Setting
import java.util.*
import kotlin.math.max

/**
 *
 * @author Bunnyspa
 */
class Progress {
    // Progress
    var nComb = 0
    var nDone: Int
    var nTotal = 0
    private val boards: TreeSet<Board>

    constructor(sortType: Int) {
        nDone = 0
        boards = TreeSet(getComparator(sortType))
    }

    constructor(sortType: Int, nComb: Int, progress: Int, progMax: Int, boards: List<Board>?) {
        this.nComb = nComb
        nDone = progress
        nTotal = progMax
        this.boards = TreeSet(getComparator(sortType))
        this.boards.addAll(boards!!)
    }

    fun getBoards(): List<Board> {
        return boards.toList()
    }

    fun getBoardSize(): Int {
        return boards.size
    }

    fun addBoard(board: Board) {
        boards.add(board)
    }

    fun removeLastBoard() {
        boards.pollLast()
    }

    fun getChipFreqs(): List<ChipFreq> {
        val countMap: MutableMap<String?, Int> = HashMap()
        val percMap: MutableMap<String?, Double> = HashMap()
        val chipIDMap: MutableMap<String?, Chip> = HashMap()
        for (b in boards) {
            for (id in b.getChipIDs()) {
                if (!countMap.containsKey(id)) {
                    countMap[id] = 0
                    percMap[id] = 0.0
                    chipIDMap[id] = Chip(b.getChip(id)!!)
                }
                countMap[id] = countMap[id]!! + 1
                percMap[id] = max(percMap[id]!!, b.getStatPerc())
            }
        }
        var max = 1.0
        for (id in countMap.keys) {
            val `val` = countMap[id]!! * percMap[id]!!
            if (max < `val`) {
                max = `val`
            }
        }
        val max_ = max
        val out: MutableList<ChipFreq> = ArrayList(countMap.size)
        for (id in countMap.keys) {
            val count = countMap[id]!!
            val c = chipIDMap[id]
            c!!.resetRotation()
            c.resetLevel()
            val freq = count * percMap[id]!! / max_
            out.add(ChipFreq(c, count, freq))
        }
        out.sortWith({ o1: ChipFreq, o2: ChipFreq -> o2.freq.compareTo(o1.freq) })
        return out
    }

    companion object {
        private fun getComparator(sortType: Int): Comparator<Board> {
            return when (sortType) {
                Setting.BOARD_SORTTYPE_XP -> Comparator { o1, o2 ->
                    val percent = o2.getStatPerc().compareTo(o1.getStatPerc())
                    if (percent != 0) {
                        return@Comparator percent
                    }
                    val xp = o1.xp.compareTo(o2.xp)
                    if (xp != 0) {
                        return@Comparator xp
                    }
                    val ticket = o1.getTicketCount().compareTo(o2.getTicketCount())
                    if (ticket != 0) {
                        return@Comparator ticket
                    }
                    o1.compareTo(o2)
                }
                else -> Comparator { o1, o2 ->
                    val percent = o2.getStatPerc().compareTo(o1.getStatPerc())
                    if (percent != 0) {
                        return@Comparator percent
                    }
                    val ticket = o1.getTicketCount().compareTo(o2.getTicketCount())
                    if (ticket != 0) {
                        return@Comparator ticket
                    }
                    val xp = o1.xp.compareTo(o2.xp)
                    if (xp != 0) {
                        return@Comparator xp
                    }
                    o1.compareTo(o2)
                }
            }
        }
    }
}