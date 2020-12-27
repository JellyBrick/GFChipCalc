/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle

import main.util.DoubleKeyHashMap
import main.util.Fn
import java.awt.Point
import java.util.*

/**
 *
 * @author Bunnyspa
 */
enum class Shape(val id: Int) {
    NONE(0),  // 1 = A
    _1(1),  // 2 = B
    _2(2),  // 3 = C
    _3_I(3), _3_L(4),  // 4 = D
    _4_I(5), _4_O(6), _4_Lm(7), _4_L(8), _4_Zm(9), _4_Z(10), _4_T(11),  // 5A = E
    _5A_Pm(12), _5A_P(13), _5A_I(14), _5A_C(15), _5A_Z(16), _5A_Zm(17), _5A_V(18), _5A_L(19), _5A_Lm(20),  // 5B = F
    _5B_W(21), _5B_Nm(22), _5B_N(23), _5B_Ym(24), _5B_Y(25), _5B_X(26), _5B_T(27), _5B_F(28), _5B_Fm(29),  // 6 = G
    _6_O(30), _6_A(31), _6_D(32), _6_Z(33), _6_Zm(34), _6_Y(35), _6_T(36), _6_I(37), _6_C(38), _6_R(39);

    enum class Type(val id: Int) {
        NONE(0), _1(1), _2(2), _3(3), _4(4), _5A(5), _5B(6), _6(7);

        fun getSize(): Int {
            return id - if (6 <= id) 1 else 0
        }

        override fun toString(): String {
            return when (id) {
                7 -> "6"
                6 -> "5B"
                5 -> "5A"
                4, 3, 2, 1 -> id.toString()
                else -> ""
            }
        }

        companion object {
            private val byId: Map<Int, Type> = object : HashMap<Int, Type>() {
                init {
                    for (e in values()) {
                        put(e.id, e)
                    }
                }
            }

            fun compare(o1: Type, o2: Type): Int {
                return o1.id.compareTo(o2.id)
            }
        }
    }

    fun getSize(): Int {
        if (30 <= id) {
            return 6
        }
        if (21 <= id) {
            return 5
        }
        if (12 <= id) {
            return 5
        }
        if (5 <= id) {
            return 4
        }
        if (3 <= id) {
            return 3
        }
        if (2 == id) {
            return 2
        }
        return if (1 == id) {
            1
        } else 0
    }

    fun getType(): Type {
        if (30 <= id) {
            return Type._6
        }
        if (21 <= id) {
            return Type._5B
        }
        if (12 <= id) {
            return Type._5A
        }
        if (5 <= id) {
            return Type._4
        }
        if (3 <= id) {
            return Type._3
        }
        if (2 == id) {
            return Type._2
        }
        return if (1 == id) {
            Type._1
        } else Type.NONE
    }

    val maxRotation: Int
        get() = MAX_ROTATIONS[this] ?: 1

    fun getPivot(rotation: Int): Point {
        return Point(PIVOTS.getValue(this, rotation))
    }

    fun getPoints(rotation: Int): Set<Point> {
        val points: MutableSet<Point> = HashSet()
        for (point in POINTS.getValue(this, rotation)) {
            points.add(Point(point))
        }
        return points
    }

    companion object {
        val DEFAULT = _1
        private val byId: Map<Int, Shape> = object : HashMap<Int, Shape>() {
            init {
                for (e in values()) {
                    put(e.id, e)
                }
            }
        }
        private val SHAPES_1 = arrayOf(_1)
        private val SHAPES_2 = arrayOf(_2)
        private val SHAPES_3 = arrayOf(_3_I, _3_L)
        private val SHAPES_4 = arrayOf(_4_I, _4_O, _4_Lm, _4_L, _4_Zm, _4_Z, _4_T)
        private val SHAPES_5A = arrayOf(_5A_Pm, _5A_P, _5A_I, _5A_C, _5A_Z, _5A_Zm, _5A_V, _5A_L, _5A_Lm)
        private val SHAPES_5B = arrayOf(_5B_W, _5B_Nm, _5B_N, _5B_Ym, _5B_Y, _5B_X, _5B_T, _5B_F, _5B_Fm)
        private val SHAPES_5 = Fn.concatAll(SHAPES_5A, SHAPES_5B)
        private val SHAPES_6 = arrayOf(_6_O, _6_A, _6_D, _6_Z, _6_Zm, _6_Y, _6_T, _6_I, _6_C, _6_R)
        fun byId(id: Int): Shape {
            return byId.getValue(id)
        }

        fun byName(name: String): Shape {
            return when (name) {
                "1" -> _1
                "2" -> _2
                "3I" -> _3_I
                "3L" -> _3_L
                "4I" -> _4_I
                "4O" -> _4_O
                "4Lm" -> _4_Lm
                "4L" -> _4_L
                "4Zm" -> _4_Zm
                "4Z" -> _4_Z
                "4T" -> _4_T
                "5Pm" -> _5A_Pm
                "5P" -> _5A_P
                "5I" -> _5A_I
                "5C" -> _5A_C
                "5Z" -> _5A_Z
                "5Zm" -> _5A_Zm
                "5V" -> _5A_V
                "5L" -> _5A_L
                "5Lm" -> _5A_Lm
                "5W" -> _5B_W
                "5Nm" -> _5B_Nm
                "5N" -> _5B_N
                "5Ym" -> _5B_Ym
                "5Y" -> _5B_Y
                "5X" -> _5B_X
                "5T" -> _5B_T
                "5F" -> _5B_F
                "5Fm" -> _5B_Fm
                "6O" -> _6_O
                "6A" -> _6_A
                "6D" -> _6_D
                "6Z" -> _6_Z
                "6Zm" -> _6_Zm
                "6Y" -> _6_Y
                "6T" -> _6_T
                "6I" -> _6_I
                "6C" -> _6_C
                "6R" -> _6_R
                else -> NONE
            }
        }

        fun getShapes(type: Type): Array<Shape> {
            return when (type) {
                Type._6 -> SHAPES_6
                Type._5B -> SHAPES_5B
                Type._5A -> SHAPES_5A
                Type._4 -> SHAPES_4
                Type._3 -> SHAPES_3
                Type._2 -> SHAPES_2
                Type._1 -> SHAPES_1
                else -> arrayOf()
            }
        }

        fun getShapes(size: Int): Array<Shape> {
            return when (size) {
                6 -> SHAPES_6
                5 -> SHAPES_5
                4 -> SHAPES_4
                3 -> SHAPES_3
                2 -> SHAPES_2
                1 -> SHAPES_1
                else -> arrayOf()
            }
        }

        private const val O = true
        private const val X = false
        val MATRIX_MAP: Map<Shape, Array<Array<Boolean>>> =
            object : HashMap<Shape, Array<Array<Boolean>>>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    // 1
                    put(_1, arrayOf(arrayOf(O)))

                    // 2
                    put(_2, arrayOf(arrayOf(O), arrayOf(O)))

                    // 3
                    put(_3_I, arrayOf(arrayOf(O), arrayOf(O), arrayOf(O)))
                    put(_3_L, arrayOf(arrayOf(O, X), arrayOf(O, O)))

                    // 4
                    put(_4_I, arrayOf(arrayOf(O, O, O, O)))
                    put(_4_L, arrayOf(arrayOf(O, X), arrayOf(O, X), arrayOf(O, O)))
                    put(_4_Lm, arrayOf(arrayOf(O, X, X), arrayOf(O, O, O)))
                    put(_4_O, arrayOf(arrayOf(O, O), arrayOf(O, O)))
                    put(_4_T, arrayOf(arrayOf(X, O, X), arrayOf(O, O, O)))
                    put(_4_Z, arrayOf(arrayOf(O, O, X), arrayOf(X, O, O)))
                    put(_4_Zm, arrayOf(arrayOf(X, O, O), arrayOf(O, O, X)))

                    // 5A
                    put(_5A_C, arrayOf(arrayOf(O, O, O), arrayOf(O, X, O)))
                    put(_5A_I, arrayOf(arrayOf(O, O, O, O, O)))
                    put(_5A_L, arrayOf(arrayOf(O, X), arrayOf(O, X), arrayOf(O, X), arrayOf(O, O)))
                    put(_5A_Lm, arrayOf(arrayOf(X, O), arrayOf(X, O), arrayOf(X, O), arrayOf(O, O)))
                    put(_5A_P, arrayOf(arrayOf(X, O), arrayOf(O, O), arrayOf(O, O)))
                    put(_5A_Pm, arrayOf(arrayOf(O, X), arrayOf(O, O), arrayOf(O, O)))
                    put(_5A_V, arrayOf(arrayOf(O, X, X), arrayOf(O, X, X), arrayOf(O, O, O)))
                    put(_5A_Z, arrayOf(arrayOf(X, X, O), arrayOf(O, O, O), arrayOf(O, X, X)))
                    put(_5A_Zm, arrayOf(arrayOf(O, X, X), arrayOf(O, O, O), arrayOf(X, X, O)))

                    // 5B
                    put(_5B_F, arrayOf(arrayOf(O, X, X), arrayOf(O, O, O), arrayOf(X, O, X)))
                    put(_5B_Fm, arrayOf(arrayOf(X, X, O), arrayOf(O, O, O), arrayOf(X, O, X)))
                    put(_5B_N, arrayOf(arrayOf(X, O), arrayOf(O, O), arrayOf(O, X), arrayOf(O, X)))
                    put(_5B_Nm, arrayOf(arrayOf(O, X), arrayOf(O, O), arrayOf(X, O), arrayOf(X, O)))
                    put(_5B_T, arrayOf(arrayOf(X, O, X), arrayOf(X, O, X), arrayOf(O, O, O)))
                    put(_5B_W, arrayOf(arrayOf(X, O, O), arrayOf(O, O, X), arrayOf(O, X, X)))
                    put(_5B_X, arrayOf(arrayOf(X, O, X), arrayOf(O, O, O), arrayOf(X, O, X)))
                    put(_5B_Y, arrayOf(arrayOf(X, O), arrayOf(O, O), arrayOf(X, O), arrayOf(X, O)))
                    put(_5B_Ym, arrayOf(arrayOf(O, X), arrayOf(O, O), arrayOf(O, X), arrayOf(O, X)))

                    // 6
                    put(_6_A, arrayOf(arrayOf(O, X, X), arrayOf(O, O, X), arrayOf(O, O, O)))
                    put(_6_C, arrayOf(arrayOf(O, X, X, O), arrayOf(O, O, O, O)))
                    put(_6_D, arrayOf(arrayOf(X, O, O, X), arrayOf(O, O, O, O)))
                    put(_6_I, arrayOf(arrayOf(O, O, O, O, O, O)))
                    put(_6_O, arrayOf(arrayOf(O, O), arrayOf(O, O), arrayOf(O, O)))
                    put(_6_R, arrayOf(arrayOf(X, O, X), arrayOf(O, O, O), arrayOf(O, O, X)))
                    put(_6_T, arrayOf(arrayOf(X, X, O, X), arrayOf(O, O, O, O), arrayOf(X, X, O, X)))
                    put(_6_Y, arrayOf(arrayOf(X, O, X), arrayOf(O, O, O), arrayOf(O, X, O)))
                    put(_6_Z, arrayOf(arrayOf(O, O, O, X), arrayOf(X, O, O, O)))
                    put(_6_Zm, arrayOf(arrayOf(X, O, O, O), arrayOf(O, O, O, X)))
                }
            } // </editor-fold>
        private val MAX_ROTATIONS: Map<Shape, Int> =
            object : HashMap<Shape, Int>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    for (shape in values()) {
                        if (shape == NONE) {
                            continue
                        }
                        val a = PuzzleMatrix(MATRIX_MAP[shape]!!)
                        for (i in 1..4) {
                            val b = PuzzleMatrix(MATRIX_MAP[shape]!!)
                            b.rotate(i)
                            if (a == b) {
                                put(shape, i)
                                break
                            }
                        }
                    }
                }
            } // </editor-fold>
        val PIVOTS: DoubleKeyHashMap<Shape, Int, Point> =
            object : DoubleKeyHashMap<Shape, Int, Point>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    for (shape in values()) {
                        if (shape == NONE) {
                            continue
                        }
                        val matrix = PuzzleMatrix(MATRIX_MAP[shape]!!)
                        for (r in 0 until shape.maxRotation) {
                            val pivot = matrix.getPivot(true)
                            put(shape, r, pivot)
                            matrix.rotate()
                        }
                    }
                }
            } // </editor-fold>
        val POINTS: DoubleKeyHashMap<Shape, Int, Set<Point>> =
            object : DoubleKeyHashMap<Shape, Int, Set<Point>>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    for (shape in values()) {
                        if (shape == NONE) {
                            continue
                        }
                        val matrix = PuzzleMatrix(MATRIX_MAP[shape]!!)
                        for (r in 0 until shape.maxRotation) {
                            val pts: Set<Point> = matrix.getPoints(true)
                            put(shape, r, pts)
                            matrix.rotate()
                        }
                    }
                }
            } // </editor-fold>

        fun compare(o1: Shape, o2: Shape): Int {
            return o1.id.compareTo(o2.id)
        }
    }
}