package main.puzzle.assembly.dxz.zdd;

import main.util.Fn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Bunnyspa
 */
public class ZDDNode {

    private final UUID uuid = UUID.randomUUID();

    final int label;
    @Nullable
    final ZDDNode loChild; // false terminal
    @Nullable
    final ZDDNode hiChild; // true terminal

    ZDDNode() {
        this.label = 0;
        this.loChild = null;
        this.hiChild = null;
    }

    ZDDNode(int i, @Nullable ZDDNode l, @Nullable ZDDNode h) {
        this.label = i;
        this.loChild = l;
        this.hiChild = h;
    }

    boolean isTerminal() {
        return hiChild == null;
    }

    boolean equals(int i, ZDDNode l, ZDDNode h) {
        return label == i && loChild == l && hiChild == h;
    }

    @NotNull
    public Set<Set<Integer>> get() {
        Set<Set<Integer>> out = new HashSet<>();
        if (isTerminal()) {
            out.add(new HashSet<>());
            return out;
        }
        hiChild.get().forEach((set) -> {
            Set<Integer> e = new HashSet<>(set);
            e.add(label);
            out.add(e);
        });
        if (loChild != null) {
            loChild.get().forEach((set) -> {
                Set<Integer> e = new HashSet<>(set);
                out.add(e);
            });
        }
        return out;
    }

    @NotNull
    @Override
    public String toString() {
        return toString_tree(0);
    }

    @NotNull
    private String toString_tree(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" ").append(uuid.toString(), 0, 4);
        String pad = Fn.pad("  ", depth);
        if (hiChild != null && !hiChild.isTerminal()) {
            sb.append(System.lineSeparator()).append(pad).append("+").append(hiChild.toString_tree(depth + 1));
        }
        if (loChild != null) {
            sb.append(System.lineSeparator()).append(pad).append("-").append(loChild.toString_tree(depth + 1));
        }
        return sb.toString();
    }

}
