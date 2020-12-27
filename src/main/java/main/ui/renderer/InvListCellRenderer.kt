package main.ui.renderer

import main.App
import main.puzzle.Board
import main.puzzle.Chip
import main.puzzle.assembly.ChipFreq
import main.ui.resource.AppImage
import main.util.Ref
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JTabbedPane

/**
 *
 * @author Bunnyspa
 */
class InvListCellRenderer(
    private val app: App,
    invList: JList<*>?,
    private val combList: JList<*>?,
    private val combChipListTabbedPane: JTabbedPane?,
    private val combChipList: JList<*>?,
    private val combChipFreqList: JList<*>?,
    private val blink: Ref<Boolean>
) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val cr =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as DefaultListCellRenderer
        cr.horizontalAlignment = CENTER
        cr.text = ""
        val c = value as Chip
        var combSelected = false
        if (!combList!!.isSelectionEmpty) {
            for (id in (combList.selectedValue as Board).getChipIDs()) {
                if (c.id == id) {
                    combSelected = true
                    break
                }
            }
        }
        var resultSelected = false
        if (combChipListTabbedPane!!.selectedIndex == 0) {
            if (!combChipList!!.isSelectionEmpty) {
                val chip = combChipList.selectedValue as Chip
                if (c == chip) {
                    resultSelected = true
                }
            }
        } else {
            if (!combChipFreqList!!.isSelectionEmpty) {
                val cf = combChipFreqList.selectedValue as ChipFreq
                if (c == cf.chip) {
                    resultSelected = true
                }
            }
        }
        cr.icon = AppImage.Chip[app, c]
        val selBlink = isSelected && blink.v
        val selColor = app.orange()
        if (resultSelected) {
            cr.background = if (selBlink) selColor else Color.LIGHT_GRAY
        } else if (combSelected) {
            cr.background = if (selBlink) selColor else app.blue()
        } else if (!c.isPtValid()) {
            cr.background = if (selBlink) selColor else Color.PINK
        } else {
            cr.background = if (isSelected) selColor else Color.WHITE
        }
        cr.toolTipText = c.id //DEBUG
        return cr
    }
}