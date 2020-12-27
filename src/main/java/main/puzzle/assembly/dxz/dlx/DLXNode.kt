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
open class DLXNode internal constructor(rowIndex: Int) {
    var U: DLXNode
    var D: DLXNode
    var L: DLXNode
    var R: DLXNode
    var column: ColumnNode? = null
    val rowIndex: Int

    internal constructor(rowIndex: Int, c: ColumnNode?) : this(rowIndex) {
        column = c
    }

    fun linkDown(node: DLXNode): DLXNode {
        node.D = D
        node.D.U = node
        node.U = this
        D = node
        return node
    }

    fun linkRight(node: DLXNode): DLXNode {
        node.R = R
        node.R.L = node
        node.L = this
        R = node
        return node
    }

    fun coverLR() {
        // System.out.println(L + "-" + this + "-" + R);
        // System.out.println(L + "-" + L.R + " ... " + R.L + "-" + R);
        L.R = R
        R.L = L
    }

    fun coverUD() {
        U.D = D
        D.U = U
    }

    fun uncoverLR() {
        L.R = this
        R.L = this
    }

    fun uncoverUD() {
        U.D = this
        D.U = this
    }

    override fun toString(): String {
        return "(" + rowIndex + ", " + column!!.colIndex + ")"
    }

    init {
        R = this
        L = R
        D = L
        U = D
        this.rowIndex = rowIndex
    }
}