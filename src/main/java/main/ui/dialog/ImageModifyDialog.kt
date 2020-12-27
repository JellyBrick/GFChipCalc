package main.ui.dialog

import main.App
import main.puzzle.Board
import main.puzzle.Chip
import main.puzzle.Shape
import main.puzzle.Stat
import main.ui.renderer.ChipListCellRenderer
import main.ui.resource.AppColor
import main.ui.resource.AppImage
import main.ui.resource.AppText
import main.util.Fn
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent

/**
 *
 * @author Bunnyspa
 */
class ImageModifyDialog private constructor(private val app: App, c: Chip?) : JDialog() {
    private val poolLM = DefaultListModel<Chip>()
    private var shape: Shape? = Shape.DEFAULT
    private var star = 5
    private var color: Int = Chip.COLOR_ORANGE
    private var pt = IntArray(4)
    private var level = 0
    private var rotation = 0
    private var cancelled = true
    private fun init() {
        initPool()
        poolRotLButton!!.icon = AppImage.ROTATE_LEFT
        poolRotRButton!!.icon = AppImage.ROTATE_RIGHT
        dmgTextLabel!!.icon = AppImage.DMG
        brkTextLabel!!.icon = AppImage.BRK
        hitTextLabel!!.icon = AppImage.HIT
        rldTextLabel!!.icon = AppImage.RLD
        val piStarCBList = arrayOf(
            Board.getStarHTML_star(5),
            Board.getStarHTML_star(4),
            Board.getStarHTML_star(3),
            Board.getStarHTML_star(2)
        )
        poolStarComboBox!!.model = DefaultComboBoxModel(piStarCBList)
        levelSpinner!!.model = SpinnerNumberModel(0, 0, Chip.LEVEL_MAX, 1)
        initText()
        addListeners()
        preferredSize = Dimension(preferredSize.width, app.mf.preferredDialogSize.height)
        pack()
    }

    private fun initPool() {
        poolList!!.model = poolLM // Renderer
        poolList!!.cellRenderer = ChipListCellRenderer(app)
        // Rows
        for (type in Shape.Type.values()) {
            for (s in Shape.getShapes(type)) {
                poolLM.addElement(Chip(s))
            }
        }
        poolList!!.addListSelectionListener { e: ListSelectionEvent? -> selectShape() }
    }

    private fun initText() {
        title = app.getText(app.getText(AppText.IMAGE_TITLE))
        statPanel!!.border = TitledBorder(app.getText(AppText.CSET_GROUP_STAT))
        dmgTextLabel!!.text = app.getText(AppText.CHIP_STAT_DMG)
        brkTextLabel!!.text = app.getText(AppText.CHIP_STAT_BRK)
        hitTextLabel!!.text = app.getText(AppText.CHIP_STAT_HIT)
        rldTextLabel!!.text = app.getText(AppText.CHIP_STAT_RLD)
        levelLabel!!.text = app.getText(AppText.CHIP_LEVEL)
        okButton!!.text = app.getText(AppText.ACTION_OK)
        cancelButton!!.text = app.getText(AppText.ACTION_CANCEL)
    }

    private fun addListeners() {
        poolRotLButton!!.addActionListener { e: ActionEvent? -> rotate(Chip.COUNTERCLOCKWISE) }
        poolRotRButton!!.addActionListener { e: ActionEvent? -> rotate(Chip.CLOCKWISE) }
        poolStarComboBox!!.addActionListener { e: ActionEvent? -> setStar() }
        poolColorButton!!.addActionListener { e: ActionEvent? -> cycleColor() }
        dmgComboBox!!.addItemListener { e: ItemEvent? -> setPt() }
        brkComboBox!!.addItemListener { e: ItemEvent? -> setPt() }
        hitComboBox!!.addItemListener { e: ItemEvent? -> setPt() }
        rldComboBox!!.addItemListener { e: ItemEvent? -> setPt() }
        levelSpinner!!.addChangeListener { e: ChangeEvent? -> setLevel() }
        Fn.addEscDisposeListener(this)
    }

    private fun readChip(chip: Chip?) {
        // Read data
        if (chip != null) {
            shape = chip.shape
            star = chip.star
            color = chip.color
            pt = chip.pt!!.toArray()
            level = chip.level
            rotation = chip.rotation
        }
        // Select shape
        for (i in 0 until poolLM.size()) {
            val c = poolLM[i]
            if (c.shape == shape) {
                poolList!!.selectedIndex = i
                break
            }
        }
        // Rotate
        for (i in 0 until rotation) {
            rotatePool(Chip.CLOCKWISE)
        }
        // Level
        levelSpinner!!.value = level
        // Color
        setColorText()
        updateAndVerify()
    }

    private var updating = false
    private fun updateAndVerify() {
        updating = true

        // Update stat values
        dmgComboBox!!.removeAllItems()
        brkComboBox!!.removeAllItems()
        hitComboBox!!.removeAllItems()
        rldComboBox!!.removeAllItems()
        for (i in 0..Chip.getMaxPt(shape!!.getSize())) {
            dmgComboBox!!.addItem(
                Chip.getStat(Chip.RATE_DMG, shape!!.getType(), star, level, i).toString()
            )
            brkComboBox!!.addItem(
                Chip.getStat(Chip.RATE_BRK, shape!!.getType(), star, level, i).toString()
            )
            hitComboBox!!.addItem(
                Chip.getStat(Chip.RATE_HIT, shape!!.getType(), star, level, i).toString()
            )
            rldComboBox!!.addItem(
                Chip.getStat(Chip.RATE_RLD, shape!!.getType(), star, level, i).toString()
            )
        }
        pt[0] = Fn.limit(pt[0], 0, dmgComboBox!!.itemCount - 1)
        pt[1] = Fn.limit(pt[1], 0, brkComboBox!!.itemCount - 1)
        pt[2] = Fn.limit(pt[2], 0, hitComboBox!!.itemCount - 1)
        pt[3] = Fn.limit(pt[3], 0, rldComboBox!!.itemCount - 1)
        dmgComboBox!!.selectedIndex = pt[0]
        brkComboBox!!.selectedIndex = pt[1]
        hitComboBox!!.selectedIndex = pt[2]
        rldComboBox!!.selectedIndex = pt[3]
        dmgPtLabel!!.text = pt[0].toString()
        brkPtLabel!!.text = pt[1].toString()
        hitPtLabel!!.text = pt[2].toString()
        rldPtLabel!!.text = pt[3].toString()

        // Verification
        val chip = Chip(shape!!, star, color, Stat(pt), level, rotation)
        val valid: Boolean = ImageDialog.isValid(chip)
        dmgComboBox!!.foreground = if (valid) Color.BLACK else Color.RED
        brkComboBox!!.foreground = if (valid) Color.BLACK else Color.RED
        hitComboBox!!.foreground = if (valid) Color.BLACK else Color.RED
        rldComboBox!!.foreground = if (valid) Color.BLACK else Color.RED
        dmgPtLabel!!.foreground = if (valid) Color.BLACK else Color.RED
        brkPtLabel!!.foreground = if (valid) Color.BLACK else Color.RED
        hitPtLabel!!.foreground = if (valid) Color.BLACK else Color.RED
        rldPtLabel!!.foreground = if (valid) Color.BLACK else Color.RED
        okButton!!.isEnabled = valid
        updating = false
    }

    private fun selectShape() {
        shape = poolList!!.selectedValue.shape
        updateAndVerify()
    }

    private fun rotate(direction: Boolean) {
        rotatePool(direction)
        rotation = poolList!!.selectedValue.rotation
        updateAndVerify()
    }

    private fun rotatePool(direction: Boolean) {
        val elements = poolLM.elements()
        while (elements.hasMoreElements()) {
            val c = elements.nextElement()
            c.initRotate(direction)
        }
        poolList!!.repaint()
    }

    private fun setStar() {
        star = 5 - poolStarComboBox!!.selectedIndex
        updateAndVerify()
    }

    private fun setLevel() {
        level = levelSpinner!!.value as Int
        updateAndVerify()
    }

    private fun setPt() {
        if (!updating) {
            pt[0] = dmgComboBox!!.selectedIndex
            pt[1] = brkComboBox!!.selectedIndex
            pt[2] = hitComboBox!!.selectedIndex
            pt[3] = rldComboBox!!.selectedIndex
            updateAndVerify()
        }
    }

    private fun cycleColor() {
        setColor((color + 1) % AppText.TEXT_MAP_COLOR.size)
    }

    private fun setColor(c: Int) {
        color = c
        setColorText()
    }

    private fun setColorText() {
        poolColorButton!!.text = app.getText(AppText.TEXT_MAP_COLOR[color]!!)
        poolColorButton!!.foreground = AppColor.CHIPS[color]
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private fun initComponents() {
        okButton = JButton()
        cancelButton = JButton()
        chipScrollPane = JScrollPane()
        poolList = JList()
        poolRotLButton = JButton()
        poolRotRButton = JButton()
        jPanel13 = JPanel()
        poolColorButton = JButton()
        poolStarComboBox = JComboBox()
        statPanel = JPanel()
        dmgPanel = JPanel()
        dmgTextLabel = JLabel()
        dmgComboBox = JComboBox()
        dmgPtLabel = JLabel()
        brkPanel = JPanel()
        brkTextLabel = JLabel()
        brkComboBox = JComboBox()
        brkPtLabel = JLabel()
        hitPanel = JPanel()
        hitTextLabel = JLabel()
        hitComboBox = JComboBox()
        hitPtLabel = JLabel()
        rldPanel = JPanel()
        rldTextLabel = JLabel()
        rldComboBox = JComboBox()
        rldPtLabel = JLabel()
        levelSpinner = JSpinner()
        levelLabel = JLabel()
        defaultCloseOperation = DISPOSE_ON_CLOSE
        title = "도움말"
        modalityType = ModalityType.APPLICATION_MODAL
        isResizable = false
        type = Type.UTILITY
        okButton!!.text = "ok"
        okButton!!.addActionListener { evt: ActionEvent -> okButtonActionPerformed(evt) }
        cancelButton!!.text = "cancel"
        cancelButton!!.addActionListener { evt: ActionEvent -> cancelButtonActionPerformed(evt) }
        chipScrollPane!!.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        chipScrollPane!!.preferredSize = Dimension(250, 250)
        poolList!!.selectionMode = ListSelectionModel.SINGLE_SELECTION
        poolList!!.layoutOrientation = JList.HORIZONTAL_WRAP
        poolList!!.visibleRowCount = -1
        chipScrollPane!!.setViewportView(poolList)
        poolRotLButton!!.minimumSize = Dimension(50, 50)
        poolRotLButton!!.preferredSize = Dimension(50, 50)
        poolRotRButton!!.minimumSize = Dimension(50, 50)
        poolRotRButton!!.preferredSize = Dimension(50, 50)
        poolColorButton!!.margin = Insets(2, 2, 2, 2)
        poolColorButton!!.preferredSize = Dimension(100, 22)
        poolStarComboBox!!.preferredSize = Dimension(100, 22)
        val jPanel13Layout = GroupLayout(jPanel13)
        jPanel13!!.layout = jPanel13Layout
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(poolStarComboBox, 0, 150, Short.MAX_VALUE.toInt())
                .addComponent(
                    poolColorButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel13Layout.createSequentialGroup()
                        .addComponent(
                            poolStarComboBox,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            poolColorButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        statPanel!!.border = BorderFactory.createTitledBorder("stat")
        statPanel!!.isFocusable = false
        dmgPanel!!.layout = BorderLayout(5, 0)
        dmgTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        dmgTextLabel!!.text = "살상"
        dmgTextLabel!!.isFocusable = false
        dmgTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        dmgPanel!!.add(dmgTextLabel, BorderLayout.LINE_START)
        dmgComboBox!!.preferredSize = Dimension(50, 22)
        dmgPanel!!.add(dmgComboBox, BorderLayout.CENTER)
        dmgPtLabel!!.horizontalAlignment = SwingConstants.CENTER
        dmgPtLabel!!.text = "-"
        dmgPtLabel!!.border = BorderFactory.createEtchedBorder()
        dmgPtLabel!!.isFocusable = false
        dmgPtLabel!!.preferredSize = Dimension(22, 22)
        dmgPanel!!.add(dmgPtLabel, BorderLayout.LINE_END)
        brkPanel!!.layout = BorderLayout(5, 0)
        brkTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        brkTextLabel!!.text = "파쇄"
        brkTextLabel!!.isFocusable = false
        brkTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        brkPanel!!.add(brkTextLabel, BorderLayout.LINE_START)
        brkComboBox!!.preferredSize = Dimension(50, 22)
        brkPanel!!.add(brkComboBox, BorderLayout.CENTER)
        brkPtLabel!!.horizontalAlignment = SwingConstants.CENTER
        brkPtLabel!!.text = "-"
        brkPtLabel!!.border = BorderFactory.createEtchedBorder()
        brkPtLabel!!.isFocusable = false
        brkPtLabel!!.preferredSize = Dimension(22, 22)
        brkPanel!!.add(brkPtLabel, BorderLayout.LINE_END)
        hitPanel!!.layout = BorderLayout(5, 0)
        hitTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        hitTextLabel!!.text = "정밀"
        hitTextLabel!!.isFocusable = false
        hitTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        hitPanel!!.add(hitTextLabel, BorderLayout.LINE_START)
        hitComboBox!!.preferredSize = Dimension(50, 22)
        hitPanel!!.add(hitComboBox, BorderLayout.CENTER)
        hitPtLabel!!.horizontalAlignment = SwingConstants.CENTER
        hitPtLabel!!.text = "-"
        hitPtLabel!!.border = BorderFactory.createEtchedBorder()
        hitPtLabel!!.isFocusable = false
        hitPtLabel!!.preferredSize = Dimension(22, 22)
        hitPanel!!.add(hitPtLabel, BorderLayout.LINE_END)
        rldPanel!!.layout = BorderLayout(5, 0)
        rldTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        rldTextLabel!!.text = "장전"
        rldTextLabel!!.isFocusable = false
        rldTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        rldPanel!!.add(rldTextLabel, BorderLayout.LINE_START)
        rldComboBox!!.preferredSize = Dimension(50, 22)
        rldPanel!!.add(rldComboBox, BorderLayout.CENTER)
        rldPtLabel!!.horizontalAlignment = SwingConstants.CENTER
        rldPtLabel!!.text = "-"
        rldPtLabel!!.border = BorderFactory.createEtchedBorder()
        rldPtLabel!!.isFocusable = false
        rldPtLabel!!.preferredSize = Dimension(22, 22)
        rldPanel!!.add(rldPtLabel, BorderLayout.LINE_END)
        val statPanelLayout = GroupLayout(statPanel)
        statPanel!!.layout = statPanelLayout
        statPanelLayout.setHorizontalGroup(
            statPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(dmgPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(brkPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(hitPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(rldPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        statPanelLayout.setVerticalGroup(
            statPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    statPanelLayout.createSequentialGroup()
                        .addComponent(
                            dmgPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            brkPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            hitPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            rldPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        levelSpinner!!.preferredSize = Dimension(100, 22)
        levelLabel!!.text = "level"
        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addComponent(
                                            poolRotLButton,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addGap(0, 0, 0)
                                        .addComponent(
                                            poolRotRButton,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            jPanel13,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                )
                                .addComponent(
                                    chipScrollPane,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                                        .addComponent(okButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cancelButton)
                                )
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addComponent(levelLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            levelSpinner,
                                            GroupLayout.DEFAULT_SIZE,
                                            113,
                                            Short.MAX_VALUE.toInt()
                                        )
                                )
                                .addComponent(
                                    statPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addContainerGap()
                )
        )
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addComponent(
                                            statPanel,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(
                                            layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(
                                                    levelSpinner,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.PREFERRED_SIZE
                                                )
                                                .addComponent(levelLabel)
                                        )
                                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                                )
                                .addComponent(
                                    chipScrollPane,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addGroup(
                                    GroupLayout.Alignment.TRAILING,
                                    layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(cancelButton)
                                        .addComponent(okButton)
                                )
                                .addComponent(
                                    poolRotRButton,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    poolRotLButton,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    jPanel13,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addContainerGap()
                )
        )
        pack()
    } // </editor-fold>//GEN-END:initComponents

    private fun okButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_okButtonActionPerformed
        cancelled = false
        dispose()
    } //GEN-LAST:event_okButtonActionPerformed

    private fun cancelButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_cancelButtonActionPerformed
        dispose()
    } //GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var brkComboBox: JComboBox<String>? = null
    private var brkPanel: JPanel? = null
    private var brkPtLabel: JLabel? = null
    private var brkTextLabel: JLabel? = null
    private var cancelButton: JButton? = null
    private var chipScrollPane: JScrollPane? = null
    private var dmgComboBox: JComboBox<String>? = null
    private var dmgPanel: JPanel? = null
    private var dmgPtLabel: JLabel? = null
    private var dmgTextLabel: JLabel? = null
    private var hitComboBox: JComboBox<String>? = null
    private var hitPanel: JPanel? = null
    private var hitPtLabel: JLabel? = null
    private var hitTextLabel: JLabel? = null
    private var jPanel13: JPanel? = null
    private var levelLabel: JLabel? = null
    private var levelSpinner: JSpinner? = null
    private var okButton: JButton? = null
    private var poolColorButton: JButton? = null
    private var poolList: JList<Chip>? = null
    private var poolRotLButton: JButton? = null
    private var poolRotRButton: JButton? = null
    private var poolStarComboBox: JComboBox<String>? = null
    private var rldComboBox: JComboBox<String>? = null
    private var rldPanel: JPanel? = null
    private var rldPtLabel: JLabel? = null
    private var rldTextLabel: JLabel? = null
    private var statPanel: JPanel? = null // End of variables declaration//GEN-END:variables

    companion object {
        fun modify(app: App, c: Chip?): Chip? {
            val d = ImageModifyDialog(app, c)
            d.isVisible = true
            return if (d.cancelled) {
                null
            } else Chip(d.shape!!, d.star, d.color, Stat(d.pt), d.level, d.rotation)
        }
    }

    init {
        initComponents()
        init()
        readChip(c)
    }
}