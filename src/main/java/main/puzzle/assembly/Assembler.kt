package main.puzzle.assembly

import main.iterator.ChipCiterator
import main.iterator.ShapeCiterator
import main.puzzle.*
import main.puzzle.assembly.dxz.DXZ
import main.setting.Setting
import main.util.IO
import main.util.ThreadPoolManager
import java.awt.Point
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BooleanSupplier
import kotlin.concurrent.withLock

/**
 *
 * @author Bunnyspa
 */
class Assembler(private val intermediate: Intermediate) {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    interface Intermediate {
        fun stop()
        fun update(nDone: Int)
        operator fun set(nDone: Int, nTotal: Int)
        fun show(template: BoardTemplate)
    }

    enum class Status {
        STOPPED, RUNNING, PAUSED
    }

    companion object {
        private const val RESULT_LIMIT = 100
        private var fullBTM = BoardTemplateMap()
        private var partialBTM: BoardTemplateMap
        fun generateTemplate(
            boardName: String,
            boardStar: Int,
            shapes: List<Shape>,
            checkPause: BooleanSupplier
        ): BoardTemplate {
            return generateTemplate_DXZ(boardName, boardStar, shapes, checkPause)
        }

        private fun generateTemplate_DXZ(
            boardName: String,
            boardStar: Int,
            shapes: List<Shape>,
            checkPause: BooleanSupplier
        ): BoardTemplate {
            val puzzle: PuzzleMatrix<Int> = Board.initMatrix(boardName, boardStar)
            val emptyCoords =
                puzzle.getPoints(Board.EMPTY)
            val nCol_name = shapes.size
            val nCol_cell = puzzle.getNumContaining(Board.EMPTY)
            val nCol = nCol_name + nCol_cell
            val cols_pt: List<Point?> = ArrayList(emptyCoords)
            val rows: MutableList<BooleanArray> =
                mutableListOf()
            val rows_puzzle: MutableList<Puzzle> = mutableListOf()
            for (i in 0 until nCol_name) {
                val shape = shapes[i]
                for (rot in 0 until shape.maxRotation) {
                    for (bp in emptyCoords) {
                        val tps =
                            translate(shape, rot, bp)
                        if (Board.isChipPlaceable(puzzle, tps)) {
                            val row = BooleanArray(nCol)
                            row[i] = true
                            tps.forEach { p ->
                                row[nCol_name + cols_pt.indexOf(p)] = true
                            }
                            rows.add(row)
                            rows_puzzle.add(Puzzle(shape, rot, bp))
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                return BoardTemplate.empty()
            }
            val resultRows = DXZ.solve(rows, checkPause) ?: return BoardTemplate.empty()
            val puzzles: MutableList<Puzzle> = mutableListOf()
            val sortedRows = ArrayList(resultRows)
            sortedRows.sortWith { o1, o2 ->
                Shape.compare(
                    rows_puzzle[o1].shape, rows_puzzle[o2].shape
                )
            }
            sortedRows.forEach { r ->
                puzzles.add(
                    rows_puzzle[r!!]
                )
            }
            // System.out.println(puzzles);
            return BoardTemplate(boardName, boardStar, puzzles)
        }

        private fun translate(shape: Shape, rotation: Int, bp: Point): Set<Point> {
            val cfp = shape.getPivot(rotation)
            val cps = shape.getPoints(rotation)
            for (cp in cps) {
                cp.translate(bp.x - cfp.x, bp.y - cfp.y)
            }
            return cps
        }

        init {
            // Full BoardTemplate
            for (name in Board.NAMES) {
                for (star in 1..5) {
                    val templates = IO.loadBoardTemplates(name, star, false)
                    var minType = Shape.Type._5A
                    if (Board.NAME_M2 == name || Board.NAME_MK153 == name && star <= 2) {
                        minType = Shape.Type._4
                    } else if (Board.NAME_MK153 == name && star == 5) {
                        minType = Shape.Type._5B
                    }
                    fullBTM.put(
                        name,
                        star,
                        templates,
                        minType
                    )
                }
            }
            // Partial BoardTemplate
            partialBTM = BoardTemplateMap()
            val templates = IO.loadBoardTemplates(Board.NAME_M2, 5, true)
            partialBTM.put(Board.NAME_M2, 5, templates, Shape.Type._5B)
        }
    }

    private lateinit var cs: CalcSetting
    private lateinit var ces: CalcExtraSetting
    private lateinit var progress: Progress
    private var boardsChanged = false

    @Volatile
    var status = Status.STOPPED
    private val checkPause = BooleanSupplier { checkPause() }
    fun btExists(name: String?, star: Int, alt: Boolean): Boolean {
        return getBT(name, star, alt) != null
    }

    private fun getBT(name: String?, star: Int, alt: Boolean): List<BoardTemplate?>? {
        return when {
            alt -> partialBTM[name, star]
            else -> fullBTM[name, star]
        }
    }

    fun getMinType(name: String?, star: Int, alt: Boolean): Shape.Type {
        return when {
            alt -> partialBTM.getMinType(name, star)
            else -> fullBTM.getMinType(name, star)
        }
    }

    fun hasPartial(name: String?, star: Int): Boolean {
        return partialBTM.containsKey(name, star)
    }

    operator fun set(cs: CalcSetting, ces: CalcExtraSetting, p: Progress) {
        this.cs = cs
        this.ces = ces
        progress = p
        ThreadPoolManager.threadPool.execute {
            if (status == Status.STOPPED) {
                status = Status.PAUSED
            }
            if (ces.calcMode != CalcExtraSetting.CALCMODE_FINISHED) {
                combine()
            } else {
                setProgBar()
            }
            status = Status.STOPPED
            intermediate.stop()
        }
    }

    private fun prog_inc() {
        progress.nDone++
        intermediate.update(progress.nDone)
    }

    fun pause() {
        status = Status.PAUSED
    }

    fun resume() {
        status = Status.RUNNING
    }

    fun stop() {
        status = Status.STOPPED
    }

    fun boardsUpdated(): Boolean {
        return boardsChanged
    }

    fun getResult(): AssemblyResult {
        boardsChanged = false
        return AssemblyResult(progress.getBoards(), progress.getChipFreqs())
    }

    private fun setProgBar() {
        intermediate[progress.nDone] = progress.nTotal
    }

    private fun combine() {
        val q: BlockingQueue<BoardTemplate> = ArrayBlockingQueue(5)
        val cIt = ChipCiterator(ces.chips)
        val calls: MutableList<Callable<Any>> = mutableListOf()
        calls.add(Executors.callable { combine_template(q, cIt) })
        calls.add(Executors.callable { combine_assemble(q, cIt) })
        try {
            ThreadPoolManager.threadPool.invokeAll(calls)
        } catch (ignored: InterruptedException) {
        }
    }

    private fun combine_template(q: BlockingQueue<BoardTemplate>, cIt: ChipCiterator) {
        // Dictionary
        if (ces.calcMode == CalcExtraSetting.CALCMODE_DICTIONARY) {
            val templates = getBT(cs.boardName, cs.boardStar, ces.calcModeTag == 1)
            var count = 0
            for (boardTemplate in templates!!) {
                if (cIt.hasEnoughChips(boardTemplate!!)) {
                    if (!cs.symmetry || boardTemplate.symmetry) {
                        count++
                    }
                }
            }
            progress.nTotal = count
            setProgBar()
            val pIt = templates.subList(progress.nDone, templates.size).iterator()
            while (checkPause() && pIt.hasNext()) {
                val template = pIt.next()
                if ((!cs.symmetry || template!!.symmetry)
                    && cIt.hasEnoughChips(template!!)
                ) {
                    offer(q, template)
                }
            }
        } //
        else {
            val it = ShapeCiterator(cs.boardName, cs.boardStar, ces.chips)
            progress.nTotal = it.total()
            setProgBar()
            it.skip(progress.nDone)
            while (checkPause() && it.hasNext()) {
                val template = combine_template_algX(it)
                offer(q, template)
            }
        }
        offer(q, BoardTemplate.end())
    }

    private fun combine_template_algX(iterator: ShapeCiterator): BoardTemplate {
        if (!iterator.isNextValid()) {
            iterator.skip()
            return BoardTemplate.empty()
        }
        val shapes = iterator.next()
        return generateTemplate(cs.boardName, cs.boardStar, shapes, checkPause)
    }

    private fun combine_assemble(q: BlockingQueue<BoardTemplate>, cIt: ChipCiterator) {
        while (checkPause()) {
            val template = poll(q)
            if (template == null || template.isEnd()) {
                return
            }
            if (!template.isEmpty()) {
                // Show progress
                intermediate.show(template)
                cIt.init(template)
                // For all combinations
                while (checkPause() && cIt.hasNext()) {
                    val candidates = cIt.next()
                    // Check PT
                    var addable = true
                    val pt = cs.pt!!.toArray()
                    for (c in candidates) {
                        val cpt = c!!.pt!!.toArray()
                        for (i in 0..3) {
                            pt[i] -= cpt[i]
                            if (pt[i] < 0) {
                                addable = false
                                break
                            }
                        }
                    }

                    // add
                    if (addable) {
                        val chips: MutableList<Chip> = mutableListOf()
                        for (i in candidates.indices) {
                            val c = Chip(candidates[i]!!)
                            c.rotation = template.getChipRotations()[i]
                            chips.add(c)
                        }
                        publishBoard(Board(cs.boardName, cs.boardStar, cs.stat, chips, template.getChipLocations()))
                    }
                }
            }
            prog_inc()
        }
    }

    @Synchronized
    fun publishBoard(board: Board) {
        when (ces.markType) {
            Setting.BOARD_MARKTYPE_CELL -> if (board.getMarkedCellCount() < ces.markMin || ces.markMax < board.getMarkedCellCount()) {
                return
            }
            Setting.BOARD_MARKTYPE_CHIP -> if (board.getMarkedCellCount() < ces.markMin || ces.markMax < board.getMarkedCellCount()) {
                return
            }
            else -> throw AssertionError()
        }
        board.minimizeTicket()
        board.colorChips()
        if (cs.rotation || board.getTicketCount() == 0) {
            progress.addBoard(board)
            if (RESULT_LIMIT > 0 && progress.getBoardSize() > RESULT_LIMIT) {
                progress.removeLastBoard()
            }
            progress.nComb++
            boardsChanged = true
        }
    }

    @Synchronized
    private fun checkPause(): Boolean {
        while (status == Status.PAUSED) {
            wait_()
        }
        return status == Status.RUNNING
    }

    private fun offer(q: BlockingQueue<BoardTemplate>, template: BoardTemplate) {
        while (checkPause() && !q.offer(template)) {
            wait_()
        }
    }

    private fun poll(q: BlockingQueue<BoardTemplate>): BoardTemplate? {
        var template: BoardTemplate? = null
        while (checkPause() && null == q.poll().also { template = it }) {
            wait_()
        }
        return template
    }

    private fun wait_() {
        lock.withLock {
            try {
                condition.await(10, TimeUnit.MILLISECONDS)
            } catch (ignored: InterruptedException) {
            }
        }
    }
}