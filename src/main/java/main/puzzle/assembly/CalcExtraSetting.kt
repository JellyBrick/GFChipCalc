/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle.assembly

import main.puzzle.Chip

/**
 *
 * @author Bunnyspa
 */
class CalcExtraSetting(
    var calcMode: Int,
    val calcModeTag: Int,
    val matchColor: Boolean,
    val markMin: Int,
    val markMax: Int,
    val markType: Int,
    val sortType: Int,
    val chips: List<Chip>
) {
    companion object {
        const val CALCMODE_FINISHED = 0
        const val CALCMODE_DICTIONARY = 1
        const val CALCMODE_ALGX = 2
    }
}