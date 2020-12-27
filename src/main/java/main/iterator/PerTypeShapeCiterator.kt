package main.iterator

import main.puzzle.Board
import main.puzzle.Chip
import main.puzzle.Shape
import java.util.*
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
internal class PerTypeShapeCiterator : Iterator<List<Shape>> {
    private var next: List<List<Shape>>
    private val total: Int

    constructor(typeCountMap: Map<Shape.Type, Int>) {
        next = initComb(typeCountMap)
        total = total(typeCountMap)
    }

    override fun hasNext(): Boolean {
        return next.isNotEmpty()
    }

    override fun next(): List<Shape> {
        val out: List<Shape> = peek()
        next = nextComb(next)
        return out
    }

    fun peek(): List<Shape> {
        val out: MutableList<Shape> = mutableListOf()
        for (nextType: List<Shape>? in next) {
            out.addAll((nextType)!!)
        }
        return out
    }

    fun total(): Int {
        return total
    }

    companion object {
        private fun nextComb(comb: List<List<Shape>>): List<List<Shape>> {
            var n: Int = comb.size - 1
            while (0 <= n) {
                val chips: List<Shape> = nextComb_type(comb[n])
                if (chips.isEmpty()) {
                    n--
                } else {
                    val out: MutableList<List<Shape>> = ArrayList(comb.size)
                    for (i in 0 until n) {
                        out.add(comb[i])
                    }
                    out.add(chips)
                    for (i in n + 1 until comb.size) {
                        val chip: Shape = comb[i][0]
                        val length: Int = comb[i].size
                        out.add(initComb_type(chip.getType(), length))
                    }
                    return out
                }
            }
            return mutableListOf()
        }

        private fun nextComb_type(chips: List<Shape>): List<Shape> {
            var n: Int = chips.size - 1
            while (0 <= n) {
                val shape: Shape = nextShape(chips[n])
                if (shape == Shape.NONE) {
                    n--
                } else {
                    val out: MutableList<Shape> = ArrayList(chips.size)
                    for (i in 0 until n) {
                        out.add(chips[i])
                    }
                    for (i in n until chips.size) {
                        out.add(shape)
                    }
                    return out
                }
            }
            return mutableListOf()
        }

        private fun nextShape(shape: Shape): Shape {
            val chips: List<Shape> = listOf(*Shape.getShapes(shape.getType()))
            val i: Int = chips.indexOf(shape) + 1
            if (chips.size <= i) {
                return Shape.NONE
            }
            return chips[i]
        }

        private fun initComb(combType: Map<Shape.Type, Int>): List<List<Shape>> {
            val out: MutableList<List<Shape>> = ArrayList(combType.keys.size)
            val types = combType.keys.toMutableList()
            types.sortWith { o1: Shape.Type, o2: Shape.Type -> Shape.Type.compare(o1, o2) }
            for (type: Shape.Type in types) {
                out.add(initComb_type(type, (combType[type])!!))
            }
            return out
        }

        private fun initComb_type(type: Shape.Type, length: Int): List<Shape> {
            val shape: Shape = Shape.getShapes(type)[0]
            val out: MutableList<Shape> = ArrayList(length)
            for (i in 0 until length) {
                out.add(shape)
            }
            return out
        }

        fun getTypeCount(comb: List<Shape>): Map<Shape.Type, Int> {
            val combType: MutableMap<Shape.Type, Int> = EnumMap(main.puzzle.Shape.Type::class.java)
            for (shape: Shape in comb) {
                val type: Shape.Type = shape.getType()
                if (!combType.containsKey(type)) {
                    combType[type] = 0
                }
                var count: Int = (combType[type])!!
                count++
                combType[type] = count
            }
            return combType
        }

        fun total(typeCountMap: Map<Shape.Type, Int>): Int {
            var total: Int = 1
            for (type: Shape.Type in typeCountMap.keys) {
                val nHr: Int = nHr(Shape.getShapes(type).size, (typeCountMap[type])!!)
                total *= nHr
            }
            return total
        }

        private fun nHr(n: Int, r: Int): Int {
            return nCr(n + r - 1, r)
        }

        private fun nCr(n: Int, r: Int): Int {
            var r: Int = r
            r = min(r, n - r)
            var num: Int = 1
            for (i in n - r + 1..n) {
                num *= i
            }
            for (i in 1..r) {
                num /= i
            }
            return num
        }

        fun getTypeCountMaps(name: String, star: Int, types: Set<Shape.Type>): List<Map<Shape.Type, Int>> {
            val nCell: Int = Board.getCellCount(name, star)
            return getTypeCountMaps(nCell, types)
        }

        private fun getTypeCountMaps(nCell: Int, types: Set<Shape.Type>): List<Map<Shape.Type, Int>> {
            val out: MutableList<Map<Shape.Type, Int>> = mutableListOf()
            val typesAvail = types.toMutableList()
            typesAvail.sortWith { o1: Shape.Type, o2: Shape.Type -> Shape.Type.compare(o1, o2) }
            getTypeCountMaps_rec(out, typesAvail, nCell, Stack())
            return out
        }

        private fun getTypeCountMaps_rec(
            out: MutableList<Map<Shape.Type, Int>>,
            typesAvail: List<Shape.Type>,
            nCell: Int,
            typeBuffer: Stack<Shape.Type>
        ) {
            if (nCell == 0) {
                val e: MutableMap<Shape.Type, Int> = EnumMap(main.puzzle.Shape.Type::class.java)
                for (type: Shape.Type in typeBuffer) {
                    if (!e.containsKey(type)) {
                        e[type] = 0
                    }
                    e[type] = e.get(type)!! + 1
                }
                out.add(e)
                return
            }
            val range: Int = min(Chip.SIZE_MAX, nCell)
            for (t: Shape.Type in typesAvail) {
                if (t.getSize() <= range) {
                    typeBuffer.push(t)
                    val typeAvail_new: MutableList<Shape.Type> = mutableListOf()
                    for (t2: Shape.Type in typesAvail) {
                        if (Shape.Type.compare(t, t2) <= 0) {
                            typeAvail_new.add(t2)
                        }
                    }
                    val nCell_new: Int = nCell - t.getSize()
                    getTypeCountMaps_rec(out, typeAvail_new, nCell_new, typeBuffer)
                    typeBuffer.pop()
                }
            }
        }

        private fun toComb(shapes: List<Shape>): List<List<Shape>> {
            val map: MutableMap<Shape.Type, MutableList<Shape>> = EnumMap(main.puzzle.Shape.Type::class.java)
            for (shape: Shape in shapes) {
                val type: Shape.Type = shape.getType()
                if (!map.containsKey(type)) {
                    map[type] = mutableListOf()
                }
                map[type]!!.add(shape)
            }
            val out: MutableList<List<Shape>> = mutableListOf()
            for (type: Shape.Type in Shape.Type.values()) {
                if (map.containsKey(type)) {
                    out.add((map[type])!!)
                }
            }
            return out
        }
    }
}