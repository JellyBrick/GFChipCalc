package main.ui.help

import main.App
import main.puzzle.Board
import main.puzzle.Chip
import main.puzzle.Shape
import main.puzzle.Stat
import main.ui.renderer.ChipListCellRenderer
import main.ui.renderer.HelpLevelStatTableCellRenderer
import main.ui.renderer.HelpTableCellRenderer
import main.ui.resource.AppImage
import main.ui.resource.AppText
import main.util.Fn
import main.util.Rational
import main.util.Ref
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableModel

/**
 *
 * @author Bunnyspa
 */
class HelpChipDialog(private val app: App) : JDialog() {
    private val alm = DefaultListModel<Chip>()
    private val blm = DefaultListModel<Chip>()
    private val resonanceTM: DefaultTableModel = HelpTableModel()
    private val percTM: DefaultTableModel = HelpTableModel()
    private val multTM: DefaultTableModel = HelpTableModel()
    private val lossTM: DefaultTableModel = HelpTableModel()
    private val toggleType = Ref(STAT)
    private val aTimer = Timer(1000) { e: ActionEvent? -> rotateChips() }

    // private static final boolean CUMULATIVE = true;
    private var resonanceType = false

    private class HelpTableModel : DefaultTableModel() {
        override fun getColumnClass(column: Int): Class<*> {
            return String::class.java
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }

    private fun init() {
        title = app.getText(AppText.HELP_CHIP)
        aTabbedPane!!.addTab(
            app.getText(AppText.HELP_CHIP_INFO_POINT_TITLE), genTextPanel(
                app.getText(AppText.HELP_CHIP_INFO_POINT_BODY), multiplierPanel
            )
        )
        aTabbedPane!!.addTab(
            app.getText(AppText.HELP_CHIP_INFO_EFFICIENCY_TITLE), genTextPanel(
                app.getText(AppText.HELP_CHIP_INFO_EFFICIENCY_BODY), efficiencyPanel, chipPanel
            )
        )
        aTabbedPane!!.addTab(
            app.getText(AppText.HELP_CHIP_INFO_COLOR_TITLE), genTextPanel(
                app.getText(AppText.HELP_CHIP_INFO_COLOR_BODY), bonusPanel
            )
        )
        aTabbedPane!!.addTab(
            app.getText(AppText.HELP_CHIP_INFO_CALC_TITLE), genTextPanel(
                app.getText(AppText.HELP_CHIP_INFO_CALC_BODY), calcPanel
            )
        )
        fiveAPanel!!.border = TitledBorder(app.getText(AppText.UNIT_CELLTYPE, "5", "A"))
        fiveBPanel!!.border = TitledBorder(app.getText(AppText.UNIT_CELLTYPE, "5", "B"))
        fiveAList!!.model = alm
        fiveAList!!.cellRenderer = ChipListCellRenderer(app, CHIP_SIZE_FACTOR)
        for (shape in Shape.getShapes(Shape.Type._5A)) {
            alm.addElement(Chip(shape))
        }
        fiveBList!!.model = blm
        fiveBList!!.cellRenderer = ChipListCellRenderer(app, CHIP_SIZE_FACTOR)
        for (shape in Shape.getShapes(Shape.Type._5B)) {
            blm.addElement(Chip(shape))
        }
        val chipWidth = (AppImage.Chip.width(false) * CHIP_SIZE_FACTOR).toInt()
        val chipHeight = (AppImage.Chip.height(false) * CHIP_SIZE_FACTOR).toInt()
        fiveAList!!.fixedCellWidth = chipWidth
        fiveAList!!.fixedCellHeight = chipHeight
        fiveAScrollPane!!.preferredSize = Dimension(chipWidth * 3 + GAP * 2, chipHeight * 3 + GAP * 2)
        fiveBList!!.fixedCellWidth = chipWidth
        fiveBList!!.fixedCellHeight = chipHeight
        fiveBScrollPane!!.preferredSize = Dimension(chipWidth * 3 + GAP * 2, chipHeight * 3 + GAP * 2)
        for (boardName in Board.NAMES) {
            resonanceBoardComboBox!!.addItem(boardName)
        }
        closeButton!!.text = app.getText(AppText.ACTION_CLOSE)
        initTables()
        addListeners()
        aTimer.start()
        this.preferredSize = app.mf.preferredDialogSize
        pack()
    }

    private fun initTables() {
        resonanceTable!!.model = resonanceTM
        resonanceTable!!.setDefaultRenderer(String::class.java, HelpTableCellRenderer(1, 1))
        val resonanceCols = arrayOf(
            app.getText(AppText.HELP_CHIP_COL_CELL),
            app.getText(AppText.CHIP_STAT_DMG_LONG),
            app.getText(AppText.CHIP_STAT_BRK_LONG),
            app.getText(AppText.CHIP_STAT_HIT_LONG),
            app.getText(AppText.CHIP_STAT_RLD_LONG)
        )
        for (col in resonanceCols) {
            resonanceTM.addColumn(col)
        }
        resonanceTable!!.tableHeader.setUI(null)
        resonanceTM.addRow(resonanceCols)
        val resonanceRowCount: Int = Board.MAP_STAT_RESONANCE.size(Board.NAMES[0])
        resonanceTablePanel!!.preferredSize = Dimension(200, resonanceTable!!.rowHeight * (resonanceRowCount + 1) + GAP)
        percTable!!.model = percTM
        percTable!!.setDefaultRenderer(String::class.java, HelpTableCellRenderer(1, 1))
        val percCols = arrayOf(
            app.getText(AppText.HELP_CHIP_COL_CELL),
            app.getText(AppText.UNIT_STAR_SHORT, "5"),
            app.getText(AppText.UNIT_STAR_SHORT, "4"),
            app.getText(AppText.UNIT_STAR_SHORT, "3"),
            app.getText(AppText.UNIT_STAR_SHORT, "2")
        )
        for (col in percCols) {
            percTM.addColumn(col)
        }
        percTable!!.tableHeader.setUI(null)
        percTM.addRow(percCols)
        for (row in Shape.Type.values()) {
            if (row == Shape.Type.NONE) {
                continue
            }
            percTM.addRow(
                arrayOf<Any>(
                    row.toString(),
                    Fn.iPercStr(Chip.getTypeMult(row, 5).double),
                    Fn.iPercStr(Chip.getTypeMult(row, 4).double),
                    Fn.iPercStr(Chip.getTypeMult(row, 3).double),
                    Fn.iPercStr(Chip.getTypeMult(row, 2).double)
                )
            )
        }
        percTablePanel!!.preferredSize = Dimension(200, percTable!!.rowHeight * percTable!!.rowCount + GAP)
        multTable!!.model = multTM
        multTable!!.setDefaultRenderer(String::class.java, HelpTableCellRenderer(0, 1))
        val multCols = arrayOf(
            app.getText(AppText.CHIP_STAT_DMG_LONG),
            app.getText(AppText.CHIP_STAT_BRK_LONG),
            app.getText(AppText.CHIP_STAT_HIT_LONG),
            app.getText(AppText.CHIP_STAT_RLD_LONG)
        )
        for (col in multCols) {
            multTM.addColumn(col)
        }
        multTable!!.tableHeader.setUI(null)
        multTM.addRow(multCols)
        multTM.addRow(
            arrayOf<Any>(
                Chip.RATE_DMG.double,
                Chip.RATE_BRK.double,
                Chip.RATE_HIT.double,
                Chip.RATE_RLD.double
            )
        )
        multTablePanel!!.preferredSize = Dimension(200, multTable!!.rowHeight * multTable!!.rowCount + GAP)
        setResonanceType(SECTION)
        lossLabel!!.text = app.getText(AppText.HELP_CHIP_CALC_DESC)
        lossTable!!.model = lossTM
        lossTable!!.setDefaultRenderer(String::class.java, HelpLevelStatTableCellRenderer(app, 1, 2, toggleType))
        val header1 = arrayOfNulls<String>(Chip.LEVEL_MAX + 2)
        header1[0] = app.getText(AppText.HELP_CHIP_COL_LEVEL)
        for (level in 0..Chip.LEVEL_MAX) {
            header1[level + 1] = level.toString()
        }
        val header2 = arrayOfNulls<String>(Chip.LEVEL_MAX + 2)
        header2[0] = "<html>&times;</html>"
        for (level in 0..Chip.LEVEL_MAX) {
            header2[level + 1] = Chip.getLevelMult(level).double.toString()
        }
        for (header in header1) {
            lossTM.addColumn(header)
        }
        lossTable!!.tableHeader.setUI(null)
        lossTM.addRow(header1)
        lossTM.addRow(header2)
        lossComboBox!!.addItem(app.getText(AppText.CHIP_STAT_DMG_LONG))
        lossComboBox!!.addItem(app.getText(AppText.CHIP_STAT_BRK_LONG))
        lossComboBox!!.addItem(app.getText(AppText.CHIP_STAT_HIT_LONG))
        lossComboBox!!.addItem(app.getText(AppText.CHIP_STAT_RLD_LONG))
        setLossType(LOSS)
        statScrollPane!!.preferredSize =
            Dimension(statScrollPane!!.preferredSize.width, lossTable!!.rowHeight * 7 + GAP)
        updateStatTable()
    }

    private fun addListeners() {
        resonanceBoardComboBox!!.addActionListener { e: ActionEvent? -> updateResonance() }
        lossComboBox!!.addActionListener { e: ActionEvent? -> updateStatTable() }
        Fn.addEscDisposeListener(this)
    }

    private fun rotateChips() {
        run {
            val elements = alm.elements()
            while (elements.hasMoreElements()) {
                val c = elements.nextElement()
                c.initRotate(Chip.CLOCKWISE)
            }
        }
        fiveAList!!.repaint()
        val elements = blm.elements()
        while (elements.hasMoreElements()) {
            val c = elements.nextElement()
            c.initRotate(Chip.CLOCKWISE)
        }
        fiveBList!!.repaint()
    }

    private fun setResonanceType(type: Boolean) {
        resonanceType = type
        if (type == SECTION) {
            resonanceButton!!.text = app.getText(AppText.HELP_CHIP_COLOR_SECTION)
        } else {
            resonanceButton!!.text = app.getText(AppText.HELP_CHIP_COLOR_CUMULATIVE)
        }
        updateResonance()
    }

    private fun setLossType(type: Boolean) {
        toggleType.v = type
        if (toggleType.v == STAT) {
            lossButton!!.text = app.getText(AppText.HELP_CHIP_CALC_STAT)
        } else {
            lossButton!!.text = app.getText(AppText.HELP_CHIP_CALC_LOSS)
        }
        updateStatTable()
    }

    private fun updateResonance() {
        val name = resonanceBoardComboBox!!.getItemAt(resonanceBoardComboBox!!.selectedIndex)
        val color: Int = Board.getColor(name)
        boardLabel!!.text =
            app.getText(AppText.CHIP_COLOR) + ": " + app.getText(AppText.TEXT_MAP_COLOR[color]!!)
        val steps = Board.MAP_STAT_RESONANCE.keySet(name)
        val resonanceSteps = ArrayList(steps)
        resonanceSteps.sortWith(Collections.reverseOrder())
        resonanceTM.rowCount = 1
        if (resonanceType == SECTION) {
            resonanceSteps.forEach(Consumer { step: Int ->
                val stat: Stat = Board.MAP_STAT_RESONANCE[name, step]!!
                resonanceTM.addRow(arrayOf(step, stat.dmg, stat.brk, stat.hit, stat.rld))
            })
        } else {
            for (step in resonanceSteps) {
                val stats: MutableList<Stat?> = mutableListOf()
                if (steps != null) {
                    for (key in steps) {
                        if (key <= step) {
                            stats.add(Board.MAP_STAT_RESONANCE[name, key])
                        }
                    }
                }
                val s = Stat(stats)
                resonanceTM.addRow(arrayOf(step, s.dmg, s.brk, s.hit, s.rld))
            }
        }
    }

    private fun updateStatTable() {
        lossTM.rowCount = 2
        for (pt in 1..5) {
            val rows = arrayOfNulls<String>(Chip.LEVEL_MAX + 2)
            rows[0] = pt.toString() + "pt"
            for (level in 0..Chip.LEVEL_MAX) {
                if (toggleType.v == STAT) {
                    rows[level + 1] = Chip.getMaxEffStat(rate, pt, level).toString()
                } else {
                    val one: Int = Chip.getMaxEffStat(rate, 1, level)
                    val current: Int = Chip.getMaxEffStat(rate, pt, level)
                    val value = current - one * pt
                    rows[level + 1] = value.toString()
                }
            }
            lossTM.addRow(rows)
        }
    }

    private val rate: Rational
        private get() = when (lossComboBox!!.selectedIndex) {
            0 -> Chip.RATE_DMG
            1 -> Chip.RATE_BRK
            2 -> Chip.RATE_HIT
            3 -> Chip.RATE_RLD
            else -> throw AssertionError()
        }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private fun initComponents() {
        efficiencyPanel = JPanel()
        percTablePanel = JPanel()
        jScrollPane3 = JScrollPane()
        percTable = JTable()
        calcPanel = JPanel()
        lossLabel = JLabel()
        lossButton = JButton()
        lossComboBox = JComboBox()
        statScrollPane = JScrollPane()
        lossTable = JTable()
        bonusPanel = JPanel()
        resonanceBoardComboBox = JComboBox()
        boardLabel = JLabel()
        resonanceTablePanel = JPanel()
        jScrollPane4 = JScrollPane()
        resonanceTable = JTable()
        resonanceButton = JButton()
        multiplierPanel = JPanel()
        multTablePanel = JPanel()
        jScrollPane5 = JScrollPane()
        multTable = JTable()
        chipPanel = JPanel()
        fiveAPanel = JPanel()
        fiveAScrollPane = JScrollPane()
        fiveAList = JList()
        fiveBPanel = JPanel()
        fiveBScrollPane = JScrollPane()
        fiveBList = JList()
        closeButton = JButton()
        aTabbedPane = JTabbedPane()
        jScrollPane3!!.preferredSize = Dimension(100, 100)
        percTable!!.isFocusable = false
        percTable!!.rowSelectionAllowed = false
        jScrollPane3!!.setViewportView(percTable)
        val percTablePanelLayout = GroupLayout(percTablePanel)
        percTablePanel!!.layout = percTablePanelLayout
        percTablePanelLayout.setHorizontalGroup(
            percTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    jScrollPane3,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        percTablePanelLayout.setVerticalGroup(
            percTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        val efficiencyPanelLayout = GroupLayout(efficiencyPanel)
        efficiencyPanel!!.layout = efficiencyPanelLayout
        efficiencyPanelLayout.setHorizontalGroup(
            efficiencyPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    percTablePanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        efficiencyPanelLayout.setVerticalGroup(
            efficiencyPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    percTablePanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        lossLabel!!.text = "<html><b>손실 계산</b>: (P포인트일 때의 스텟) - P &times; (1포인트일 때의 스텟)</html>"
        lossButton!!.text = "스텟"
        lossButton!!.addActionListener { evt: ActionEvent -> lossButtonActionPerformed(evt) }
        statScrollPane!!.preferredSize = Dimension(100, 100)
        lossTable!!.isFocusable = false
        lossTable!!.rowSelectionAllowed = false
        statScrollPane!!.setViewportView(lossTable)
        val calcPanelLayout = GroupLayout(calcPanel)
        calcPanel!!.layout = calcPanelLayout
        calcPanelLayout.setHorizontalGroup(
            calcPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, calcPanelLayout.createSequentialGroup()
                        .addComponent(lossLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lossButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            lossComboBox,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
                .addComponent(
                    statScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        calcPanelLayout.setVerticalGroup(
            calcPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    calcPanelLayout.createSequentialGroup()
                        .addGroup(
                            calcPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(
                                    lossComboBox,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(lossButton)
                                .addComponent(
                                    lossLabel,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            statScrollPane,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        resonanceBoardComboBox!!.preferredSize = Dimension(100, 21)
        boardLabel!!.text = "jLabel1"
        jScrollPane4!!.preferredSize = Dimension(100, 100)
        resonanceTable!!.isFocusable = false
        resonanceTable!!.rowSelectionAllowed = false
        jScrollPane4!!.setViewportView(resonanceTable)
        val resonanceTablePanelLayout = GroupLayout(resonanceTablePanel)
        resonanceTablePanel!!.layout = resonanceTablePanelLayout
        resonanceTablePanelLayout.setHorizontalGroup(
            resonanceTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        resonanceTablePanelLayout.setVerticalGroup(
            resonanceTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        resonanceButton!!.text = "구간"
        resonanceButton!!.addActionListener { evt: ActionEvent -> resonanceButtonActionPerformed(evt) }
        val bonusPanelLayout = GroupLayout(bonusPanel)
        bonusPanel!!.layout = bonusPanelLayout
        bonusPanelLayout.setHorizontalGroup(
            bonusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    resonanceTablePanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(resonanceBoardComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addGroup(
                    bonusPanelLayout.createSequentialGroup()
                        .addComponent(
                            boardLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resonanceButton)
                )
        )
        bonusPanelLayout.setVerticalGroup(
            bonusPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    bonusPanelLayout.createSequentialGroup()
                        .addComponent(
                            resonanceBoardComboBox,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            bonusPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(resonanceButton)
                                .addComponent(boardLabel)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            resonanceTablePanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        jScrollPane5!!.preferredSize = Dimension(100, 100)
        multTable!!.isFocusable = false
        multTable!!.rowSelectionAllowed = false
        jScrollPane5!!.setViewportView(multTable)
        val multTablePanelLayout = GroupLayout(multTablePanel)
        multTablePanel!!.layout = multTablePanelLayout
        multTablePanelLayout.setHorizontalGroup(
            multTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane5, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        multTablePanelLayout.setVerticalGroup(
            multTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane5, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        val multiplierPanelLayout = GroupLayout(multiplierPanel)
        multiplierPanel!!.layout = multiplierPanelLayout
        multiplierPanelLayout.setHorizontalGroup(
            multiplierPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    multTablePanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        multiplierPanelLayout.setVerticalGroup(
            multiplierPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    multTablePanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        fiveAPanel!!.border = BorderFactory.createTitledBorder("5칸 A형 칩")
        fiveAScrollPane!!.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        fiveAScrollPane!!.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        fiveAScrollPane!!.preferredSize = Dimension(130, 130)
        fiveAList!!.selectionMode = ListSelectionModel.SINGLE_SELECTION
        fiveAList!!.layoutOrientation = JList.HORIZONTAL_WRAP
        fiveAList!!.visibleRowCount = -1
        fiveAScrollPane!!.setViewportView(fiveAList)
        val fiveAPanelLayout = GroupLayout(fiveAPanel)
        fiveAPanel!!.layout = fiveAPanelLayout
        fiveAPanelLayout.setHorizontalGroup(
            fiveAPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    fiveAScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        fiveAPanelLayout.setVerticalGroup(
            fiveAPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    fiveAScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        fiveBPanel!!.border = BorderFactory.createTitledBorder("5칸 B형 칩")
        fiveBScrollPane!!.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        fiveBScrollPane!!.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        fiveBScrollPane!!.preferredSize = Dimension(130, 130)
        fiveBList!!.selectionMode = ListSelectionModel.SINGLE_SELECTION
        fiveBList!!.layoutOrientation = JList.HORIZONTAL_WRAP
        fiveBList!!.visibleRowCount = -1
        fiveBScrollPane!!.setViewportView(fiveBList)
        val fiveBPanelLayout = GroupLayout(fiveBPanel)
        fiveBPanel!!.layout = fiveBPanelLayout
        fiveBPanelLayout.setHorizontalGroup(
            fiveBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    fiveBScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        fiveBPanelLayout.setVerticalGroup(
            fiveBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    fiveBScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        val chipPanelLayout = GroupLayout(chipPanel)
        chipPanel!!.layout = chipPanelLayout
        chipPanelLayout.setHorizontalGroup(
            chipPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(fiveAPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(fiveBPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        chipPanelLayout.setVerticalGroup(
            chipPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, chipPanelLayout.createSequentialGroup()
                        .addComponent(
                            fiveAPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            fiveBPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        defaultCloseOperation = DISPOSE_ON_CLOSE
        title = "칩셋 가이드"
        modalityType = ModalityType.APPLICATION_MODAL
        type = Type.UTILITY
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(evt: WindowEvent) {
                formWindowClosing(evt)
            }
        })
        closeButton!!.text = "닫기"
        closeButton!!.addActionListener { evt: ActionEvent -> closeButtonActionPerformed(evt) }
        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(aTabbedPane)
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                                        .addComponent(closeButton)
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
                        .addComponent(aTabbedPane)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeButton)
                        .addContainerGap()
                )
        )
        pack()
    } // </editor-fold>//GEN-END:initComponents

    private fun closeButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_closeButtonActionPerformed
        dispose()
    } //GEN-LAST:event_closeButtonActionPerformed

    private fun resonanceButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_resonanceButtonActionPerformed
        setResonanceType(!resonanceType)
    } //GEN-LAST:event_resonanceButtonActionPerformed

    private fun formWindowClosing(evt: WindowEvent) { //GEN-FIRST:event_formWindowClosing
        aTimer.stop()
        dispose()
    } //GEN-LAST:event_formWindowClosing

    private fun lossButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_lossButtonActionPerformed
        setLossType(!toggleType.v)
    } //GEN-LAST:event_lossButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var aTabbedPane: JTabbedPane? = null
    private var boardLabel: JLabel? = null
    private var bonusPanel: JPanel? = null
    private var calcPanel: JPanel? = null
    private var chipPanel: JPanel? = null
    private var closeButton: JButton? = null
    private var efficiencyPanel: JPanel? = null
    private var fiveAList: JList<Chip>? = null
    private var fiveAPanel: JPanel? = null
    private var fiveAScrollPane: JScrollPane? = null
    private var fiveBList: JList<Chip>? = null
    private var fiveBPanel: JPanel? = null
    private var fiveBScrollPane: JScrollPane? = null
    private var jScrollPane3: JScrollPane? = null
    private var jScrollPane4: JScrollPane? = null
    private var jScrollPane5: JScrollPane? = null
    private var lossButton: JButton? = null
    private var lossComboBox: JComboBox<String?>? = null
    private var lossLabel: JLabel? = null
    private var lossTable: JTable? = null
    private var multTable: JTable? = null
    private var multTablePanel: JPanel? = null
    private var multiplierPanel: JPanel? = null
    private var percTable: JTable? = null
    private var percTablePanel: JPanel? = null
    private var resonanceBoardComboBox: JComboBox<String>? = null
    private var resonanceButton: JButton? = null
    private var resonanceTable: JTable? = null
    private var resonanceTablePanel: JPanel? = null
    private var statScrollPane: JScrollPane? = null // End of variables declaration//GEN-END:variables

    companion object {
        private const val CHIP_SIZE_FACTOR = 0.7
        private const val GAP = 5
        const val STAT = false
        const val LOSS = true
        private const val SECTION = false
        private fun genTextPanel(text: String?, pageEndPanel: JPanel?): JPanel {
            return genTextPanel(text, pageEndPanel, null)
        }

        private fun genTextPanel(text: String?, pageEndPanel: JPanel?, lineEndPanel: JPanel?): JPanel {
            val panel = JPanel()
            panel.layout = BorderLayout()
            val label = JLabel(text)
            label.verticalAlignment = JLabel.TOP
            panel.add(label, BorderLayout.CENTER)
            if (pageEndPanel != null) {
                panel.add(pageEndPanel, BorderLayout.PAGE_END)
            }
            if (lineEndPanel != null) {
                panel.add(lineEndPanel, BorderLayout.LINE_END)
            }
            panel.border = EmptyBorder(
                GAP,
                GAP,
                GAP,
                GAP
            )
            return panel
        }
    }

    init {
        initComponents()
        init()
    }
}