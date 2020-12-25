package main.puzzle.assembly;

import main.iterator.ChipCiterator;
import main.iterator.ShapeCiterator;
import main.puzzle.Shape;
import main.puzzle.*;
import main.puzzle.assembly.dxz.DXZ;
import main.setting.Setting;
import main.util.IO;
import main.util.ThreadPoolManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 *
 * @author Bunnyspa
 */
public class Assembler {

    public interface Intermediate {

        void stop();

        void update(int nDone);

        void set(int nDone, int nTotal);

        void show(BoardTemplate template);
    }

    public enum Status {
        STOPPED, RUNNING, PAUSED
    }

    private static final int RESULT_LIMIT = 100;

    private static final BoardTemplateMap fullBTM, partialBTM;

    static {
        // Full BoardTemplate
        fullBTM = new BoardTemplateMap();
        for (String name : Board.NAMES) {
            for (int star = 1; star <= 5; star++) {
                List<BoardTemplate> templates = IO.loadBoardTemplates(name, star, false);
                Shape.Type minType = Shape.Type._5A;
                if (Board.NAME_M2.equals(name)
                        || (Board.NAME_MK153.equals(name) && star <= 2)) {
                    minType = Shape.Type._4;
                } else if (Board.NAME_MK153.equals(name) && star == 5) {
                    minType = Shape.Type._5B;
                }
                fullBTM.put(name, star, templates, minType);
            }
        }
        // Partial BoardTemplate
        partialBTM = new BoardTemplateMap();
        List<BoardTemplate> templates = IO.loadBoardTemplates(Board.NAME_M2, 5, true);
        partialBTM.put(Board.NAME_M2, 5, templates, Shape.Type._5B);
    }

    private final Intermediate intermediate;

    private CalcSetting cs;
    private CalcExtraSetting ces;
    private Progress progress;
    private boolean boardsChanged;

    private volatile Status status = Status.STOPPED;
    private final BooleanSupplier checkPause = this::checkPause;

    public Assembler(Intermediate i) {
        this.intermediate = i;
    }

    public boolean btExists(String name, int star, boolean alt) {
        return getBT(name, star, alt) != null;
    }

    private List<BoardTemplate> getBT(String name, int star, boolean alt) {
        return alt ? partialBTM.get(name, star) : fullBTM.get(name, star);
    }

    public Shape.Type getMinType(String name, int star, boolean alt) {
        return alt ? partialBTM.getMinType(name, star) : fullBTM.getMinType(name, star);
    }

    public boolean hasPartial(String name, int star) {
        return partialBTM.containsKey(name, star);
    }

    public void set(CalcSetting cs, CalcExtraSetting ces, Progress p) {
        this.cs = cs;
        this.ces = ces;
        this.progress = p;
        ThreadPoolManager.getThreadPool().execute(() -> {
            if (status == Status.STOPPED) {
                status = Status.PAUSED;
            }
            if (ces.calcMode != CalcExtraSetting.CALCMODE_FINISHED) {
                combine();
            } else {
                setProgBar();
            }
            status = Status.STOPPED;
            intermediate.stop();
        });
    }

    private void prog_inc() {
        progress.nDone++;
        intermediate.update(progress.nDone);
    }

    public void pause() {
        status = Status.PAUSED;
    }

    public void resume() {
        status = Status.RUNNING;
    }

    public void stop() {
        status = Status.STOPPED;
    }

    public Status getStatus() {
        return status;
    }

    public boolean boardsUpdated() {
        return boardsChanged;
    }

    public boolean resultEmpty() {
        return progress.getBoardSize() == 0;
    }

    public AssemblyResult getResult() {
        boardsChanged = false;
        return new AssemblyResult(progress.getBoards(), progress.getChipFreqs());
    }

    private void setProgBar() {
        intermediate.set(progress.nDone, progress.nTotal);
    }

    private void combine() {
        BlockingQueue<BoardTemplate> q = new ArrayBlockingQueue<>(5);
        ChipCiterator cIt = new ChipCiterator(ces.chips);

        List<Callable<Object>> calls = new ArrayList<>();
        calls.add(Executors.callable(() -> combine_template(q, cIt)));
        calls.add(Executors.callable(() -> combine_assemble(q, cIt)));

        try {
            ThreadPoolManager.getThreadPool().invokeAll(calls);
        } catch (InterruptedException ignored) {
        }
    }

    private void combine_template(BlockingQueue<BoardTemplate> q, ChipCiterator cIt) {
        // Dictionary
        if (ces.calcMode == CalcExtraSetting.CALCMODE_DICTIONARY) {
            List<BoardTemplate> templates = getBT(cs.boardName, cs.boardStar, ces.calcModeTag == 1);

            int count = 0;
            for (BoardTemplate boardTemplate : templates) {
                if (cIt.hasEnoughChips(boardTemplate)) {
                    if (!cs.symmetry || boardTemplate.isSymmetric()) {
                        count++;
                    }
                }
            }
            progress.nTotal = count;

            setProgBar();

            Iterator<BoardTemplate> pIt = templates.subList(progress.nDone, templates.size()).iterator();
            while (checkPause() && pIt.hasNext()) {
                BoardTemplate template = pIt.next();
                if ((!cs.symmetry || template.isSymmetric())
                        && cIt.hasEnoughChips(template)) {
                    offer(q, template);
                }
            }
        } //
        // Algorithm X
        else {
            ShapeCiterator it = new ShapeCiterator(cs.boardName, cs.boardStar, ces.chips);
            progress.nTotal = it.total();
            setProgBar();

            it.skip(progress.nDone);
            while (checkPause() && it.hasNext()) {
                BoardTemplate template = combine_template_algX(it);
                offer(q, template);
            }
        }
        offer(q, BoardTemplate.end());
    }

    private BoardTemplate combine_template_algX(ShapeCiterator iterator) {
        if (!iterator.isNextValid()) {
            iterator.skip();
            return BoardTemplate.empty();
        }
        List<Shape> shapes = iterator.next();
        return generateTemplate(cs.boardName, cs.boardStar, shapes, checkPause);
    }

    public static BoardTemplate generateTemplate(String boardName, int boardStar, List<Shape> shapes, BooleanSupplier checkPause) {
        return generateTemplate_DXZ(boardName, boardStar, shapes, checkPause);
    }

    private static BoardTemplate generateTemplate_DXZ(String boardName, int boardStar, List<Shape> shapes, BooleanSupplier checkPause) {
        PuzzleMatrix<Integer> puzzle = Board.initMatrix(boardName, boardStar);

        Set<Point> emptyCoords = puzzle.getPoints(Board.EMPTY);

        int nCol_name = shapes.size();
        int nCol_cell = puzzle.getNumContaining(Board.EMPTY);
        int nCol = nCol_name + nCol_cell;

        List<Point> cols_pt = new ArrayList<>(emptyCoords);
        List<boolean[]> rows = new ArrayList<>();
        List<Puzzle> rows_puzzle = new ArrayList<>();

        for (int i = 0; i < nCol_name; i++) {
            Shape shape = shapes.get(i);
            for (int rot = 0; rot < shape.getMaxRotation(); rot++) {
                for (Point bp : emptyCoords) {
                    Set<Point> tps = translate(shape, rot, bp);
                    if (Board.isChipPlaceable(puzzle, tps)) {
                        boolean[] row = new boolean[nCol];
                        row[i] = true;
                        tps.forEach((p) -> row[nCol_name + cols_pt.indexOf(p)] = true);
                        rows.add(row);
                        rows_puzzle.add(new Puzzle(shape, rot, bp));
                    }
                }
            }
        }

        if (rows.isEmpty()) {
            return BoardTemplate.empty();
        }

        Set<Integer> resultRows = DXZ.solve(rows, checkPause);
        if (resultRows == null) {
            return BoardTemplate.empty();
        }

        List<Puzzle> puzzles = new ArrayList<>();

        List<Integer> sortedRows = new ArrayList<>(resultRows);
        sortedRows.sort((o1, o2) -> Shape.compare(rows_puzzle.get(o1).shape, rows_puzzle.get(o2).shape));
        sortedRows.forEach((r) -> puzzles.add(rows_puzzle.get(r)));
        // System.out.println(puzzles);
        BoardTemplate bt = new BoardTemplate(boardName, boardStar, puzzles);
        return bt;
    }

    private static Set<Point> translate(Shape shape, int rotation, Point bp) {
        Point cfp = shape.getPivot(rotation);
        Set<Point> cps = shape.getPoints(rotation);
        for (Point cp : cps) {
            cp.translate(bp.x - cfp.x, bp.y - cfp.y);
        }
        return cps;
    }

    private void combine_assemble(BlockingQueue<BoardTemplate> q, ChipCiterator cIt) {
        while (checkPause()) {
            BoardTemplate template = poll(q);
            if (template == null || template.isEnd()) {
                return;
            }
            if (!template.isEmpty()) {
                // Show progress
                intermediate.show(template);
                cIt.init(template);
                // For all combinations
                while (checkPause() && cIt.hasNext()) {
                    List<Chip> candidates = cIt.next();
                    // Check PT
                    boolean addable = true;
                    int[] pt = cs.pt.toArray();
                    for (Chip c : candidates) {
                        int[] cpt = c.getPt().toArray();
                        for (int i = 0; i < 4; i++) {
                            pt[i] -= cpt[i];
                            if (pt[i] < 0) {
                                addable = false;
                                break;
                            }
                        }
                    }

                    // add
                    if (addable) {
                        List<Chip> chips = new ArrayList<>();
                        for (int i = 0; i < candidates.size(); i++) {
                            Chip c = new Chip(candidates.get(i));
                            c.setRotation(template.getChipRotations().get(i));
                            chips.add(c);
                        }
                        publishBoard(new Board(cs.boardName, cs.boardStar, cs.stat, chips, template.getChipLocations()));
                    }
                }
            }
            prog_inc();
        }
    }

    public synchronized void publishBoard(Board board) {
        switch (ces.markType) {
            case Setting.BOARD_MARKTYPE_CELL:
                if (board.getMarkedCellCount() < ces.markMin || ces.markMax < board.getMarkedCellCount()) {
                    return;
                }
                break;
            case Setting.BOARD_MARKTYPE_CHIP:
                if (board.getMarkedChipCount() < ces.markMin || ces.markMax < board.getMarkedChipCount()) {
                    return;
                }
                break;
            default:
                throw new AssertionError();
        }

        board.minimizeTicket();
        board.colorChips();

        if (cs.rotation || board.getTicketCount() == 0) {
            progress.addBoard(board);
            if (RESULT_LIMIT > 0 && progress.getBoardSize() > RESULT_LIMIT) {
                progress.removeLastBoard();
            }
            progress.nComb++;
            boardsChanged = true;
        }
    }

    private synchronized boolean checkPause() {
        while (status == Status.PAUSED) {
            wait_();
        }
        return status == Status.RUNNING;
    }

    private void offer(BlockingQueue<BoardTemplate> q, BoardTemplate template) {
        while (checkPause() && !q.offer(template)) {
            wait_();
        }
    }

    private BoardTemplate poll(BlockingQueue<BoardTemplate> q) {
        BoardTemplate template = null;
        while (checkPause() && null == (template = q.poll())) {
            wait_();
        }
        return template;
    }

    private synchronized void wait_() {
        try {
            wait(10);
        } catch (InterruptedException ignored) {
        }
    }
}
