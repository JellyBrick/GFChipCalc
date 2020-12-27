/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle.assembly.dxz

import main.puzzle.assembly.dxz.dlx.DancingLinksMatrix
import main.puzzle.assembly.dxz.zdd.ZDD
import main.puzzle.assembly.dxz.zdd.ZDDMemoCache
import main.puzzle.assembly.dxz.zdd.ZDDNode
import main.puzzle.assembly.dxz.zdd.ZDDNodeTable
import main.util.Pair
import java.util.*
import java.util.function.BooleanSupplier

/**
 *
 * @author Bunnyspa
 */
object DXZ {
    fun solve(rows: List<BooleanArray>, checkPause: BooleanSupplier): Set<Int>? {
        return dxz(DancingLinksMatrix(rows), checkPause)
    }

    private fun axz_search(
        A: ExactCoverMatrix,
        C: ZDDMemoCache,
        Z: ZDDNodeTable,
        checkPause: BooleanSupplier
    ): ZDDNode? {
        if (A.isEmpty()) {
            return ZDD.TRUE_TERMINAL
        }
        val colA = A.getCols()
        if (C.containsKey(colA)) {
            return C[colA]
        }
        val rowA = A.getRows()
        val c = A.getCol()
        var x: ZDDNode? = null
        for (r in rowA) {
            if (checkPause.asBoolean && A[r, c]) {
                A.cover(r)
                val y = axz_search(A, C, Z, checkPause)
                if (y != null) {
                    x = ZDD.unique(r, x, y, Z)
                }
                A.uncover()
            }
        }
        C[colA] = x
        return x
    }

    private fun dlx_search(
        A: DancingLinksMatrix,
        R: Stack<Int>,
        ans: MutableSet<Int>,
        checkPause: BooleanSupplier
    ): Boolean {
        val colA = A.getColumns()
        if (colA.isEmpty()) {
            ans.addAll(R)
            return true
        }
        val c = A.selectColumn()
        c!!.cover()
        var r = c.D
        while (r !== c) {
            R.push(r.rowIndex)
            run {
                var j = r.R
                while (j !== r) {
                    j.column!!.cover()
                    j = j.R
                }
            }
            if (dlx_search(A, R, ans, checkPause)) {
                return true
            }
            R.pop()
            var j = r.L
            while (j !== r) {
                j.column!!.uncover()
                j = j.L
            }
            r = r.D
        }
        c.uncover()
        return false
    }

    private fun dxz(X: DancingLinksMatrix, checkPause: BooleanSupplier): Set<Int>? {
        val node = dxz_search(X, ZDDMemoCache(), ZDDNodeTable(), checkPause).first ?: return null
        val sets = node.get()
        return if (sets.isEmpty()) {
            null
        } else sets.iterator().next()
    }

    private fun dxz_search(
        A: DancingLinksMatrix,
        C: ZDDMemoCache,
        Z: ZDDNodeTable,
        checkPause: BooleanSupplier
    ): Pair<ZDDNode?, Boolean> {
        // System.out.println(depth + ": " + A);
        val colA = A.getColumns()
        if (colA.isEmpty()) {
            return Pair(ZDD.TRUE_TERMINAL, true)
        }
        if (C.containsKey(colA)) {
            return Pair(C[colA], false)
        }
        val c = A.selectColumn()
        var x: ZDDNode? = null
        c!!.cover()
        var r = c.D
        while (r !== c) {
            run {
                var j = r.R
                while (j !== r) {
                    j.column!!.cover()
                    j = j.R
                }
            }
            val p = dxz_search(A, C, Z, checkPause)
            val y = p.first
            if (y != null) {
                x = ZDD.unique(r.rowIndex, x, y, Z)
            }
            if (p.second) {
                return Pair(x, true)
            }
            var j = r.L
            while (j !== r) {
                j.column!!.uncover()
                j = j.L
            }
            r = r.D
        }
        C[colA] = x
        c.uncover()
        return Pair(x, false)
    }
}