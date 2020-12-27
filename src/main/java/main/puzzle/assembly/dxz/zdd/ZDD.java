package main.puzzle.assembly.dxz.zdd;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Bunnyspa
 */
public class ZDD {

    public static final ZDDNode TRUE_TERMINAL = new ZDDNode();

    @NotNull
    public static ZDDNode unique(int i, ZDDNode l, ZDDNode h, @NotNull ZDDNodeTable Z) {
        ZDDNode element = Z.get(i, l, h);
        if (element != null) {
            return element;
        }

        ZDDNode node = new ZDDNode(i, l, h);
        Z.add(node);
        return node;
    }
}
