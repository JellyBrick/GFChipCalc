/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.ui.resource

import main.App
import java.awt.Font
import java.awt.FontFormatException
import java.io.IOException
import java.io.InputStream

/**
 *
 * @author Bunnyspa
 */
object AppFont {
    val FONT_DIGIT = AppFont["mohave/Mohave-Light.otf"]
    private operator fun get(s: String): Font? {
        try {
            val `is`: InputStream = App.getResourceAsStream("font/$s")
            return Font.createFont(Font.TRUETYPE_FONT, `is`).deriveFont(14.0f)
        } catch (ignored: FontFormatException) {
        } catch (ignored: IOException) {
        }
        return null
    }

    val default: Font
        get() = Font(Font.SANS_SERIF, Font.PLAIN, 12)
}