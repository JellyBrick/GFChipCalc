package main.puzzle;

import main.util.IO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Bunnyspa
 */
public class BoardTemplate implements Comparable<BoardTemplate> {

    @Nullable
    private final List<Puzzle> puzzles;
    @Nullable
    private final Map<Shape, Integer> shapeCountMap;
    private final boolean symmetry;

    @Nullable
    private final PuzzleMatrix<Integer> placement;

    public static final int END = 0;
    public static final int EMPTY = 1;
    public static final int NORMAL = 2;

    private final int state;

    private static final BoardTemplate BOARD_EMPTY = new BoardTemplate(false);
    private static final BoardTemplate BOARD_END = new BoardTemplate(true);

    private BoardTemplate(boolean isEnd) {
        puzzles = null;
        shapeCountMap = null;
        placement = null;
        symmetry = false;
        state = isEnd ? END : EMPTY;
    }

    @NotNull
    public static BoardTemplate empty() {
        return BOARD_EMPTY;
    }

    @NotNull
    public static BoardTemplate end() {
        return BOARD_END;
    }

    public BoardTemplate(String name, int star, @NotNull List<Puzzle> puzzles) {
        this.puzzles = puzzles;
        shapeCountMap = new HashMap<>();

        // Placement
        placement = Board.toPlacement(name, star, puzzles);

        // Symmetry
        this.symmetry = placement.isSymmetric(Board.UNUSED);

        init();
        state = NORMAL;
    }

    public BoardTemplate(String name, int star, @NotNull List<Puzzle> puzzles, boolean symmetry) {
        this.puzzles = puzzles;
        shapeCountMap = new HashMap<>();

        // Placement
        placement = Board.toPlacement(name, star, puzzles);

        // Symmetry
        this.symmetry = symmetry;

        init();
        state = NORMAL;
    }

    private void init() {
        for (Puzzle p : puzzles) {
            Shape shape = p.shape;
            if (!shapeCountMap.containsKey(shape)) {
                shapeCountMap.put(shape, 0);
            }
            shapeCountMap.put(shape, shapeCountMap.get(shape) + 1);
        }
    }

    @Nullable
    public PuzzleMatrix<Integer> getMatrix() {
        return new PuzzleMatrix<>(placement);
    }

    public boolean isEnd() {
        return state == END;
    }

    public boolean isEmpty() {
        return state == EMPTY;
    }

    public boolean isSymmetric() {
        return symmetry;
    }

    public boolean calcSymmetry() {
        return placement.isSymmetric(Board.UNUSED);
    }

    @Nullable
    public Map<Shape, Integer> getShapeCountMap() {
        return shapeCountMap;
    }

    @NotNull
    public List<Integer> getChipRotations() {
        List<Integer> list = new ArrayList<>();
        for (Puzzle p : puzzles) {
            Integer rotation = p.rotation;
            list.add(rotation);
        }
        return list;
    }

    @NotNull
    public List<Point> getChipLocations() {
        List<Point> list = new ArrayList<>();
        for (Puzzle p : puzzles) {
            Point location = p.location;
            list.add(location);
        }
        return list;
    }

    @NotNull
    public String toData() {
        // Names

        String sb = puzzles.parallelStream().map(p -> String.valueOf(p.shape.id)).collect(Collectors.joining(",")) +
                ";" +

                // Rotations
                puzzles.parallelStream().map(p -> String.valueOf(p.rotation)).collect(Collectors.joining(",")) +
                ";" +

                // Locations
                puzzles.parallelStream().map(p -> IO.data(p.location)).collect(Collectors.joining(",")) +
                ";" +

                // Symmetry
                IO.data(calcSymmetry());
        return sb;
    }

    public void sortPuzzle() {
        Collections.sort(puzzles);
    }

    @Override
    public int compareTo(@NotNull BoardTemplate o) {
        for (int i = 0; i < Math.min(puzzles.size(), o.puzzles.size()); i++) {
            int nameC = Shape.compare(puzzles.get(i).shape, o.puzzles.get(i).shape);
            if (nameC != 0) {
                return nameC;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        if (state == EMPTY) {
            return "EMPTY";
        }
        if (state == END) {
            return "end";
        }
        return puzzles.toString();
    }
}
