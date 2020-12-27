package main.puzzle.assembly.dxz.zdd

import main.util.ThreadPoolManager
import java.util.*

/**
 *
 * @author Bunnyspa
 */
class ZDDNodeTable {
    private val map: MutableMap<Int, MutableSet<ZDDNode>> = HashMap()
    operator fun get(i: Int, l: ZDDNode?, h: ZDDNode): ZDDNode? {
        if (!map.containsKey(i)) {
            return null
        }
        for (node in map[i]!!) {
            if (node.equals(i, l, h)) {
                return node
            }
        }
        return null
    }

    fun add(node: ZDDNode) {
        ThreadPoolManager.threadPool.execute {
            val key = node.label
            if (!map.containsKey(key)) {
                map[key] = HashSet()
            }
            map[key]!!.add(node)
        }
    }
}