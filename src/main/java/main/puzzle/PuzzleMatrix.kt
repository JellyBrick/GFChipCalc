package main.puzzle

import java.awt.Point
import java.io.Serializable
import java.util.*

/**
 *
 * @author Bunnyspa
 * @param <E>
</E> */
class PuzzleMatrix<E> : Serializable {
    var numRow: Int
        private set
    var numCol: Int
        private set
    private var list: MutableList<E?>

    constructor(data: Array<Array<E>>?) {
        numRow = data?.size ?: 0
        numCol = if (numRow > 0) data?.get(0)?.size ?: 0 else 0
        list = ArrayList(numCol * numRow)
        for (r in 0 until numRow) {
            if (data !== null)
                list.addAll(listOf(*data[r]).subList(0, numCol))
        }
    }

    constructor(a: PuzzleMatrix<E>) {
        numRow = a.numRow
        numCol = a.numCol
        list = ArrayList(a.list)
    }

    constructor(height: Int, width: Int, initial: E) {
        numRow = height
        numCol = width
        list = ArrayList(width * height)
        for (i in 0 until height * width) {
            list.add(initial)
        }
    }

    private fun isValid(row: Int, col: Int): Boolean {
        return row in 0 until numRow && 0 <= col && col < numCol
    }

    operator fun get(row: Int, col: Int): E? {
        return if (isValid(row, col)) {
            list[row * numCol + col]
        } else null
    }

    operator fun set(row: Int, col: Int, value: E?) {
        if (isValid(row, col)) {
            list[row * numCol + col] = value
        }
    }

    fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    operator fun contains(e: E): Boolean {
        return list.contains(e)
    }

    fun getNumContaining(e: E): Int {
        var count = 0
        for (cell in list) {
            if (cell == e) {
                count++
            }
        }
        return count
    }

    fun getNumNotContaining(e: E): Int {
        var count = 0
        for (cell in list) {
            if (cell != e) {
                count++
            }
        }
        return count
    }

    fun getPivot(e: E): Point {
        for (row in 0 until numRow) {
            for (col in 0 until numCol) {
                if (get(row, col) === e) {
                    return Point(row, col)
                }
            }
        }
        throw NullPointerException(e.toString())
    }

    fun getPoints(e: E): Set<Point> {
        val ps: MutableSet<Point> = HashSet()
        for (row in 0 until numRow) {
            for (col in 0 until numCol) {
                if (get(row, col) === e) {
                    ps.add(Point(row, col))
                }
            }
        }
        return ps
    }

    private fun getCoordsExcept(e: E): Set<Point> {
        val ps: MutableSet<Point> = HashSet()
        for (row in 0 until numRow) {
            for (col in 0 until numCol) {
                if (get(row, col) !== e) {
                    ps.add(Point(row, col))
                }
            }
        }
        return ps
    }

    fun rotate() {
        val newList: MutableList<E?> = ArrayList(numCol * numRow)
        val newHeight = numCol
        val newWidth = numRow
        for (row in 0 until newHeight) {
            for (col in 0 until newWidth) {
                newList.add(list[(newWidth - col - 1) * newHeight + row])
            }
        }
        numRow = newHeight
        numCol = newWidth
        list = newList
    }

    fun rotate(num: Int) {
        var num = num
        num %= 4
        for (i in 0 until num) {
            rotate()
        }
    }

    fun rotateContent(num: Int, e: E) {
        if (num == 2) {
            var rMin = numRow
            var rMax = 0
            var cMin = numCol
            var cMax = 0
            for (p in getCoordsExcept(e)) {
                if (rMin > p.x) {
                    rMin = p.x
                }
                if (rMax < p.x) {
                    rMax = p.x
                }
                if (cMin > p.y) {
                    cMin = p.y
                }
                if (cMax < p.y) {
                    cMax = p.y
                }
            }
            val pts: MutableList<Point> = ArrayList(getCoordsExcept(e))
            while (pts.isNotEmpty()) {
                val p1 = pts[0]
                val p2 = Point(rMin + rMax - p1.x, cMin + cMax - p1.y)
                if (p1 == p2) {
                    pts.remove(p1)
                } else {
                    val e1 = get(p1.x, p1.y)
                    val e2 = get(p2.x, p2.y)
                    // Swap
                    set(p1.x, p1.y, e2)
                    set(p2.x, p2.y, e1)
                    // Remove
                    pts.remove(p1)
                    pts.remove(p2)
                }
            }
        } else {
            rotate(num)
        }
    }

    fun isSymmetric(e: E): Boolean {
        // Bound
        var xMin = numRow
        var xMax = 0
        var yMin = numCol
        var yMax = 0
        for (p in getCoordsExcept(e)) {
            if (xMin > p.x) {
                xMin = p.x
            }
            if (xMax < p.x) {
                xMax = p.x
            }
            if (yMin > p.y) {
                yMin = p.y
            }
            if (yMax < p.y) {
                yMax = p.y
            }
        }

        // Line -
        var sym = true
        val comp: MutableMap<E?, E?> = HashMap()
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val p1 = get(x, y)
                val p2 = get(xMax - x + xMin, y)
                if (comp.containsKey(p1) && comp[p1] != p2
                    || comp.containsKey(p2) && comp[p2] != p1
                ) {
                    sym = false
                    break
                }
                comp[p1] = p2
                comp[p2] = p1
            }
            if (!sym) {
                break
            }
        }
        if (sym) {
            // System.out.println("Line -");
            return true
        }

        // Line |
        sym = true
        comp.clear()
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val p1 = get(x, y)
                val p2 = get(x, yMax - y + yMin)
                if (comp.containsKey(p1) && comp[p1] != p2) {
                    sym = false
                    break
                }
                comp[p1] = p2
            }
            if (!sym) {
                break
            }
        }
        if (sym) {
            // System.out.println("Line |");
            return true
        }

        // Line \
        sym = true
        comp.clear()
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val p1 = get(x, y)
                val p2 = get(y, x)
                if (comp.containsKey(p1) && comp[p1] != p2
                    || comp.containsKey(p2) && comp[p2] != p1
                ) {
                    sym = false
                    break
                }
                comp[p1] = p2
                comp[p2] = p1
            }
            if (!sym) {
                break
            }
        }
        if (sym) {
            // System.out.println("Line \\");
            return true
        }

        // Line /
        sym = true
        comp.clear()
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val p1 = get(x, y)
                val p2 = get(yMax - y + yMin, xMax - x + xMin)
                if (comp.containsKey(p1) && comp[p1] != p2
                    || comp.containsKey(p2) && comp[p2] != p1
                ) {
                    sym = false
                    break
                }
                comp[p1] = p2
                comp[p2] = p1
            }
            if (!sym) {
                break
            }
        }
        if (sym) {
            // System.out.println("Line /");
            return true
        }

        // Dot
        sym = true
        comp.clear()
        for (x in xMin..xMax) {
            for (y in yMin..yMax) {
                val p1 = get(x, y)
                val p2 = get(xMax - x + xMin, yMax - y + yMin)
                if (comp.containsKey(p1) && comp[p1] != p2) {
                    sym = false
                    break
                }
                comp[p1] = p2
            }
            if (!sym) {
                break
            }
        }
        // System.out.println("Dot"); 
        return sym
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (row in 0 until numRow) {
            for (col in 0 until numCol) {
                when (val data = get(row, col).toString()) {
                    "-1" -> sb.append("  ")
                    "-2" -> sb.append("X ")
                    else -> sb.append(data).append(" ")
                }
            }
            sb.append(System.lineSeparator())
        }
        return sb.toString()
    }

    // For Chip.CHIP_ROTATION_MAP
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || this.javaClass != obj.javaClass) {
            return false
        }
        val array = obj as PuzzleMatrix<E>
        return numCol == array.numCol && numRow == array.numRow && list == array.list
    }

    override fun hashCode(): Int {
        var hash = 5
        hash = 83 * hash + numCol
        hash = 83 * hash + numRow
        hash = 83 * hash + list.hashCode()
        return hash
    }
}