package main.puzzle.assembly.dxz.zdd;

import main.util.ThreadPoolManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Bunnyspa
 */
public class ZDDNodeTable {

    private final Map<Integer, Set<ZDDNode>> map = new HashMap<>();

    @Nullable
    ZDDNode get(int i, ZDDNode l, ZDDNode h) {
        if (!map.containsKey(i)) {
            return null;
        }
        for (ZDDNode node : map.get(i)) {
            if (node.equals(i, l, h)) {
                return node;
            }
        }
        return null;
    }

    void add(@NotNull ZDDNode node) {
        ThreadPoolManager.getThreadPool().execute(() -> {
            int key = node.label;
            if (!map.containsKey(key)) {
                map.put(key, new HashSet<>());
            }
            map.get(key).add(node);
        });
    }

}
