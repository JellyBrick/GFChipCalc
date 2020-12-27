package main.iterator

import main.puzzle.BoardTemplate
import main.puzzle.Chip
import main.puzzle.Shape
import java.util.*

/**
 *
 * @author Bunnyspa
 */
class ChipCiterator constructor(candidates: Collection<Chip?>) : Iterator<List<Chip?>> {
    private val candidateMap: MutableMap<Shape?, MutableList<Chip?>>
    private val shapes: MutableList<Shape?>
    private val combs: MutableList<IntArray?>
    override fun hasNext(): Boolean {
        return combs.isEmpty() || combs[combs.size - 1] != null
    }

    override fun next(): List<Chip?> {
        // Generate output
        val out: MutableList<Chip?> = mutableListOf()
        for (i in combs.indices) {
            val comb: IntArray? = combs[i]
            val candidates: List<Chip?> = (candidateMap[shapes[i]])!!
            for (index: Int in comb!!) {
                out.add(candidates[index])
            }
        }
        // Generate next
        for (i in combs.indices) {
            val comb: IntArray? = nextComb(i)
            if (comb == null) {
                combs[i] = if ((i < combs.size - 1)) nCrInit(combs[i]?.size ?: 0) else null
            } else {
                combs[i] = comb
                break
            }
        }
        return out
    }

    fun init(bt: BoardTemplate) {
        init((bt.shapeCountMap))
    }

    fun init(shapeCountMap: Map<Shape, Int>) {
        shapes.clear()
        combs.clear()
        val keys = ArrayList(shapeCountMap.keys)
        keys.sortWith { o1, o2 -> Shape.compare(o1, o2) }
        for (shape: Shape? in keys) {
            val count: Int = (shapeCountMap[shape])!!
            shapes.add(shape)
            combs.add(nCrInit(count))
        }
    }

    private fun getCandidateSize(shape: Shape?): Int {
        if (!candidateMap.containsKey(shape)) {
            return 0
        }
        return candidateMap.getValue(shape).size
    }

    fun hasEnoughChips(template: BoardTemplate): Boolean {
        val nameCountMap = template.shapeCountMap
        for (shape in nameCountMap.keys) {
            if (nameCountMap.getValue(shape) > getCandidateSize(shape)) {
                return false
            }
        }
        return true
    }

    private fun nextComb(i: Int): IntArray? {
        return nCrNext((combs[i])!!, getCandidateSize(shapes[i]))
    }

    companion object {
        private fun nCrInit(n: Int): IntArray {
            val out = IntArray(n)
            for (i in 0 until n) {
                out[i] = i
            }
            return out
        }

        private fun nCrNext(l: IntArray, max: Int): IntArray? {
            var currentMax: Int = max - 1
            var index: Int = l.size - 1
            while (-1 < index) {
                val nextNum: Int = l[index] + 1
                if (nextNum > currentMax) {
                    currentMax--
                    index--
                } else {
                    val out: IntArray = IntArray(l.size)
                    System.arraycopy(l, 0, out, 0, index)
                    for (i in index until l.size) {
                        out[i] = nextNum + i - index
                    }
                    return out
                }
            }
            return null
        }
    }

    init {
        candidateMap = EnumMap(main.puzzle.Shape::class.java)
        for (c in candidates) {
            val shape: Shape? = c?.shape
            if (!candidateMap.containsKey(shape)) {
                candidateMap[shape] = mutableListOf()
            }
            candidateMap[shape]!!.add(c)
        }
        shapes = mutableListOf()
        combs = mutableListOf()
    }
}