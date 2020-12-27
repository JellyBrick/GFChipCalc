/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.puzzle.assembly

import main.puzzle.Stat

/**
 *
 * @author Bunnyspa
 */
data class CalcSetting(
    val boardName: String,
    val boardStar: Int,
    val maxLevel: Boolean,
    val rotation: Boolean,
    val symmetry: Boolean,
    val stat: Stat?,
    val pt: Stat?
)