package main.ui.resource

import main.App
import main.puzzle.PuzzleMatrix
import main.setting.Setting
import main.util.Fn
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.UIManager
import kotlin.math.max

/**
 *
 * @author Bunnyspa
 */
object AppImage {
    val FAVICON: Image = getImage("favicon.png")
    val BANNER = getIcon("banner.png")
    val DONATION = getIcon("donation.png")
    val PAYPALQR = getIcon("paypalqr.png")
    val PAYPAL = getIcon("paypal.png")
    val CHIP_MARKED: Image = getImage("chip_marked.png")
    val CHIP_ROTATED: Image = getImage("chip_rotated.png")
    val CHIP_EQUIPPED: Image = getImage("chip_equipped.png")
    val UI_WARNING: Icon = UIManager.getIcon("OptionPane.warningIcon")
    val MP448 = getIcon("MP448.png")
    val FONT = getIcon("font.png")
    val QUESTION = getIcon("question.png")
    val PICTURE = getIcon("picture.png")
    val PHONE = getIcon("phone.png")
    val ASCNEDING = getIcon("ascending.png")
    val DESCENDING = getIcon("descending.png")
    val ROTATE_LEFT = getIcon("rotate_left.png")
    val ROTATE_RIGHT = getIcon("rotate_right.png")
    val PANEL_OPEN = getIcon("panel_open.png")
    val PANEL_CLOSE = getIcon("panel_close.png")
    val ADD = getIcon("add.png")
    val DMG = getIcon("dmg.png")
    val BRK = getIcon("brk.png")
    val HIT = getIcon("hit.png")
    val RLD = getIcon("rld.png")
    val STATS = arrayOf(DMG, BRK, HIT, RLD)
    val SAVE = getIcon("save.png")
    val NEW = getIcon("new.png")
    val SAVEAS = getIcon("saveas.png")
    val OPEN = getIcon("open.png")
    val DISPLAY_PT = getIcon("display_pt.png")
    val DISPLAY_STAT = getIcon("display_stat.png")
    val FILTER = getIcon("filter.png")
    val FILTER_APPLY = getIcon("filter_apply.png")
    val DELETE = getIcon("delete.png")
    val SETTING = getIcon("setting.png")
    val SETTING_PRESET = getIcon("setting_preset.png")
    val SETTING_PT = getIcon("setting_pt.png")
    val SETTING_STAT = getIcon("setting_stat.png")
    val COMB_START = getIcon("combine_start.png")
    val COMB_PAUSE = getIcon("combine_pause.png")
    val COMB_STOP = getIcon("combine_stop.png")
    val LOADING = getIcon("loading.gif")
    val PAUSED = getIcon("paused.png")
    val TICKET = getIcon("ticket.png")
    val CHECKED = getIcon("checked.png")
    val UNCHECKED = getIcon("unchecked.png")
    val HELP_PROXY = getIcon("help/proxy.jpg")
    private val IP_0 = getImage("imgproc/0.png")
    private val IP_1 = getImage("imgproc/1.png")
    private val IP_2 = getImage("imgproc/2.png")
    private val IP_3 = getImage("imgproc/3.png")
    private val IP_4 = getImage("imgproc/4.png")
    private val IP_5 = getImage("imgproc/5.png")
    private val IP_6 = getImage("imgproc/6.png")
    private val IP_7 = getImage("imgproc/7.png")
    private val IP_8 = getImage("imgproc/8.png")
    private val IP_9 = getImage("imgproc/9.png")
    val IP_DIGITS = arrayOf(
        IP_0, IP_1, IP_2, IP_3, IP_4,
        IP_5, IP_6, IP_7, IP_8, IP_9
    )
    val IP_DMG = getImage("imgproc/dmg.png")
    val IP_BRK = getImage("imgproc/brk.png")
    val IP_HIT = getImage("imgproc/hit.png")
    val IP_RLD = getImage("imgproc/rld.png")
    private fun getIcon(path: String): ImageIcon {
        return ImageIcon(App.getResource(path))
    }

    private fun getImage(path: String): BufferedImage {
        return ImageIO.read(App.getResource(path))
    }

    fun getScaledIcon(icon: Icon, width: Int, height: Int): ImageIcon {
        val bi = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val g = bi.createGraphics()
        icon.paintIcon(null, g, 0, 0)
        g.dispose()
        return ImageIcon(bi.getScaledInstance(width, height, Image.SCALE_SMOOTH))
    }

    object Chip {
        private const val CHIP_TILESIZE = 12
        private const val CHIP_GAP = 2
        private const val CHIP_HEIGHT1 = CHIP_TILESIZE * 6 + CHIP_GAP * 2
        private const val CHIP_HEIGHT2 = CHIP_TILESIZE * 3 + CHIP_GAP * 4
        operator fun get(app: App, chip: main.puzzle.Chip): ImageIcon {
            val statExists = chip.statExists()
            // Chip
            val star = chip.star
            val boardIndex = chip.boardIndex
            val marked = chip.isMarked
            val color = chip.color
            val level = chip.level
            val initLevel = chip.initLevel
            val rotation = chip.rotation
            val initRotation = chip.initRotation
            val displayType = chip.displayType
            // Matrix
            val matrix = chip.generateMatrix()
            val mw = matrix.numCol
            val mh = matrix.numRow
            // Image
            val iw = width(statExists)
            val ih = height(statExists)
            val yOffset1 = CHIP_TILESIZE + CHIP_GAP * 2
            val yOffset2 = CHIP_TILESIZE * 7 + CHIP_GAP * 4
            val bi = BufferedImage(iw + 1, ih + 1, BufferedImage.TYPE_INT_ARGB)
            val g = bi.graphics as Graphics2D
            if (statExists) {
                g.color = Color.BLACK
                g.drawRect(0, 0, iw, ih)
                g.fillRect(0, 0, iw, yOffset1 + 1)
                g.fillRect(0, yOffset2, iw, ih - yOffset2)
            }
            for (row in 0 until mh) {
                for (col in 0 until mw) {
                    if (matrix[row, col]!!) {
                        val tileXOffset = (iw / 2 + (col - mw.toDouble() / 2) * CHIP_TILESIZE).toInt()
                        val tileYOffset =
                            ((3 + row - mh.toDouble() / 2) * CHIP_TILESIZE).toInt() + (if (statExists) CHIP_TILESIZE + CHIP_GAP * 2 else 0) + CHIP_GAP
                        g.color = Color.BLACK
                        g.fillRect(tileXOffset, tileYOffset, CHIP_TILESIZE + 1, CHIP_TILESIZE + 1)
                        g.color =
                            if (boardIndex > -1) app.colors()[boardIndex % app.colors().size] else if (!statExists) AppColor.getPoolColor(
                                app,
                                chip
                            ) else if (marked) AppColor.CHIPS[color]!!
                                .darker().darker() else AppColor.CHIPS[color]
                        g.fillRect(tileXOffset + 1, tileYOffset + 1, CHIP_TILESIZE - 1, CHIP_TILESIZE - 1)
                    }
                }
            }
            if (statExists) {
                g.color = AppColor.YELLOW_STAR
                val starString = StringBuilder()
                starString.append(AppText.TEXT_STAR_FULL.repeat(max(0, star)))
                val xOffset = iw / 2
                g.drawString(starString.toString(), CHIP_GAP, CHIP_TILESIZE + CHIP_GAP)
                // Level
                g.font = AppFont.FONT_DIGIT
                if (0 < level) {
                    val levelStr = if (initLevel == level) "+$level" else initLevel.toString() + "\u2192" + level
                    val levelWidth = Fn.getWidth(levelStr, g.font)
                    g.color = AppColor.LEVEL
                    if (initLevel == level) {
                        g.fillPolygon(
                            intArrayOf(iw, iw, iw - CHIP_TILESIZE * 2 - CHIP_GAP),
                            intArrayOf(yOffset2, yOffset2 - CHIP_TILESIZE * 2 - CHIP_GAP, yOffset2),
                            3
                        )
                    } else {
                        g.fillRect(
                            iw - levelWidth - 1,
                            yOffset2 - CHIP_TILESIZE - CHIP_GAP,
                            levelWidth + 1,
                            CHIP_TILESIZE + CHIP_GAP
                        )
                    }
                    g.color = Color.WHITE
                    g.drawString(levelStr, iw - levelWidth, yOffset2 - 1)
                }
                // Equipped
                if (chip.containsHOCTagName()) {
                    g.drawImage(CHIP_EQUIPPED, 0, yOffset1, CHIP_TILESIZE, CHIP_TILESIZE, null)
                }
                // Rotation
                if (initRotation != rotation) {
                    g.drawImage(CHIP_ROTATED, iw - CHIP_TILESIZE + 1, yOffset1, CHIP_TILESIZE, CHIP_TILESIZE, null)
                }
                // Mark
                if (marked) {
                    g.drawImage(CHIP_MARKED, 0, yOffset2 - CHIP_TILESIZE, CHIP_TILESIZE, CHIP_TILESIZE, null)
                }
                if (chip.isPtValid()) {
                    val stats = (if (displayType == Setting.DISPLAY_STAT) chip.getStat() else chip.pt)!!.toArray()
                    val iPts = arrayOf(
                        Point(CHIP_GAP, yOffset2 + CHIP_TILESIZE + CHIP_GAP),
                        Point(xOffset, yOffset2 + CHIP_TILESIZE + CHIP_GAP),
                        Point(
                            CHIP_GAP, yOffset2 + CHIP_GAP
                        ),
                        Point(xOffset, yOffset2 + CHIP_GAP)
                    )
                    val sPts = arrayOf(
                        Point(CHIP_TILESIZE + CHIP_GAP + 1, yOffset2 + CHIP_TILESIZE * 2 + CHIP_GAP - 1), Point(
                            CHIP_TILESIZE + 1 + xOffset, yOffset2 + CHIP_TILESIZE * 2 + CHIP_GAP - 1
                        ), Point(
                            CHIP_TILESIZE + CHIP_GAP + 1, yOffset2 + CHIP_TILESIZE + CHIP_GAP - 1
                        ), Point(
                            CHIP_TILESIZE + 1 + xOffset, yOffset2 + CHIP_TILESIZE + CHIP_GAP - 1
                        )
                    )
                    var pi = 0
                    for (i in 0..3) {
                        if (stats[i] > 0) {
                            g.color = Color.WHITE
                            g.fillRect(iPts[pi].x, iPts[pi].y, CHIP_TILESIZE, CHIP_TILESIZE)
                            val image = STATS[i].image
                            g.drawImage(image, iPts[pi].x, iPts[pi].y, CHIP_TILESIZE, CHIP_TILESIZE, null)
                            g.color =
                                if (level == 0 || displayType == Setting.DISPLAY_PT) Color.WHITE else AppColor.LEVEL
                            val x = sPts[pi].x + (xOffset - CHIP_TILESIZE - Fn.getWidth(
                                stats[i].toString(), AppFont.FONT_DIGIT
                            )) / 2
                            val y = sPts[pi].y
                            g.drawString(stats[i].toString(), x, y)
                            pi++
                        }
                    }
                }
            }
            return ImageIcon(bi)
        }

        fun height(statExists: Boolean): Int {
            return if (statExists) CHIP_HEIGHT1 + CHIP_HEIGHT2 else CHIP_HEIGHT1
        }

        fun width(statExists: Boolean): Int {
            var width = CHIP_TILESIZE * 6 + CHIP_GAP * 2
            if (statExists) {
                width = max(width, CHIP_TILESIZE * 5)
            }
            return width
        }
    }

    object Board {
        operator fun get(app: App, size: Int, boardName: String, boardStar: Int): ImageIcon {
            return get(app, size, main.puzzle.Board.initMatrix(boardName, boardStar))
        }

        operator fun get(app: App, size: Int, board: main.puzzle.Board): ImageIcon {
            return Board[app, size, board.matrix]
        }

        operator fun get(app: App, size: Int, matrix: PuzzleMatrix<Int>): ImageIcon {
            val tileSize = size / 8
            val h: Int = main.puzzle.Board.HEIGHT
            val w: Int = main.puzzle.Board.WIDTH
            val i = BufferedImage(h * tileSize + 1, w * tileSize + 1, BufferedImage.TYPE_INT_ARGB)
            val g = i.graphics as Graphics2D
            for (row in 0 until h) {
                for (col in 0 until w) {
                    val s = matrix[row, col]!!
                    val x = col * tileSize
                    val y = row * tileSize
                    // Tiles
                    g.color =
                        if (s == main.puzzle.Board.UNUSED) Color.BLACK else if (s == main.puzzle.Board.EMPTY) Color.WHITE else app.colors()[s % app.colors().size]
                    g.fillRect(x, y, tileSize, tileSize)
                    // Horizontal Border
                    g.color = Color.BLACK
                    if (0 < row && matrix[row - 1, col] != s) {
                        g.drawLine(x, y, x + tileSize, y)
                    }
                    // Vertical Border
                    if (0 < col && matrix[row, col - 1] != s) {
                        g.drawLine(x, y, x, y + tileSize)
                    }
                }
            }
            // Border
            g.color = Color.BLACK
            g.drawLine(0, 0, tileSize * w, 0)
            g.drawLine(0, 0, 0, tileSize * h)
            g.drawLine(0, tileSize * h, tileSize * w, tileSize * h)
            g.drawLine(tileSize * w, 0, tileSize * w, tileSize * h)
            return ImageIcon(i)
        }
    }
}