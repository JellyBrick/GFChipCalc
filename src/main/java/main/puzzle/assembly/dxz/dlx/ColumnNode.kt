/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle.assembly.dxz.dlx

/**
 *
 * @author Bunnyspa
 */
class ColumnNode internal constructor(val colIndex: Int) : DLXNode(-1) {
    var size = 0
    override fun hashCode(): Int {
        var hash = 7
        hash = 29 * hash + colIndex
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as ColumnNode
        return colIndex == other.colIndex
    }

    fun cover() {
        //    System.out.println("Cover");
        //System.out.println("cover: " + colIndex);
        coverLR()
        var i = D
        while (i !== this) {
            var j = i.R
            while (j !== i) {
                j.coverUD()
                j = j.R
            }
            i = i.D
        }
        // System.out.println("Cover Done");
    }

    fun uncover() {
        // System.out.println("Uncover");
        //System.out.println("uncover: " + colIndex);
        var i = U
        while (i !== this) {
            var j = i.L
            while (j !== i) {
                j.uncoverUD()
                j = j.L
            }
            i = i.U
        }
        uncoverLR()
        // System.out.println("Uncover Done");
    }

    init {
        column = this
    }
}