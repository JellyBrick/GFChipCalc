package main.puzzle.assembly.dxz.zdd

import main.util.Fn
import java.util.*
import java.util.function.Consumer

/**
 *
 * @author Bunnyspa
 */
class ZDDNode {
    private val uuid = UUID.randomUUID()
    val label: Int
    val loChild // false terminal
            : ZDDNode?
    val hiChild // true terminal
            : ZDDNode?

    internal constructor() {
        label = 0
        loChild = null
        hiChild = null
    }

    internal constructor(i: Int, l: ZDDNode?, h: ZDDNode?) {
        label = i
        loChild = l
        hiChild = h
    }

    fun isTerminal(): Boolean {
        return hiChild == null
    }

    fun equals(i: Int, l: ZDDNode?, h: ZDDNode): Boolean {
        return label == i && loChild === l && hiChild === h
    }

    fun get(): Set<Set<Int>> {
        val out: MutableSet<Set<Int>> = HashSet()
        if (isTerminal()) {
            out.add(HashSet())
            return out
        }
        hiChild!!.get().forEach(Consumer { set: Set<Int>? ->
            val e: MutableSet<Int> = HashSet(set)
            e.add(label)
            out.add(e)
        })
        loChild?.get()?.forEach(Consumer { set: Set<Int>? ->
            val e: Set<Int> = HashSet(set)
            out.add(e)
        })
        return out
    }

    override fun toString(): String {
        return toString_tree(0)
    }

    private fun toString_tree(depth: Int): String {
        val sb = StringBuilder()
        sb.append(label).append(" ").append(uuid.toString(), 0, 4)
        val pad = Fn.pad("  ", depth)
        if (hiChild != null && !hiChild.isTerminal()) {
            sb.append(System.lineSeparator()).append(pad).append("+").append(hiChild.toString_tree(depth + 1))
        }
        if (loChild != null) {
            sb.append(System.lineSeparator()).append(pad).append("-").append(loChild.toString_tree(depth + 1))
        }
        return sb.toString()
    }
}