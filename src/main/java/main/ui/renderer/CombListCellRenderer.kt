package main.ui.renderer

import main.App
import main.puzzle.Board
import main.puzzle.assembly.ChipFreq
import main.util.Fn
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JList

/**
 *
 * @author Bunnyspa
 */
class CombListCellRenderer(private val app: App, private val combChipFreqList: JList<*>?) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val cr =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as DefaultListCellRenderer
        try {
            cr.horizontalAlignment = CENTER
            val b = value as Board
            cr.text = Fn.fPercStr(b.getStatPerc())
            val freqChipSelected = !combChipFreqList!!.isSelectionEmpty
            var freqChipIncluded = false
            if (!combChipFreqList.isSelectionEmpty) {
                val freqID = (combChipFreqList.selectedValue as ChipFreq).chip!!.id
                freqChipIncluded = b.getChipIDs().stream().anyMatch { id: String? -> id == freqID }
            }
            val combList = list.model as DefaultListModel<*>
            val bMax = combList.firstElement() as Board
            val bMin = combList.lastElement() as Board
            cr.foreground =
                if (freqChipSelected) if (isSelected) Color.GRAY else Color.BLACK else if (isSelected) Color.WHITE else Color.BLACK
            cr.background =
                if (freqChipIncluded) Color.LIGHT_GRAY else if (freqChipSelected) Color.WHITE else Fn.percColor(
                    app.orange()!!, app.green()!!, app.blue()!!, b.getStatPerc(), bMin.getStatPerc(), bMax.getStatPerc()
                )
        } catch (ignored: Exception) {
        }
        return cr
    }
}