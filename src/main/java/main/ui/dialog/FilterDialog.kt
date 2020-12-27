package main.ui.dialog

import main.App
import main.puzzle.Board
import main.puzzle.Chip
import main.puzzle.Stat
import main.puzzle.Tag
import main.setting.BoardSetting
import main.setting.Filter
import main.setting.StatPresetMap
import main.ui.component.TagPanel
import main.ui.resource.AppImage
import main.ui.resource.AppText
import main.util.Fn
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.TableModelEvent

/**
 *
 * @author Bunnyspa
 */
class FilterDialog private constructor(app: App) : JDialog() {
    private val app: App
    private val tip: TagPanel
    private val txp: TagPanel
    private val starTBs: Array<JToggleButton?>
    private val colorTBs: Array<JToggleButton?>
    private val typeTBs: Array<JToggleButton?>
    private val markedTBs: Array<JToggleButton?>
    private val ptMinSpinners: Array<JSpinner?>
    private val ptMaxSpinners: Array<JSpinner?>
    private fun init() {
        title = app.getText(AppText.FILTER_TITLE)
        okButton!!.text = app.getText(AppText.ACTION_OK)
        cancelButton!!.text = app.getText(AppText.ACTION_CANCEL)
        presetButton!!.text = app.getText(AppText.FILTER_PRESET)
        resetButton!!.text = app.getText(AppText.FILTER_RESET)
        tagResetButton!!.text = app.getText(AppText.FILTER_TAG_RESET)
        markedTrueTB!!.icon = AppImage.CHECKED
        markedFalseTB!!.icon = AppImage.UNCHECKED
        dmgTextLabel!!.icon = AppImage.DMG
        brkTextLabel!!.icon = AppImage.BRK
        hitTextLabel!!.icon = AppImage.HIT
        rldTextLabel!!.icon = AppImage.RLD
        starPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_STAR))
        for (i in 0 until Filter.NUM_STAR) {
            val starStr = (5 - i).toString()
            starTBs[i]!!.text = app.getText(AppText.UNIT_STAR, starStr)
            starTBs[i]!!.isSelected = app.filter.getStar(i)
        }
        colorPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_COLOR))
        colorOrangeTB!!.text = app.getText(AppText.CHIP_COLOR_ORANGE)
        colorBlueTB!!.text = app.getText(AppText.CHIP_COLOR_BLUE)
        for (i in 0 until Filter.NUM_COLOR) {
            colorTBs[i]!!.isSelected = app.filter.getColor(i)
        }
        cellPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_CELL))
        val typeStrs = arrayOf(
            app.getText(AppText.UNIT_CELL, "6"),
            app.getText(AppText.UNIT_CELLTYPE, "5", "B"),
            app.getText(AppText.UNIT_CELLTYPE, "5", "A"),
            app.getText(AppText.UNIT_CELL, "4"),
            app.getText(AppText.UNIT_CELL, "3"),
            app.getText(AppText.UNIT_CELL, "2"),
            app.getText(AppText.UNIT_CELL, "1")
        )
        for (i in 0 until Filter.NUM_TYPE) {
            typeTBs[i]!!.text = typeStrs[i]
            typeTBs[i]!!.isSelected = app.filter.getType(i)
        }
        markPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_MARK))
        for (i in 0 until Filter.NUM_MARK) {
            markedTBs[i]!!.isSelected = app.filter.getMark(i)
        }
        ptPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_PT))
        dmgTextLabel!!.text = app.getText(AppText.CHIP_STAT_DMG)
        brkTextLabel!!.text = app.getText(AppText.CHIP_STAT_BRK)
        hitTextLabel!!.text = app.getText(AppText.CHIP_STAT_HIT)
        rldTextLabel!!.text = app.getText(AppText.CHIP_STAT_RLD)
        ptMinDmgSpinner!!.model = SpinnerNumberModel(0, 0, Chip.PT_MAX, 1)
        ptMinBrkSpinner!!.model = SpinnerNumberModel(0, 0, Chip.PT_MAX, 1)
        ptMinHitSpinner!!.model = SpinnerNumberModel(0, 0, Chip.PT_MAX, 1)
        ptMinRldSpinner!!.model = SpinnerNumberModel(0, 0, Chip.PT_MAX, 1)
        val ptMin = app.filter.ptMin
        ptMinDmgSpinner!!.value = ptMin.dmg
        ptMinBrkSpinner!!.value = ptMin.brk
        ptMinHitSpinner!!.value = ptMin.hit
        ptMinRldSpinner!!.value = ptMin.rld
        ptMaxDmgSpinner!!.model =
            SpinnerNumberModel(Chip.PT_MAX, 0, Chip.PT_MAX, 1)
        ptMaxBrkSpinner!!.model =
            SpinnerNumberModel(Chip.PT_MAX, 0, Chip.PT_MAX, 1)
        ptMaxHitSpinner!!.model =
            SpinnerNumberModel(Chip.PT_MAX, 0, Chip.PT_MAX, 1)
        ptMaxRldSpinner!!.model =
            SpinnerNumberModel(Chip.PT_MAX, 0, Chip.PT_MAX, 1)
        val ptMax = app.filter.ptMax
        ptMaxDmgSpinner!!.value = ptMax.dmg
        ptMaxBrkSpinner!!.value = ptMax.brk
        ptMaxHitSpinner!!.value = ptMax.hit
        ptMaxRldSpinner!!.value = ptMax.rld
        enhancementPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_ENHANCEMENT))
        levelMinSpinner!!.model = SpinnerNumberModel(0, 0, Chip.LEVEL_MAX, 1)
        levelMaxSpinner!!.model = SpinnerNumberModel(0, 0, Chip.LEVEL_MAX, 1)
        levelMinSpinner!!.value = app.filter.levelMin
        levelMaxSpinner!!.value = app.filter.levelMax
        tagIncludedPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_TAG_INCLUDE))
        tagExcludedPanel!!.border = TitledBorder(app.getText(AppText.FILTER_GROUP_TAG_EXCLUDE))
        tagIncludedPanel!!.add(tip)
        tagExcludedPanel!!.add(txp)
        val name = app.mf.boardName
        val star = app.mf.boardStar
        if (star != 5 || app.setting.board.getStatMode(name, star) != BoardSetting.MAX_PRESET) {
            presetButton!!.isVisible = false
        }
        addListeners()
        pack()
    }

    private fun addListeners() {
        levelMinSpinner!!.addChangeListener { e: ChangeEvent? ->
            if (levelMinSpinnerValue > levelMaxSpinnerValue) {
                levelMaxSpinner!!.value = levelMinSpinnerValue
            }
        }
        levelMaxSpinner!!.addChangeListener { e: ChangeEvent? ->
            if (levelMinSpinnerValue > levelMaxSpinnerValue) {
                levelMinSpinner!!.value = levelMaxSpinnerValue
            }
        }
        for (ptMinSpinner in ptMinSpinners) {
            ptMinSpinner!!.addChangeListener { e: ChangeEvent? -> fixPtMaxSpinners() }
        }
        for (ptMaxSpinner in ptMaxSpinners) {
            ptMaxSpinner!!.addChangeListener { e: ChangeEvent? -> fixPtMinSpinners() }
        }
        tip.addTableModelListener { e: TableModelEvent? ->
            for (i in 0 until tip.count) {
                if (tip.isChecked(i) && txp.isChecked(i)) {
                    txp.setChecked(i, false)
                }
            }
        }
        txp.addTableModelListener { e: TableModelEvent? ->
            for (i in 0 until tip.count) {
                if (tip.isChecked(i) && txp.isChecked(i)) {
                    tip.setChecked(i, false)
                }
            }
        }
        Fn.addEscDisposeListener(this)
    }

    private fun fixPtMinSpinners() {
        for (i in ptMinSpinners.indices) {
            val ptMinSpinner = ptMinSpinners[i]
            val ptMaxSpinner = ptMaxSpinners[i]
            if (ptMinSpinner!!.value as Int > ptMaxSpinner!!.value as Int) {
                ptMinSpinner.value = ptMaxSpinner.value
            }
        }
    }

    private fun fixPtMaxSpinners() {
        for (i in ptMaxSpinners.indices) {
            val ptMinSpinner = ptMinSpinners[i]
            val ptMaxSpinner = ptMaxSpinners[i]
            if (ptMinSpinner!!.value as Int > ptMaxSpinner!!.value as Int) {
                ptMaxSpinner.value = ptMinSpinner.value
            }
        }
    }

    private val levelMinSpinnerValue: Int
        private get() = levelMinSpinner!!.value as Int
    private val levelMaxSpinnerValue: Int
        private get() = levelMaxSpinner!!.value as Int

    private fun applyPreset() {
        val name = app.mf.boardName
        val star = app.mf.boardStar
        val presetIndex = app.setting.board.getPresetIndex(name, star)
        val presetMap: StatPresetMap = StatPresetMap.PRESET
        for (starTB in starTBs) {
            starTB!!.isSelected = starTB === star5TB
        }
        colorOrangeTB!!.isSelected = Board.getColor(name) == Chip.COLOR_ORANGE
        colorBlueTB!!.isSelected = Board.getColor(name) == Chip.COLOR_BLUE
        val typeArray = presetMap.getTypeFilter(name, 5, presetIndex)
        for (i in typeTBs.indices) {
            typeTBs[i]!!.isSelected = typeArray[i]
        }
        val minPtArray = presetMap[name, 5, presetIndex]!!.ptMin.toArray()
        for (i in ptMinSpinners.indices) {
            ptMinSpinners[i]!!.value = minPtArray[i]
        }
        val maxPtArray = presetMap[name, 5, presetIndex]!!.ptMax.toArray()
        for (i in ptMaxSpinners.indices) {
            ptMaxSpinners[i]!!.value = maxPtArray[i]
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private fun initComponents() {
        starPanel = JPanel()
        star5TB = JToggleButton()
        star4TB = JToggleButton()
        star3TB = JToggleButton()
        star2TB = JToggleButton()
        colorPanel = JPanel()
        colorOrangeTB = JToggleButton()
        colorBlueTB = JToggleButton()
        cellPanel = JPanel()
        size6TB = JToggleButton()
        size5BTB = JToggleButton()
        size5ATB = JToggleButton()
        size4TB = JToggleButton()
        size3TB = JToggleButton()
        size2TB = JToggleButton()
        size1TB = JToggleButton()
        markPanel = JPanel()
        markedTrueTB = JToggleButton()
        markedFalseTB = JToggleButton()
        okButton = JButton()
        cancelButton = JButton()
        resetButton = JButton()
        enhancementPanel = JPanel()
        levelMinSpinner = JSpinner()
        jLabel1 = JLabel()
        levelMaxSpinner = JSpinner()
        ptPanel = JPanel()
        dmgTextLabel = JLabel()
        brkTextLabel = JLabel()
        hitTextLabel = JLabel()
        rldTextLabel = JLabel()
        ptMaxDmgSpinner = JSpinner()
        ptMaxBrkSpinner = JSpinner()
        ptMaxHitSpinner = JSpinner()
        ptMaxRldSpinner = JSpinner()
        ptMinDmgSpinner = JSpinner()
        ptMinBrkSpinner = JSpinner()
        ptMinHitSpinner = JSpinner()
        ptMinRldSpinner = JSpinner()
        jLabel2 = JLabel()
        jLabel3 = JLabel()
        jLabel5 = JLabel()
        jLabel7 = JLabel()
        jPanel6 = JPanel()
        tagIncludedPanel = JPanel()
        tagExcludedPanel = JPanel()
        tagResetButton = JButton()
        presetButton = JButton()
        defaultCloseOperation = DISPOSE_ON_CLOSE
        title = "인벤토리 필터"
        modalityType = ModalityType.APPLICATION_MODAL
        isResizable = false
        type = Type.UTILITY
        starPanel!!.border = BorderFactory.createTitledBorder("레어도")
        star5TB!!.text = "5성"
        star5TB!!.isFocusPainted = false
        star5TB!!.isFocusable = false
        star5TB!!.isRolloverEnabled = false
        star4TB!!.text = "4성"
        star4TB!!.isFocusPainted = false
        star4TB!!.isFocusable = false
        star4TB!!.isRolloverEnabled = false
        star3TB!!.text = "3성"
        star3TB!!.isFocusPainted = false
        star3TB!!.isFocusable = false
        star3TB!!.isRolloverEnabled = false
        star2TB!!.text = "2성"
        star2TB!!.isFocusPainted = false
        star2TB!!.isFocusable = false
        star2TB!!.isRolloverEnabled = false
        val starPanelLayout = GroupLayout(starPanel)
        starPanel!!.layout = starPanelLayout
        starPanelLayout.setHorizontalGroup(
            starPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    starPanelLayout.createSequentialGroup()
                        .addComponent(star5TB)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(star4TB)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(star3TB)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(star2TB)
                )
        )
        starPanelLayout.linkSize(SwingConstants.HORIZONTAL, star2TB, star3TB, star4TB, star5TB)
        starPanelLayout.setVerticalGroup(
            starPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    starPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(star5TB)
                        .addComponent(star4TB)
                        .addComponent(star3TB)
                        .addComponent(star2TB)
                )
        )
        starPanelLayout.linkSize(SwingConstants.VERTICAL, star2TB, star3TB, star4TB, star5TB)
        colorPanel!!.border = BorderFactory.createTitledBorder("color")
        colorOrangeTB!!.text = "주황"
        colorOrangeTB!!.isFocusPainted = false
        colorOrangeTB!!.isFocusable = false
        colorOrangeTB!!.isRolloverEnabled = false
        colorBlueTB!!.text = "파랑"
        colorBlueTB!!.isFocusPainted = false
        colorBlueTB!!.isFocusable = false
        colorBlueTB!!.isRolloverEnabled = false
        val colorPanelLayout = GroupLayout(colorPanel)
        colorPanel!!.layout = colorPanelLayout
        colorPanelLayout.setHorizontalGroup(
            colorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    colorPanelLayout.createSequentialGroup()
                        .addComponent(colorOrangeTB)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(colorBlueTB)
                )
        )
        colorPanelLayout.linkSize(SwingConstants.HORIZONTAL, colorBlueTB, colorOrangeTB)
        colorPanelLayout.setVerticalGroup(
            colorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    colorPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(colorOrangeTB)
                        .addComponent(colorBlueTB)
                )
        )
        colorPanelLayout.linkSize(SwingConstants.VERTICAL, colorBlueTB, colorOrangeTB)
        cellPanel!!.border = BorderFactory.createTitledBorder("칸 수")
        size6TB!!.text = "6칸"
        size6TB!!.isFocusPainted = false
        size6TB!!.isFocusable = false
        size6TB!!.isRolloverEnabled = false
        size5BTB!!.text = "5칸 B형"
        size5BTB!!.isFocusPainted = false
        size5BTB!!.isFocusable = false
        size5BTB!!.isRolloverEnabled = false
        size5ATB!!.text = "5칸 A형"
        size5ATB!!.isFocusPainted = false
        size5ATB!!.isFocusable = false
        size5ATB!!.isRolloverEnabled = false
        size4TB!!.text = "4칸"
        size4TB!!.isFocusPainted = false
        size4TB!!.isFocusable = false
        size4TB!!.isRolloverEnabled = false
        size3TB!!.text = "3칸"
        size3TB!!.isFocusPainted = false
        size3TB!!.isFocusable = false
        size3TB!!.isRolloverEnabled = false
        size2TB!!.text = "2칸"
        size2TB!!.isFocusPainted = false
        size2TB!!.isFocusable = false
        size2TB!!.isRolloverEnabled = false
        size1TB!!.text = "1칸"
        size1TB!!.isFocusPainted = false
        size1TB!!.isFocusable = false
        size1TB!!.isRolloverEnabled = false
        val cellPanelLayout = GroupLayout(cellPanel)
        cellPanel!!.layout = cellPanelLayout
        cellPanelLayout.setHorizontalGroup(
            cellPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    cellPanelLayout.createSequentialGroup()
                        .addGroup(
                            cellPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                .addComponent(
                                    size6TB,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    size4TB,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            cellPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    cellPanelLayout.createSequentialGroup()
                                        .addComponent(size3TB)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(size2TB)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(size1TB)
                                )
                                .addGroup(
                                    cellPanelLayout.createSequentialGroup()
                                        .addComponent(
                                            size5BTB,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            size5ATB,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                )
                        )
                )
        )
        cellPanelLayout.linkSize(SwingConstants.HORIZONTAL, size1TB, size2TB, size3TB, size4TB, size6TB)
        cellPanelLayout.setVerticalGroup(
            cellPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    cellPanelLayout.createSequentialGroup()
                        .addGroup(
                            cellPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(size6TB)
                                .addComponent(size5BTB)
                                .addComponent(size5ATB)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            cellPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(size4TB)
                                .addComponent(size3TB)
                                .addComponent(size2TB)
                                .addComponent(size1TB)
                        )
                )
        )
        cellPanelLayout.linkSize(
            SwingConstants.VERTICAL,
            size1TB,
            size2TB,
            size3TB,
            size4TB,
            size5ATB,
            size5BTB,
            size6TB
        )
        markPanel!!.border = BorderFactory.createTitledBorder("마킹 여부")
        markedTrueTB!!.isFocusPainted = false
        markedTrueTB!!.isFocusable = false
        markedTrueTB!!.margin = Insets(2, 2, 2, 2)
        markedTrueTB!!.isRolloverEnabled = false
        markedFalseTB!!.isFocusPainted = false
        markedFalseTB!!.isFocusable = false
        markedFalseTB!!.margin = Insets(2, 2, 2, 2)
        markedFalseTB!!.isRolloverEnabled = false
        val markPanelLayout = GroupLayout(markPanel)
        markPanel!!.layout = markPanelLayout
        markPanelLayout.setHorizontalGroup(
            markPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    markPanelLayout.createSequentialGroup()
                        .addComponent(
                            markedTrueTB,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            markedFalseTB,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        markPanelLayout.setVerticalGroup(
            markPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    markPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(
                            markedTrueTB,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addComponent(
                            markedFalseTB,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        okButton!!.text = "ok"
        okButton!!.addActionListener { evt: ActionEvent -> okButtonActionPerformed(evt) }
        cancelButton!!.text = "cancel"
        cancelButton!!.addActionListener { evt: ActionEvent -> cancelButtonActionPerformed(evt) }
        resetButton!!.text = "reset"
        resetButton!!.addActionListener { evt: ActionEvent -> resetButtonActionPerformed(evt) }
        enhancementPanel!!.border = BorderFactory.createTitledBorder("강화 레벨 범위")
        levelMinSpinner!!.model = SpinnerNumberModel(0, 0, 20, 1)
        levelMinSpinner!!.preferredSize = Dimension(50, 22)
        jLabel1!!.text = "-"
        levelMaxSpinner!!.model = SpinnerNumberModel(20, 0, 20, 1)
        levelMaxSpinner!!.preferredSize = Dimension(50, 22)
        val enhancementPanelLayout = GroupLayout(enhancementPanel)
        enhancementPanel!!.layout = enhancementPanelLayout
        enhancementPanelLayout.setHorizontalGroup(
            enhancementPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    enhancementPanelLayout.createSequentialGroup()
                        .addComponent(
                            levelMinSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            levelMaxSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        enhancementPanelLayout.setVerticalGroup(
            enhancementPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    enhancementPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(
                            levelMinSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addComponent(jLabel1)
                        .addComponent(
                            levelMaxSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        ptPanel!!.border = BorderFactory.createTitledBorder("포인트 범위")
        dmgTextLabel!!.text = "살상"
        dmgTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        brkTextLabel!!.text = "파쇄"
        brkTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        hitTextLabel!!.text = "정밀"
        hitTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        rldTextLabel!!.text = "장전"
        rldTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        ptMaxDmgSpinner!!.preferredSize = Dimension(50, 22)
        ptMaxBrkSpinner!!.preferredSize = Dimension(50, 22)
        ptMaxHitSpinner!!.preferredSize = Dimension(50, 22)
        ptMaxRldSpinner!!.preferredSize = Dimension(50, 22)
        ptMinDmgSpinner!!.preferredSize = Dimension(50, 22)
        ptMinBrkSpinner!!.preferredSize = Dimension(50, 22)
        ptMinHitSpinner!!.preferredSize = Dimension(50, 22)
        ptMinRldSpinner!!.preferredSize = Dimension(50, 22)
        jLabel2!!.text = "-"
        jLabel3!!.text = "-"
        jLabel5!!.text = "-"
        jLabel7!!.text = "-"
        val ptPanelLayout = GroupLayout(ptPanel)
        ptPanel!!.layout = ptPanelLayout
        ptPanelLayout.setHorizontalGroup(
            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    ptPanelLayout.createSequentialGroup()
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(dmgTextLabel, GroupLayout.Alignment.TRAILING)
                                .addComponent(brkTextLabel, GroupLayout.Alignment.TRAILING)
                                .addComponent(hitTextLabel, GroupLayout.Alignment.TRAILING)
                                .addComponent(rldTextLabel, GroupLayout.Alignment.TRAILING)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    ptMinDmgSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptMinBrkSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptMinHitSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptMinRldSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel5, GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel7, GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel3, GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel2, GroupLayout.Alignment.TRAILING)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    ptMaxDmgSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptMaxBrkSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptMaxHitSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptMaxRldSpinner,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        ptPanelLayout.setVerticalGroup(
            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    ptPanelLayout.createSequentialGroup()
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(dmgTextLabel)
                                .addComponent(jLabel2)
                                .addComponent(
                                    ptMaxDmgSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(
                                    ptMinDmgSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(brkTextLabel)
                                .addComponent(
                                    ptMinBrkSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(jLabel3)
                                .addComponent(
                                    ptMaxBrkSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(hitTextLabel)
                                .addComponent(
                                    ptMinHitSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(jLabel5)
                                .addComponent(
                                    ptMaxHitSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            ptPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(rldTextLabel)
                                .addComponent(
                                    ptMinRldSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(jLabel7)
                                .addComponent(
                                    ptMaxRldSpinner,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                )
        )
        tagIncludedPanel!!.border = BorderFactory.createTitledBorder("tag included")
        tagIncludedPanel!!.preferredSize = Dimension(150, 150)
        tagIncludedPanel!!.layout = BorderLayout()
        tagExcludedPanel!!.border = BorderFactory.createTitledBorder("tag excluded")
        tagExcludedPanel!!.preferredSize = Dimension(150, 150)
        tagExcludedPanel!!.layout = BorderLayout()
        tagResetButton!!.text = "tag reset"
        tagResetButton!!.addActionListener { evt: ActionEvent -> tagResetButtonActionPerformed(evt) }
        val jPanel6Layout = GroupLayout(jPanel6)
        jPanel6!!.layout = jPanel6Layout
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    tagResetButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addGroup(
                    GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addComponent(
                            tagIncludedPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            tagExcludedPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel6Layout.createSequentialGroup()
                        .addGroup(
                            jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    tagIncludedPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    tagExcludedPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tagResetButton)
                )
        )
        presetButton!!.text = "preset"
        presetButton!!.addActionListener { evt: ActionEvent -> presetButtonActionPerformed(evt) }
        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                .addComponent(
                                    cellPanel,
                                    GroupLayout.Alignment.LEADING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    starPanel,
                                    GroupLayout.Alignment.LEADING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ptPanel,
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
                                        .addGroup(
                                            layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                .addComponent(
                                                    enhancementPanel,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE.toInt()
                                                )
                                                .addComponent(
                                                    colorPanel,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE.toInt()
                                                )
                                                .addComponent(
                                                    markPanel,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE.toInt()
                                                )
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            jPanel6,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                )
                                .addGroup(
                                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                                        .addComponent(presetButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(resetButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(okButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cancelButton)
                                )
                        )
                        .addContainerGap()
                )
        )
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addComponent(
                            jPanel6,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(cancelButton)
                                .addComponent(okButton)
                                .addComponent(resetButton)
                                .addComponent(presetButton)
                        )
                        .addContainerGap()
                )
                .addGroup(
                    layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    starPanel,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(
                                    colorPanel,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(
                                    cellPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    markPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    enhancementPanel,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(
                                    ptPanel,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                        )
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                )
        )
        pack()
    } // </editor-fold>//GEN-END:initComponents

    private fun okButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_okButtonActionPerformed
        for (i in 0 until Filter.NUM_STAR) {
            app.filter.setStar(i, starTBs[i]!!.isSelected)
        }
        for (i in 0 until Filter.NUM_COLOR) {
            app.filter.setColor(i, colorTBs[i]!!.isSelected)
        }
        for (i in 0 until Filter.NUM_TYPE) {
            app.filter.setType(i, typeTBs[i]!!.isSelected)
        }
        for (i in 0 until Filter.NUM_MARK) {
            app.filter.setMark(i, markedTBs[i]!!.isSelected)
        }
        app.filter.levelMin = levelMinSpinnerValue
        app.filter.levelMax = levelMaxSpinnerValue
        app.filter.ptMin = Stat(
            ptMinDmgSpinner!!.value as Int,
            ptMinBrkSpinner!!.value as Int,
            ptMinHitSpinner!!.value as Int,
            ptMinRldSpinner!!.value as Int
        )
        app.filter.ptMax = Stat(
            ptMaxDmgSpinner!!.value as Int,
            ptMaxBrkSpinner!!.value as Int,
            ptMaxHitSpinner!!.value as Int,
            ptMaxRldSpinner!!.value as Int
        )
        app.filter.includedTags.clear()
        for (i in 0 until tip.count) {
            if (tip.isChecked(i)) {
                app.filter.includedTags.add(tip.getTag(i))
            }
        }
        app.filter.excludedTags.clear()
        for (i in 0 until tip.count) {
            if (txp.isChecked(i)) {
                app.filter.excludedTags.add(txp.getTag(i))
            }
        }
        app.mf.display_applyFilterSort()
        dispose()
    } //GEN-LAST:event_okButtonActionPerformed

    private fun cancelButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_cancelButtonActionPerformed
        dispose()
    } //GEN-LAST:event_cancelButtonActionPerformed

    private fun resetButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_resetButtonActionPerformed
        app.filter.reset()
        app.mf.display_applyFilterSort()
        dispose()
    } //GEN-LAST:event_resetButtonActionPerformed

    private fun tagResetButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_tagResetButtonActionPerformed
        for (i in 0 until tip.count) {
            tip.setChecked(i, false)
            txp.setChecked(i, false)
        }
    } //GEN-LAST:event_tagResetButtonActionPerformed

    private fun presetButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_presetButtonActionPerformed
        applyPreset()
    } //GEN-LAST:event_presetButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var brkTextLabel: JLabel? = null
    private var cancelButton: JButton? = null
    private var cellPanel: JPanel? = null
    private var colorBlueTB: JToggleButton? = null
    private var colorOrangeTB: JToggleButton? = null
    private var colorPanel: JPanel? = null
    private var dmgTextLabel: JLabel? = null
    private var enhancementPanel: JPanel? = null
    private var hitTextLabel: JLabel? = null
    private var jLabel1: JLabel? = null
    private var jLabel2: JLabel? = null
    private var jLabel3: JLabel? = null
    private var jLabel5: JLabel? = null
    private var jLabel7: JLabel? = null
    private var jPanel6: JPanel? = null
    private var levelMaxSpinner: JSpinner? = null
    private var levelMinSpinner: JSpinner? = null
    private var markPanel: JPanel? = null
    private var markedFalseTB: JToggleButton? = null
    private var markedTrueTB: JToggleButton? = null
    private var okButton: JButton? = null
    private var presetButton: JButton? = null
    private var ptMaxBrkSpinner: JSpinner? = null
    private var ptMaxDmgSpinner: JSpinner? = null
    private var ptMaxHitSpinner: JSpinner? = null
    private var ptMaxRldSpinner: JSpinner? = null
    private var ptMinBrkSpinner: JSpinner? = null
    private var ptMinDmgSpinner: JSpinner? = null
    private var ptMinHitSpinner: JSpinner? = null
    private var ptMinRldSpinner: JSpinner? = null
    private var ptPanel: JPanel? = null
    private var resetButton: JButton? = null
    private var rldTextLabel: JLabel? = null
    private var size1TB: JToggleButton? = null
    private var size2TB: JToggleButton? = null
    private var size3TB: JToggleButton? = null
    private var size4TB: JToggleButton? = null
    private var size5ATB: JToggleButton? = null
    private var size5BTB: JToggleButton? = null
    private var size6TB: JToggleButton? = null
    private var star2TB: JToggleButton? = null
    private var star3TB: JToggleButton? = null
    private var star4TB: JToggleButton? = null
    private var star5TB: JToggleButton? = null
    private var starPanel: JPanel? = null
    private var tagExcludedPanel: JPanel? = null
    private var tagIncludedPanel: JPanel? = null
    private var tagResetButton: JButton? = null // End of variables declaration//GEN-END:variables

    companion object {
        fun getInstance(app: App): FilterDialog {
            return FilterDialog(app)
        }
    }

    init {
        initComponents()
        this.app = app
        tip = TagPanel(
            app,
            this,
            { t1: Tag? -> app.filter.includedTags.stream().anyMatch { obj: Tag? -> t1!! == obj } },
            false
        )
        txp = TagPanel(
            app,
            this,
            { t1: Tag? -> app.filter.excludedTags.stream().anyMatch { obj: Tag? -> t1!! == obj } },
            false
        )
        starTBs = arrayOf(star5TB, star4TB, star3TB, star2TB)
        colorTBs = arrayOf(colorOrangeTB, colorBlueTB)
        typeTBs = arrayOf(size6TB, size5BTB, size5ATB, size4TB, size3TB, size2TB, size1TB)
        markedTBs = arrayOf(markedTrueTB, markedFalseTB)
        ptMinSpinners = arrayOf(ptMinDmgSpinner, ptMinBrkSpinner, ptMinHitSpinner, ptMinRldSpinner)
        ptMaxSpinners = arrayOf(ptMaxDmgSpinner, ptMaxBrkSpinner, ptMaxHitSpinner, ptMaxRldSpinner)
        init()
    }
}