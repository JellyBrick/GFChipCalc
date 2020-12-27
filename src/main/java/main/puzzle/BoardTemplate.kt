package main.puzzle

import main.util.IO
import java.awt.Point
import java.util.*
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
class BoardTemplate : Comparable<BoardTemplate> {
    private lateinit var puzzles: List<Puzzle>
    lateinit var shapeCountMap: MutableMap<Shape, Int>
    val symmetry: Boolean
    private lateinit var placement: PuzzleMatrix<Int>
    private val state: Int

    private constructor(isEnd: Boolean) {
        symmetry = false
        state = if (isEnd) END else EMPTY
    }

    constructor(name: String, star: Int, puzzles: List<Puzzle>) {
        this.puzzles = puzzles
        shapeCountMap = EnumMap(Shape::class.java)

        // Placement
        placement = Board.toPlacement(name, star, puzzles)

        // Symmetry
        symmetry = placement.isSymmetric(Board.UNUSED)
        init()
        state = NORMAL
    }

    constructor(name: String, star: Int, puzzles: List<Puzzle>, symmetry: Boolean) {
        this.puzzles = puzzles
        shapeCountMap = EnumMap(Shape::class.java)

        // Placement
        placement = Board.toPlacement(name, star, puzzles)

        // Symmetry
        this.symmetry = symmetry
        init()
        state = NORMAL
    }

    private fun init() {
        for (p: Puzzle in puzzles) {
            val shape: Shape = p.shape
            if (!shapeCountMap.containsKey(shape)) {
                shapeCountMap[shape] = 0
            }
            shapeCountMap[shape] = shapeCountMap.getValue(shape) + 1
        }
    }

    fun getMatrix(): PuzzleMatrix<Int> {
        return PuzzleMatrix(placement)
    }

    fun isEnd(): Boolean {
        return state == END
    }

    fun isEmpty(): Boolean {
        return state == EMPTY
    }

    fun calcSymmetry(): Boolean {
        return placement.isSymmetric(Board.UNUSED)
    }

    fun getChipRotations(): List<Int> {
        val list: MutableList<Int> = mutableListOf()
        for (p: Puzzle in puzzles) {
            val rotation: Int = p.rotation
            list.add(rotation)
        }
        return list
    }

    fun getChipLocations(): List<Point> {
        val list: MutableList<Point> = mutableListOf()
        for (p: Puzzle in puzzles) {
            list.add(p.location)
        }
        return list
    }

    fun toData(): String {
        // Names
        return (puzzles.joinToString(",") { p: Puzzle -> p.shape.id.toString() }) +
                ";" +  // Rotations
                puzzles.joinToString(",") { p: Puzzle -> p.rotation.toString() } +
                ";" +  // Locations
                puzzles.joinToString(",") { p: Puzzle -> IO.data(p.location) } +
                ";" +  // Symmetry
                IO.data(calcSymmetry())
    }

    fun sortPuzzle() {
        Collections.sort(puzzles)
    }

    override fun compareTo(o: BoardTemplate): Int {
        for (i in 0 until min(puzzles.size, o.puzzles.size)) {
            val nameC: Int = Shape.compare(puzzles[i].shape, o.puzzles[i].shape)
            if (nameC != 0) {
                return nameC
            }
        }
        return 0
    }

    override fun toString(): String {
        if (state == EMPTY) {
            return "EMPTY"
        }
        if (state == END) {
            return "end"
        }
        return puzzles.toString()
    }

    companion object {
        const val END: Int = 0
        const val EMPTY: Int = 1
        const val NORMAL: Int = 2
        private val BOARD_EMPTY: BoardTemplate = BoardTemplate(false)
        private val BOARD_END: BoardTemplate = BoardTemplate(true)
        fun empty(): BoardTemplate {
            return BOARD_EMPTY
        }

        fun end(): BoardTemplate {
            return BOARD_END
        }
    }
}