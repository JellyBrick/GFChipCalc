package main.ui.shortcut

/**
 *
 * @author Bunnyspa
 */
class Shortcut {
    private val keyCode: Int
    private val ctrl: Boolean
    private val shift: Boolean

    constructor(keyCode: Int) {
        this.keyCode = keyCode
        ctrl = false
        shift = false
    }

    constructor(keyCode: Int, ctrl: Boolean) {
        this.keyCode = keyCode
        this.ctrl = ctrl
        shift = false
    }

    constructor(keyCode: Int, ctrl: Boolean, shift: Boolean) {
        this.keyCode = keyCode
        this.ctrl = ctrl
        this.shift = shift
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || this.javaClass != obj.javaClass) {
            return false
        }
        val shortcut = obj as Shortcut
        return keyCode == shortcut.keyCode && ctrl == shortcut.ctrl && shift == shortcut.shift
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 29 * hash + keyCode
        hash = 29 * hash + if (ctrl) 1 else 0
        hash = 29 * hash + if (shift) 1 else 0
        return hash
    }
}