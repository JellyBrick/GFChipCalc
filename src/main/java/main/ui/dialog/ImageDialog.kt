package main.ui.dialog

import main.App
import main.image.ImageProcessor
import main.puzzle.*
import main.puzzle.Shape
import main.ui.resource.AppImage
import main.ui.resource.AppText
import main.util.Fn
import main.util.ThreadPoolManager
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
class ImageDialog private constructor(private val app: App) : JDialog() {
    private val fileChooser = JFileChooser(File("."))
    private lateinit var imageLabel: JLabel
    private var image: BufferedImage? = null
    private var cancelled = true

    private class RC(val rect: Rectangle, var chip: Chip)

    private val rcs: MutableList<RC> = mutableListOf()
    private val chipImagePoppedup: MutableMap<Int, JLabel> = HashMap()
    private var zoom = 100

    internal enum class Interaction {
        DISABLED, ADD, DELETE
    }

    private var interactionMode = Interaction.DISABLED
    private var isMouseDown = false
    private var startP: Point? = null
    private var currentP: Point? = null
    private fun init() {
        title = app.getText(AppText.IMAGE_TITLE)
        fileChooser.fileFilter = FileNameExtensionFilter(
            app.getText(AppText.IMAGE_OPEN_EXT) + " (.png, .jpg, .gif, .bmp)",
            "png", "jpg", "gif", "bmp"
        )
        openButton!!.text = app.getText(AppText.IMAGE_OPEN)
        addToggleButton!!.text = app.getText(AppText.ACTION_ADD)
        deleteToggleButton!!.text = app.getText(AppText.ACTION_DEL)
        okButton!!.text = app.getText(AppText.ACTION_OK)
        cancelButton!!.text = app.getText(AppText.ACTION_CANCEL)
        initLabel()
        aScrollPane!!.setViewportView(imageLabel)
        zoomSpinner!!.model = SpinnerNumberModel(100, ZOOM_MIN, ZOOM_MAX, 10)
        addListeners()
        this.preferredSize = app.mf.preferredDialogSize
        pack()
    }

    private fun initLabel() {
        imageLabel = object : JLabel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                if (image != null) {
                    val g2 = g as Graphics2D
                    g2.stroke = BasicStroke(3F)
                    if (interactionMode == Interaction.ADD) {
                        if (isMouseDown) {
                            val r = getRectWithPts(startP!!, currentP!!)
                            g2.color = Color.WHITE
                            g2.drawRect(r.x, r.y, r.width, r.height)
                        }
                    }
                    val invalidRectIndices: MutableSet<Int> = HashSet()
                    for (i in rcs.indices) {
                        val chip = rcs[i].chip
                        if (!isValid(chip)) {
                            invalidRectIndices.add(i)
                        }
                    }
                    for (i in rcs.indices) {
                        val rect = rcs[i].rect
                        g2.color =
                            if (i == selectedRCIndex) Color.YELLOW else if (invalidRectIndices.contains(i)) Color.RED else Color.GREEN
                        g2.drawRect(
                            rect.x * zoom / 100,
                            rect.y * zoom / 100,
                            rect.width * zoom / 100,
                            rect.height * zoom / 100
                        )
                    }
                }
            }
        }
        imageLabel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(me: MouseEvent) {
                startP = me.point
                isMouseDown = true
            }

            override fun mouseReleased(me: MouseEvent) {
                when (interactionMode) {
                    Interaction.ADD -> {
                        val rect = getRectWithPts(startP!!, currentP!!)
                        addRect(unzoomRect(rect, zoom), true)
                    }
                    Interaction.DELETE -> deleteSelectedChip()
                    else -> modifySelectedChip()
                }
                setInteractionMode(Interaction.DISABLED)
                addToggleButton!!.isSelected = false
                deleteToggleButton!!.isSelected = false
                isMouseDown = false
            }
        })
        imageLabel.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(me: MouseEvent) {
                updatePoint(me)
            }

            override fun mouseMoved(me: MouseEvent) {
                updatePoint(me)
                popupChipImage()
            }

            fun updatePoint(me: MouseEvent) {
                val p = me.point
                p.x = Fn.limit(p.x, 0, imageLabel.width)
                p.y = Fn.limit(p.y, 0, imageLabel.height)
                currentP = p
                aScrollPane!!.repaint()
            }
        })
        imageLabel.addMouseWheelListener({ e: MouseWheelEvent ->
            var bar = aScrollPane!!.verticalScrollBar
            if (e.isControlDown) {
                if (e.wheelRotation < 0) {
                    zoomSpinner!!.value = Fn.limit(zoom + 10, ZOOM_MIN, ZOOM_MAX)
                } else {
                    zoomSpinner!!.value = Fn.limit(zoom - 10, ZOOM_MIN, ZOOM_MAX)
                }
            } else if (e.isShiftDown) {
                bar = aScrollPane!!.horizontalScrollBar
            }
            val `val` = bar.value
            val inc = 100
            if (e.wheelRotation < 0) {
                bar.value = bar.value - inc
            } else {
                bar.value = bar.value + inc
            }
        })
        imageLabel.background = Color.GRAY
        imageLabel.verticalAlignment = JLabel.TOP
        //  imageLabel.setLayout(new FlowLayout());
    }

    private fun addListeners() {
        Fn.addEscDisposeListener(this)
        zoomSpinner!!.addChangeListener { e: ChangeEvent? ->
            zoom = zoomSpinner!!.value as Int
            resizeImageWindow()
        }
    }

    private fun resizeImageWindow() {
        imageLabel.preferredSize = Dimension(image!!.width * zoom / 100, image!!.height * zoom / 100)
        val i = image!!.getScaledInstance(image!!.width * zoom / 100, image!!.height * zoom / 100, Image.SCALE_DEFAULT)
        imageLabel.icon = ImageIcon(i)
        refresh()
    }

    private fun refresh() {
        aScrollPane!!.revalidate()
        aScrollPane!!.repaint()
    }

    private fun setInteractionMode(interaction: Interaction) {
        interactionMode = interaction
        deleteToggleButton!!.isSelected = interactionMode == Interaction.DELETE
        addToggleButton!!.isSelected = interactionMode == Interaction.ADD
        imageLabel.cursor =
            if (interactionMode == Interaction.DISABLED) Cursor.getDefaultCursor() else Cursor.getPredefinedCursor(
                Cursor.CROSSHAIR_CURSOR
            )
        imageLabel.repaint()
    }

    private fun openImage(): Boolean {
        val retval = fileChooser.showOpenDialog(app.mf)
        if (retval == JFileChooser.APPROVE_OPTION) {
            try {
                readImage(ImageIO.read(fileChooser.selectedFile))
                resizeImageWindow()
            } catch (ex: IOException) {
                App.log(ex)
            }
            return true
        }
        return false
    }

    private fun readImage(image: BufferedImage) {
        this.image = image
        ThreadPoolManager.threadPool.execute {
            SwingUtilities.invokeLater { scanProgressBar!!.isIndeterminate = true }
            val candidates = ImageProcessor.detectChips(image)
            rcs.clear()
            SwingUtilities.invokeLater {
                scanProgressBar!!.isIndeterminate = false
                scanProgressBar!!.maximum = candidates.size
                scanProgressBar!!.value = 0
            }
            candidates.sortedWith { o1, o2 ->
                if (o2.y < o1.y + o1.height && o1.y < o2.y + o2.height) {
                    return@sortedWith o2.x.compareTo(o1.x)
                }
                o2.y.compareTo(o1.y)
            }.forEach { r: Rectangle ->
                addRect(r, false)
                SwingUtilities.invokeLater { scanProgressBar!!.value = scanProgressBar!!.value + 1 }
            }
        }
    }

    private fun addRect(rect: Rectangle, addedByUser: Boolean) {
        val overlapped = rcs.stream().anyMatch { rc: RC -> Fn.isOverlapped(rect, rc.rect) }
        if (!overlapped) {
            val c = ImageProcessor.idChip(image!!, rect)
            rcs.add(RC(rect, c))
            refresh()
        } else if (addedByUser) {
            JOptionPane.showMessageDialog(
                this,
                app.getText(AppText.IMAGE_OVERLAPPED_BODY),
                app.getText(AppText.IMAGE_OVERLAPPED_TITLE),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun modifySelectedChip() {
        val selected = selectedRCIndex
        if (-1 < selected) {
            val c: Chip? = ImageModifyDialog.modify(app, rcs[selected].chip)
            if (isValid(c)) {
                rcs[selected].chip = c!!
                refresh()
            }
        }
    }

    private fun deleteSelectedChip() {
        val selected = selectedRCIndex
        if (-1 < selected) {
            setPopupImageVisible(selected, false)
            rcs.removeAt(selected)
            refresh()
        }
    }

    private val selectedRCIndex: Int
        private get() {
            for (i in rcs.indices) {
                val rect = rcs[i].rect
                if (Fn.isInside(currentP, zoomRect(rect, zoom))) {
                    return i
                }
            }
            return -1
        }

    private fun popupChipImage() {
        val selected = selectedRCIndex
        if (-1 < selected) {
            val chip = rcs[selected].chip
            if (isValid(chip) && !chipImagePoppedup.containsKey(selected)) {
                setPopupImageVisible(selected, true)
            }
        }
        for (i in rcs.indices) {
            if (i != selected && chipImagePoppedup.containsKey(i)) {
                setPopupImageVisible(i, false)
            }
        }
    }

    private fun setPopupImageVisible(i: Int, b: Boolean) {
        if (b) {
            val chip = rcs[i].chip
            val rect = rcs[i].rect
            val icon = AppImage.Chip[app, chip]
            val label = genChipLabel(icon, zoomRect(rect, zoom))
            chipImagePoppedup[i] = label
            imageLabel.add(label)
        } else {
            val label = chipImagePoppedup[i]
            if (label != null) {
                chipImagePoppedup.remove(i)
                imageLabel.remove(label)
            }
        }
        imageLabel.revalidate()
        imageLabel.repaint()
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private fun initComponents() {
        cancelButton = JButton()
        okButton = JButton()
        openButton = JButton()
        jPanel1 = JPanel()
        aScrollPane = JScrollPane()
        addToggleButton = JToggleButton()
        deleteToggleButton = JToggleButton()
        zoomSpinner = JSpinner()
        scanProgressBar = JProgressBar()
        defaultCloseOperation = DISPOSE_ON_CLOSE
        modalityType = ModalityType.APPLICATION_MODAL
        type = Type.UTILITY
        cancelButton!!.text = "취소"
        cancelButton!!.isFocusable = false
        cancelButton!!.addActionListener { evt: ActionEvent -> cancelButtonActionPerformed(evt) }
        okButton!!.text = "확인"
        okButton!!.isFocusable = false
        okButton!!.addActionListener { evt: ActionEvent -> okButtonActionPerformed(evt) }
        openButton!!.text = "불러오기"
        openButton!!.isFocusable = false
        openButton!!.addActionListener { evt: ActionEvent -> openButtonActionPerformed(evt) }
        aScrollPane!!.cursor = Cursor(Cursor.DEFAULT_CURSOR)
        val jPanel1Layout = GroupLayout(jPanel1)
        jPanel1!!.layout = jPanel1Layout
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 0, Short.MAX_VALUE.toInt())
                .addGroup(
                    jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(aScrollPane)
                )
        )
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 415, Short.MAX_VALUE.toInt())
                .addGroup(
                    jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(aScrollPane)
                )
        )
        addToggleButton!!.text = "추가"
        addToggleButton!!.isFocusable = false
        addToggleButton!!.addActionListener { evt: ActionEvent -> addToggleButtonActionPerformed(evt) }
        deleteToggleButton!!.text = "삭제"
        deleteToggleButton!!.isFocusable = false
        deleteToggleButton!!.addActionListener { evt: ActionEvent -> deleteToggleButtonActionPerformed(evt) }
        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(openButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(zoomSpinner, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addToggleButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteToggleButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scanProgressBar, GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE.toInt())
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(okButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)
                        .addContainerGap()
                )
                .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(
                            jPanel1,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(cancelButton)
                                .addComponent(okButton)
                                .addComponent(openButton)
                                .addComponent(addToggleButton)
                                .addComponent(deleteToggleButton)
                                .addComponent(
                                    zoomSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(
                                    scanProgressBar,
                                    GroupLayout.PREFERRED_SIZE,
                                    23,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addContainerGap()
                )
        )
        pack()
    } // </editor-fold>//GEN-END:initComponents

    private fun cancelButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_cancelButtonActionPerformed
        dispose()
    } //GEN-LAST:event_cancelButtonActionPerformed

    private fun openButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_openButtonActionPerformed
        openImage()
    } //GEN-LAST:event_openButtonActionPerformed

    private fun addToggleButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_addToggleButtonActionPerformed
        setInteractionMode(if (interactionMode == Interaction.ADD) Interaction.DISABLED else Interaction.ADD)
    } //GEN-LAST:event_addToggleButtonActionPerformed

    private fun deleteToggleButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_deleteToggleButtonActionPerformed
        setInteractionMode(if (interactionMode == Interaction.DELETE) Interaction.DISABLED else Interaction.DELETE)
    } //GEN-LAST:event_deleteToggleButtonActionPerformed

    private fun okButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_okButtonActionPerformed
        rcs.sortWith { o1, o2 ->
            val r1 = o1.rect
            val r2 = o2.rect
            if (r2.y < r1.y + r1.height && r1.y < r2.y + r2.height) {
                return@sortWith r2.x.compareTo(r1.x)
            }
            r2.y.compareTo(r2.y)
        }
        cancelled = false
        dispose()
    } //GEN-LAST:event_okButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var aScrollPane: JScrollPane? = null
    private var addToggleButton: JToggleButton? = null
    private var cancelButton: JButton? = null
    private var deleteToggleButton: JToggleButton? = null
    private var jPanel1: JPanel? = null
    private var okButton: JButton? = null
    private var openButton: JButton? = null
    private var scanProgressBar: JProgressBar? = null
    private var zoomSpinner: JSpinner? = null // End of variables declaration//GEN-END:variables

    companion object {
        private const val ZOOM_MIN = 10
        private const val ZOOM_MAX = 1000
        fun getData(app: App): List<Chip> {
            val d = ImageDialog(app)
            d.isVisible = true
            val chips: MutableList<Chip> = ArrayList(d.rcs.size)
            if (!d.cancelled) {
                d.rcs.forEach { rc: RC -> chips.add(rc.chip) }
            }
            return chips
        }

        private fun getRectWithPts(p1: Point, p2: Point): Rectangle {
            val x = min(p1.x, p2.x)
            val y = min(p1.y, p2.y)
            val width = max(p1.x, p2.x) - x
            val height = max(p1.y, p2.y) - y
            return Rectangle(x, y, width, height)
        }

        private fun zoomRect(r: Rectangle, zoom: Int): Rectangle {
            return Rectangle(r.x * zoom / 100, r.y * zoom / 100, r.width * zoom / 100, r.height * zoom / 100)
        }

        private fun unzoomRect(r: Rectangle, zoom: Int): Rectangle {
            return Rectangle(r.x * 100 / zoom, r.y * 100 / zoom, r.width * 100 / zoom, r.height * 100 / zoom)
        }

        fun isValid(chip: Chip?): Boolean {
            return if (chip?.shape == null || chip.shape == Shape.NONE) {
                false
            } else chip.pt!!.sum() == chip.getSize()
        }

        private fun genChipLabel(icon: ImageIcon, rect: Rectangle): JLabel {
            val fitRect = Fn.fit(icon.iconWidth, icon.iconHeight, rect)
            val scaled = icon.image.getScaledInstance(fitRect.width, fitRect.height, Image.SCALE_SMOOTH)
            val label = JLabel()
            label.icon = ImageIcon(scaled)
            label.bounds = fitRect
            label.isOpaque = true
            return label
        }
    }

    init {
        initComponents()
        init()
        openImage()
    }
}