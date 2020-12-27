package main.puzzle.assembly.dxz.zdd

/**
 *
 * @author Bunnyspa
 */
object ZDD {
    val TRUE_TERMINAL = ZDDNode()
    fun unique(i: Int, l: ZDDNode?, h: ZDDNode, Z: ZDDNodeTable): ZDDNode {
        val element = Z[i, l, h]
        if (element != null) {
            return element
        }
        val node = ZDDNode(i, l, h)
        Z.add(node)
        return node
    }
}