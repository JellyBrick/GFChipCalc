package main.ui.renderer

import main.App
import main.ui.help.HelpChipDialog
import main.util.Fn
import main.util.Ref
import java.awt.Color
import java.awt.Component
import java.awt.SystemColor
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 *
 * @author Bunnyspa
 */
class HelpLevelStatTableCellRenderer(
    private val app: App,
    private val rowHeaderNum: Int,
    private val colHeaderNum: Int,
    private val toggleType: Ref<Boolean>
) : DefaultTableCellRenderer() {
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
        } else if (toggleType.v == HelpChipDialog.STAT) {
            cr.background = Color.WHITE
        } else if ((value as String).toInt() == 0) {
            cr.background = app.green()
        } else {
            cr.background =
                Fn.percColor(app.orange()!!, Color.WHITE, value.toDouble(), -10.0, 0.0)
        }
        return cr
    }
}