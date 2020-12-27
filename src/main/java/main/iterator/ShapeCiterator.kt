package main.iterator

import main.puzzle.Chip
import main.puzzle.Shape
import java.util.*

/**
 *
 * @author Bunnyspa
 */
class ShapeCiterator : Iterator<List<Shape?>> {
    private val chipNameCountMap: Map<Shape, Int>
    private val iterators: MutableList<PerTypeShapeCiterator> = mutableListOf()
    private val limited: Boolean
    private var iteratorIndex: Int = 0

    constructor(name: String, star: Int, chips: List<Chip>) {
        val chipTypes: MutableList<Shape.Type> = mutableListOf()
        val chipShapes: MutableList<Shape> = mutableListOf()
        for (c in chips) {
            chipTypes.add(c.getType())
            chipShapes.add(c.shape)
        }
        val typeCandidateCountMap: Map<Shape.Type, Int> = getTypeCount(chipTypes)
        chipNameCountMap = getShapeCount(chipShapes)
        val typeCountMaps: MutableList<Map<Shape.Type, Int>> = mutableListOf()
        val types: Set<Shape.Type> = typeCandidateCountMap.keys
        for (typeCountMap: Map<Shape.Type, Int> in PerTypeShapeCiterator.getTypeCountMaps(
            name,
            star,
            types
        )) {
            if (allTypeEnough(typeCountMap, typeCandidateCountMap)) {
                typeCountMaps.add(typeCountMap)
            }
        }
        for (map: Map<Shape.Type, Int> in typeCountMaps) {
            iterators.add(PerTypeShapeCiterator(map))
        }
        limited = true
    }

    private fun getIterator(): PerTypeShapeCiterator {
        return iterators[iteratorIndex]
    }

    fun total(): Int {
        var sum: Int = 0
        for (it: PerTypeShapeCiterator in iterators) {
            val total: Int = it.total()
            sum += total
        }
        return sum
    }

    fun skip() {
        next()
    }

    fun skip(progress: Int) {
        for (i in 0 until progress) {
            skip()
        }
    }

    override fun hasNext(): Boolean {
        if (iterators.isEmpty()) {
            return false
        }
        if (iteratorIndex < iterators.size - 1) {
            return true
        }
        return getIterator().hasNext()
    }

    override fun next(): List<Shape> {
        if (iterators.isEmpty()) {
            return mutableListOf()
        }
        var next: List<Shape> = getIterator().next()
        if (next.isEmpty() && hasNext()) {
            iteratorIndex++
            next = getIterator().next()
        }
        return next
    }

    fun isNextValid(): Boolean {
        if (iterators.isEmpty()) {
            return false
        }
        if (!limited) {
            return true
        }
        var next = getIterator().peek()
        if (next.isEmpty() && hasNext()) {
            next = iterators[iteratorIndex + 1].peek()
        }
        val nameCount = getShapeCount(next)
        return allShapeEnough(nameCount, chipNameCountMap)
    }

    companion object {
        private fun getTypeCount(types: Collection<Shape.Type>): Map<Shape.Type, Int> {
            val out: MutableMap<Shape.Type, Int> = EnumMap(Shape.Type::class.java)
            for (t in types) {
                if (!out.containsKey(t)) {
                    out[t] = 0
                }
                var nc: Int = out.getValue(t)
                nc++
                out[t] = nc
            }
            return out
        }

        private fun getShapeCount(shapes: Collection<Shape>): Map<Shape, Int> {
            val out: MutableMap<Shape, Int> = EnumMap(Shape::class.java)
            for (s in shapes) {
                if (!out.containsKey(s)) {
                    out[s] = 0
                }
                var nc: Int = out.getValue(s)
                nc++
                out[s] = nc
            }
            return out
        }

        private fun allTypeEnough(required: Map<Shape.Type, Int>, candidates: Map<Shape.Type, Int>): Boolean {
            for (type: Shape.Type in required.keys) {
                if (!candidates.containsKey(type) || required.getValue(type) > (candidates.getValue(type))) {
                    return false
                }
            }
            return true
        }

        private fun allShapeEnough(required: Map<Shape, Int>, candidates: Map<Shape, Int>): Boolean {
            for (type in required.keys) {
                if (!candidates.containsKey(type) || required[type]!! > (candidates[type])!!) {
                    return false
                }
            }
            return true
        }
    }
}