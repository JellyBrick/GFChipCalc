package main.puzzle.assembly.dxz

import java.util.*
import java.util.function.Consumer
import kotlin.math.max

/**
 *
 * @author Bunnyspa
 */
class ExactCoverMatrix(rows: List<BooleanArray>) {
    private val array: Array<BooleanArray>
    private val nRow: Int = rows.size
    private val nCol: Int
    private val hiddenRowSetStack = Stack<Set<Int>>()
    private val hiddenColSetStack = Stack<Set<Int>>()
    private val hiddenRows: BooleanArray
    private val hiddenCols: BooleanArray
    private val rowCounts: IntArray
    private var nVisibleRow: Int
    private var nVisibleCol: Int
    fun uncover() {
        val hiddenColSet = hiddenColSetStack.pop()
        val hiddenRowSet = hiddenRowSetStack.pop()
        hiddenColSet.forEach(Consumer { j: Int? -> hiddenCols[j!!] = false })
        hiddenRowSet.forEach(Consumer { j: Int? -> hiddenRows[j!!] = false })
        nVisibleCol += hiddenColSet.size
        nVisibleRow += hiddenRowSet.size
    }

    fun cover(r: Int) {
        val hiddenColSet = HashSet<Int>()
        val hiddenRowSet = HashSet<Int>()
        getCols().stream().filter { j: Int -> get(r, j) }.forEach { j: Int ->
            hiddenColSet.add(j)
            hiddenCols[j] = true
            getRows().stream().filter { i: Int -> get(i, j) }.forEach { i: Int ->
                hiddenRowSet.add(i)
                hiddenRows[i] = true
            }
        }
        nVisibleCol -= hiddenColSet.size
        nVisibleRow -= hiddenRowSet.size
        hiddenColSetStack.push(hiddenColSet)
        hiddenRowSetStack.push(hiddenRowSet)
    }

    operator fun get(row: Int, col: Int): Boolean {
        return array[row][col]
    }

    operator fun set(row: Int, col: Int, b: Boolean) {
        array[row][col] = b
    }

    fun getRows(): Set<Int> {
        return getVisibleIndices(hiddenRows)
    }

    fun getCols(): Set<Int> {
        return getVisibleIndices(hiddenCols)
    }

    fun getCol(): Int {
        val cols = getCols()
        var minCol = cols.iterator().next()
        var min = rowCounts[minCol]
        for (c in cols) {
            if (rowCounts[c] < min) {
                min = rowCounts[c]
                minCol = c
            }
        }
        return minCol
    }

    fun isEmpty(): Boolean {
        return nVisibleRow == 0 && nVisibleCol == 0
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (r in hiddenRows.indices) {
            val rHidden = hiddenRows[r]
            val rStr = r.toString()
            sb.append(" ".repeat(max(0, 2 - rStr.length)))
            sb.append(rStr).append(":")
            for (c in hiddenCols.indices) {
                val cHidden = hiddenCols[c]
                if (rHidden && cHidden) {
                    sb.append("+")
                } else if (rHidden) {
                    sb.append("-")
                } else if (cHidden) {
                    sb.append("|")
                } else {
                    sb.append(if (array[r][c]) "1" else "0")
                }
            }
            if (r < hiddenRows.size - 1) {
                sb.append(System.lineSeparator())
            }
        }
        return sb.toString()
    }

    companion object {
        private fun getVisibleIndices(hidden: BooleanArray): Set<Int> {
            val out: MutableSet<Int> = HashSet()
            for (i in hidden.indices) {
                if (!hidden[i]) {
                    out.add(i)
                }
            }
            return out
        }
    }

    init {
        nCol = if (rows.isEmpty()) 0 else rows[0].size
        hiddenRows = BooleanArray(nRow)
        hiddenCols = BooleanArray(nCol)
        rowCounts = IntArray(nCol)
        nVisibleRow = nRow
        nVisibleCol = nCol
        array = Array(nRow) { BooleanArray(nCol) }
        for (r in 0 until nRow) {
            val rowArray = rows[r]
            for (c in 0 until nCol) {
                if (rowArray[c]) {
                    array[r][c] = true
                    rowCounts[c]++
                }
            }
        }
    }
}