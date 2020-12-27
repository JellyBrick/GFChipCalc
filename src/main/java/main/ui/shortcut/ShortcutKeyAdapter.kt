package main.ui.shortcut

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*

/**
 *
 * @author Bunnyspa
 */
class ShortcutKeyAdapter : KeyAdapter() {
    val map: MutableMap<Shortcut, Runnable> = HashMap()
    fun addShortcut(keyCode: Int, r: Runnable) {
        map[Shortcut(keyCode)] = r
    }

    fun addShortcut_c(keyCode: Int, r: Runnable) {
        map[Shortcut(keyCode, true)] = r
    }

    fun addShortcut_cs(keyCode: Int, r: Runnable) {
        map[Shortcut(keyCode, true, true)] = r
    }

    override fun keyPressed(evt: KeyEvent) {
        val key = evt.keyCode
        val ctrl = evt.isControlDown
        val shift = evt.isShiftDown
        var sc: Shortcut
        if (map.containsKey(Shortcut(key, ctrl, shift).also { sc = it })) {
            map[sc]!!.run()
        } else if (map.containsKey(Shortcut(key, ctrl).also { sc = it })) {
            map[sc]!!.run()
        } else if (map.containsKey(Shortcut(key).also { sc = it })) {
            map[sc]!!.run()
        }
    }
}