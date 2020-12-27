package main.ui.renderer

import main.App
import main.puzzle.Chip
import main.puzzle.Tag
import java.awt.Color
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 *
 * @author Bunnyspa
 */
class TagTableCellRenderer(private val app: App, private val checkBox: Boolean) : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val cr = super.getTableCellRendererComponent(
            table,
            value,
            isSelected,
            hasFocus,
            row,
            column
        ) as DefaultTableCellRenderer
        val tag = value as Tag
        cr.text = tag.name
        cr.foreground = tag.color
        if (!checkBox) {
            val chips = app.mf.inv_getFilteredChips()
            if (chips.stream().allMatch { t: Chip? -> t!!.containsTag(tag) }) {
                cr.background = app.green()
            } else {
                cr.background = Color.WHITE
            }
        }
        return cr
    }
}