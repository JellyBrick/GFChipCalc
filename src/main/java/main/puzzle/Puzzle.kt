/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle

import main.util.IO
import java.awt.Point

/**
 *
 * @author Bunnyspa
 */
class Puzzle(shape: Shape?, rotation: Int, location: Point?) : Comparable<Puzzle> {
    val shape: Shape = shape!!
    val rotation: Int
    val location: Point
    override fun compareTo(o: Puzzle): Int {
        val shapeCompare: Int = Shape.compare(shape, o.shape)
        return if (shapeCompare != 0) {
            shapeCompare
        } else compareLocation(location, o.location)
    }

    override fun toString(): String {
        return "{" + shape + ", " + rotation + ", " + IO.data(location) + "}"
    }

    companion object {
        fun compareLocation(o1: Point, o2: Point): Int {
            val xCompare = o1.x.compareTo(o2.x)
            return if (xCompare != 0) {
                xCompare
            } else o1.y.compareTo(o2.y)
        }
    }

    init {
        this.rotation = rotation
        this.location = location!!
    }
}