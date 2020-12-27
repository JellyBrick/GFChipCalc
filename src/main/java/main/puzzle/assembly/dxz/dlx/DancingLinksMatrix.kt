/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle.assembly.dxz.dlx

import java.util.*

/**
 *
 * @author Bunnyspa
 */
class DancingLinksMatrix(matrix: List<BooleanArray>) {
    private val header: ColumnNode
    private fun create(matrix: List<BooleanArray>): ColumnNode {
        val nCol: Int = matrix[0].size
        var headerNode: ColumnNode? = ColumnNode(-1)
        val columnNodes: MutableList<ColumnNode> = mutableListOf()
        for (i in 0 until nCol) {
            val n = ColumnNode(i)
            columnNodes.add(n)
            headerNode = headerNode!!.linkRight(n) as ColumnNode
        }
        headerNode = headerNode!!.R.column
        for (r in matrix.indices) {
            val row = matrix[r]
            var prev: DLXNode? = null
            for (c in 0 until nCol) {
                if (row[c]) {
                    val colNode = columnNodes[c]
                    val newNode = DLXNode(r, colNode)
                    if (prev == null) {
                        prev = newNode
                    }
                    colNode.U.linkDown(newNode)
                    prev = prev.linkRight(newNode)
                    colNode.size++
                }
            }
        }
        headerNode!!.size = nCol
        return headerNode
    }

    fun selectColumn(): ColumnNode? {
        var out = header.R.column
        var minSize = out!!.size
        var c = out.R.column
        while (c !== header) {
            if (c!!.size < minSize) {
                out = c
                minSize = c.size
            }
            c = c.R.column
        }
        return out
    }

    fun getColumns(): Set<Int> {
        val out: MutableSet<Int> = HashSet()
        var c = header.R.column
        while (c !== header) {
            out.add(c!!.colIndex)
            c = c.R.column
        }
        return out
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var c = header.R.column
        while (c !== header) {
            sb.append(c!!.colIndex)
            sb.append(" ")
            c = c.R.column
        }
        return sb.toString()
    }

    init {
        header = create(matrix)
    }
}