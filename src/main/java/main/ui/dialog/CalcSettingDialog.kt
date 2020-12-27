package main.ui.dialog

import main.App
import main.puzzle.Board
import main.puzzle.Stat
import main.setting.BoardSetting
import main.setting.Setting
import main.setting.StatPresetMap
import main.ui.resource.AppImage
import main.ui.resource.AppText
import main.util.Fn
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.ChangeEvent

/**
 *
 * @author Bunnyspa
 */
class CalcSettingDialog private constructor(app: App) : JDialog() {
    private val app: App
    private val star: Int
    private var advancedSetting = false
    private var mode = 0
    private var presetIndex = 0
    private var stat: Stat? = null
    private var pt: Stat? = null
    private var radioLoading = false
    private var markType = 0
    private fun loadResources() {
        title = app.getText(AppText.CSET_TITLE)
        okButton!!.text = app.getText(AppText.ACTION_OK)
        cancelButton!!.text = app.getText(AppText.ACTION_CANCEL)
        dmgTextLabel!!.icon = AppImage.DMG
        brkTextLabel!!.icon = AppImage.BRK
        hitTextLabel!!.icon = AppImage.HIT
        rldTextLabel!!.icon = AppImage.RLD

        // Group
        statPanel!!.border = TitledBorder(app.getText(AppText.CSET_GROUP_STAT))
        markPanel!!.border = TitledBorder(app.getText(AppText.CSET_GROUP_MARK))
        sortPanel!!.border = TitledBorder(app.getText(AppText.CSET_GROUP_SORT))
        miscPanel!!.border = TitledBorder(app.getText(AppText.CSET_GROUP_MISC))

        // Stat
        maxNormalRadioButton!!.text = app.getText(AppText.CSET_DEFAULT_STAT)
        maxStatRadioButton!!.text = app.getText(AppText.CSET_STAT)
        maxPtRadioButton!!.text = app.getText(AppText.CSET_PT)
        maxPresetRadioButton!!.text = app.getText(AppText.CSET_PRESET)
        dmgTextLabel!!.text = app.getText(AppText.CHIP_STAT_DMG)
        brkTextLabel!!.text = app.getText(AppText.CHIP_STAT_BRK)
        hitTextLabel!!.text = app.getText(AppText.CHIP_STAT_HIT)
        rldTextLabel!!.text = app.getText(AppText.CHIP_STAT_RLD)

        // Mark
        markDescLabel!!.text = app.getText(AppText.CSET_MARK_DESC)

        // Sort
        sortTicketRadioButton!!.text = app.getText(AppText.CSET_SORT_TICKET)
        sortXPRadioButton!!.text = app.getText(AppText.CSET_SORT_XP)

        // Misc.
        maxLevelCheckBox!!.text = app.getText(AppText.CSET_MAXLEVEL_DESC)
        colorCheckBox!!.text = app.getText(AppText.CSET_COLOR_DESC)
        rotationCheckBox!!.text = app.getText(AppText.CSET_ROTATION_DESC)
        symmetryCheckBox!!.text = app.getText(AppText.CSET_SYMMETRY_DESC)
    }

    private fun loadSettings() {
        val setting = app.setting
        setAdvandedSetting(setting.advancedSetting)
        mode = setting.board.getStatMode(name, star)
        stat = setting.board.getStat(name, star)
        pt = setting.board.getPt(name, star)
        presetIndex = setting.board.getPresetIndex(name, star)
        if (star != 5 || !StatPresetMap.PRESET.containsKey(name, star)) {
            maxPresetRadioButton!!.isEnabled = false
            statPresetComboBox!!.isVisible = false
        }
        when (mode) {
            BoardSetting.MAX_STAT -> maxStatRadioButton!!.isSelected = true
            BoardSetting.MAX_PT -> maxPtRadioButton!!.isSelected = true
            BoardSetting.MAX_PRESET -> maxPresetRadioButton!!.isSelected = true
            else -> maxNormalRadioButton!!.isSelected = true
        }
        maxRadioEvent()

        // Mark
        markMinSpinner!!.model = SpinnerNumberModel(
            Fn.limit(setting.boardMarkMin, MARK_MIN, MARK_MAX),
            MARK_MIN, MARK_MAX, 1
        )
        markMaxSpinner!!.model = SpinnerNumberModel(
            Fn.limit(setting.boardMarkMax, MARK_MIN, MARK_MAX),
            MARK_MIN, MARK_MAX, 1
        )
        setMarkType(setting.boardMarkType)

        // Sort
        if (setting.boardSortType == Setting.BOARD_SORTTYPE_XP) {
            sortXPRadioButton!!.isSelected = true
        } else {
            sortTicketRadioButton!!.isSelected = true
        }

        // Misc.
        maxLevelCheckBox!!.isSelected = setting.maxLevel
        colorCheckBox!!.isSelected = setting.colorMatch
        rotationCheckBox!!.isSelected = setting.rotation
        symmetryCheckBox!!.isSelected = setting.symmetry
    }

    private fun addListeners() {
        maxNormalRadioButton!!.addItemListener { e: ItemEvent? -> maxRadioEvent() }
        maxStatRadioButton!!.addItemListener { e: ItemEvent? -> maxRadioEvent() }
        maxPtRadioButton!!.addItemListener { e: ItemEvent? -> maxRadioEvent() }
        maxPresetRadioButton!!.addItemListener { e: ItemEvent? -> maxRadioEvent() }
        statPresetComboBox!!.addActionListener { e: ActionEvent? -> maxComboBoxEvent() }
        maxDmgSpinner!!.addChangeListener { e: ChangeEvent? -> maxSpinnerEvent() }
        maxBrkSpinner!!.addChangeListener { e: ChangeEvent? -> maxSpinnerEvent() }
        maxHitSpinner!!.addChangeListener { e: ChangeEvent? -> maxSpinnerEvent() }
        maxRldSpinner!!.addChangeListener { e: ChangeEvent? -> maxSpinnerEvent() }
        markMinSpinner!!.addChangeListener { e: ChangeEvent? -> markSpinnerEvent(false) }
        markMaxSpinner!!.addChangeListener { e: ChangeEvent? -> markSpinnerEvent(true) }
        Fn.addEscDisposeListener(this)
    }

    private fun maxRadioEvent() {
        radioLoading = true
        mode = if (maxPtRadioButton!!.isSelected) {
            BoardSetting.MAX_PT
        } else if (maxStatRadioButton!!.isSelected) {
            BoardSetting.MAX_STAT
        } else if (maxPresetRadioButton!!.isSelected) {
            BoardSetting.MAX_PRESET
        } else {
            BoardSetting.MAX_DEFAULT
        }
        val editable = mode == BoardSetting.MAX_STAT || mode == BoardSetting.MAX_PT
        maxDmgSpinner!!.isEnabled = editable
        maxBrkSpinner!!.isEnabled = editable
        maxHitSpinner!!.isEnabled = editable
        maxRldSpinner!!.isEnabled = editable
        statPresetComboBox!!.isVisible = mode == BoardSetting.MAX_PRESET
        statPresetComboBox!!.removeAllItems()
        when (mode) {
            BoardSetting.MAX_STAT -> {
                maxDmgSpinner!!.value = stat!!.dmg
                maxBrkSpinner!!.value = stat!!.brk
                maxHitSpinner!!.value = stat!!.hit
                maxRldSpinner!!.value = stat!!.rld
            }
            BoardSetting.MAX_PT -> {
                maxDmgSpinner!!.value = pt!!.dmg
                maxBrkSpinner!!.value = pt!!.brk
                maxHitSpinner!!.value = pt!!.hit
                maxRldSpinner!!.value = pt!!.rld
            }
            BoardSetting.MAX_PRESET -> {
                val strs: List<String?> = StatPresetMap.PRESET.getStrings(app, name, star)
                var i = 0
                while (i < strs.size) {
                    val s = strs[i]
                    statPresetComboBox!!.addItem((i + 1).toString() + ": " + s)
                    i++
                }
                statPresetComboBox!!.setSelectedIndex(if (presetIndex < statPresetComboBox!!.itemCount) presetIndex else 0)
            }
            else -> {
                val maxStat: Stat = Board.getMaxStat(boardName, boardStar)
                maxDmgSpinner!!.value = maxStat.dmg
                maxBrkSpinner!!.value = maxStat.brk
                maxHitSpinner!!.value = maxStat.hit
                maxRldSpinner!!.value = maxStat.rld
            }
        }
        radioLoading = false
        maxComboBoxEvent()
        maxSpinnerEvent()
    }

    private fun maxComboBoxEvent() {
        if (!radioLoading) {
            val i = statPresetComboBox!!.selectedIndex
            if (0 <= i) {
                presetIndex = i
                val presetStat: Stat = StatPresetMap.PRESET[boardName, star, i]!!.stat
                maxDmgSpinner!!.value = presetStat.dmg
                maxBrkSpinner!!.value = presetStat.brk
                maxHitSpinner!!.value = presetStat.hit
                maxRldSpinner!!.value = presetStat.rld
            }
        }
    }

    private fun maxSpinnerEvent() {
        if (!radioLoading) {
            val spinnerStat = Stat(
                maxDmgSpinner!!.value as Int,
                maxBrkSpinner!!.value as Int,
                maxHitSpinner!!.value as Int,
                maxRldSpinner!!.value as Int
            )
            when (mode) {
                BoardSetting.MAX_STAT -> stat = spinnerStat
                BoardSetting.MAX_PT -> pt = spinnerStat
                else -> {
                }
            }
            val total: Int = Board.getCellCount(boardName, boardStar)
            if (maxPtRadioButton!!.isSelected) {
                ptSumLabel!!.text = app.getText(AppText.UNIT_PT, pt!!.sum().toString() + "/" + total)
            } else {
                ptSumLabel!!.text = app.getText(AppText.UNIT_PT, total.toString())
            }
        }
    }

    private fun markSpinnerEvent(isMax: Boolean) {
        val min = markMinSpinner!!.value as Int
        val max = markMaxSpinner!!.value as Int
        if (min > max) {
            if (isMax) {
                markMinSpinner!!.value = max
            } else {
                markMaxSpinner!!.value = min
            }
        }
    }

    private fun setAdvandedSetting(b: Boolean) {
        advancedSetting = b
        advancedButton!!.text =
            app.getText(AppText.CSET_ADVANCED_MODE) + ": " + if (advancedSetting) "ON" else "OFF"
        advancedButton!!.font = font.deriveFont(if (advancedSetting) Font.BOLD else Font.PLAIN)
        maxNormalRadioButton!!.isVisible = advancedSetting
        maxPresetRadioButton!!.isVisible = advancedSetting
        maxStatRadioButton!!.isVisible = advancedSetting
        maxPtRadioButton!!.isVisible = advancedSetting
        colorCheckBox!!.isVisible = advancedSetting
    }

    private fun setMarkType(t: Int) {
        markType = t % Setting.NUM_BOARD_MARKTYPE
        if (markType == Setting.BOARD_MARKTYPE_CHIP) {
            markTypeButton!!.text = app.getText(AppText.CSET_MARK_CHIP)
        } else {
            markTypeButton!!.text = app.getText(AppText.CSET_MARK_CELL)
        }
    }

    private val boardName: String
        private get() = app.mf.boardName
    private val boardStar: Int
        private get() = app.mf.boardStar

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private fun initComponents() {
        maxButtonGroup = ButtonGroup()
        sortButtonGroup = ButtonGroup()
        miscPanel = JPanel()
        maxLevelCheckBox = JCheckBox()
        colorCheckBox = JCheckBox()
        rotationCheckBox = JCheckBox()
        symmetryCheckBox = JCheckBox()
        statPanel = JPanel()
        maxPanel = JPanel()
        dmgTextLabel = JLabel()
        brkTextLabel = JLabel()
        hitTextLabel = JLabel()
        rldTextLabel = JLabel()
        maxDmgSpinner = JSpinner()
        maxBrkSpinner = JSpinner()
        maxHitSpinner = JSpinner()
        maxRldSpinner = JSpinner()
        ptSumLabel = JLabel()
        statPresetComboBox = JComboBox()
        jPanel2 = JPanel()
        maxNormalRadioButton = JRadioButton()
        maxPresetRadioButton = JRadioButton()
        maxPtRadioButton = JRadioButton()
        maxStatRadioButton = JRadioButton()
        cancelButton = JButton()
        okButton = JButton()
        markPanel = JPanel()
        markTypeButton = JButton()
        jPanel1 = JPanel()
        markMaxSpinner = JSpinner()
        jLabel1 = JLabel()
        markMinSpinner = JSpinner()
        markDescLabel = JLabel()
        sortPanel = JPanel()
        sortTicketRadioButton = JRadioButton()
        sortXPRadioButton = JRadioButton()
        advancedButton = JButton()
        defaultCloseOperation = DISPOSE_ON_CLOSE
        title = "조합 설정"
        modalityType = ModalityType.APPLICATION_MODAL
        isResizable = false
        type = Type.UTILITY
        miscPanel!!.border = BorderFactory.createTitledBorder("misc")
        maxLevelCheckBox!!.isSelected = true
        maxLevelCheckBox!!.text = "level"
        colorCheckBox!!.font = colorCheckBox!!.font.deriveFont(colorCheckBox!!.font.style or Font.BOLD)
        colorCheckBox!!.isSelected = true
        colorCheckBox!!.text = "color"
        rotationCheckBox!!.isSelected = true
        rotationCheckBox!!.text = "rotation"
        symmetryCheckBox!!.text = "symmetry"
        val miscPanelLayout = GroupLayout(miscPanel)
        miscPanel!!.layout = miscPanelLayout
        miscPanelLayout.setHorizontalGroup(
            miscPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    symmetryCheckBox,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    colorCheckBox,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    maxLevelCheckBox,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    rotationCheckBox,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        miscPanelLayout.setVerticalGroup(
            miscPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    miscPanelLayout.createSequentialGroup()
                        .addComponent(colorCheckBox)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxLevelCheckBox)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rotationCheckBox)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(symmetryCheckBox)
                )
        )
        statPanel!!.border = BorderFactory.createTitledBorder("최대치 설정")
        statPanel!!.preferredSize = Dimension(250, 242)
        dmgTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        dmgTextLabel!!.text = "D"
        dmgTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        brkTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        brkTextLabel!!.text = "B"
        brkTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        hitTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        hitTextLabel!!.text = "H"
        hitTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        rldTextLabel!!.horizontalAlignment = SwingConstants.TRAILING
        rldTextLabel!!.text = "R"
        rldTextLabel!!.horizontalTextPosition = SwingConstants.LEADING
        maxDmgSpinner!!.model = SpinnerNumberModel(0, 0, null, 1)
        maxDmgSpinner!!.isEnabled = false
        maxDmgSpinner!!.preferredSize = Dimension(50, 22)
        maxBrkSpinner!!.model = SpinnerNumberModel(0, 0, null, 1)
        maxBrkSpinner!!.isEnabled = false
        maxBrkSpinner!!.preferredSize = Dimension(50, 22)
        maxHitSpinner!!.model = SpinnerNumberModel(0, 0, null, 1)
        maxHitSpinner!!.isEnabled = false
        maxHitSpinner!!.preferredSize = Dimension(50, 22)
        maxRldSpinner!!.model = SpinnerNumberModel(0, 0, null, 1)
        maxRldSpinner!!.isEnabled = false
        maxRldSpinner!!.preferredSize = Dimension(50, 22)
        val maxPanelLayout = GroupLayout(maxPanel)
        maxPanel!!.layout = maxPanelLayout
        maxPanelLayout.setHorizontalGroup(
            maxPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    maxPanelLayout.createSequentialGroup()
                        .addComponent(dmgTextLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            maxDmgSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
                .addGroup(
                    maxPanelLayout.createSequentialGroup()
                        .addComponent(brkTextLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            maxBrkSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
                .addGroup(
                    maxPanelLayout.createSequentialGroup()
                        .addComponent(hitTextLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            maxHitSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
                .addGroup(
                    maxPanelLayout.createSequentialGroup()
                        .addComponent(rldTextLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            maxRldSpinner,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        maxPanelLayout.linkSize(SwingConstants.HORIZONTAL, brkTextLabel, dmgTextLabel, hitTextLabel, rldTextLabel)
        maxPanelLayout.setVerticalGroup(
            maxPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, maxPanelLayout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addGroup(
                            maxPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    dmgTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    maxDmgSpinner,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(7, 7, 7)
                        .addGroup(
                            maxPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    brkTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    maxBrkSpinner,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(7, 7, 7)
                        .addGroup(
                            maxPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    hitTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    maxHitSpinner,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(7, 7, 7)
                        .addGroup(
                            maxPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    rldTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    maxRldSpinner,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        ptSumLabel!!.horizontalAlignment = SwingConstants.CENTER
        ptSumLabel!!.text = "pt: 123"
        statPresetComboBox!!.preferredSize = Dimension(100, 21)
        maxButtonGroup!!.add(maxNormalRadioButton)
        maxNormalRadioButton!!.font =
            maxNormalRadioButton!!.font.deriveFont(maxNormalRadioButton!!.font.style or Font.BOLD)
        maxNormalRadioButton!!.isSelected = true
        maxNormalRadioButton!!.text = "default"
        maxButtonGroup!!.add(maxPresetRadioButton)
        maxPresetRadioButton!!.font =
            maxPresetRadioButton!!.font.deriveFont(maxPresetRadioButton!!.font.style or Font.BOLD)
        maxPresetRadioButton!!.text = "preset"
        maxButtonGroup!!.add(maxPtRadioButton)
        maxPtRadioButton!!.font = maxPtRadioButton!!.font.deriveFont(maxPtRadioButton!!.font.style or Font.BOLD)
        maxPtRadioButton!!.text = "pt"
        maxButtonGroup!!.add(maxStatRadioButton)
        maxStatRadioButton!!.font = maxStatRadioButton!!.font.deriveFont(maxStatRadioButton!!.font.style or Font.BOLD)
        maxStatRadioButton!!.text = "stat"
        val jPanel2Layout = GroupLayout(jPanel2)
        jPanel2!!.layout = jPanel2Layout
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel2Layout.createSequentialGroup()
                        .addGroup(
                            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    jPanel2Layout.createSequentialGroup()
                                        .addComponent(maxNormalRadioButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(maxPresetRadioButton)
                                )
                                .addGroup(
                                    jPanel2Layout.createSequentialGroup()
                                        .addComponent(maxStatRadioButton)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(maxPtRadioButton)
                                )
                        )
                        .addGap(24, 24, 24)
                )
        )
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel2Layout.createSequentialGroup()
                        .addGroup(
                            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(maxNormalRadioButton)
                                .addComponent(maxPresetRadioButton)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(maxStatRadioButton)
                                .addComponent(maxPtRadioButton)
                        )
                )
        )
        val statPanelLayout = GroupLayout(statPanel)
        statPanel!!.layout = statPanelLayout
        statPanelLayout.setHorizontalGroup(
            statPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(ptSumLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(maxPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(statPresetComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        statPanelLayout.setVerticalGroup(
            statPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    statPanelLayout.createSequentialGroup()
                        .addComponent(
                            jPanel2,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            statPresetComboBox,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            maxPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ptSumLabel)
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                )
        )
        cancelButton!!.text = "cancel"
        cancelButton!!.addActionListener { evt: ActionEvent -> cancelButtonActionPerformed(evt) }
        okButton!!.text = "ok"
        okButton!!.addActionListener { evt: ActionEvent -> okButtonActionPerformed(evt) }
        markPanel!!.border = BorderFactory.createTitledBorder("mark")
        markTypeButton!!.text = "type"
        markTypeButton!!.addActionListener { evt: ActionEvent -> markTypeButtonActionPerformed(evt) }
        jLabel1!!.text = "-"
        val jPanel1Layout = GroupLayout(jPanel1)
        jPanel1!!.layout = jPanel1Layout
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel1Layout.createSequentialGroup()
                        .addComponent(markMinSpinner)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(markMaxSpinner)
                )
        )
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(
                            markMinSpinner,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addComponent(jLabel1)
                        .addComponent(
                            markMaxSpinner,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        markDescLabel!!.horizontalAlignment = SwingConstants.CENTER
        markDescLabel!!.text = "desc"
        val markPanelLayout = GroupLayout(markPanel)
        markPanel!!.layout = markPanelLayout
        markPanelLayout.setHorizontalGroup(
            markPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    markPanelLayout.createSequentialGroup()
                        .addComponent(
                            jPanel1,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(markTypeButton)
                )
                .addComponent(
                    markDescLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        markPanelLayout.setVerticalGroup(
            markPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addGroup(
                    markPanelLayout.createSequentialGroup()
                        .addGroup(
                            markPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    jPanel1,
                                    GroupLayout.PREFERRED_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.PREFERRED_SIZE
                                )
                                .addComponent(markTypeButton)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(markDescLabel)
                )
        )
        sortPanel!!.border = BorderFactory.createTitledBorder("sort")
        sortButtonGroup!!.add(sortTicketRadioButton)
        sortTicketRadioButton!!.text = "ticket"
        sortButtonGroup!!.add(sortXPRadioButton)
        sortXPRadioButton!!.text = "xp"
        val sortPanelLayout = GroupLayout(sortPanel)
        sortPanel!!.layout = sortPanelLayout
        sortPanelLayout.setHorizontalGroup(
            sortPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    sortPanelLayout.createSequentialGroup()
                        .addComponent(sortTicketRadioButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sortXPRadioButton)
                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                )
        )
        sortPanelLayout.setVerticalGroup(
            sortPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    sortPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(sortTicketRadioButton)
                        .addComponent(sortXPRadioButton)
                )
        )
        advancedButton!!.text = "advanced"
        advancedButton!!.addActionListener { evt: ActionEvent -> advancedButtonActionPerformed(evt) }
        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    advancedButton,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addComponent(
                                            statPanel,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(
                                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addGroup(
                                                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                                                        .addComponent(okButton)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(cancelButton)
                                                )
                                                .addComponent(
                                                    miscPanel,
                                                    GroupLayout.Alignment.TRAILING,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE.toInt()
                                                )
                                                .addComponent(
                                                    markPanel,
                                                    GroupLayout.Alignment.TRAILING,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE.toInt()
                                                )
                                                .addComponent(
                                                    sortPanel,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    Short.MAX_VALUE.toInt()
                                                )
                                        )
                                )
                        )
                        .addContainerGap()
                )
        )
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(advancedButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    layout.createSequentialGroup()
                                        .addComponent(
                                            markPanel,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            sortPanel,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            miscPanel,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addPreferredGap(
                                            LayoutStyle.ComponentPlacement.RELATED,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                        .addGroup(
                                            layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(cancelButton)
                                                .addComponent(okButton)
                                        )
                                )
                                .addComponent(statPanel, GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE.toInt())
                        )
                        .addContainerGap()
                )
        )
        pack()
    } // </editor-fold>//GEN-END:initComponents

    private fun cancelButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_cancelButtonActionPerformed
        dispose()
    } //GEN-LAST:event_cancelButtonActionPerformed

    private fun okButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_okButtonActionPerformed
        val setting = app.setting
        setting.advancedSetting = advancedSetting
        setting.maxLevel = maxLevelCheckBox!!.isSelected
        setting.colorMatch = colorCheckBox!!.isSelected
        setting.rotation = rotationCheckBox!!.isSelected
        setting.symmetry = symmetryCheckBox!!.isSelected
        setting.board.setMode(name, star, mode)
        setting.board.setPt(name, star, pt)
        setting.board.setStat(name, star, stat)
        setting.board.setPresetIndex(name, star, presetIndex)
        setting.boardMarkType = markType
        setting.boardMarkMin = markMinSpinner!!.value as Int
        setting.boardMarkMax = markMaxSpinner!!.value as Int
        if (sortXPRadioButton!!.isSelected) {
            setting.boardSortType = Setting.BOARD_SORTTYPE_XP
        } else {
            setting.boardSortType = Setting.BOARD_SORTTYPE_TICKET
        }
        app.mf.setting_resetDisplay()
        app.mf.settingFile_save()

        // Preset - Apply Filter
        if (advancedSetting && mode == BoardSetting.MAX_PRESET && !app.mf.setting_isPresetFilter()) {
            val retval = JOptionPane.showConfirmDialog(
                this,
                app.getText(AppText.CSET_CONFIRM_FILTER_BODY),
                app.getText(AppText.CSET_CONFIRM_FILTER_TITLE),
                JOptionPane.YES_NO_OPTION
            )
            if (retval == JOptionPane.YES_OPTION) {
                app.mf.setting_applyPresetFilter()
            }
        }
        dispose()
    } //GEN-LAST:event_okButtonActionPerformed

    private fun markTypeButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_markTypeButtonActionPerformed
        setMarkType(markType + 1)
    } //GEN-LAST:event_markTypeButtonActionPerformed

    private fun advancedButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_advancedButtonActionPerformed
        setAdvandedSetting(!advancedSetting)
    } //GEN-LAST:event_advancedButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var advancedButton: JButton? = null
    private var brkTextLabel: JLabel? = null
    private var cancelButton: JButton? = null
    private var colorCheckBox: JCheckBox? = null
    private var dmgTextLabel: JLabel? = null
    private var hitTextLabel: JLabel? = null
    private var jLabel1: JLabel? = null
    private var jPanel1: JPanel? = null
    private var jPanel2: JPanel? = null
    private var markDescLabel: JLabel? = null
    private var markMaxSpinner: JSpinner? = null
    private var markMinSpinner: JSpinner? = null
    private var markPanel: JPanel? = null
    private var markTypeButton: JButton? = null
    private var maxBrkSpinner: JSpinner? = null
    private var maxButtonGroup: ButtonGroup? = null
    private var maxDmgSpinner: JSpinner? = null
    private var maxHitSpinner: JSpinner? = null
    private var maxLevelCheckBox: JCheckBox? = null
    private var maxNormalRadioButton: JRadioButton? = null
    private var maxPanel: JPanel? = null
    private var maxPresetRadioButton: JRadioButton? = null
    private var maxPtRadioButton: JRadioButton? = null
    private var maxRldSpinner: JSpinner? = null
    private var maxStatRadioButton: JRadioButton? = null
    private var miscPanel: JPanel? = null
    private var okButton: JButton? = null
    private var ptSumLabel: JLabel? = null
    private var rldTextLabel: JLabel? = null
    private var rotationCheckBox: JCheckBox? = null
    private var sortButtonGroup: ButtonGroup? = null
    private var sortPanel: JPanel? = null
    private var sortTicketRadioButton: JRadioButton? = null
    private var sortXPRadioButton: JRadioButton? = null
    private var statPanel: JPanel? = null
    private var statPresetComboBox: JComboBox<String>? = null
    private var symmetryCheckBox: JCheckBox? = null // End of variables declaration//GEN-END:variables

    companion object {
        private const val MARK_MIN = 0
        private const val MARK_MAX = 64
        fun getInstance(app: App): CalcSettingDialog {
            return CalcSettingDialog(app)
        }
    }

    init {
        initComponents()
        this.app = app
        name = app.mf.boardName
        star = app.mf.boardStar
        loadResources()
        pack()
        loadSettings()
        addListeners()
    }
}