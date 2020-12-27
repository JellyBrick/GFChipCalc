package main.ui.tip

import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.swing.JLabel

/**
 *
 * @author Bunnyspa
 */
class TipMouseListener(label: JLabel?) : MouseListener {
    private val label: JLabel?
    private val map: MutableMap<Component, String?>
    fun clearTips() {
        map.clear()
    }

    fun setTip(c: Component, s: String?) {
        map[c] = s
    }

    override fun mouseClicked(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {
        val c = e.component
        if (c != null && map.containsKey(c)) {
            label!!.text = map[c]
        } else {
            label!!.text = " "
        }
    }

    override fun mouseExited(e: MouseEvent) {
        label!!.text = " "
    }

    init {
        map = HashMap()
        this.label = label
    }
}