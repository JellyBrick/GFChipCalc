package main.puzzle

import main.ui.resource.AppColor
import main.ui.resource.AppText
import main.util.DoubleKeyHashMap
import main.util.Fn
import main.util.IO
import main.util.Rational
import java.awt.Point
import java.io.Serializable
import java.util.*
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
class Board : Comparable<Board>, Serializable {
    // </editor-fold>
    val name: String
    val star: Int
    private var chips: MutableList<Chip>
    var matrix: PuzzleMatrix<Int>
    private val maxStat: Stat
    private val stat: Stat
    private val pt: Stat
    private val statPerc: Double
    val xp: Int
    override fun compareTo(o: Board): Int {
        val size = o.chips.size.compareTo(chips.size)
        if (size != 0) {
            return size
        }
        for (i in chips.indices) {
            val id = chips[i].id!!.compareTo(o.chips[i].id!!)
            if (id != 0) {
                return id
            }
        }
        return 0
    }

    // Combinator - fitness
    constructor(board: Board) {
        name = board.name
        star = board.star
        chips = mutableListOf()
        for (c in board.chips) {
            chips.add(Chip(c))
        }
        colorChips()
        matrix = PuzzleMatrix(board.matrix)
        maxStat = board.maxStat
        stat = board.stat
        pt = board.pt
        statPerc = board.statPerc
        xp = board.xp
    }

    // Combination File / Board Template
    constructor(name: String, star: Int, maxStat: Stat?, chips: List<Chip>, chipLocs: List<Point>) {
        this.name = name
        this.star = star
        this.chips = ArrayList(chips)
        colorChips()
        matrix = toPlacement(name, star, chips, chipLocs)
        this.maxStat = maxStat!!
        stat = Stat.chipStatSum(chips)
        pt = Stat.chipPtSum(chips)
        statPerc = getStatPerc(stat, this.maxStat)
        var xpSum = 0
        for (chip in chips) {
            xpSum += chip.getCumulXP()
        }
        xp = xpSum
    }

    // <editor-fold defaultstate="collapsed" desc="Color">
    fun getColor(): Int {
        return MAP_COLOR.getValue(name)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Rotation and Ticket">
    private fun rotate(i: Int) {
        matrix.rotateContent(i, UNUSED)
        for (chip in chips) {
            chip.rotate(i)
        }
    }

    fun getTicketCount(): Int {
        var sum = 0
        for (chip in chips) {
            sum += chip.getNumTicket()
        }
        return sum
    }

    fun minimizeTicket() {
        var rotation = 0
        while (rotation < 4) {

            // Start a new board
            val b = Board(this)
            val cShapes: MutableSet<Shape?> = HashSet()
            val newUsedChips: MutableList<Chip> = ArrayList(b.chips.size)
            // Rotate board
            b.rotate(rotation)
            for (chip in b.chips) {
                cShapes.add(chip.shape)
            }

            // Get indicies and candidates
            for (cs in cShapes) {
                val cIndices: MutableSet<Int> = hashSetOf()
                val cCandidates: MutableList<Chip> = mutableListOf()
                for (i in b.chips.indices) {
                    val c = b.chips[i]
                    if (c.shape == cs) {
                        cIndices.add(i)
                        cCandidates.add(Chip(c))
                    }
                }
                // Put matching initial rotation
                for (cIndex in cIndices) {
                    val r = b.chips[cIndex].rotation
                    for (c in cCandidates) {
                        if (c.initRotation == r) {
                            c.rotation = c.initRotation
                            newUsedChips[cIndex] = c
                            cCandidates.remove(c)
                            break
                        }
                    }
                }
            }
            b.chips = newUsedChips.toMutableList()
            // Replace if better
            if (getTicketCount() > b.getTicketCount()) {
                matrix = b.matrix
                chips = b.chips
            }
            // Exit if 0
            if (getTicketCount() == 0) {
                break
            }
            rotation += MAP_ROTATIONSTEP[name, star]!!
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="PT">
    fun getPt(): Stat {
        return pt
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Stat">
    fun getStat(): Stat {
        return stat
    }

    fun getOldStat(): Stat {
        return Stat.chipOldStatSum(chips)
    }

    fun getCustomMaxStat(): Stat {
        return maxStat
    }

    fun getOrigMaxStat(): Stat {
        return getMaxStat(name, star)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Stat Perc">
    fun getStatPerc(): Double {
        return statPerc
    }

    fun getStatPerc(type: Int): Double {
        val s = getStat()
        val m = getCustomMaxStat()
        return getStatPerc(type, s, m)
    }

    fun getResonance(): Stat {
        var numCell = 0
        for (chip in chips) {
            if (chip.color == getColor()) {
                numCell += chip.getSize()
            }
        }
        val stats: MutableList<Stat?> = mutableListOf()
        for (key in MAP_STAT_RESONANCE.keySet(name)!!) {
            if (key <= numCell) {
                stats.add(MAP_STAT_RESONANCE[name, key])
            }
        }
        return Stat(stats)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Mark">
    fun getMarkedCellCount(): Int {
        var sum = 0
        for (c in chips) {
            if (c.isMarked) {
                sum += c.getSize()
            }
        }
        return sum
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Chips">
    fun getChipCount(): Int {
        return chips.size
    }

    fun getChipIDs(): List<String?> {
        val IDs: MutableList<String?> = mutableListOf()
        for (c in chips) {
            IDs.add(c.id)
        }
        return IDs
    }

    fun forEachChip(action: Consumer<in Chip>?) {
        chips.forEach(action)
    }

    fun getChip(id: String?): Chip? {
        for (c in chips) {
            if (c.id == id) {
                return c
            }
        }
        return null
    }

    fun getLocation(c: Chip): Point? {
        val i = chips.indexOf(c)
        return if (i < 0) {
            null
        } else matrix.getPivot(i)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Image">
    fun colorChips() {
        for (i in chips.indices) {
            val c = chips[i]
            c.boardIndex = i
        }
    }

    companion object {
        const val UNUSED = -2
        const val EMPTY = -1
        const val HEIGHT = 8
        const val WIDTH = 8
        const val NAME_BGM71 = "BGM-71"
        const val NAME_AGS30 = "AGS-30"
        const val NAME_2B14 = "2B14"
        const val NAME_M2 = "M2"
        const val NAME_AT4 = "AT4"
        const val NAME_QLZ04 = "QLZ-04"
        const val NAME_MK153 = "Mk 153"
        val NAMES = arrayOf(NAME_BGM71, NAME_AGS30, NAME_2B14, NAME_M2, NAME_AT4, NAME_QLZ04, NAME_MK153)
        fun getTrueName(fileName: String): String {
            for (name in NAMES) {
                if (IO.toFileName(name) == fileName) {
                    return name
                }
            }
            return ""
        }

        private val MAP_MATRIX: Map<String, Array<Array<Int>>> =
            object : HashMap<String, Array<Array<Int>>>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(
                        NAME_BGM71,
                        arrayOf(
                            arrayOf(6, 6, 6, 6, 6, 6, 6, 6),
                            arrayOf(6, 4, 4, 4, 3, 3, 3, 6),
                            arrayOf(6, 4, 1, 1, 1, 1, 2, 6),
                            arrayOf(6, 2, 1, 1, 1, 1, 2, 6),
                            arrayOf(6, 2, 1, 1, 1, 1, 2, 6),
                            arrayOf(6, 2, 1, 1, 1, 1, 5, 6),
                            arrayOf(6, 3, 3, 3, 5, 5, 5, 6),
                            arrayOf(6, 6, 6, 6, 6, 6, 6, 6)
                        )
                    )
                    put(
                        NAME_AGS30,
                        arrayOf(
                            arrayOf(6, 6, 5, 5, 6, 6, 6, 6),
                            arrayOf(6, 3, 3, 2, 2, 6, 6, 6),
                            arrayOf(4, 3, 1, 1, 1, 1, 6, 6),
                            arrayOf(4, 2, 1, 1, 1, 1, 2, 6),
                            arrayOf(6, 2, 1, 1, 1, 1, 2, 4),
                            arrayOf(6, 6, 1, 1, 1, 1, 3, 4),
                            arrayOf(6, 6, 6, 2, 2, 3, 3, 6),
                            arrayOf(6, 6, 6, 6, 5, 5, 6, 6)
                        )
                    )
                    put(
                        NAME_2B14,
                        arrayOf(
                            arrayOf(6, 6, 6, 6, 6, 6, 6, 6),
                            arrayOf(6, 6, 5, 6, 6, 5, 6, 6),
                            arrayOf(6, 2, 1, 1, 1, 1, 3, 6),
                            arrayOf(4, 2, 1, 1, 1, 1, 3, 4),
                            arrayOf(4, 2, 1, 1, 1, 1, 3, 4),
                            arrayOf(6, 2, 1, 1, 1, 1, 3, 6),
                            arrayOf(6, 6, 5, 6, 6, 5, 6, 6),
                            arrayOf(6, 6, 6, 6, 6, 6, 6, 6)
                        )
                    )
                    put(
                        NAME_M2,
                        arrayOf(
                            arrayOf(5, 3, 3, 6, 6, 6, 6, 5),
                            arrayOf(6, 3, 1, 1, 6, 6, 2, 4),
                            arrayOf(6, 6, 1, 1, 6, 2, 2, 4),
                            arrayOf(6, 6, 1, 1, 1, 1, 2, 6),
                            arrayOf(6, 2, 1, 1, 1, 1, 6, 6),
                            arrayOf(4, 2, 2, 6, 1, 1, 6, 6),
                            arrayOf(4, 2, 6, 6, 1, 1, 3, 6),
                            arrayOf(5, 6, 6, 6, 6, 3, 3, 5)
                        )
                    )
                    put(
                        NAME_AT4,
                        arrayOf(
                            arrayOf(6, 6, 6, 1, 1, 6, 6, 6),
                            arrayOf(6, 6, 1, 1, 1, 1, 6, 6),
                            arrayOf(6, 1, 1, 1, 1, 1, 1, 6),
                            arrayOf(2, 1, 1, 6, 6, 1, 1, 3),
                            arrayOf(2, 2, 2, 6, 6, 3, 3, 3),
                            arrayOf(6, 2, 2, 4, 4, 3, 3, 6),
                            arrayOf(6, 6, 5, 4, 4, 5, 6, 6),
                            arrayOf(6, 6, 6, 5, 5, 6, 6, 6)
                        )
                    )
                    put(
                        NAME_QLZ04,
                        arrayOf(
                            arrayOf(6, 6, 6, 6, 6, 6, 6, 6),
                            arrayOf(5, 3, 6, 6, 6, 6, 3, 5),
                            arrayOf(5, 3, 3, 6, 6, 3, 3, 5),
                            arrayOf(4, 1, 1, 1, 1, 1, 1, 4),
                            arrayOf(4, 1, 1, 1, 1, 1, 1, 4),
                            arrayOf(6, 1, 1, 2, 2, 1, 1, 6),
                            arrayOf(6, 6, 2, 2, 2, 2, 6, 6),
                            arrayOf(6, 6, 6, 2, 2, 6, 6, 6)
                        )
                    )
                    put(
                        NAME_MK153,
                        arrayOf(
                            arrayOf(6, 6, 2, 2, 6, 6, 6, 6),
                            arrayOf(6, 6, 2, 2, 5, 5, 5, 6),
                            arrayOf(6, 6, 2, 2, 4, 4, 4, 6),
                            arrayOf(6, 6, 2, 2, 3, 3, 4, 6),
                            arrayOf(1, 1, 1, 1, 1, 1, 3, 3),
                            arrayOf(1, 1, 1, 1, 1, 1, 3, 3),
                            arrayOf(6, 5, 1, 1, 6, 6, 6, 6),
                            arrayOf(6, 6, 1, 1, 6, 6, 6, 6)
                        )
                    )
                }
            } // </editor-fold>
        private val MAP_COLOR: Map<String, Int> =
            object : HashMap<String, Int>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(NAME_BGM71, Chip.COLOR_BLUE)
                    put(NAME_AGS30, Chip.COLOR_ORANGE)
                    put(NAME_2B14, Chip.COLOR_ORANGE)
                    put(NAME_M2, Chip.COLOR_BLUE)
                    put(NAME_AT4, Chip.COLOR_BLUE)
                    put(NAME_QLZ04, Chip.COLOR_ORANGE)
                    put(NAME_MK153, Chip.COLOR_BLUE)
                }
            } // </editor-fold>
        private val MAP_STAT_UNIT: Map<String, Stat> =
            object : HashMap<String, Stat>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(NAME_BGM71, Stat(155, 402, 349, 83))
                    put(NAME_AGS30, Stat(78, 144, 198, 386))
                    put(NAME_2B14, Stat(152, 58, 135, 160))
                    put(NAME_M2, Stat(113, 49, 119, 182))
                    put(NAME_AT4, Stat(113, 261, 284, 134))
                    put(NAME_QLZ04, Stat(77, 136, 188, 331))
                    put(NAME_MK153, Stat(107, 224, 233, 107))
                }
            } // </editor-fold>
        private val MAP_STAT_CHIP: Map<String, Array<Stat>> =
            object : HashMap<String, Array<Stat>>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(
                        NAME_BGM71, arrayOf(
                            Stat(95, 165, 96, 23),
                            Stat(114, 198, 115, 28),
                            Stat(133, 231, 134, 32),
                            Stat(162, 280, 162, 39),
                            Stat(190, 329, 191, 46)
                        )
                    )
                    put(
                        NAME_AGS30, arrayOf(
                            Stat(53, 65, 60, 117),
                            Stat(64, 78, 72, 140),
                            Stat(75, 91, 84, 163),
                            Stat(90, 111, 102, 198),
                            Stat(106, 130, 120, 233)
                        )
                    )
                    put(
                        NAME_2B14, arrayOf(
                            Stat(114, 29, 45, 54),
                            Stat(136, 35, 54, 64),
                            Stat(159, 41, 63, 75),
                            Stat(193, 49, 77, 91),
                            Stat(227, 58, 90, 107)
                        )
                    )
                    put(
                        NAME_M2, arrayOf(
                            Stat(103, 30, 49, 74),
                            Stat(124, 36, 59, 89),
                            Stat(145, 42, 68, 104),
                            Stat(176, 51, 83, 126),
                            Stat(206, 60, 97, 148)
                        )
                    )
                    put(
                        NAME_AT4, arrayOf(
                            Stat(85, 131, 95, 45),
                            Stat(102, 157, 114, 54),
                            Stat(118, 183, 133, 63),
                            Stat(144, 222, 161, 76),
                            Stat(169, 261, 190, 90)
                        )
                    )
                    put(
                        NAME_QLZ04, arrayOf(
                            Stat(61, 72, 66, 117),
                            Stat(73, 86, 79, 140),
                            Stat(85, 100, 93, 163),
                            Stat(103, 122, 112, 198),
                            Stat(122, 143, 132, 233)
                        )
                    )
                    put(
                        NAME_MK153, arrayOf(
                            Stat(98, 137, 95, 44),
                            Stat(117, 164, 114, 52),
                            Stat(137, 191, 133, 61),
                            Stat(166, 232, 162, 74),
                            Stat(195, 273, 190, 87)
                        )
                    )
                }
            } // </editor-fold>
        val MAP_STAT_RESONANCE: DoubleKeyHashMap<String, Int, Stat> =
            object : DoubleKeyHashMap<String, Int, Stat>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(NAME_BGM71, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(16, 0, 6, 0))
                            put(10, Stat(0, 8, 0, 3))
                            put(16, Stat(36, 0, 8, 0))
                            put(22, Stat(0, 14, 10, 0))
                            put(28, Stat(46, 0, 0, 6))
                            put(32, Stat(0, 18, 14, 0))
                            put(36, Stat(60, 26, 0, 0))
                        }
                    })
                    put(NAME_AGS30, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(8, 0, 4, 0))
                            put(10, Stat(0, 4, 0, 8))
                            put(16, Stat(14, 0, 6, 0))
                            put(24, Stat(0, 8, 0, 10))
                            put(30, Stat(26, 0, 12, 0))
                            put(34, Stat(0, 14, 0, 12))
                            put(38, Stat(36, 0, 0, 16))
                        }
                    })
                    put(NAME_2B14, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(16, 0, 6, 0))
                            put(10, Stat(0, 3, 0, 5))
                            put(16, Stat(36, 0, 0, 0))
                            put(20, Stat(0, 4, 8, 0))
                            put(24, Stat(58, 0, 0, 7))
                            put(28, Stat(0, 8, 0, 10))
                            put(32, Stat(82, 0, 8, 0))
                        }
                    })
                    put(NAME_M2, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(13, 0, 6, 0))
                            put(10, Stat(0, 3, 0, 6))
                            put(16, Stat(30, 0, 0, 0))
                            put(20, Stat(0, 4, 8, 0))
                            put(24, Stat(48, 0, 0, 9))
                            put(28, Stat(0, 8, 0, 13))
                            put(32, Stat(68, 0, 8, 0))
                        }
                    })
                    put(NAME_AT4, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(12, 0, 5, 0))
                            put(10, Stat(0, 5, 0, 5))
                            put(16, Stat(27, 0, 7, 0))
                            put(22, Stat(0, 10, 9, 0))
                            put(28, Stat(35, 0, 0, 10))
                            put(32, Stat(0, 12, 12, 0))
                            put(36, Stat(46, 18, 0, 0))
                        }
                    })
                    put(NAME_QLZ04, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(9, 0, 6, 0))
                            put(10, Stat(0, 6, 0, 6))
                            put(16, Stat(15, 0, 6, 0))
                            put(24, Stat(0, 9, 0, 9))
                            put(30, Stat(28, 0, 12, 0))
                            put(34, Stat(0, 15, 0, 10))
                            put(38, Stat(38, 0, 0, 14))
                        }
                    })
                    put(NAME_MK153, object : HashMap<Int, Stat>() {
                        init {
                            put(4, Stat(24, 0, 6, 0))
                            put(10, Stat(0, 12, 0, 10))
                            put(16, Stat(24, 0, 6, 0))
                            put(24, Stat(0, 12, 12, 0))
                            put(30, Stat(32, 0, 0, 10))
                            put(34, Stat(0, 18, 12, 0))
                            put(38, Stat(32, 18, 0, 0))
                        }
                    })
                }
            } // </editor-fold>
        private val MAP_STAT_ITERATION: Map<String, Array<Stat>> =
            object : HashMap<String, Array<Stat>>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(
                        NAME_BGM71, arrayOf(
                            Stat(4, 0, 6, 0),
                            Stat(0, 10, 9, 0),
                            Stat(5, 0, 0, 6),
                            Stat(0, 12, 10, 0),
                            Stat(7, 15, 0, 0),
                            Stat(8, 0, 12, 0),
                            Stat(0, 16, 14, 0),
                            Stat(9, 0, 0, 10),
                            Stat(0, 18, 18, 0),
                            Stat(12, 24, 0, 0)
                        )
                    )
                    put(
                        NAME_AGS30, arrayOf(
                            Stat(2, 0, 5, 0),
                            Stat(0, 4, 0, 8),
                            Stat(3, 0, 8, 0),
                            Stat(0, 6, 0, 12),
                            Stat(3, 0, 0, 13),
                            Stat(4, 0, 12, 0),
                            Stat(0, 10, 0, 12),
                            Stat(4, 0, 16, 0),
                            Stat(0, 16, 0, 16),
                            Stat(8, 0, 0, 18)
                        )
                    )
                    put(
                        NAME_2B14, arrayOf(
                            Stat(2, 0, 4, 0),
                            Stat(2, 3, 0, 4),
                            Stat(3, 0, 6, 0),
                            Stat(4, 4, 0, 4),
                            Stat(5, 0, 0, 5),
                            Stat(6, 0, 9, 0),
                            Stat(7, 3, 0, 6),
                            Stat(7, 0, 10, 0),
                            Stat(4, 5, 0, 9),
                            Stat(10, 0, 0, 6)
                        )
                    )
                    put(
                        NAME_M2, arrayOf(
                            Stat(2, 0, 4, 0),
                            Stat(1, 3, 0, 5),
                            Stat(3, 0, 6, 0),
                            Stat(3, 4, 0, 5),
                            Stat(4, 0, 0, 6),
                            Stat(5, 0, 9, 0),
                            Stat(6, 2, 0, 8),
                            Stat(6, 0, 9, 0),
                            Stat(3, 5, 0, 11),
                            Stat(8, 0, 0, 8)
                        )
                    )
                    put(
                        NAME_AT4, arrayOf(
                            Stat(3, 0, 5, 0),
                            Stat(0, 7, 8, 0),
                            Stat(4, 0, 0, 10),
                            Stat(0, 8, 9, 0),
                            Stat(5, 10, 0, 0),
                            Stat(6, 0, 10, 0),
                            Stat(0, 11, 12, 0),
                            Stat(7, 0, 0, 17),
                            Stat(0, 12, 15, 0),
                            Stat(9, 16, 0, 0)
                        )
                    )
                    put(
                        NAME_QLZ04, arrayOf(
                            Stat(3, 0, 3, 0),
                            Stat(2, 3, 0, 4),
                            Stat(4, 0, 6, 0),
                            Stat(3, 3, 0, 4),
                            Stat(5, 0, 0, 5),
                            Stat(5, 0, 10, 0),
                            Stat(6, 4, 0, 6),
                            Stat(6, 0, 10, 0),
                            Stat(4, 6, 0, 10),
                            Stat(8, 0, 0, 8)
                        )
                    )
                    put(
                        NAME_MK153, arrayOf(
                            Stat(4, 0, 4, 0),
                            Stat(0, 8, 6, 0),
                            Stat(6, 0, 0, 10),
                            Stat(0, 8, 8, 0),
                            Stat(6, 6, 0, 0),
                            Stat(10, 0, 10, 0),
                            Stat(0, 10, 10, 0),
                            Stat(10, 0, 0, 10),
                            Stat(0, 12, 12, 0),
                            Stat(16, 16, 0, 0)
                        )
                    )
                }
            } // </editor-fold>
        private val MAP_ROTATIONSTEP: DoubleKeyHashMap<String, Int, Int> =
            object : DoubleKeyHashMap<String, Int, Int>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    // generateRotationStep();
                    put(NAME_BGM71, 1, 1)
                    put(NAME_BGM71, 2, 2)
                    put(NAME_BGM71, 3, 2)
                    put(NAME_BGM71, 4, 4)
                    put(NAME_BGM71, 5, 1)
                    put(NAME_AGS30, 1, 1)
                    put(NAME_AGS30, 2, 1)
                    put(NAME_AGS30, 3, 2)
                    put(NAME_AGS30, 4, 2)
                    put(NAME_AGS30, 5, 2)
                    put(NAME_2B14, 1, 1)
                    put(NAME_2B14, 2, 2)
                    put(NAME_2B14, 3, 2)
                    put(NAME_2B14, 4, 2)
                    put(NAME_2B14, 5, 2)
                    put(NAME_M2, 1, 2)
                    put(NAME_M2, 2, 2)
                    put(NAME_M2, 3, 2)
                    put(NAME_M2, 4, 2)
                    put(NAME_M2, 5, 2)
                    put(NAME_AT4, 1, 4)
                    put(NAME_AT4, 2, 4)
                    put(NAME_AT4, 3, 4)
                    put(NAME_AT4, 4, 4)
                    put(NAME_AT4, 5, 1)
                    put(NAME_QLZ04, 1, 4)
                    put(NAME_QLZ04, 2, 4)
                    put(NAME_QLZ04, 3, 4)
                    put(NAME_QLZ04, 4, 4)
                    put(NAME_QLZ04, 5, 4)
                    put(NAME_MK153, 1, 4)
                    put(NAME_MK153, 2, 4)
                    put(NAME_MK153, 3, 4)
                    put(NAME_MK153, 4, 4)
                    put(NAME_MK153, 5, 4)
                }
            }

        fun getStarHTML_star(star: Int): String {
            val starStr = StringBuilder()
            starStr.append(AppText.TEXT_STAR_FULL.repeat(max(0, star)))
            return Fn.toHTML(Fn.htmlColor(starStr.toString(), AppColor.YELLOW_STAR))
        }

        fun getStarHTML_version(version: Int): String {
            val nFullRed = version / 2
            val fullRedStr = StringBuilder()
            fullRedStr.append(AppText.TEXT_STAR_FULL.repeat(max(0, nFullRed)))
            val nHalfRed = version % 2
            val halfRedStr = StringBuilder()
            halfRedStr.append(AppText.TEXT_STAR_EMPTY.repeat(max(0, nHalfRed)))
            val yellowStr = StringBuilder()
            yellowStr.append(
                AppText.TEXT_STAR_FULL.repeat(
                    max(
                        0,
                        5 - (fullRedStr.length + halfRedStr.length)
                    )
                )
            )
            return Fn.toHTML(
                Fn.htmlColor(fullRedStr.toString() + halfRedStr, AppColor.RED_STAR)
                        + Fn.htmlColor(yellowStr.toString(), AppColor.YELLOW_STAR)
            )
        }

        fun getColor(name: String?): Int {
            return if (MAP_COLOR.containsKey(name)) {
                MAP_COLOR[name]!!
            } else -1
        }

        fun getMaxPt(name: String, star: Int): Stat {
            return getMaxPt(name, star, getMaxStat(name, star))
        }

        fun getMaxPt(name: String, star: Int, stat: Stat): Stat {
            val statArray = stat.toArray()
            val optimalPtArray = IntArray(4)
            for (nChip in get56ChipCount(name, star)) {
                for (i in 0..3) {
                    val dist = getPtDistribution(
                        Chip.RATES[i],
                        nChip,
                        statArray[i]
                    )
                    var total = 0
                    for (d in dist) {
                        total += d
                    }
                    if (optimalPtArray[i] < total) {
                        optimalPtArray[i] = total
                    }
                }
            }
            val residue = (getCellCount(name, star)
                    - (optimalPtArray[0]
                    + optimalPtArray[1]
                    + optimalPtArray[2]
                    + optimalPtArray[3]))
            if (residue > 0) {
                for (i in 0..3) {
                    optimalPtArray[i] += residue
                }
            }
            return Stat(optimalPtArray)
        }

        fun getMaxStat(name: String, star: Int): Stat {
            return MAP_STAT_CHIP.getValue(name)[Fn.limit(
                star - 1,
                0,
                MAP_STAT_CHIP.getValue(name).size
            )]
        }

        private fun getStatPerc(stat: Stat, max: Stat): Double {
            if (max.allZero()) {
                return 1.0
            }
            if (stat.allGeq(max)) {
                return 1.0
            }
            val sArray = stat.limit(max).toArray()
            val mArray = max.toArray()
            var s = 0.0
            var m = 0.0
            for (i in 0..3) {
                s += Rational(sArray[i]).div(Chip.RATES[i]).double
                m += Rational(mArray[i]).div(Chip.RATES[i]).double
            }
            return if (m == 0.0) {
                1.0
            } else s / m
        }

        fun getStatPerc(type: Int, stat: Stat, max: Stat): Double {
            val s = stat.toArray()[type]
            val m = max.toArray()[type]
            return if (m == 0) {
                1.0
            } else min(s, m).toDouble() / m
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="HOC, Resonance, and Version">
        fun getHOCStat(name: String?): Stat? {
            return MAP_STAT_UNIT[name]
        }

        fun getVersionStat(name: String?, v: Int): Stat {
            val stats: MutableList<Stat?> = ArrayList(v)
            val array = MAP_STAT_ITERATION[name]!!
            stats.addAll(listOf(*array).subList(0, v))
            return Stat(stats)
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Stat Calc">
        private fun getPtDistribution(rate: Rational, nChip: Int, stat: Int): IntArray {
            val stat_1pt: Int = Chip.getMaxEffStat(rate, 1)
            val ptArray = IntArray(nChip)
            var i = 0
            while (calcStat(rate, ptArray) < stat) {
                ptArray[i]++
                val iPt = ptArray[i]
                val prevLoss = if (iPt > 0) (iPt - 1) * stat_1pt - Chip.getMaxEffStat(rate, iPt - 1) else 0
                val currentLoss: Int = iPt * stat_1pt - Chip.getMaxEffStat(rate, iPt)
                val nextLoss: Int = (iPt + 1) * stat_1pt - Chip.getMaxEffStat(rate, iPt + 1)
                if (currentLoss * 2 < prevLoss + nextLoss) {
                    i = (i + 1) % nChip
                }
            }
            return ptArray
        }

        private fun calcStat(rate: Rational, pts: IntArray): Int {
            var out = 0
            for (pt in pts) {
                out += Chip.getMaxEffStat(rate, pt)
            }
            return out
        }

        private fun get56ChipCount(name: String, star: Int): List<Int> {
            val nCell = getCellCount(name, star)
            val out: MutableList<Int> = mutableListOf()
            for (nSix in 0 until nCell / 6) {
                val rest = nCell - nSix * 6
                if (rest % 5 == 0) {
                    val nFive = rest / 5
                    out.add(nFive + nSix)
                }
            }
            return out
        }

        fun isChipPlaceable(matrix: PuzzleMatrix<Int>, cps: Set<Point>): Boolean {
            for (cp in cps) {
                if (matrix[cp.x, cp.y] == null || matrix[cp.x, cp.y] != EMPTY) {
                    return false
                }
            }
            return true
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Matrix and Cells">
        fun initMatrix(name: String, star: Int): PuzzleMatrix<Int> {
            val matrix = PuzzleMatrix(MAP_MATRIX.getValue(name))
            for (r in 0 until matrix.numRow) {
                for (c in 0 until matrix.numCol) {
                    matrix[r, c] =
                        if (matrix[r, c]!! <= star) EMPTY else UNUSED
                }
            }
            return matrix
        }

        fun getCellCount(name: String, star: Int): Int {
            val s = initMatrix(name, star)
            return s.getNumNotContaining(UNUSED)
        }

        private fun rs_getBoolMatrix(name: String, star: Int): PuzzleMatrix<Boolean> {
            val im = MAP_MATRIX.getValue(name)
            val bm = PuzzleMatrix(HEIGHT, WIDTH, false)
            for (r in 0 until HEIGHT) {
                for (c in 0 until WIDTH) {
                    bm[r, c] = im[r][c] <= star
                }
            }
            return bm
        }

        private fun rs_getPts(shape: Shape, rotation: Int, location: Point): Set<Point?> {
            val cm = PuzzleMatrix(Chip.generateMatrix(shape, rotation))
            val pivot = cm.getPivot(true)
            val pts = cm.getPoints(true)
            pts.forEach(Consumer { p: Point? -> p!!.translate(location.x - pivot.x, location.y - pivot.y) })
            return pts
        }

        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="File">
        fun toPlacement(name: String, star: Int, chips: List<Chip>, locations: List<Point>): PuzzleMatrix<Int> {
            val puzzles: MutableList<Puzzle> = ArrayList(chips.size)
            for (i in chips.indices) {
                val c = chips[i]
                val s = c.shape
                val r = c.rotation
                val l = locations[i]
                puzzles.add(Puzzle(s, r, l))
            }
            return toPlacement(name, star, puzzles)
        }

        fun toPlacement(name: String, star: Int, puzzles: List<Puzzle>): PuzzleMatrix<Int> {
            // Placement
            val placement = initMatrix(name, star)
            for (i in puzzles.indices) {
                val matrix = Chip.generateMatrix(puzzles[i].shape, puzzles[i].rotation)
                val pts = matrix.getPoints(true)
                val fp = matrix.getPivot(true)
                val bp = puzzles[i].location
                for (p in pts) {
                    p.translate(bp.x - fp.x, bp.y - fp.y)
                    placement[p.x, p.y] = i
                }
            }
            return placement
        }

        fun toLocation(placement: PuzzleMatrix<Int>): List<Point> {
            val location: MutableList<Point> = mutableListOf()
            var i = 0
            var found = true
            while (found) {
                val p = placement.getPivot(i)
                found = true
                location.add(p)
                i++
            }
            return location
        } // </editor-fold>
    }
}