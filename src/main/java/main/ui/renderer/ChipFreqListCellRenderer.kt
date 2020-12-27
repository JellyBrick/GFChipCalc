package main.ui.renderer

import main.App
import main.puzzle.assembly.ChipFreq
import main.ui.resource.AppImage
import main.util.Fn
import main.util.Ref
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 *
 * @author Bunnyspa
 */
class ChipFreqListCellRenderer(private val app: App, private val blink: Ref<Boolean>) : DefaultListCellRenderer() {
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
        val cf = value as ChipFreq?
        if (value != null) {
            cr.icon = AppImage.Chip[app, cf!!.chip!!]
            cr.background = if (isSelected && blink.v) Color.LIGHT_GRAY else Fn.percColor(
                app.orange()!!,
                app.green()!!,
                app.blue()!!,
                cf.freq,
                0.0,
                1.0
            )
        } else {
            cr.icon = null
            cr.background = Color.WHITE
        }
        return cr
    }
}