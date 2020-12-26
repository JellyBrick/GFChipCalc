package main.iterator;

import main.puzzle.Board;
import main.puzzle.Chip;
import main.puzzle.Shape;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 *
 * @author Bunnyspa
 */
class PerTypeShapeCiterator implements Iterator<List<Shape>> {

    private List<List<Shape>> next;
    private final int total;

    public PerTypeShapeCiterator(@NotNull Map<Shape.Type, Integer> typeCountMap) {
        next = initComb(typeCountMap);
        total = total(typeCountMap);
    }

    public PerTypeShapeCiterator(@NotNull List<Shape> progress) {
        next = toComb(progress);
        total = total(getTypeCount(progress));
    }

    @Override
    public boolean hasNext() {
        return !next.isEmpty();
    }

    @NotNull
    @Override
    public List<Shape> next() {
        List<Shape> out = peek();
        next = nextComb(next);
        return out;
    }

    @NotNull
    public List<Shape> peek() {
        List<Shape> out = new ArrayList<>();
        for (List<Shape> nextType : next) {
            out.addAll(nextType);
        }
        return out;
    }

    public int total() {
        return total;
    }

    @NotNull
    private static List<List<Shape>> nextComb(@NotNull List<List<Shape>> comb) {
        int n = comb.size() - 1;
        while (0 <= n) {
            List<Shape> chips = nextComb_type(comb.get(n));
            if (chips.isEmpty()) {
                n--;
            } else {
                List<List<Shape>> out = new ArrayList<>(comb.size());
                for (int i = 0; i < n; i++) {
                    out.add(comb.get(i));
                }
                out.add(chips);
                for (int i = n + 1; i < comb.size(); i++) {
                    Shape chip = comb.get(i).get(0);
                    int length = comb.get(i).size();
                    out.add(initComb_type(chip.getType(), length));
                }
                return out;
            }
        }
        return new ArrayList<>();
    }

    @NotNull
    private static List<Shape> nextComb_type(@NotNull List<Shape> chips) {
        int n = chips.size() - 1;
        while (0 <= n) {
            Shape shape = nextShape(chips.get(n));
            if (shape == Shape.NONE) {
                n--;
            } else {
                List<Shape> out = new ArrayList<>(chips.size());
                for (int i = 0; i < n; i++) {
                    out.add(chips.get(i));
                }
                for (int i = n; i < chips.size(); i++) {
                    out.add(shape);
                }
                return out;
            }
        }
        return new ArrayList<>();
    }

    private static Shape nextShape(@NotNull Shape shape) {
        List<Shape> chips = Arrays.asList(Shape.getShapes(shape.getType()));
        int i = chips.indexOf(shape) + 1;
        if (chips.size() <= i) {
            return Shape.NONE;
        }
        return chips.get(i);
    }

    @NotNull
    private static List<List<Shape>> initComb(@NotNull Map<Shape.Type, Integer> combType) {
        List<List<Shape>> out = new ArrayList<>(combType.keySet().size());
        List<Shape.Type> types = new ArrayList<>(combType.keySet());

        types.sort(Shape.Type::compare);

        for (Shape.Type type : types) {
            out.add(initComb_type(type, combType.get(type)));
        }
        return out;
    }

    @NotNull
    private static List<Shape> initComb_type(@NotNull Shape.Type type, int length) {
        Shape shape = Shape.getShapes(type)[0];
        List<Shape> out = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            out.add(shape);
        }
        return out;
    }

    @NotNull
    public static Map<Shape.Type, Integer> getTypeCount(@NotNull List<Shape> comb) {
        Map<Shape.Type, Integer> combType = new HashMap<>();
        for (Shape shape : comb) {
            Shape.Type type = shape.getType();
            if (!combType.containsKey(type)) {
                combType.put(type, 0);
            }
            int count = combType.get(type);
            count++;
            combType.put(type, count);
        }
        return combType;
    }

    public static int total(@NotNull Map<Shape.Type, Integer> typeCountMap) {
        int total = 1;
        for (Shape.Type type : typeCountMap.keySet()) {
            int nHr = nHr(Shape.getShapes(type).length, typeCountMap.get(type));
            total *= nHr;
        }
        return total;
    }

    private static int nHr(int n, int r) {
        return nCr(n + r - 1, r);
    }

    private static int nCr(int n, int r) {
        r = Math.min(r, n - r);
        int num = 1;
        for (int i = n - r + 1; i <= n; i++) {
            num *= i;
        }
        for (int i = 1; i <= r; i++) {
            num /= i;
        }
        return num;
    }

    @NotNull
    public static List<Map<Shape.Type, Integer>> getTypeCountMaps(String name, int star, @NotNull Set<Shape.Type> types) {
        int nCell = Board.getCellCount(name, star);
        return getTypeCountMaps(nCell, types);
    }

    @NotNull
    private static List<Map<Shape.Type, Integer>> getTypeCountMaps(int nCell, @NotNull Set<Shape.Type> types) {
        List<Map<Shape.Type, Integer>> out = new ArrayList<>();
        List<Shape.Type> typesAvail = new ArrayList<>(types);
        typesAvail.sort(Shape.Type::compare);
        getTypeCountMaps_rec(out, typesAvail, nCell, new Stack<>());
        return out;
    }

    private static void getTypeCountMaps_rec(@NotNull List<Map<Shape.Type, Integer>> out, @NotNull List<Shape.Type> typesAvail, int nCell, @NotNull Stack<Shape.Type> typeBuffer) {
        if (nCell == 0) {
            Map<Shape.Type, Integer> e = new HashMap<>();
            for (Shape.Type type : typeBuffer) {
                if (!e.containsKey(type)) {
                    e.put(type, 0);
                }
                e.put(type, e.get(type) + 1);
            }
            out.add(e);
            return;
        }
        int range = Math.min(Chip.SIZE_MAX, nCell);
        for (Shape.Type t : typesAvail) {
            if (t.getSize() <= range) {
                typeBuffer.push(t);
                List<Shape.Type> typeAvail_new = new ArrayList<>();
                for (Shape.Type t2 : typesAvail) {
                    if (Shape.Type.compare(t, t2) <= 0) {
                        typeAvail_new.add(t2);
                    }
                }
                int nCell_new = nCell - t.getSize();
                getTypeCountMaps_rec(out, typeAvail_new, nCell_new, typeBuffer);
                typeBuffer.pop();
            }
        }
    }

    @NotNull
    private static List<List<Shape>> toComb(@NotNull List<Shape> shapes) {
        Map<Shape.Type, List<Shape>> map = new HashMap<>();
        for (Shape shape : shapes) {
            Shape.Type type = shape.getType();
            if (!map.containsKey(type)) {
                map.put(type, new ArrayList<>());
            }
            map.get(type).add(shape);
        }

        List<List<Shape>> out = new ArrayList<>();
        for (Shape.Type type : Shape.Type.values()) {
            if (map.containsKey(type)) {
                out.add(map.get(type));
            }
        }
        return out;
    }
}
