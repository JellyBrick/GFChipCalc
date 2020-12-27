package main.ui.renderer

import java.awt.Color
import java.awt.Component
import java.awt.SystemColor
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 *
 * @author Bunnyspa
 */
class HelpTableCellRenderer(private val rowHeaderNum: Int, private val colHeaderNum: Int) : DefaultTableCellRenderer() {
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
        cr.horizontalAlignment = CENTER
        if (row < colHeaderNum || column < rowHeaderNum) {
            cr.background = SystemColor.control
        } else {
            cr.background = Color.WHITE
        }
        return cr
    }
}