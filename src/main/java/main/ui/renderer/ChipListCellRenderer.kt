package main.ui.renderer

import main.App
import main.puzzle.Chip
import main.ui.resource.AppImage
import java.awt.Color
import java.awt.Component
import java.awt.Image
import javax.swing.DefaultListCellRenderer
import javax.swing.ImageIcon
import javax.swing.JList

/**
 *
 * @author Bunnyspa
 */
class ChipListCellRenderer : DefaultListCellRenderer {
    private val app: App
    private val factored: Boolean
    private val factor: Double

    constructor(app: App) : super() {
        this.app = app
        factored = false
        factor = 1.0
    }

    constructor(app: App, factor: Double) : super() {
        this.app = app
        factored = true
        this.factor = factor
    }

    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val cr =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as DefaultListCellRenderer
        cr.horizontalAlignment = CENTER
        cr.text = ""
        val c = value as Chip?
        if (value != null) {
            val icon = AppImage.Chip[app, c!!]
            if (!factored) {
                cr.icon = icon
            } else {
                val image = icon.image.getScaledInstance(
                    (icon.iconWidth * factor).toInt(),
                    (icon.iconHeight * factor).toInt(),
                    Image.SCALE_SMOOTH
                )
                cr.icon = ImageIcon(image)
            }
            cr.background = if (isSelected) Color.LIGHT_GRAY else Color.WHITE
        } else {
            cr.icon = null
            cr.background = Color.WHITE
        }
        return cr
    }
}