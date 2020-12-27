package main.ui

import main.App
import main.http.ResearchConnection
import main.json.JsonParser
import main.puzzle.*
import main.puzzle.Shape
import main.puzzle.assembly.*
import main.puzzle.assembly.Assembler.Intermediate
import main.setting.BoardSetting
import main.setting.Filter
import main.setting.Setting
import main.setting.StatPresetMap
import main.ui.dialog.*
import main.ui.help.HelpDialog
import main.ui.renderer.ChipFreqListCellRenderer
import main.ui.renderer.ChipListCellRenderer
import main.ui.renderer.CombListCellRenderer
import main.ui.renderer.InvListCellRenderer
import main.ui.resource.AppColor
import main.ui.resource.AppFont
import main.ui.resource.AppImage
import main.ui.resource.AppText
import main.ui.shortcut.ShortcutKeyAdapter
import main.ui.tip.TipMouseListener
import main.ui.transfer.InvListTransferHandler
import main.util.Fn
import main.util.IO
import main.util.Ref
import main.util.ThreadPoolManager
import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.Timer
import javax.swing.border.BevelBorder
import javax.swing.border.Border
import javax.swing.border.LineBorder
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

/**
 *
 * @author Bunnyspa
 */
class MainFrame(  /* VARIABLES */
    private val app: App
) : JFrame() {
    // UI
    private var initSize: Dimension? = null
    private var onBorder: Border? = null
    private val offBorder: Border = LineBorder(background, BORDERSIZE)
    private val tml: TipMouseListener

    // Chip
    private val invChips: MutableList<Chip> = mutableListOf()

    // File
    private val iofc = JFileChooser(File(".")) // Inventory File Chooser
    private val isfc = JFileChooser(File(".")) // Inventory File Chooser
    private val cfc = JFileChooser(File(".")) // Combination File Chooser
    private var invFile_path = ""

    // List
    private val poolLM = DefaultListModel<Chip>()
    private val invLM = DefaultListModel<Chip>()
    private val combChipLM = DefaultListModel<Chip>()
    private val combLM = DefaultListModel<Board>()
    private val combFreqLM = DefaultListModel<ChipFreq?>()
    private var invListMouseDragIndex = 0
    private val blink = Ref(false)
    private val blinkTimer: Timer

    // Chip Stat 
    private var invStat_loading = false
    private var invStat_color = -1
    private var focusedStat = FOCUSED_NONE
    private val statInputBuffer: MutableList<Int> = ArrayList(INPUT_BUFFER_SIZE + 1)

    // Sort
    private var inv_order = DESCENDING

    // Calculator
    private val assembler = Assembler(object : Intermediate {
        override fun stop() {
            ThreadPoolManager.threadPool.execute { process_stop() }
        }

        override fun update(nDone: Int) {
            ThreadPoolManager.threadPool.execute { process_prog(nDone) }
        }

        override fun set(nDone: Int, nTotal: Int) {
            ThreadPoolManager.threadPool.execute { process_setProgBar(nDone, nTotal) }
        }

        override fun show(template: BoardTemplate) {
            ThreadPoolManager.threadPool.execute { process_showImage(template) }
        }
    })
    private var time: Long = 0
    private var pauseTime: Long = 0
    private var prevDoneTime: Long = 0
    private val doneTimes: MutableList<Long> = LinkedList()
    private val calcTimer = Timer(100) { e: ActionEvent? -> calcTimer() }

    // Setting
    private var settingFile_loading = false

    // Array
    private val invComboBoxes: MutableList<JComboBox<String>?> = ArrayList(4)
    private val invStatPanels: MutableList<JPanel?> = ArrayList(4)
    private fun init() {
        initImages()
        initTables()
        invComboBoxes.add(invDmgComboBox)
        invComboBoxes.add(invBrkComboBox)
        invComboBoxes.add(invHitComboBox)
        invComboBoxes.add(invRldComboBox)
        invStatPanels.add(invDmgPanel)
        invStatPanels.add(invBrkPanel)
        invStatPanels.add(invHitPanel)
        invStatPanels.add(invRldPanel)
        combTabbedPane.add(app.getText(AppText.COMB_TAB_RESULT), combResultPanel)
        combTabbedPane.add(app.getText(AppText.COMB_TAB_FREQ), combFreqPanel)
        settingFile_load()
        for (string in Board.NAMES) {
            boardNameComboBox.addItem(string)
        }
        (invSortTypeComboBox.renderer as JLabel).horizontalAlignment = SwingConstants.CENTER
        poolListPanel.border = offBorder
        invListPanel.border = offBorder
        combListPanel.border = offBorder
        invStatPanels.forEach { t -> t?.border = offBorder }
        invLevelSlider.maximum = Chip.LEVEL_MAX
        combStopButton.isVisible = false
        researchButton.isVisible = false
        timeWarningButton.isVisible = false
        blinkTimer.start()
        addListeners()
        setting_resetBoard()
        packAndSetInitSize()
    }

    fun afterLoad() {
        Thread {

            // Check app version
            IO.checkNewVersion(app)

            // Check research
            val version = ResearchConnection.version
            if (version != null && version.isNotEmpty()) {
                if (!App.VERSION.isCurrent(version)) {
                    researchButton.isEnabled = false
                }
                researchButton.isVisible = true
            }
        }.start()
    }

    private fun initImages() {
        this.iconImage = AppImage.FAVICON
        donationButton.icon = AppImage.DONATION
        helpButton.icon = AppImage.QUESTION
        displaySettingButton.icon = AppImage.FONT
        poolRotLButton.icon = AppImage.ROTATE_LEFT
        poolRotRButton.icon = AppImage.ROTATE_RIGHT
        poolSortButton.icon = AppImage.DESCENDING
        imageButton.icon = AppImage.PICTURE
        proxyButton.icon = AppImage.PHONE
        poolWindowButton.icon = AppImage.PANEL_CLOSE
        addButton.icon = AppImage.ADD
        invNewButton.icon = AppImage.NEW
        invOpenButton.icon = AppImage.OPEN
        invSaveButton.icon = AppImage.SAVE
        invSaveAsButton.icon = AppImage.SAVEAS
        invSortOrderButton.icon = AppImage.DESCENDING
        filterButton.icon = AppImage.FILTER
        displayTypeButton.icon = AppImage.DISPLAY_STAT
        invRotLButton.icon = AppImage.ROTATE_LEFT
        invRotRButton.icon = AppImage.ROTATE_RIGHT
        invDelButton.icon = AppImage.DELETE
        invDmgTextLabel.icon = AppImage.DMG
        invBrkTextLabel.icon = AppImage.BRK
        invHitTextLabel.icon = AppImage.HIT
        invRldTextLabel.icon = AppImage.RLD
        combWarningButton.icon = AppImage.getScaledIcon(AppImage.UI_WARNING, 16, 16)
        timeWarningButton.icon = AppImage.getScaledIcon(AppImage.UI_WARNING, 16, 16)
        settingButton.icon = AppImage.SETTING
        combStopButton.icon = AppImage.COMB_STOP
        combStartPauseButton.icon = AppImage.COMB_START
        combDmgTextLabel.icon = AppImage.DMG
        combBrkTextLabel.icon = AppImage.BRK
        combHitTextLabel.icon = AppImage.HIT
        combRldTextLabel.icon = AppImage.RLD
        combSaveButton.icon = AppImage.SAVE
        combOpenButton.icon = AppImage.OPEN
        ticketTextLabel.icon = AppImage.TICKET
        legendEquippedLabel.icon = ImageIcon(AppImage.CHIP_EQUIPPED)
        legendRotatedLabel.icon = ImageIcon(AppImage.CHIP_ROTATED)
    }

    private fun initTables() {
        /* POOL */
        // Model
        poolList.model = poolLM
        // Renderer
        poolList.cellRenderer = ChipListCellRenderer(app)
        // Rows
        for (type in Shape.Type.values()) {
            for (s in Shape.getShapes(type)) {
                poolLM.addElement(Chip(s))
            }
        }

        /* INVENTORY */invList.fixedCellHeight = AppImage.Chip.height(true) + 3
        invList.fixedCellWidth = AppImage.Chip.width(true) + 3
        val invD = invListPanel.size
        invD.width =
            invList.fixedCellWidth * 4 + BORDERSIZE * 2 + 10 + invListScrollPane.verticalScrollBar.preferredSize.width
        invListPanel.preferredSize = invD

        // Model
        invList.model = invLM
        // Renderer
        invList.cellRenderer = InvListCellRenderer(
            app,
            invList,
            combList,
            combTabbedPane,
            combChipList,
            combFreqList,
            blink
        )

        // Transfer Handler
        invList.transferHandler = InvListTransferHandler(this)
        invList.actionMap.parent.remove("cut")
        invList.actionMap.parent.remove("copy")
        invList.actionMap.parent.remove("paste")

        /* COMBINATION */
        // Model
        combList.model = combLM
        // Renderer
        combList.cellRenderer = CombListCellRenderer(app, combFreqList)

        /* RESULT */
        val height = AppImage.Chip.height(true) + 3
        val width = AppImage.Chip.width(true) + 3
        combChipList.fixedCellHeight = height
        combChipList.fixedCellWidth = width
        val ccD = combChipListPanel.size
        ccD.width = width + 10 + combChipListScrollPane.verticalScrollBar.preferredSize.width
        combChipListPanel.preferredSize = ccD
        combFreqList.fixedCellHeight = height
        combFreqList.fixedCellWidth = width
        val ccfD = combFreqListPanel.size
        ccfD.width = width + 10 + combFreqListScrollPane.verticalScrollBar.preferredSize.width
        combFreqListPanel.preferredSize = ccfD

        // Model
        combChipList.model = combChipLM
        combFreqList.model = combFreqLM
        // Renderer
        combChipList.cellRenderer = ChipListCellRenderer(app)
        combFreqList.cellRenderer = ChipFreqListCellRenderer(app, blink)
    }

    // </editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Listener Methods">
    private fun addListeners() {
        // Tip
        Fn.getAllComponents(this).forEach(Consumer { t: Component? -> t!!.addMouseListener(tml) })

        // Shortcuts
        for (kl in invList.keyListeners) {
            invList.removeKeyListener(kl)
        }
        Fn.getAllComponents(this).stream()
            .filter { obj: Component? -> obj!!.isFocusable }
            .forEach { c: Component? -> c!!.addKeyListener(initSKA_focusable()) }
        val piKA = initSKA_pi()
        poolList.addKeyListener(piKA)
        invList.addKeyListener(piKA)
        poolList.addKeyListener(initSKA_pool())
        invList.addKeyListener(initSKA_inv())
        combList.addKeyListener(initSKA_comb())

        // Pool Top
        displaySettingButton.addActionListener { e: ActionEvent? ->
            openDialog(
                AppSettingDialog.getInstance(
                    app
                )
            )
        }
        helpButton.addActionListener { e: ActionEvent? ->
            openDialog(
                HelpDialog.getInstance(
                    app
                )
            )
        }
        donationButton.addActionListener { e: ActionEvent? ->
            openDialog(
                DonationDialog.getInstance(
                    app
                )
            )
        }
        poolWindowButton.addActionListener { e: ActionEvent? -> setPoolPanelVisible(!poolPanel.isVisible) }
        imageButton.addActionListener { e: ActionEvent? -> invFile_openImageDialog() }
        proxyButton.addActionListener { e: ActionEvent? -> invFile_openProxyDialog() }

        // Pool Mid
        poolList.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                addButton.isEnabled = !poolList.isSelectionEmpty
            }
        }
        poolList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                if (!poolList.isSelectionEmpty && 2 <= evt.clickCount) {
                    pool_addToInv()
                }
            }
        })
        poolList.addFocusListener(object : FocusAdapter() {
            override fun focusGained(evt: FocusEvent) {
                poolListPanel.border = onBorder
            }

            override fun focusLost(evt: FocusEvent) {
                poolListPanel.border = offBorder
            }
        })

        // Pool Bot
        poolRotLButton.addActionListener { e: ActionEvent? -> pool_rotate(Chip.COUNTERCLOCKWISE) }
        poolRotRButton.addActionListener { e: ActionEvent? -> pool_rotate(Chip.CLOCKWISE) }
        poolSortButton.addActionListener { e: ActionEvent? -> pool_toggleOrder() }
        poolStarComboBox.addActionListener { e: ActionEvent? -> pool_starChanged() }
        poolColorButton.addActionListener { e: ActionEvent? -> pool_cycleColor() }

        // Pool Right
        addButton.addActionListener { e: ActionEvent? -> pool_addToInv() }

        // Inv Top
        invNewButton.addActionListener { e: ActionEvent? -> invFile_new() }
        invOpenButton.addActionListener { e: ActionEvent? -> invFile_open() }
        invSaveButton.addActionListener { e: ActionEvent? -> invFile_save() }
        invSaveAsButton.addActionListener { e: ActionEvent? -> invFile_saveAs() }
        invSortOrderButton.addActionListener { e: ActionEvent? -> display_toggleOrder() }
        invSortTypeComboBox.addActionListener { e: ActionEvent? -> display_applyFilterSort() }
        filterButton.addActionListener { e: ActionEvent? ->
            openDialog(
                FilterDialog.getInstance(
                    app
                )
            )
        }
        displayTypeButton.addActionListener { e: ActionEvent? -> display_toggleType() }

        // Inv Mid
        invList.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                if (invList.selectedIndices.size == 1) {
                    invList.ensureIndexIsVisible(invList.selectedIndex)
                }
                invStat_loadStats()
            }
        }
        invList.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(evt: MouseEvent) {
                invListMouseDragIndex = invList.selectedIndex
            }
        })
        invList.addFocusListener(object : FocusAdapter() {
            override fun focusGained(evt: FocusEvent) {
                invListPanel.border = onBorder
            }

            override fun focusLost(evt: FocusEvent) {
                invListPanel.border = offBorder
            }
        })

        // Inv Bot
        invApplyButton.addActionListener { e: ActionEvent? ->
            openDialog(
                ApplyDialog.getInstance(
                    app
                )
            )
        }
        invLevelSlider.addChangeListener { e: ChangeEvent? ->
            invStat_setStats()
            invStat_refreshStatComboBoxes()
        }
        invStarComboBox.addActionListener { e: ActionEvent? ->
            invStat_setStats()
            invStat_refreshStatComboBoxes()
        }
        invColorButton.addActionListener { e: ActionEvent? -> invStat_cycleColor() }
        invComboBoxes.forEach(Consumer { t: JComboBox<String>? -> t!!.addItemListener { e: ItemEvent? -> invStat_setStats() } })
        invRotLButton.addActionListener { e: ActionEvent? -> invStat_rotate(Chip.COUNTERCLOCKWISE) }
        invRotRButton.addActionListener { e: ActionEvent? -> invStat_rotate(Chip.CLOCKWISE) }
        invDelButton.addActionListener { e: ActionEvent? -> invStat_delete() }
        invMarkCheckBox.addItemListener { e: ItemEvent? -> invStat_setStats() }
        invTagButton.addActionListener { e: ActionEvent? -> invStat_openTagDialog() }

        // Comb Left
        combList.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                comb_loadCombination()
            }
        }
        combList.addFocusListener(object : FocusAdapter() {
            override fun focusGained(evt: FocusEvent) {
                combListPanel.border = onBorder
            }

            override fun focusLost(evt: FocusEvent) {
                combListPanel.border = offBorder
            }
        })

        // Comb Top
        boardNameComboBox.addActionListener { e: ActionEvent? -> setting_resetBoard() }
        boardStarComboBox.addActionListener { e: ActionEvent? -> setting_resetBoard() }
        settingButton.addActionListener { e: ActionEvent? ->
            openDialog(
                CalcSettingDialog.getInstance(
                    app
                )
            )
        }
        combWarningButton.addActionListener { e: ActionEvent? ->
            Fn.popup(
                combWarningButton,
                app.getText(AppText.WARNING_HOCMAX),
                app.getText(AppText.WARNING_HOCMAX_DESC)
            )
        }
        timeWarningButton.addActionListener { e: ActionEvent? ->
            Fn.popup(
                timeWarningButton,
                app.getText(AppText.WARNING_TIME),
                app.getText(AppText.WARNING_TIME_DESC)
            )
        }
        showProgImageCheckBox.addItemListener { e: ItemEvent? -> comb_setShowProgImage() }
        combStartPauseButton.addActionListener { e: ActionEvent? -> process_toggleStartPause() }
        combStopButton.addActionListener { e: ActionEvent? -> process_stop() }

        // Comb Bot
        researchButton.addActionListener { e: ActionEvent? ->
            openFrame(
                ResearchFrame.getInstance(
                    app
                )
            )
        }
        statButton.addActionListener { e: ActionEvent? -> comb_openStatDialog() }
        combOpenButton.addActionListener { e: ActionEvent? -> progFile_open() }
        combSaveButton.addActionListener { e: ActionEvent? -> progFile_saveAs() }

        // Comb Right
        combTabbedPane.addChangeListener { e: ChangeEvent? ->
            if (combTabbedPane.selectedIndex != 1) {
                combFreqList.clearSelection()
            }
        }
        combChipList.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                comb_ensureInvListIndexIsVisible_combChipList()
                invList.repaint()
            }
        }
        combMarkButton.addActionListener { e: ActionEvent? -> comb_result_mark() }
        combTagButton.addActionListener { e: ActionEvent? -> comb_result_openTagDialog() }
        combFreqList.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                comb_ensureInvListIndexIsVisible_combChipFreqList()
                comb_updateFreqLabel()
                invList.repaint()
                combList.repaint()
            }
        }
        combFreqMarkButton.addActionListener { e: ActionEvent? -> comb_freq_mark() }
        combFreqTagButton.addActionListener { e: ActionEvent? -> comb_freq_openTagDialog() }
    }

    private fun initSKA_focusable(): ShortcutKeyAdapter {
        val ska = ShortcutKeyAdapter()
        ska.addShortcut_c(KeyEvent.VK_F) {
            openDialog(
                FilterDialog.getInstance(
                    app
                )
            )
        }
        ska.addShortcut_c(KeyEvent.VK_D) { display_toggleType() }
        ska.addShortcut_c(KeyEvent.VK_E) {
            openDialog(
                CalcSettingDialog.getInstance(
                    app
                )
            )
        }
        ska.addShortcut(KeyEvent.VK_F1) {
            openDialog(
                HelpDialog.getInstance(
                    app
                )
            )
        }
        ska.addShortcut(KeyEvent.VK_F5) { process_toggleStartPause() }
        ska.addShortcut(KeyEvent.VK_F6) { process_stop() }
        return ska
    }

    private fun initSKA_pi(): ShortcutKeyAdapter {
        val ska = ShortcutKeyAdapter()
        ska.addShortcut_c(KeyEvent.VK_N) { invFile_new() }
        ska.addShortcut_c(KeyEvent.VK_O) { invFile_open() }
        ska.addShortcut_c(KeyEvent.VK_S) { invFile_save() }
        ska.addShortcut_cs(KeyEvent.VK_S) { invFile_saveAs() }
        ska.addShortcut(KeyEvent.VK_ENTER) { invStat_focusNextStat() }
        for (i in KeyEvent.VK_0..KeyEvent.VK_9) {
            val number = i - KeyEvent.VK_0
            ska.addShortcut(i) { invStat_readInput(number) }
        }
        for (i in KeyEvent.VK_NUMPAD0..KeyEvent.VK_NUMPAD9) {
            val number = i - KeyEvent.VK_NUMPAD0
            ska.addShortcut(i) { invStat_readInput(number) }
        }
        return ska
    }

    private fun initSKA_pool(): ShortcutKeyAdapter {
        val ska = ShortcutKeyAdapter()
        ska.addShortcut(KeyEvent.VK_COMMA) { pool_rotate(Chip.COUNTERCLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_OPEN_BRACKET) { pool_rotate(Chip.COUNTERCLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_PERIOD) { pool_rotate(Chip.CLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_CLOSE_BRACKET) { pool_rotate(Chip.CLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_C) { pool_cycleColor() }
        ska.addShortcut(KeyEvent.VK_R) { pool_toggleOrder() }
        ska.addShortcut(KeyEvent.VK_SPACE) { pool_addToInv() }
        return ska
    }

    private fun initSKA_inv(): ShortcutKeyAdapter {
        val ska = ShortcutKeyAdapter()
        ska.addShortcut(KeyEvent.VK_COMMA) { invStat_rotate(Chip.COUNTERCLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_OPEN_BRACKET) { invStat_rotate(Chip.COUNTERCLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_PERIOD) { invStat_rotate(Chip.CLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_CLOSE_BRACKET) { invStat_rotate(Chip.CLOCKWISE) }
        ska.addShortcut(KeyEvent.VK_A) {
            openDialog(
                ApplyDialog.getInstance(
                    app
                )
            )
        }
        ska.addShortcut(KeyEvent.VK_C) { invStat_cycleColor() }
        ska.addShortcut(KeyEvent.VK_M) { invStat_toggleMarked() }
        ska.addShortcut(KeyEvent.VK_T) { invStat_openTagDialog() }
        ska.addShortcut(KeyEvent.VK_R) { display_toggleOrder() }
        ska.addShortcut(KeyEvent.VK_DELETE) { invStat_delete() }
        ska.addShortcut(KeyEvent.VK_MINUS) { invStat_decLevel() }
        ska.addShortcut(KeyEvent.VK_SUBTRACT) { invStat_decLevel() }
        ska.addShortcut(KeyEvent.VK_EQUALS) { invStat_incLevel() }
        ska.addShortcut(KeyEvent.VK_ADD) { invStat_incLevel() }
        ska.addShortcut_c(KeyEvent.VK_A) {}
        return ska
    }

    private fun initSKA_comb(): ShortcutKeyAdapter {
        val ska = ShortcutKeyAdapter()
        ska.addShortcut_c(KeyEvent.VK_O) { progFile_open() }
        ska.addShortcut_c(KeyEvent.VK_S) { progFile_saveAs() }
        ska.addShortcut(KeyEvent.VK_C) { comb_nextBoardName() }
        ska.addShortcut(KeyEvent.VK_M) {
            if (combTabbedPane.selectedIndex == 0) {
                comb_result_mark()
            } else {
                comb_freq_mark()
            }
        }
        ska.addShortcut(KeyEvent.VK_T) {
            if (combTabbedPane.selectedIndex == 0) {
                comb_result_openTagDialog()
            } else {
                comb_freq_openTagDialog()
            }
        }
        return ska
    }

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Util Methods">
    private fun openDialog(dialog: JDialog) {
        Fn.open(this, dialog)
    }

    private fun openFrame(frame: JFrame) {
        Fn.open(this, frame)
        this.isVisible = false
    }

    val preferredDialogSize: Dimension
        get() {
            val dim = Dimension()
            dim.width = piButtonPanel.width + invPanel.width + combLeftPanel.width + combRightPanel.width
            dim.height = height
            return dim
        }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Refresh Methods">
    fun refreshDisplay() {
        refreshLang()
        refreshFont()
        refreshColor()
    }

    private fun refreshLang() {
        val piStarCBList = arrayOf(
            Board.getStarHTML_star(5),
            Board.getStarHTML_star(4),
            Board.getStarHTML_star(3),
            Board.getStarHTML_star(2)
        )
        poolStarComboBox.model = DefaultComboBoxModel(piStarCBList)
        invStarComboBox.model = DefaultComboBoxModel(piStarCBList)
        val bStarCBList = arrayOf(
            Board.getStarHTML_star(5),
            Board.getStarHTML_star(4),
            Board.getStarHTML_star(3),
            Board.getStarHTML_star(2),
            Board.getStarHTML_star(1)
        )
        boardStarComboBox.model = DefaultComboBoxModel(bStarCBList)
        invDmgTextLabel.text = app.getText(AppText.CHIP_STAT_DMG)
        invBrkTextLabel.text = app.getText(AppText.CHIP_STAT_BRK)
        invHitTextLabel.text = app.getText(AppText.CHIP_STAT_HIT)
        invRldTextLabel.text = app.getText(AppText.CHIP_STAT_RLD)
        combDmgTextLabel.text = app.getText(AppText.CHIP_STAT_DMG)
        combBrkTextLabel.text = app.getText(AppText.CHIP_STAT_BRK)
        combHitTextLabel.text = app.getText(AppText.CHIP_STAT_HIT)
        combRldTextLabel.text = app.getText(AppText.CHIP_STAT_RLD)
        invApplyButton.text = app.getText(AppText.APPLY_TITLE)
        enhancementTextLabel.text = app.getText(AppText.CHIP_LEVEL)
        invMarkCheckBox.text = app.getText(AppText.CHIP_MARK)
        researchButton.text = app.getText(AppText.RESEARCH_TITLE)
        statButton.text = app.getText(AppText.STAT_TITLE)
        combTabbedPane.setTitleAt(0, app.getText(AppText.COMB_TAB_RESULT))
        combTabbedPane.setTitleAt(1, app.getText(AppText.COMB_TAB_FREQ))
        ticketTextLabel.text = app.getText(AppText.CHIP_TICKET)
        xpTextLabel.text = app.getText(AppText.CHIP_XP)
        combMarkButton.text = app.getText(AppText.CHIP_MARK)
        combTagButton.text = app.getText(AppText.CHIP_TAG)
        combFreqMarkButton.text = app.getText(AppText.CHIP_MARK)
        combFreqTagButton.text = app.getText(AppText.CHIP_TAG)
        legendEquippedLabel.text = app.getText(AppText.LEGEND_EQUIPPED)
        legendRotatedLabel.text = app.getText(AppText.LEGEND_ROTATED)
        iofc.resetChoosableFileFilters()
        isfc.resetChoosableFileFilters()
        cfc.resetChoosableFileFilters()
        iofc.fileFilter = FileNameExtensionFilter(
            app.getText(
                AppText.FILE_EXT_INV_OPEN,
                IO.EXT_INVENTORY
            ), IO.EXT_INVENTORY, "json"
        )
        isfc.fileFilter = FileNameExtensionFilter(
            app.getText(
                AppText.FILE_EXT_INV_SAVE,
                IO.EXT_INVENTORY
            ), IO.EXT_INVENTORY
        )
        cfc.fileFilter = FileNameExtensionFilter(
            app.getText(
                AppText.FILE_EXT_COMB,
                IO.EXT_COMBINATION
            ), IO.EXT_COMBINATION
        )
        invSortTypeComboBox.removeAllItems()
        invSortTypeComboBox.addItem(app.getText(AppText.SORT_CUSTOM))
        invSortTypeComboBox.addItem(app.getText(AppText.SORT_CELL))
        invSortTypeComboBox.addItem(app.getText(AppText.SORT_ENHANCEMENT))
        invSortTypeComboBox.addItem(app.getText(AppText.SORT_STAR))
        invSortTypeComboBox.addItem(app.getText(AppText.CHIP_STAT_DMG_LONG))
        invSortTypeComboBox.addItem(app.getText(AppText.CHIP_STAT_BRK_LONG))
        invSortTypeComboBox.addItem(app.getText(AppText.CHIP_STAT_HIT_LONG))
        invSortTypeComboBox.addItem(app.getText(AppText.CHIP_STAT_RLD_LONG))
        pool_setColorText()
        invStat_setColorText()
        display_refreshInvListCountText()
        process_setCombLabelText()
        refreshTips()
        val isKorean = app.setting.locale == Locale.KOREA || app.setting.locale == Locale.KOREAN
        title = if (isKorean) App.NAME_KR else App.NAME_EN
    }

    private fun refreshTips() {
        tml.clearTips()
        addTip(displaySettingButton, app.getText(AppText.TIP_DISPLAY))
        addTip(helpButton, app.getText(AppText.TIP_HELP))
        addTip(imageButton, app.getText(AppText.TIP_IMAGE))
        addTip(proxyButton, app.getText(AppText.TIP_PROXY))
        addTip(poolList, app.getText(AppText.TIP_POOL))
        addTip(poolRotLButton, app.getText(AppText.TIP_POOL_ROTATE_LEFT))
        addTip(poolRotRButton, app.getText(AppText.TIP_POOL_ROTATE_RIGHT))
        addTip(poolSortButton, app.getText(AppText.TIP_POOL_SORT_ORDER))
        addTip(poolStarComboBox, app.getText(AppText.TIP_POOL_STAR))
        addTip(poolColorButton, app.getText(AppText.TIP_POOL_COLOR))
        addTip(poolWindowButton, app.getText(AppText.TIP_POOLWINDOW))
        addTip(addButton, app.getText(AppText.TIP_ADD))
        addTip(invList, app.getText(AppText.TIP_INV))
        addTip(invNewButton, app.getText(AppText.TIP_INV_NEW))
        addTip(invOpenButton, app.getText(AppText.TIP_INV_OPEN))
        addTip(invSaveButton, app.getText(AppText.TIP_INV_SAVE))
        addTip(invSaveAsButton, app.getText(AppText.TIP_INV_SAVEAS))
        addTip(invSortOrderButton, app.getText(AppText.TIP_INV_SORT_ORDER))
        addTip(invSortTypeComboBox, app.getText(AppText.TIP_INV_SORT_TYPE))
        addTip(filterButton, app.getText(AppText.TIP_INV_FILTER))
        addTip(displayTypeButton, app.getText(AppText.TIP_INV_STAT))
        addTip(invApplyButton, app.getText(AppText.TIP_INV_APPLY))
        addTip(invStarComboBox, app.getText(AppText.TIP_INV_STAR))
        addTip(invColorButton, app.getText(AppText.TIP_INV_COLOR))
        addTip(invLevelSlider, app.getText(AppText.TIP_INV_ENHANCEMENT))
        addTip(invRotLButton, app.getText(AppText.TIP_INV_ROTATE_LEFT))
        addTip(invRotRButton, app.getText(AppText.TIP_INV_ROTATE_RIGHT))
        addTip(invDelButton, app.getText(AppText.TIP_INV_DELETE))
        addTip(invMarkCheckBox, app.getText(AppText.TIP_INV_MARK))
        addTip(invTagButton, app.getText(AppText.TIP_INV_TAG))
        addTip(invDmgTextLabel, app.getText(AppText.CHIP_STAT_DMG_LONG))
        addTip(invBrkTextLabel, app.getText(AppText.CHIP_STAT_BRK_LONG))
        addTip(invHitTextLabel, app.getText(AppText.CHIP_STAT_HIT_LONG))
        addTip(invRldTextLabel, app.getText(AppText.CHIP_STAT_RLD_LONG))
        addTip(boardNameComboBox, app.getText(AppText.TIP_BOARD_NAME))
        addTip(boardStarComboBox, app.getText(AppText.TIP_BOARD_STAR))
        addTip(combWarningButton, app.getText(AppText.WARNING_HOCMAX))
        addTip(timeWarningButton, app.getText(AppText.WARNING_TIME))
        if (!researchButton.isEnabled) {
            addTip(researchButton, app.getText(AppText.TIP_RESEARCH_OLD))
        }
        addTip(combList, app.getText(AppText.TIP_COMB_LIST))
        addTip(combChipList, app.getText(AppText.TIP_COMB_CHIPLIST))
        addTip(combFreqList, app.getText(AppText.TIP_COMB_FREQLIST))
        addTip(combDmgTextLabel, app.getText(AppText.CHIP_STAT_DMG_LONG))
        addTip(combBrkTextLabel, app.getText(AppText.CHIP_STAT_BRK_LONG))
        addTip(combHitTextLabel, app.getText(AppText.CHIP_STAT_HIT_LONG))
        addTip(combRldTextLabel, app.getText(AppText.CHIP_STAT_RLD_LONG))
        addTip(settingButton, app.getText(AppText.TIP_COMB_SETTING))
        addTip(showProgImageCheckBox, app.getText(AppText.TIP_COMB_SHOWPROGIMAGE))
        addTip(combStartPauseButton, app.getText(AppText.TIP_COMB_START))
        addTip(statButton, app.getText(AppText.TIP_COMB_STAT))
        addTip(combOpenButton, app.getText(AppText.TIP_COMB_OPEN))
        addTip(combSaveButton, app.getText(AppText.TIP_COMB_SAVE))
        addTip(combMarkButton, app.getText(AppText.TIP_COMB_MARK))
        addTip(combTagButton, app.getText(AppText.TIP_COMB_TAG))
        addTip(combFreqMarkButton, app.getText(AppText.TIP_COMB_MARK))
        addTip(combFreqTagButton, app.getText(AppText.TIP_COMB_TAG))
    }

    private fun addTip(c: JComponent, s: String?) {
        tml.setTip(c, s)
        c.toolTipText = s
    }

    private fun refreshFont() {
        val defaultFont = AppFont.default.deriveFont(app.setting.fontSize.toFloat())
        // Font
        invTagButton.text = ""
        Fn.setUIFont(defaultFont)
        Fn.getAllComponents(this).forEach { c: Component? -> c!!.font = defaultFont }

        // Size
        combWarningButton.preferredSize = Dimension(combWarningButton.height, combWarningButton.height)
        timeWarningButton.preferredSize = Dimension(timeWarningButton.height, timeWarningButton.height)
        combImageLabel.preferredSize = Dimension(combImageLabel.width, combImageLabel.width)
        val height = Fn.getHeight(defaultFont)
        var levelWidth = 0
        for (i in 0..20) {
            levelWidth = max(levelWidth, Fn.getWidth(i.toString(), defaultFont))
        }
        invLevelLabel.preferredSize = Dimension(levelWidth + 10, height)
        val textWidth = Fn.max(
            Fn.getWidth(app.getText(AppText.CHIP_STAT_DMG), defaultFont),
            Fn.getWidth(app.getText(AppText.CHIP_STAT_BRK), defaultFont),
            Fn.getWidth(app.getText(AppText.CHIP_STAT_HIT), defaultFont),
            Fn.getWidth(app.getText(AppText.CHIP_STAT_RLD), defaultFont)
        )
        val textDim = Dimension(textWidth + 30, height)
        invDmgTextLabel.preferredSize = textDim
        invBrkTextLabel.preferredSize = textDim
        invHitTextLabel.preferredSize = textDim
        invRldTextLabel.preferredSize = textDim
        var statWidth = 0
        for (i in 0..Chip.PT_MAX) {
            statWidth = max(statWidth, Fn.getWidth(i.toString(), defaultFont))
        }
        val ptDim = Dimension(statWidth + 10, height)
        invDmgPtLabel.preferredSize = ptDim
        invBrkPtLabel.preferredSize = ptDim
        invHitPtLabel.preferredSize = ptDim
        invRldPtLabel.preferredSize = ptDim
        var colorWidth = 0
        for (color in AppText.TEXT_MAP_COLOR.values) {
            colorWidth = max(colorWidth, Fn.getWidth(app.getText(color), defaultFont))
        }
        invColorButton.preferredSize = Dimension(colorWidth + 10, height)
        val prefCombWidth = combStatPanel.preferredSize.width
        combImagePanel.preferredSize = Dimension(prefCombWidth, prefCombWidth)

        // Save
        packAndSetInitSize()
        invStat_setTagButtonText()
    }

    private fun refreshColor() {
        onBorder = LineBorder(app.blue(), BORDERSIZE)
        comb_loadCombination()
    }

    fun packAndSetInitSize() {
        pack()
        initSize = size
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Pool Methods">
    private fun pool_rotate(direction: Boolean) {
        val elements = poolLM.elements()
        while (elements.hasMoreElements()) {
            val c = elements.nextElement()
            c.initRotate(direction)
        }
        poolList.repaint()
    }

    private fun pool_setOrder(b: Boolean) {
        if (!poolLM.isEmpty) {
            app.setting.poolOrder = b
            val c = poolLM.firstElement()
            if (b == ASCENDING) {
                poolSortButton.icon = AppImage.ASCNEDING
                if (c.getSize() != 1) {
                    pool_reverseList()
                }
            } else {
                poolSortButton.icon = AppImage.DESCENDING
                if (c.getSize() != 6) {
                    pool_reverseList()
                }
            }
            settingFile_save()
        }
    }

    private fun pool_toggleOrder() {
        pool_setOrder(!app.setting.poolOrder)
    }

    private fun pool_reverseList() {
        var sel = poolList.selectedIndex
        val cs = poolLM.elements().toList().reversed()
        poolLM.clear()
        cs.forEach { element -> poolLM.addElement(element) }
        if (sel > -1) {
            sel = cs.size - sel - 1
            poolList.selectedIndex = sel
            poolList.ensureIndexIsVisible(sel)
        }
    }

    private fun pool_setColor(color: Int) {
        app.setting.poolColor = color
        pool_setColorText()
        settingFile_save()
    }

    private fun pool_setColorText() {
        poolColorButton.text = app.getText(AppText.TEXT_MAP_COLOR[app.setting.poolColor]!!)
        poolColorButton.foreground = AppColor.CHIPS[app.setting.poolColor]
    }

    private fun pool_cycleColor() {
        pool_setColor((app.setting.poolColor + 1) % AppText.TEXT_MAP_COLOR.size)
    }

    private fun pool_starChanged() {
        app.setting.poolStar = 5 - poolStarComboBox.selectedIndex
        settingFile_save()
    }

    private fun setPoolPanelVisible(b: Boolean) {
        if (b) {
            poolPanel.isVisible = true
            poolWindowButton.icon = AppImage.PANEL_CLOSE
        } else {
            poolPanel.isVisible = false
            poolList.clearSelection()
            poolWindowButton.icon = AppImage.PANEL_OPEN
        }
        if (size == initSize) {
            packAndSetInitSize()
        }
        settingFile_save()
    }

    private fun pool_addToInv() {
        if (!poolList.isSelectionEmpty) {
            val poolChip = poolList.selectedValue
            val c = Chip(poolChip, 5 - poolStarComboBox.selectedIndex, app.setting.poolColor)
            if (invList.selectedIndices.size == 1) {
                val i = invList.selectedIndex + 1
                inv_chipsAdd(i, c)
                invList.setSelectedIndex(i)
            } else {
                inv_chipsAdd(c)
            }
            invStat_enableSave()
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inventory Chip Methods">
    private fun inv_chipsAdd(i: Int, c: Chip) {
        invChips.add(i, c)
        invLM.add(i, c)
        c.displayType = app.setting.displayType
    }

    private fun inv_chipsAdd(c: Chip) {
        invChips.add(c)
        invLM.addElement(c)
        c.displayType = app.setting.displayType
    }

    private fun inv_chipsLoad(cs: Collection<Chip>) {
        inv_chipsClear()
        invChips.addAll(cs)
        invChips.forEach(Consumer { c: Chip? -> c!!.displayType = app.setting.displayType })
        display_applyFilterSort()
    }

    private fun inv_chipsClear() {
        invChips.clear()
        invLM.clear()
    }

    private fun inv_chipsRemove(i: Int) {
        invChips.remove(invLM[i])
        invLM.removeElementAt(i)
    }

    private fun inv_chipsRefresh() {
        invChips.clear()
        val elements = invLM.elements()
        while (elements.hasMoreElements()) {
            val c = elements.nextElement()
            invChips.add(c)
        }
    }

    fun inv_getFilteredChips(): List<Chip> {
        val chips: MutableList<Chip> = mutableListOf()
        for (i in 0 until invLM.size()) {
            chips.add(invLM.getElementAt(i))
        }
        return chips
    }

    fun inv_getAllTags(): List<Tag?> {
        return Tag.getTags(invChips)
    }

    fun invListTransferHandler_ExportDone() {
        inv_chipsRefresh()
        if (invListMouseDragIndex != invList.selectedIndex) {
            invStat_enableSave()
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inventory Stat Methods">
    fun invStat_loadStats() {
        if (!invStat_loading) {
            invStat_loading = true
            val singleSelected = invList.selectedIndices.size == 1
            val multipleSelected = invList.selectedIndices.isNotEmpty()
            invComboBoxes.forEach(Consumer { t: JComboBox<String>? -> t!!.setEnabled(singleSelected) })
            invStarComboBox.isEnabled = singleSelected
            invLevelSlider.isEnabled = singleSelected
            invColorButton.isEnabled = singleSelected
            invMarkCheckBox.isEnabled = singleSelected
            invStat_resetFocus(singleSelected)
            invDelButton.isEnabled = multipleSelected
            invRotLButton.isEnabled = multipleSelected
            invRotRButton.isEnabled = multipleSelected
            invTagButton.isEnabled = multipleSelected
            invStarComboBox.selectedIndex = if (singleSelected) 5 - invList.selectedValue.star else 0
            invLevelSlider.value = if (singleSelected) invList.selectedValue.level else 0
            invStat_setColor(if (singleSelected) invList.selectedValue.color else -1)
            invMarkCheckBox.isSelected = singleSelected && invList.selectedValue.isMarked
            invStat_setTagButtonText()
            invStat_loading = false
        }
        invStat_refreshStatComboBoxes()
        invStat_refreshLabels()
    }

    private fun invStat_setTagButtonText() {
        var tagButtonText = app.getText(AppText.TAG_NONE)
        if (invList.selectedIndices.isNotEmpty()) {
            val tags: MutableSet<Tag?> = HashSet()
            invChips.forEach(Consumer { c: Chip? -> tags.addAll(c!!.tags) })
            for (selectedIndex in invList.selectedIndices) {
                val c = invLM[selectedIndex]
                tags.retainAll(c.tags)
            }
            val widthStr = StringBuilder()
            val tagStrs: MutableList<String> = mutableListOf()
            var ellipsis = false
            for (t in tags) {
                val width = invTagButton.width - 10
                var next = t!!.name
                while (next.isNotEmpty() && Fn.getWidth("$widthStr$next ...", invTagButton.font) >= width) {
                    ellipsis = true
                    next = next.substring(0, next.length - 1)
                }
                if (next.isEmpty()) {
                    break
                }
                tagStrs.add(Fn.htmlColor(next, t.color))
                widthStr.append(t.name).append(", ")
            }
            val text = java.lang.String.join(", ", tagStrs) + if (ellipsis) " ..." else ""
            if (text.isNotEmpty()) {
                tagButtonText = Fn.toHTML(text)
            }
        }
        invTagButton.text = tagButtonText
    }

    private fun invStat_setStats() {
        if (!invStat_loading) {
            if (invList.selectedIndices.size == 1) {
                val c = invList.selectedValue
                c.pt = Stat(
                    invDmgComboBox.selectedIndex,
                    invBrkComboBox.selectedIndex,
                    invHitComboBox.selectedIndex,
                    invRldComboBox.selectedIndex
                )
                c.star = 5 - invStarComboBox.selectedIndex
                c.initLevel = invLevelSlider.value
                c.color = invStat_color
                c.isMarked = invMarkCheckBox.isSelected
                invStat_enableSave()
                comb_updateMark()
            }
            invList.repaint()
            invStat_refreshLabels()
        }
    }

    fun invStat_enableSave() {
        invSaveButton.isEnabled = true
    }

    private fun invStat_refreshLabels() {
        val singleSelected = invList.selectedIndices.size == 1
        if (singleSelected) {
            invDmgPtLabel.text = invDmgComboBox.selectedIndex.toString()
            invBrkPtLabel.text = invBrkComboBox.selectedIndex.toString()
            invHitPtLabel.text = invHitComboBox.selectedIndex.toString()
            invRldPtLabel.text = invRldComboBox.selectedIndex.toString()
            invLevelLabel.text = invLevelSlider.value.toString()
        } else {
            invDmgPtLabel.text = ""
            invBrkPtLabel.text = ""
            invHitPtLabel.text = ""
            invRldPtLabel.text = ""
            invLevelLabel.text = ""
        }
        if (singleSelected && !invList.selectedValue.isPtValid()) {
            invDmgPtLabel.foreground = Color.RED
            invBrkPtLabel.foreground = Color.RED
            invHitPtLabel.foreground = Color.RED
            invRldPtLabel.foreground = Color.RED
        } else {
            invDmgPtLabel.foreground = Color.BLACK
            invBrkPtLabel.foreground = Color.BLACK
            invHitPtLabel.foreground = Color.BLACK
            invRldPtLabel.foreground = Color.BLACK
        }
    }

    private fun invStat_refreshStatComboBoxes() {
        if (!invStat_loading) {
            invStat_loading = true
            invComboBoxes.forEach(Consumer { obj: JComboBox<String>? -> obj!!.removeAllItems() })
            if (invList.selectedIndices.size == 1) {
                val c = invList.selectedValue
                for (i in 0..c.getMaxPt()) {
                    invDmgComboBox.addItem(Chip.getStat(Chip.RATE_DMG, c, i).toString())
                    invBrkComboBox.addItem(Chip.getStat(Chip.RATE_BRK, c, i).toString())
                    invHitComboBox.addItem(Chip.getStat(Chip.RATE_HIT, c, i).toString())
                    invRldComboBox.addItem(Chip.getStat(Chip.RATE_RLD, c, i).toString())
                }
                invDmgComboBox.selectedIndex = c.pt!!.dmg
                invBrkComboBox.selectedIndex = c.pt!!.brk
                invHitComboBox.selectedIndex = c.pt!!.hit
                invRldComboBox.selectedIndex = c.pt!!.rld
            }
            invStat_loading = false
        }
    }

    private fun invStat_setColor(color: Int) {
        invStat_color = color
        invStat_setColorText()
        if (invList.selectedIndices.size == 1) {
            invStat_setStats()
        }
    }

    private fun invStat_setColorText() {
        if (invStat_color < 0) {
            invColorButton.text = " "
        } else {
            invColorButton.text = app.getText(AppText.TEXT_MAP_COLOR[invStat_color]!!)
            invColorButton.foreground = AppColor.CHIPS[invStat_color]
        }
    }

    private fun invStat_cycleColor() {
        if (invList.selectedIndices.size == 1) {
            invStat_setColor((invStat_color + 1) % AppText.TEXT_MAP_COLOR.size)
        }
    }

    private fun invStat_setLevel(i: Int) {
        if (invList.selectedIndices.size == 1) {
            invLevelSlider.value = Fn.limit(i, 0, Chip.LEVEL_MAX)
        }
    }

    private fun invStat_decLevel() {
        invStat_setLevel(invLevelSlider.value - 1)
    }

    private fun invStat_incLevel() {
        invStat_setLevel(invLevelSlider.value + 1)
    }

    private fun invStat_toggleMarked() {
        if (invList.selectedIndices.size == 1) {
            invMarkCheckBox.isSelected = !invMarkCheckBox.isSelected
        }
    }

    private fun invStat_openTagDialog() {
        if (invList.selectedIndices.isNotEmpty()) {
            val chips: MutableList<Chip?> = ArrayList(
                invList.selectedIndices.size
            )
            for (selectedIndex in invList.selectedIndices) {
                val c = invLM[selectedIndex]
                chips.add(c)
            }
            openDialog(TagDialog.getInstance(app, chips))
        }
    }

    private fun invStat_focusStat(type: Int) {
        focusedStat = type
        for (i in 0..3) {
            invStatPanels[i]!!.border = if (type == i) onBorder else offBorder
        }
        statInputBuffer.clear()
    }

    private fun invStat_resetFocus(focused: Boolean) {
        invStat_focusStat(if (focused) FOCUSED_DMG else FOCUSED_NONE)
    }

    private fun invStat_focusNextStat() {
        if (invList.selectedIndices.size == 1) {
            invStat_focusStat((focusedStat + 1) % 4)
        }
    }

    private fun invStat_readInput(number: Int) {
        if (invList.selectedIndices.size == 1) {
            statInputBuffer.add(number)
            if (statInputBuffer.size > INPUT_BUFFER_SIZE) {
                statInputBuffer.removeAt(0)
            }
            val inputs = arrayOfNulls<String>(statInputBuffer.size)
            for (i in inputs.indices) {
                if (statInputBuffer.size >= i + 1) {
                    val t = StringBuilder()
                    for (j in i until statInputBuffer.size) {
                        t.append(statInputBuffer[j])
                    }
                    inputs[i] = t.toString()
                }
            }
            val combobox = invComboBoxes[focusedStat]
            val nItems = combobox!!.itemCount
            if (app.setting.displayType == DISPLAY_STAT) {
                for (i in nItems - 1 downTo 0) {
                    val candidate = combobox.getItemAt(i)
                    for (input in inputs) {
                        if (candidate == input) {
                            combobox.selectedIndex = i
                            return
                        }
                    }
                }
            } else {
                val pt = inputs[inputs.size - 1]!!.toInt()
                if (pt < nItems) {
                    combobox.selectedIndex = pt
                }
            }
        }
    }

    fun invStat_applyAll(action: Consumer<in Chip>?) {
        val cs = inv_getFilteredChips()
        if (cs.isNotEmpty()) {
            cs.forEach(action)
            invList.repaint()
            invStat_loadStats()
            invStat_enableSave()
        }
    }

    private fun invStat_rotate(direction: Boolean) {
        if (invList.selectedIndices.isNotEmpty()) {
            for (selectedIndex in invList.selectedIndices) {
                val c = invLM[selectedIndex]
                c.initRotate(direction)
            }
            invList.repaint()
            invStat_enableSave()
        }
    }

    private fun invStat_delete() {
        if (invList.selectedIndices.isNotEmpty()) {
            val indices = invList.selectedIndices
            val indexList: MutableList<Int> = ArrayList(indices.size)
            for (i in indices) {
                indexList.add(i)
            }
            indexList.reverse()
            indexList.forEach { i -> inv_chipsRemove(i) }
            display_refreshInvListCountText()
            invStat_enableSave()
        }
    }

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inventory Display Methods">
    private fun display_setOrder(order: Boolean) {
        if (invSortOrderButton.isEnabled) {
            inv_order = order
            if (order == ASCENDING) {
                invSortOrderButton.icon = AppImage.ASCNEDING
            } else {
                invSortOrderButton.icon = AppImage.DESCENDING
            }
            display_applyFilterSort()
        }
    }

    private fun display_toggleOrder() {
        display_setOrder(!inv_order)
    }

    private fun display_anyTrueFilter(): Boolean {
        return (app.filter.anySCTMTrue()
                || app.filter.levelMin != 0 || app.filter.levelMax != Chip.LEVEL_MAX || app.filter.ptMin != Stat()
                || app.filter.ptMax != Stat(Chip.PT_MAX)
                || app.filter.includedTags.isNotEmpty()
                || app.filter.excludedTags.isNotEmpty())
    }

    fun display_applyFilterSort() {
        invLM.removeAllElements()
        val temp: MutableList<Chip> = mutableListOf()

        //// Filter
        invChips.forEach { c ->
            var pass = true
            // Star
            if (app.filter.anyStarTrue()) {
                val i = 5 - c.star
                pass = app.filter.getStar(i)
            }
            // Color
            if (pass && app.filter.anyColorTrue()) {
                val i = c.color
                pass = app.filter.getColor(i)
            }
            // Size
            if (pass && app.filter.anyTypeTrue()) {
                var i = 6 - c.getSize()
                if (c.getSize() < 5 || c.getType() == Shape.Type._5A) {
                    i++
                }
                pass = app.filter.getType(i)
            }
            // Marked
            if (pass && app.filter.anyMarkTrue()) {
                val i = if (c.isMarked) 0 else 1
                pass = app.filter.getMark(i)
            }
            // Level
            if (pass) {
                pass = app.filter.levelMin <= c.level && c.level <= app.filter.levelMax
            }
            // PT
            if (pass) {
                val cPt = c.pt
                pass = cPt!!.allGeq(app.filter.ptMin) && cPt.allLeq(app.filter.ptMax)
            }
            // Tag
            if (pass && app.filter.includedTags.isNotEmpty()) {
                pass = app.filter.includedTags.stream()
                    .allMatch { fTag: Tag? -> c.tags.stream().anyMatch { obj: Tag? -> fTag!! == obj } }
            }
            if (pass && app.filter.excludedTags.isNotEmpty()) {
                pass = app.filter.excludedTags.stream()
                    .noneMatch { fTag: Tag? -> c.tags.stream().anyMatch { obj: Tag? -> fTag!! == obj } }
            }

            // Final
            if (pass) {
                temp.add(c)
            }
        }
        when (invSortTypeComboBox.selectedIndex) {
            SORT_SIZE -> temp.sortWith { c1, c2 -> Chip.compare(c1, c2) }
            SORT_LEVEL -> temp.sortWith { c1, c2 -> Chip.compareLevel(c1, c2) }
            SORT_STAR -> temp.sortWith { c1, c2 -> Chip.compareStar(c1, c2) }
            SORT_DMG -> temp.sortWith { c1, c2 ->
                when (app.setting.displayType) {
                    Setting.DISPLAY_STAT -> c1.getStat().dmg - c2.getStat().dmg
                    else -> c1.pt!!.dmg - c2.pt!!.dmg
                }
            }
            SORT_BRK -> temp.sortWith { c1, c2 ->
                when (app.setting.displayType) {
                    Setting.DISPLAY_STAT -> c1.getStat().brk - c2.getStat().brk
                    else -> c1.pt!!.brk - c2.pt!!.brk
                }
            }
            SORT_HIT -> temp.sortWith { c1, c2 ->
                when (app.setting.displayType) {
                    Setting.DISPLAY_STAT -> c1.getStat().hit - c2.getStat().hit
                    else -> c1.pt!!.hit - c2.pt!!.hit
                }
            }
            SORT_RLD -> temp.sortWith { c1, c2 ->
                when (app.setting.displayType) {
                    Setting.DISPLAY_STAT -> c1.getStat().rld - c2.getStat().rld
                    else -> c1.pt!!.rld - c2.pt!!.rld
                }
            }
        }
        if (invSortTypeComboBox.selectedIndex != SORT_NONE && inv_order == DESCENDING) {
            temp.reverse()
        }

        // Fill
        temp.forEach(Consumer { element: Chip? -> invLM.addElement(element) })
        val anyTrueAll = display_anyTrueFilter()

        // UI
        invSortOrderButton.isEnabled = invSortTypeComboBox.selectedIndex != SORT_NONE
        val chipEnabled = invSortTypeComboBox.selectedIndex == SORT_NONE && !anyTrueAll
        poolList.isEnabled = chipEnabled
        if (!chipEnabled) {
            poolList.clearSelection()
        }
        addButton.isEnabled = chipEnabled && poolList.selectedIndex != -1
        invList.dragEnabled = chipEnabled
        filterButton.icon = if (anyTrueAll) AppImage.FILTER_APPLY else AppImage.FILTER
        display_refreshInvListCountText()
    }

    private fun display_refreshInvListCountText() {
        filterChipCountLabel.text = if (display_anyTrueFilter()) app.getText(
            AppText.FILTER_ENABLED,
            invLM.size().toString(),
            invChips.size.toString()
        ) else app.getText(AppText.FILTER_DISABLED, invChips.size.toString())
    }

    private fun display_setType(type: Int) {
        val iMod: Int = type % Setting.NUM_DISPLAY
        app.setting.displayType = iMod
        if (iMod == DISPLAY_STAT) {
            displayTypeButton.icon = AppImage.DISPLAY_STAT
        } else {
            displayTypeButton.icon = AppImage.DISPLAY_PT
        }
        invChips.forEach(Consumer { t: Chip? -> t!!.displayType = iMod })
        display_applyFilterSort()
        invList.repaint()
        for (i in 0 until combLM.size()) {
            val board = combLM[i]
            board.forEachChip { t: Chip -> t.displayType = iMod }
        }
        combChipList.repaint()
        val cfEnum = combFreqLM.elements()
        while (cfEnum.hasMoreElements()) {
            cfEnum.nextElement()!!.chip!!.displayType = iMod
        }
        combFreqList.repaint()
        settingFile_save()
    }

    private fun display_toggleType() {
        display_setType(app.setting.displayType + 1)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Setting Methods">
    val boardName: String
        get() = boardNameComboBox.getItemAt(boardNameComboBox.selectedIndex)
    val boardStar: Int
        get() = 5 - boardStarComboBox.selectedIndex

    fun setting_resetDisplay() {
        val settingIcon: Icon = when (app.setting.board.getStatMode(boardName, boardStar)) {
            BoardSetting.MAX_STAT -> AppImage.SETTING_STAT
            BoardSetting.MAX_PT -> AppImage.SETTING_PT
            BoardSetting.MAX_PRESET -> AppImage.SETTING_PRESET
            else -> AppImage.SETTING
        }
        settingButton.icon = settingIcon
        val board = app.setting.board
        val maxWarning = boardStar == 5 && board.getStatMode(
            boardName,
            boardStar
        ) != BoardSetting.MAX_PRESET && !board.hasDefaultPreset(boardName, boardStar)
        combWarningButton.isVisible = maxWarning
    }

    private fun setting_resetBoard() {
        setting_resetDisplay()
        boardImageLabel.icon = AppImage.Board[app, boardImageLabel.width, boardName, boardStar]
        boardImageLabel.repaint()
    }

    fun setting_isPresetFilter(): Boolean {
        val name = boardName
        val star = boardStar
        val presetIndex = app.setting.board.getPresetIndex(name, star)
        val stars = booleanArrayOf(true, false, false, false)
        val presetMap: StatPresetMap = StatPresetMap.PRESET
        val types = presetMap.getTypeFilter(name, star, presetIndex)
        val ptMin = presetMap[name, star, presetIndex]!!.ptMin
        val ptMax = presetMap[name, star, presetIndex]!!.ptMax
        return app.filter.equals(stars, types, ptMin, ptMax)
    }

    fun setting_applyPresetFilter() {
        val name = boardName
        val star = boardStar
        val presetIndex = app.setting.board.getPresetIndex(name, star)
        val stars = booleanArrayOf(true, false, false, false)
        val colors = BooleanArray(Filter.NUM_COLOR)
        val c: Int = Board.getColor(name)
        if (c < colors.size) {
            colors[c] = true
        }
        val presetMap: StatPresetMap = StatPresetMap.PRESET
        val types = presetMap.getTypeFilter(name, star, presetIndex)
        val ptMin = presetMap[name, star, presetIndex]!!.ptMin
        val ptMax = presetMap[name, star, presetIndex]!!.ptMax
        app.filter.setColors(*colors)
        app.filter.setStars(*stars)
        app.filter.setTypes(*types)
        app.filter.ptMin = ptMin
        app.filter.ptMax = ptMax
        display_applyFilterSort()
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Combination Methods">
    private fun comb_setBoardName(i: Int) {
        if (boardNameComboBox.isEnabled) {
            boardNameComboBox.selectedIndex = Fn.limit(i, 0, boardNameComboBox.itemCount)
        }
    }

    private fun comb_nextBoardName() {
        comb_setBoardName((boardNameComboBox.selectedIndex + 1) % boardNameComboBox.itemCount)
    }

    private fun comb_setShowProgImage() {
        app.setting.showProgImage = showProgImageCheckBox.isSelected
        if (!app.setting.showProgImage) {
            boardImageLabel.icon = AppImage.Board[app, boardImageLabel.width, boardName, boardStar]
            boardImageLabel.repaint()
        }
    }

    private fun comb_loadCombination() {
        combChipLM.clear()
        val selected = !combList.isSelectionEmpty
        statButton.isEnabled = selected
        combMarkButton.isEnabled = selected
        combTagButton.isEnabled = selected
        if (selected) {
            val board = combList.selectedValue
            val size = min(combImageLabel.height, combImageLabel.width) - 1
            combImageLabel.icon = AppImage.Board[app, size, board]
            combImageLabel.text = ""
            board.forEachChip { c: Chip ->
                c.displayType = app.setting.displayType
                combChipLM.addElement(c)
            }
            comb_updateMark()
            val stat = board.getStat()
            val cMax = board.getCustomMaxStat()
            val oMax = board.getOrigMaxStat()
            val resonance = board.getResonance()
            val pt = board.getPt()
            combDmgStatLabel.text =
                stat.dmg.toString() + " / " + cMax.dmg + if (cMax.dmg == oMax.dmg) "" else " (" + oMax.dmg + ")"
            combBrkStatLabel.text =
                stat.brk.toString() + " / " + cMax.brk + if (cMax.brk == oMax.brk) "" else " (" + oMax.brk + ")"
            combHitStatLabel.text =
                stat.hit.toString() + " / " + cMax.hit + if (cMax.hit == oMax.hit) "" else " (" + oMax.hit + ")"
            combRldStatLabel.text =
                stat.rld.toString() + " / " + cMax.rld + if (cMax.rld == oMax.rld) "" else " (" + oMax.rld + ")"
            combDmgStatLabel.foreground = if (stat.dmg >= cMax.dmg) Color.RED else Color.BLACK
            combBrkStatLabel.foreground = if (stat.brk >= cMax.brk) Color.RED else Color.BLACK
            combHitStatLabel.foreground = if (stat.hit >= cMax.hit) Color.RED else Color.BLACK
            combRldStatLabel.foreground = if (stat.rld >= cMax.rld) Color.RED else Color.BLACK
            combDmgPercLabel.text = (Fn.fPercStr(board.getStatPerc(Stat.DMG))
                    + if (cMax.dmg == oMax.dmg) "" else " (" + Fn.iPercStr(
                Board.getStatPerc(
                    Stat.DMG,
                    stat,
                    oMax
                )
            ) + ")")
            combBrkPercLabel.text = (Fn.fPercStr(board.getStatPerc(Stat.BRK))
                    + if (cMax.brk == oMax.brk) "" else " (" + Fn.iPercStr(
                Board.getStatPerc(
                    Stat.BRK,
                    stat,
                    oMax
                )
            ) + ")")
            combHitPercLabel.text = (Fn.fPercStr(board.getStatPerc(Stat.HIT))
                    + if (cMax.hit == oMax.hit) "" else " (" + Fn.iPercStr(
                Board.getStatPerc(
                    Stat.HIT,
                    stat,
                    oMax
                )
            ) + ")")
            combRldPercLabel.text = (Fn.fPercStr(board.getStatPerc(Stat.RLD))
                    + if (cMax.rld == oMax.rld) "" else " (" + Fn.iPercStr(
                Board.getStatPerc(
                    Stat.RLD,
                    stat,
                    oMax
                )
            ) + ")")
            combDmgPtLabel.text = app.getText(AppText.UNIT_PT, pt.dmg.toString())
            combBrkPtLabel.text = app.getText(AppText.UNIT_PT, pt.brk.toString())
            combHitPtLabel.text = app.getText(AppText.UNIT_PT, pt.hit.toString())
            combRldPtLabel.text = app.getText(AppText.UNIT_PT, pt.rld.toString())
            combDmgPtLabel.foreground = Color.BLACK
            combBrkPtLabel.foreground = Color.BLACK
            combHitPtLabel.foreground = Color.BLACK
            combRldPtLabel.foreground = Color.BLACK
            combDmgResonanceStatLabel.text = "+" + resonance.dmg
            combBrkResonanceStatLabel.text = "+" + resonance.brk
            combHitResonanceStatLabel.text = "+" + resonance.hit
            combRldResonanceStatLabel.text = "+" + resonance.rld
            val chipColor = AppColor.CHIPS[board.getColor()]
            combDmgResonanceStatLabel.foreground = chipColor
            combBrkResonanceStatLabel.foreground = chipColor
            combHitResonanceStatLabel.foreground = chipColor
            combRldResonanceStatLabel.foreground = chipColor
            ticketLabel.text = board.getTicketCount().toString()
            xpLabel.text = Fn.thousandComma(board.xp)
        } else {
            combImageLabel.icon = null
            combImageLabel.text = app.getText(AppText.COMB_DESC)
            combDmgStatLabel.foreground = Color.BLACK
            combDmgStatLabel.text = ""
            combBrkStatLabel.text = ""
            combHitStatLabel.text = ""
            combRldStatLabel.text = ""
            combDmgPercLabel.foreground = Color.BLACK
            combDmgPercLabel.text = ""
            combBrkPercLabel.text = ""
            combHitPercLabel.text = ""
            combRldPercLabel.text = ""
            combDmgPtLabel.foreground = Color.BLACK
            combDmgPtLabel.text = ""
            combBrkPtLabel.text = ""
            combHitPtLabel.text = ""
            combRldPtLabel.text = ""
            combDmgResonanceStatLabel.foreground = Color.BLACK
            combDmgResonanceStatLabel.text = ""
            combBrkResonanceStatLabel.text = ""
            combHitResonanceStatLabel.text = ""
            combRldResonanceStatLabel.text = ""
            ticketLabel.text = "-"
            xpLabel.text = "-"
        }
        invList.repaint()
    }

    private fun comb_updateMark() {
        val combChips = combChipLM.elements()
        while (combChips.hasMoreElements()) {
            val c = combChips.nextElement()
            for (invChip in invChips) {
                if (invChip == c) {
                    c.isMarked = invChip.isMarked
                    break
                }
            }
        }
        combChipList.repaint()
        val combCFs = combFreqLM.elements()
        while (combCFs.hasMoreElements()) {
            val c = combCFs.nextElement()!!.chip
            for (invChip in invChips) {
                if (invChip == c) {
                    c.isMarked = invChip.isMarked
                    break
                }
            }
        }
        combFreqList.repaint()
    }

    private fun comb_result_getChipsFromInv(): List<Chip?> {
        val out: MutableList<Chip?> = mutableListOf()
        val chipEnum = combChipLM.elements()
        while (chipEnum.hasMoreElements()) {
            val c = chipEnum.nextElement()
            for (invChip in invChips) {
                if (invChip == c) {
                    out.add(invChip)
                    break
                }
            }
        }
        return out
    }

    private fun comb_freq_getChipsFromInv(): List<Chip?> {
        val out: MutableList<Chip?> = mutableListOf()
        val cfEnum = combFreqLM.elements()
        while (cfEnum.hasMoreElements()) {
            val c = cfEnum.nextElement()!!.chip
            for (invChip in invChips) {
                if (invChip == c) {
                    out.add(invChip)
                    break
                }
            }
        }
        return out
    }

    private fun comb_openStatDialog() {
        if (!combList.isSelectionEmpty) {
            val board = combList.selectedValue
            StatDialog.open(app, board)
        }
    }

    private fun comb_result_mark() {
        if (!combList.isSelectionEmpty) {
            val chipList = comb_result_getChipsFromInv()
            // Continue
            var retval = JOptionPane.showConfirmDialog(
                this,
                app.getText(AppText.COMB_MARK_CONTINUE_BODY),
                app.getText(AppText.COMB_MARK_CONTINUE_TITLE),
                JOptionPane.YES_NO_OPTION
            )
            // If some chips are missing in the inventory
            if (retval == JOptionPane.YES_OPTION && combChipLM.size() != chipList.size) {
                retval = JOptionPane.showConfirmDialog(
                    this,
                    app.getText(AppText.COMB_DNE_BODY), app.getText(AppText.COMB_DNE_TITLE),
                    JOptionPane.YES_NO_OPTION
                )
            }
            // Mark
            if (retval == JOptionPane.YES_OPTION) {
                chipList.forEach(Consumer { c: Chip? -> c!!.isMarked = true })
                invList.repaint()
                comb_updateMark()
                invStat_enableSave()
            }
        }
    }

    private fun comb_freq_mark() {
        if (!combFreqLM.isEmpty) {
            val chipList = comb_freq_getChipsFromInv()
            // Continue
            var retval = JOptionPane.showConfirmDialog(
                this,
                app.getText(AppText.COMB_MARK_CONTINUE_BODY),
                app.getText(AppText.COMB_MARK_CONTINUE_TITLE),
                JOptionPane.YES_NO_OPTION
            )
            // Some chips are missing in the inventory
            if (retval == JOptionPane.YES_OPTION && combFreqLM.size() != chipList.size) {
                retval = JOptionPane.showConfirmDialog(
                    this,
                    app.getText(AppText.COMB_DNE_BODY), app.getText(AppText.COMB_DNE_TITLE),
                    JOptionPane.YES_NO_OPTION
                )
            }
            // Mark
            if (retval == JOptionPane.YES_OPTION) {
                chipList.forEach(Consumer { c: Chip? -> c!!.isMarked = true })
                invList.repaint()
                comb_updateMark()
                invStat_enableSave()
            }
        }
    }

    private fun comb_result_openTagDialog() {
        if (!combList.isSelectionEmpty) {
            val chipList = comb_result_getChipsFromInv()
            var retval = JOptionPane.YES_OPTION
            if (combChipLM.size() != chipList.size) {
                retval = JOptionPane.showConfirmDialog(
                    this,
                    app.getText(AppText.COMB_DNE_BODY), app.getText(AppText.COMB_DNE_TITLE),
                    JOptionPane.YES_NO_OPTION
                )
            }
            if (retval == JOptionPane.YES_OPTION) {
                openDialog(TagDialog.getInstance(app, chipList))
            }
        }
    }

    private fun comb_freq_openTagDialog() {
        if (!combFreqLM.isEmpty) {
            val chipList = comb_freq_getChipsFromInv()
            var retval = JOptionPane.YES_OPTION
            if (combFreqLM.size() != chipList.size) {
                retval = JOptionPane.showConfirmDialog(
                    this,
                    app.getText(AppText.COMB_DNE_BODY), app.getText(AppText.COMB_DNE_TITLE),
                    JOptionPane.YES_NO_OPTION
                )
            }
            if (retval == JOptionPane.YES_OPTION) {
                openDialog(TagDialog.getInstance(app, chipList))
            }
        }
    }

    private fun comb_ensureInvListIndexIsVisible_combChipList() {
        if (!combChipList.isSelectionEmpty) {
            val selected = combChipList.selectedValue
            for (i in 0 until invLM.size()) {
                val invChip = invLM[i]
                if (selected == invChip) {
                    invList.ensureIndexIsVisible(i)
                    break
                }
            }
        }
    }

    private fun comb_ensureInvListIndexIsVisible_combChipFreqList() {
        if (!combFreqList.isSelectionEmpty) {
            val selected = combFreqList.selectedValue
            for (i in 0 until invLM.size()) {
                val invChip = invLM[i]
                if (selected!!.chip == invChip) {
                    invList.ensureIndexIsVisible(i)
                    break
                }
            }
        }
    }

    private fun comb_updateFreqLabel() {
        if (!combFreqList.isSelectionEmpty) {
            val selected = combFreqList.selectedValue
            combFreqLabel.text =
                Fn.fPercStr(selected!!.freq) + " (" + app.getText(AppText.UNIT_COUNT, selected.count) + ")"
        } else {
            combFreqLabel.text = "-"
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Process Methods">
    private fun process_toggleStartPause() {
        when (assembler.status) {
            Assembler.Status.STOPPED -> process_start()
            Assembler.Status.RUNNING -> process_pause()
            Assembler.Status.PAUSED -> process_resume()
        }
    }

    private var calcSetting: CalcSetting? = null
    private var calcExtraSetting: CalcExtraSetting? = null
    private var progress: Progress? = null
    private fun process_start() {
        // Check for the validity of all inventory chips
        val elements = invLM.elements()
        while (elements.hasMoreElements()) {
            val chip = elements.nextElement()
            if (!chip.isPtValid()) {
                JOptionPane.showMessageDialog(
                    this,
                    app.getText(AppText.COMB_ERROR_STAT_BODY),
                    app.getText(AppText.COMB_ERROR_STAT_TITLE),
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }
        }
        val boardName = boardName
        val boardStar = boardStar

        // init
        var start = true
        var calcMode: Int = CalcExtraSetting.CALCMODE_DICTIONARY
        var alt = false
        val minType = assembler.getMinType(boardName, boardStar, false)
        if (app.setting.advancedSetting) {
            // Partial option
            if (assembler.hasPartial(boardName, boardStar)) {
                // Query
                val options = arrayOf(
                    app.getText(AppText.COMB_OPTION_M2_0),
                    app.getText(AppText.COMB_OPTION_M2_1),
                    app.getText(AppText.COMB_OPTION_M2_2),
                    app.getText(AppText.ACTION_CANCEL)
                )
                val response = JOptionPane.showOptionDialog(
                    this,
                    app.getText(AppText.COMB_OPTION_M2_DESC, options[0], options[1]),
                    app.getText(AppText.COMB_OPTION_TITLE),
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]
                )
                // Response
                start = response != JOptionPane.CLOSED_OPTION && response <= 2
                calcMode =
                    if (response <= 1) CalcExtraSetting.CALCMODE_DICTIONARY else CalcExtraSetting.CALCMODE_ALGX
                alt = response == 0
            } //
            else {
                // Check if any chip size is smaller than dictionary chip size
                val elements = invLM.elements()
                while (elements.hasMoreElements() && calcMode == CalcExtraSetting.CALCMODE_DICTIONARY) {
                    val c = elements.nextElement()
                    if (!c.typeGeq(minType)) {
                        calcMode = CalcExtraSetting.CALCMODE_ALGX
                        break
                    }
                }
                // Query
                if (calcMode == CalcExtraSetting.CALCMODE_ALGX) {
                    val combOption0Text: String = AppText.textType(app, minType)
                    val options = arrayOf(
                        app.getText(AppText.COMB_OPTION_DEFAULT_0, combOption0Text),
                        app.getText(AppText.COMB_OPTION_DEFAULT_1),
                        app.getText(AppText.ACTION_CANCEL)
                    )
                    val response = JOptionPane.showOptionDialog(
                        this,
                        app.getText(AppText.COMB_OPTION_DEFAULT_DESC, options[0], combOption0Text),
                        app.getText(AppText.COMB_OPTION_TITLE),
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]
                    )
                    // Response
                    start = response != JOptionPane.CLOSED_OPTION && response <= 1
                    calcMode =
                        if (response == 0) CalcExtraSetting.CALCMODE_DICTIONARY else CalcExtraSetting.CALCMODE_ALGX
                }
            }
        } else if (boardStar == 5 && !setting_isPresetFilter()) {
            val retval = JOptionPane.showOptionDialog(
                this,
                app.getText(AppText.COMB_OPTION_FILTER_DESC),
                app.getText(AppText.COMB_OPTION_TITLE),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null
            )
            start = retval != JOptionPane.CLOSED_OPTION && retval != JOptionPane.CANCEL_OPTION
            if (retval == JOptionPane.YES_OPTION) {
                setting_applyPresetFilter()
            }
        }

        // If preset DNE
        if (!assembler.btExists(boardName, boardStar, alt)) {
            calcMode = CalcExtraSetting.CALCMODE_ALGX
        }
        if (start) {
            // Filter and deep-copy chips
            val candidates: MutableList<Chip> = mutableListOf()
            val elements = invLM.elements()
            while (elements.hasMoreElements()) {
                val chip = elements.nextElement()
                val colorMatch = !app.setting.colorMatch || Board.getColor(boardName) == chip.color
                val sizeMatch = calcMode == CalcExtraSetting.CALCMODE_ALGX || chip.typeGeq(
                    minType
                )
                val markMatchNeg = 0 < app.setting.boardMarkMax || !chip.isMarked
                val markMatchPos =
                    app.setting.boardMarkMin < Board.getCellCount(boardName, boardStar) || chip.isMarked
                if (colorMatch && sizeMatch && markMatchNeg && markMatchPos) {
                    candidates.add(Chip(chip))
                }
            }
            if (app.setting.maxLevel) {
                candidates.forEach(Consumer { obj: Chip -> obj.setMaxLevel() })
            }
            val bs = app.setting.board
            val stat: Stat?
            val pt: Stat?
            when (bs.getStatMode(boardName, boardStar)) {
                BoardSetting.MAX_PRESET -> {
                    val presetIndex = bs.getPresetIndex(boardName, boardStar)
                    stat = BoardSetting.getPreset(boardName, boardStar, presetIndex)!!.stat
                    pt = BoardSetting.getPreset(boardName, boardStar, presetIndex)!!.pt
                }
                BoardSetting.MAX_STAT -> {
                    stat = bs.getStat(boardName, boardStar)
                    pt = bs.getPt(boardName, boardStar)
                }
                else -> {
                    stat = Board.getMaxStat(boardName, boardStar)
                    pt = Board.getMaxPt(boardName, boardStar)
                }
            }
            calcSetting = CalcSetting(
                boardName,
                boardStar,
                app.setting.maxLevel,
                app.setting.rotation,
                app.setting.symmetry,
                stat,
                pt
            )
            calcExtraSetting = CalcExtraSetting(
                calcMode, if (alt) 1 else 0,
                app.setting.colorMatch,
                app.setting.boardMarkMin, app.setting.boardMarkMax,
                app.setting.boardMarkType, app.setting.boardSortType, candidates
            )
            progress = Progress(app.setting.boardSortType)
            process_init()
            process_resume()
        }
    }

    private fun process_init() {
        assembler[calcSetting!!, calcExtraSetting!!] = progress!!
        time = System.currentTimeMillis()
        pauseTime = 0
        process_updateProgress(true)
        combStopButton.isVisible = true
    }

    private fun process_setUI(status: Assembler.Status) {
        when (status) {
            Assembler.Status.RUNNING -> loadingLabel.icon = AppImage.LOADING
            Assembler.Status.PAUSED -> loadingLabel.icon = AppImage.PAUSED
            Assembler.Status.STOPPED -> {
                loadingLabel.icon = null
                setting_resetBoard()
            }
        }
        if (status != Assembler.Status.RUNNING) {
            prevDoneTime = 0
            doneTimes.clear()
        }
        combStartPauseButton.icon =
            if (status == Assembler.Status.RUNNING) AppImage.COMB_PAUSE else AppImage.COMB_START
        combStopButton.isVisible = status != Assembler.Status.STOPPED
        boardNameComboBox.isEnabled = status == Assembler.Status.STOPPED
        boardStarComboBox.isEnabled = status == Assembler.Status.STOPPED
        settingButton.isEnabled = status == Assembler.Status.STOPPED
        researchButton.isEnabled = status == Assembler.Status.STOPPED
        combOpenButton.isEnabled = status == Assembler.Status.STOPPED
        combSaveButton.isEnabled =
            status != Assembler.Status.RUNNING && progress != null && progress!!.getBoardSize() > 0
        process_updateProgress(status != Assembler.Status.RUNNING)
    }

    private fun calcTimer() {
        if (assembler.status == Assembler.Status.RUNNING) {
            process_updateProgress(false)
        }
    }

    private fun process_pause() {
        pauseTime = System.currentTimeMillis()
        calcTimer.stop()
        process_setUI(Assembler.Status.PAUSED)
        assembler.pause()
    }

    private fun process_resume() {
        if (0 < pauseTime) {
            time += System.currentTimeMillis() - pauseTime
        }
        pauseTime = 0
        calcTimer.start()
        process_setUI(Assembler.Status.RUNNING)
        assembler.resume()
    }

    private fun process_stop() {
        if (0 < pauseTime) {
            time += System.currentTimeMillis() - pauseTime
        }
        pauseTime = 0
        calcTimer.stop()
        if (progress != null) {
            calcExtraSetting!!.calcMode = CalcExtraSetting.CALCMODE_FINISHED
        }
        process_setUI(Assembler.Status.STOPPED)
        assembler.stop()
    }

    private fun process_updateProgress(forceUpdate: Boolean) {
        process_setCombLabelText()
        process_setElapsedTime()
        process_refreshCombListModel(forceUpdate)
    }

    private fun process_setCombLabelText() {
        if (progress != null && calcExtraSetting!!.calcMode == CalcExtraSetting.CALCMODE_FINISHED && 0 == progress!!.nComb) {
            combLabel.text = app.getText(AppText.COMB_NONEFOUND)
        } else if (progress != null && 0 <= progress!!.nComb) {
            combLabel.text = Fn.thousandComma(progress!!.nComb)
        } else {
            combLabel.text = ""
        }
    }

    private fun process_setElapsedTime() {
        val sb = StringBuilder()
        val sec = (System.currentTimeMillis() - time) / 1000
        sb.append(Fn.getTime(sec))
        var warn = false
        if (doneTimes.isNotEmpty()) {
            val avg = doneTimes.stream().mapToLong { v: Long? -> v!! }.sum() / doneTimes.size
            val remaining = avg * (progress!!.nTotal - progress!!.nDone) / 1000
            warn = 60 * 60 < remaining
            sb.append(" (").append(app.getText(AppText.COMB_REMAINING, Fn.getTime(remaining))).append(")")
        }
        timeWarningButton.isVisible = app.setting.advancedSetting && warn
        timeLabel.text = sb.toString()
    }

    private fun process_refreshCombListModel(forceUpdate: Boolean) {
        SwingUtilities.invokeLater {
            try {
                if (forceUpdate || assembler.boardsUpdated()) {
                    var selectedBoard: Board? = null
                    var selectedChipID: String? = null
                    if (!combList.isSelectionEmpty) {
                        selectedBoard = combList.selectedValue
                    }
                    if (!combFreqList.isSelectionEmpty) {
                        selectedChipID = combFreqList.selectedValue!!.chip!!.id
                    }
                    val ar = assembler.getResult()
                    val exist = ar.freqs.isNotEmpty()
                    combLM.clear()
                    ar.boards.forEach(Consumer { element: Board? -> combLM.addElement(element) })
                    combFreqLM.clear()
                    ar.freqs.forEach(Consumer { element: ChipFreq? -> combFreqLM.addElement(element) })
                    combFreqMarkButton.isEnabled = exist
                    combFreqTagButton.isEnabled = exist
                    combList.setSelectedValue(selectedBoard, true)
                    if (selectedChipID != null) {
                        var i = 0
                        val size = combFreqLM.size()
                        var found = false
                        while (!found && i < size) {
                            if (selectedChipID == combFreqLM[i]!!.chip!!.id) {
                                combFreqList.selectedIndex = i
                                combFreqList.ensureIndexIsVisible(i)
                                combFreqList.repaint()
                                found = true
                            }
                            i++
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }

    // From Combinator
    private fun process_setProgBar(n: Int, max: Int) {
        combProgressBar.maximum = max
        combProgressBar.value = n
    }

    private fun process_showImage(template: BoardTemplate) {
        SwingUtilities.invokeLater {
            if (app.setting.showProgImage && assembler.status == Assembler.Status.RUNNING) {
                boardImageLabel.icon = AppImage.Board[app, boardImageLabel.width, template.getMatrix()]
                boardImageLabel.repaint()
            }
        }
    }

    private fun process_prog(prog: Int) {
        SwingUtilities.invokeLater {
            val doneTime = System.currentTimeMillis()
            if (prevDoneTime != 0L) {
                val t = doneTime - prevDoneTime
                if (doneTimes.size == SIZE_DONETIME) {
                    doneTimes.removeAt(0)
                }
                doneTimes.add(t)
            }
            prevDoneTime = doneTime
            combProgressBar.value = prog
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Setting File Methods">
    private fun settingFile_load() {
        if (!settingFile_loading) {
            settingFile_loading = true
            refreshDisplay()
            pool_setOrder(app.setting.poolOrder)
            poolStarComboBox.selectedIndex = 5 - app.setting.poolStar
            setPoolPanelVisible(app.setting.poolPanelVisible)
            display_setType(app.setting.displayType)
            showProgImageCheckBox.isSelected = app.setting.showProgImage
            settingFile_loading = false
        }
    }

    fun settingFile_save() {
        if (!settingFile_loading) {
            IO.saveSettings(app.setting)
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inventory File Methods">
    private fun invFile_confirmSave(): Boolean {
        if (invSaveButton.isEnabled) {
            val retval = JOptionPane.showConfirmDialog(
                this,
                app.getText(AppText.FILE_SAVE_BODY), app.getText(AppText.FILE_SAVE_TITLE),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
            )
            if (retval == JOptionPane.CANCEL_OPTION) {
                return false
            } else if (retval == JOptionPane.YES_OPTION) {
                invFile_save()
            }
        }
        return true
    }

    private fun invFile_new() {
        if (invFile_confirmSave()) {
            invFile_clear()
        }
    }

    private fun invFile_clear() {
        invFile_path = ""
        fileTextArea.text = ""
        inv_chipsClear()
        invSaveButton.isEnabled = false
    }

    private fun invFile_open() {
        if (invFile_confirmSave()) {
            val retval = iofc.showOpenDialog(this)
            if (retval == JFileChooser.APPROVE_OPTION) {
                invFile_path = iofc.selectedFile.path
                fileTextArea.text = iofc.selectedFile.name
                inv_chipsLoad(
                    if (invFile_path.endsWith("." + IO.EXT_INVENTORY)) IO.loadInventory(invFile_path) else JsonFilterDialog.filter(
                        app, this, JsonParser.readFile(invFile_path)
                    )
                )
                invSaveButton.isEnabled = false
            }
        }
    }

    private fun invFile_save() {
        if (invSaveButton.isEnabled) {
            if (invFile_path.isEmpty()) {
                invFile_saveAs()
            } else {
                IO.saveInventory(invFile_path, invChips)
                invSaveButton.isEnabled = false
            }
        }
    }

    private fun invFile_saveAs() {
        val retval = isfc.showSaveDialog(this)
        if (retval == JFileChooser.APPROVE_OPTION) {
            var selectedPath = isfc.selectedFile.path
            var fileName = isfc.selectedFile.name

            // Extension
            if (!selectedPath.endsWith("." + IO.EXT_INVENTORY)) {
                selectedPath += "." + IO.EXT_INVENTORY
                fileName += "." + IO.EXT_INVENTORY
            }

            // Overwrite
            var confirmed = true
            if (isfc.selectedFile.exists()) {
                val option = JOptionPane.showConfirmDialog(
                    this,
                    app.getText(AppText.FILE_OVERWRITE_BODY),
                    app.getText(AppText.FILE_OVERWRITE_TITLE),
                    JOptionPane.YES_NO_OPTION
                )
                if (option != JOptionPane.YES_OPTION) {
                    confirmed = false
                }
            }

            // Save
            if (confirmed) {
                invFile_path = selectedPath
                IO.saveInventory(invFile_path, invChips)
                fileTextArea.text = fileName
                invSaveButton.isEnabled = false
            }
        }
    }

    private fun invFile_openImageDialog() {
        ImageDialog.getData(app).forEach { c -> this.inv_chipsAdd(c) }
    }

    private fun invFile_openProxyDialog() {
        if (invFile_confirmSave()) {
            val chips: List<Chip>? = ProxyDialog.extract(app)
            if (chips != null) {
                invFile_clear()
                inv_chipsLoad(chips)
                invStat_enableSave()
            }
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Progress File Methods">
    private fun progFile_open() {
        val retval = cfc.showOpenDialog(this)
        if (retval == JFileChooser.APPROVE_OPTION) {
            val path = cfc.selectedFile.path
            val pf = IO.loadProgressFile(path, invChips)
            calcSetting = pf!!.cs
            calcExtraSetting = pf.ces
            progress = pf.p
            combSaveButton.isEnabled = false
            boardNameComboBox.selectedItem = calcSetting!!.boardName
            boardStarComboBox.selectedIndex = 5 - calcSetting!!.boardStar
            if (calcExtraSetting!!.calcMode != CalcExtraSetting.CALCMODE_FINISHED) {
                val setting = app.setting
                setting.maxLevel = calcSetting!!.maxLevel
                setting.rotation = calcSetting!!.rotation
                setting.colorMatch = calcExtraSetting!!.matchColor
                setting.boardMarkMin = calcExtraSetting!!.markMin
                setting.boardMarkMax = calcExtraSetting!!.markMax
                setting.boardMarkType = calcExtraSetting!!.markType
                setting.boardSortType = calcExtraSetting!!.sortType
            }
            process_init()
            if (calcExtraSetting!!.calcMode != CalcExtraSetting.CALCMODE_FINISHED) {
                process_pause()
            }
        }
    }

    private fun progFile_saveAs() {
        if (combSaveButton.isEnabled) {
            val retval = cfc.showSaveDialog(this)
            if (retval == JFileChooser.APPROVE_OPTION) {
                var path = cfc.selectedFile.path

                // Extension
                if (!path.endsWith("." + IO.EXT_COMBINATION)) {
                    path += "." + IO.EXT_COMBINATION
                }

                // Save
                IO.saveProgressFile(path, ProgressFile(calcSetting, calcExtraSetting, progress))
                combSaveButton.isEnabled = false
            }
        }
    }
    // </editor-fold>
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    private fun formWindowClosing(evt: WindowEvent) { //GEN-FIRST:event_formWindowClosing
        if (invFile_confirmSave()) {
            process_stop()
            blinkTimer.stop()
            dispose()
            exitProcess(0)
        }
    } //GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var addButton: JButton
    private var boardImageLabel: JLabel
    private var boardNameComboBox: JComboBox<String>
    private var boardStarComboBox: JComboBox<String>
    private var combBrkPanel: JPanel
    private var combBrkPercLabel: JLabel
    private var combBrkPtLabel: JLabel
    private var combBrkResonanceStatLabel: JLabel
    private var combBrkStatLabel: JLabel
    private var combBrkTextLabel: JLabel
    private var combChipList: JList<Chip>
    private var combChipListPanel: JPanel
    private var combChipListScrollPane: JScrollPane
    private var combDmgPanel: JPanel
    private var combDmgPercLabel: JLabel
    private var combDmgPtLabel: JLabel
    private var combDmgResonanceStatLabel: JLabel
    private var combDmgStatLabel: JLabel
    private var combDmgTextLabel: JLabel
    private var combFreqLabel: JLabel
    private var combFreqList: JList<ChipFreq?>
    private var combFreqListPanel: JPanel
    private var combFreqListScrollPane: JScrollPane
    private var combFreqMarkButton: JButton
    private var combFreqPanel: JPanel
    private var combFreqTagButton: JButton
    private var combHitPanel: JPanel
    private var combHitPercLabel: JLabel
    private var combHitPtLabel: JLabel
    private var combHitResonanceStatLabel: JLabel
    private var combHitStatLabel: JLabel
    private var combHitTextLabel: JLabel
    private var combImageLabel: JLabel
    private var combImagePanel: JPanel
    private var combInfoPanel: JPanel
    private var combLBPanel: JPanel
    private var combLTPanel: JPanel
    private var combLabel: JLabel
    private var combLeftPanel: JPanel
    private var combList: JList<Board>
    private var combListPanel: JPanel
    private var combMarkButton: JButton
    private var combOpenButton: JButton
    private var combProgressBar: JProgressBar
    private var combRBPanel: JPanel
    private var combRTPanel: JPanel
    private var combResultPanel: JPanel = JPanel()
    private var combRightPanel: JPanel
    private var combRldPanel: JPanel
    private var combRldPercLabel: JLabel
    private var combRldPtLabel: JLabel
    private var combRldResonanceStatLabel: JLabel
    private var combRldStatLabel: JLabel
    private var combRldTextLabel: JLabel
    private var combSaveButton: JButton
    private var combStartPauseButton: JButton
    private var combStatPanel: JPanel
    private var combStopButton: JButton
    private var combTabbedPane: JTabbedPane
    private var combTagButton: JButton
    private var combWarningButton: JButton
    private var displaySettingButton: JButton
    private var displayTypeButton: JButton
    private var donationButton: JButton
    private var enhancementTextLabel: JLabel
    private var fileTAPanel: JPanel
    private var fileTextArea: JTextArea
    private var filterButton: JButton
    private var filterChipCountLabel: JLabel
    private var helpButton: JButton
    private var imageButton: JButton
    private var invApplyButton: JButton
    private var invBPanel: JPanel
    private var invBrkComboBox: JComboBox<String>
    private var invBrkPanel: JPanel
    private var invBrkPtLabel: JLabel
    private var invBrkTextLabel: JLabel
    private var invColorButton: JButton
    private var invDelButton: JButton
    private var invDmgComboBox: JComboBox<String>
    private var invDmgPanel: JPanel
    private var invDmgPtLabel: JLabel
    private var invDmgTextLabel: JLabel
    private var invHitComboBox: JComboBox<String>
    private var invHitPanel: JPanel
    private var invHitPtLabel: JLabel
    private var invHitTextLabel: JLabel
    private var invLevelLabel: JLabel
    private var invLevelSlider: JSlider
    private var invList: JList<Chip>
    private var invListPanel: JPanel
    private var invListScrollPane: JScrollPane
    private var invMarkCheckBox: JCheckBox
    private var invNewButton: JButton
    private var invOpenButton: JButton
    private var invPanel: JPanel
    private var invRldComboBox: JComboBox<String>
    private var invRldPanel: JPanel
    private var invRldPtLabel: JLabel
    private var invRldTextLabel: JLabel
    private var invRotLButton: JButton
    private var invRotRButton: JButton
    private var invSaveAsButton: JButton
    private var invSaveButton: JButton
    private var invSortOrderButton: JButton
    private var invSortTypeComboBox: JComboBox<String?>
    private var invStarComboBox: JComboBox<String>
    private var invStatPanel: JPanel
    private var invTPanel: JPanel
    private var invTagButton: JButton
    private var jPanel12: JPanel
    private var jPanel13: JPanel
    private var jPanel14: JPanel
    private var jPanel17: JPanel
    private var jPanel18: JPanel
    private var jPanel2: JPanel
    private var jPanel3: JPanel
    private var jPanel4: JPanel
    private var jPanel5: JPanel
    private var jPanel6: JPanel
    private var jPanel7: JPanel
    private var jPanel8: JPanel
    private var jPanel9: JPanel
    private var jScrollPane1: JScrollPane
    private var jScrollPane4: JScrollPane
    private var legendEquippedLabel: JLabel
    private var legendRotatedLabel: JLabel
    private var loadingLabel: JLabel
    private var piButtonPanel: JPanel
    private var poolBPanel: JPanel
    private var poolColorButton: JButton
    private var poolControlPanel: JPanel
    private var poolList: JList<Chip>
    private var poolListPanel: JPanel
    private var poolListScrollPane: JScrollPane
    private var poolPanel: JPanel
    private var poolReadPanel: JPanel
    private var poolRotLButton: JButton
    private var poolRotRButton: JButton
    private var poolSortButton: JButton
    private var poolStarComboBox: JComboBox<String>
    private var poolTPanel: JPanel
    private var poolWindowButton: JButton
    private var proxyButton: JButton
    private var researchButton: JButton
    private var settingButton: JButton
    private var showProgImageCheckBox: JCheckBox
    private var statButton: JButton
    private var ticketLabel: JLabel
    private var ticketTextLabel: JLabel
    private var timeLabel: JLabel
    private var timeWarningButton: JButton
    private var tipLabel: JLabel
    private var xpLabel: JLabel
    private var xpTextLabel: JLabel // End of variables declaration//GEN-END:variables

    companion object {
        /* STATIC */ // UI
        private const val BORDERSIZE = 3

        // Chip Stat
        private const val FOCUSED_NONE = -1
        private const val FOCUSED_DMG = 0

        // private static final int FOCUSED_BRK = 1;
        // private static final int FOCUSED_HIT = 2;
        // private static final int FOCUSED_RLD = 3;
        private const val INPUT_BUFFER_SIZE = 3

        // Sort
        private const val SORT_NONE = 0
        private const val SORT_SIZE = 1
        private const val SORT_LEVEL = 2
        private const val SORT_STAR = 3
        private const val SORT_DMG = 4
        private const val SORT_BRK = 5
        private const val SORT_HIT = 6
        private const val SORT_RLD = 7

        // Calculator
        private const val SIZE_DONETIME = 100

        // Setting
        private val ASCENDING: Boolean = Setting.ASCENDING
        private val DESCENDING: Boolean = Setting.DESCENDING
        private val DISPLAY_STAT: Int = Setting.DISPLAY_STAT
    }

    // <editor-fold defaultstate="collapsed" desc="Constructor Methods">
    init {
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        combChipListPanel = JPanel()
        combChipListScrollPane = JScrollPane()
        combChipList = JList()
        combMarkButton = JButton()
        combTagButton = JButton()
        combImagePanel = JPanel()
        combImageLabel = JLabel()
        combStatPanel = JPanel()
        statButton = JButton()
        combDmgPanel = JPanel()
        combDmgTextLabel = JLabel()
        combDmgPercLabel = JLabel()
        combDmgResonanceStatLabel = JLabel()
        combDmgPtLabel = JLabel()
        combDmgStatLabel = JLabel()
        combBrkPanel = JPanel()
        combBrkTextLabel = JLabel()
        combBrkStatLabel = JLabel()
        combBrkPtLabel = JLabel()
        combBrkResonanceStatLabel = JLabel()
        combBrkPercLabel = JLabel()
        combHitPanel = JPanel()
        combHitTextLabel = JLabel()
        combHitStatLabel = JLabel()
        combHitPtLabel = JLabel()
        combHitResonanceStatLabel = JLabel()
        combHitPercLabel = JLabel()
        combRldPanel = JPanel()
        combRldTextLabel = JLabel()
        combRldStatLabel = JLabel()
        combRldPtLabel = JLabel()
        combRldResonanceStatLabel = JLabel()
        combRldPercLabel = JLabel()
        combInfoPanel = JPanel()
        ticketLabel = JLabel()
        ticketTextLabel = JLabel()
        xpTextLabel = JLabel()
        xpLabel = JLabel()
        combFreqPanel = JPanel()
        combFreqLabel = JLabel()
        combFreqListPanel = JPanel()
        combFreqListScrollPane = JScrollPane()
        combFreqList = JList()
        combFreqMarkButton = JButton()
        combFreqTagButton = JButton()
        poolPanel = JPanel()
        poolTPanel = JPanel()
        helpButton = JButton()
        displaySettingButton = JButton()
        donationButton = JButton()
        poolBPanel = JPanel()
        poolControlPanel = JPanel()
        poolRotRButton = JButton()
        poolRotLButton = JButton()
        poolSortButton = JButton()
        jPanel13 = JPanel()
        poolColorButton = JButton()
        poolStarComboBox = JComboBox()
        poolListPanel = JPanel()
        poolListScrollPane = JScrollPane()
        poolList = JList()
        poolReadPanel = JPanel()
        imageButton = JButton()
        proxyButton = JButton()
        piButtonPanel = JPanel()
        poolWindowButton = JButton()
        addButton = JButton()
        invPanel = JPanel()
        invTPanel = JPanel()
        invNewButton = JButton()
        invOpenButton = JButton()
        invSaveButton = JButton()
        invSaveAsButton = JButton()
        fileTAPanel = JPanel()
        jScrollPane1 = JScrollPane()
        fileTextArea = JTextArea()
        invBPanel = JPanel()
        jPanel9 = JPanel()
        jPanel12 = JPanel()
        filterChipCountLabel = JLabel()
        invSortTypeComboBox = JComboBox()
        invSortOrderButton = JButton()
        filterButton = JButton()
        displayTypeButton = JButton()
        invStatPanel = JPanel()
        invApplyButton = JButton()
        jPanel6 = JPanel()
        invStarComboBox = JComboBox()
        invColorButton = JButton()
        jPanel2 = JPanel()
        invDmgPanel = JPanel()
        invDmgTextLabel = JLabel()
        invDmgComboBox = JComboBox()
        invDmgPtLabel = JLabel()
        invBrkPanel = JPanel()
        invBrkTextLabel = JLabel()
        invBrkComboBox = JComboBox()
        invBrkPtLabel = JLabel()
        invHitPanel = JPanel()
        invHitTextLabel = JLabel()
        invHitComboBox = JComboBox()
        invHitPtLabel = JLabel()
        invRldPanel = JPanel()
        invRldTextLabel = JLabel()
        invRldComboBox = JComboBox()
        invRldPtLabel = JLabel()
        jPanel5 = JPanel()
        enhancementTextLabel = JLabel()
        invLevelSlider = JSlider()
        invLevelLabel = JLabel()
        jPanel7 = JPanel()
        invRotLButton = JButton()
        invRotRButton = JButton()
        invDelButton = JButton()
        invMarkCheckBox = JCheckBox()
        invTagButton = JButton()
        jPanel14 = JPanel()
        invListPanel = JPanel()
        invListScrollPane = JScrollPane()
        invList = JList()
        combLeftPanel = JPanel()
        combLTPanel = JPanel()
        settingButton = JButton()
        jPanel8 = JPanel()
        boardNameComboBox = JComboBox()
        boardStarComboBox = JComboBox()
        combLBPanel = JPanel()
        jPanel4 = JPanel()
        combLabel = JLabel()
        combWarningButton = JButton()
        combListPanel = JPanel()
        jScrollPane4 = JScrollPane()
        combList = JList()
        researchButton = JButton()
        jPanel18 = JPanel()
        legendEquippedLabel = JLabel()
        legendRotatedLabel = JLabel()
        combRightPanel = JPanel()
        combRTPanel = JPanel()
        combStopButton = JButton()
        loadingLabel = JLabel()
        boardImageLabel = JLabel()
        showProgImageCheckBox = JCheckBox()
        combStartPauseButton = JButton()
        combRBPanel = JPanel()
        jPanel3 = JPanel()
        combSaveButton = JButton()
        combOpenButton = JButton()
        jPanel17 = JPanel()
        timeLabel = JLabel()
        timeWarningButton = JButton()
        combTabbedPane = JTabbedPane()
        combProgressBar = JProgressBar()
        tipLabel = JLabel()
        combChipListPanel.isFocusable = false
        combChipListScrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        combChipListScrollPane.isFocusable = false
        combChipListScrollPane.preferredSize = Dimension(100, 100)
        combChipList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        combChipList.isFocusable = false
        combChipList.layoutOrientation = JList.HORIZONTAL_WRAP
        combChipList.visibleRowCount = -1
        combChipListScrollPane.setViewportView(combChipList)
        combMarkButton.text = "mark"
        combMarkButton.isEnabled = false
        combMarkButton.isFocusable = false
        combMarkButton.margin = Insets(2, 2, 2, 2)
        combTagButton.text = "tag"
        combTagButton.isEnabled = false
        combTagButton.isFocusable = false
        combTagButton.margin = Insets(2, 2, 2, 2)
        val combChipListPanelLayout = GroupLayout(combChipListPanel)
        combChipListPanel.layout = combChipListPanelLayout
        combChipListPanelLayout.setHorizontalGroup(
            combChipListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combChipListScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combTagButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combMarkButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combChipListPanelLayout.setVerticalGroup(
            combChipListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combChipListPanelLayout.createSequentialGroup()
                        .addComponent(
                            combChipListScrollPane,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(combMarkButton)
                        .addGap(0, 0, 0)
                        .addComponent(combTagButton)
                )
        )
        combImagePanel.isFocusable = false
        combImagePanel.preferredSize = Dimension(175, 175)
        combImageLabel.horizontalAlignment = SwingConstants.CENTER
        combImageLabel.border = BorderFactory.createEtchedBorder()
        combImageLabel.isFocusable = false
        val combImagePanelLayout = GroupLayout(combImagePanel)
        combImagePanel.layout = combImagePanelLayout
        combImagePanelLayout.setHorizontalGroup(
            combImagePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combImageLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combImagePanelLayout.setVerticalGroup(
            combImagePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combImageLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combStatPanel.isFocusable = false
        statButton.text = "detail"
        statButton.isEnabled = false
        statButton.isFocusable = false
        combDmgTextLabel.horizontalAlignment = SwingConstants.CENTER
        combDmgTextLabel.text = "D"
        combDmgTextLabel.isFocusable = false
        combDmgTextLabel.horizontalTextPosition = SwingConstants.CENTER
        combDmgTextLabel.verticalTextPosition = SwingConstants.TOP
        combDmgPercLabel.horizontalAlignment = SwingConstants.CENTER
        combDmgPercLabel.border = BorderFactory.createEtchedBorder()
        combDmgPercLabel.isFocusable = false
        combDmgPercLabel.preferredSize = Dimension(110, 22)
        combDmgResonanceStatLabel.horizontalAlignment = SwingConstants.CENTER
        combDmgResonanceStatLabel.border = BorderFactory.createEtchedBorder()
        combDmgResonanceStatLabel.isFocusable = false
        combDmgResonanceStatLabel.preferredSize = Dimension(50, 22)
        combDmgPtLabel.horizontalAlignment = SwingConstants.CENTER
        combDmgPtLabel.border = BorderFactory.createEtchedBorder()
        combDmgPtLabel.isFocusable = false
        combDmgPtLabel.preferredSize = Dimension(50, 22)
        combDmgStatLabel.horizontalAlignment = SwingConstants.CENTER
        combDmgStatLabel.border = BorderFactory.createEtchedBorder()
        combDmgStatLabel.isFocusable = false
        combDmgStatLabel.preferredSize = Dimension(110, 22)
        val combDmgPanelLayout = GroupLayout(combDmgPanel)
        combDmgPanel.layout = combDmgPanelLayout
        combDmgPanelLayout.setHorizontalGroup(
            combDmgPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, combDmgPanelLayout.createSequentialGroup()
                        .addComponent(
                            combDmgTextLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            combDmgPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combDmgStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combDmgPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combDmgPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combDmgResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combDmgPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        combDmgPanelLayout.setVerticalGroup(
            combDmgPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combDmgPanelLayout.createSequentialGroup()
                        .addGroup(
                            combDmgPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combDmgPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combDmgStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combDmgPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combDmgPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combDmgResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
                .addComponent(
                    combDmgTextLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combBrkTextLabel.horizontalAlignment = SwingConstants.CENTER
        combBrkTextLabel.text = "B"
        combBrkTextLabel.isFocusable = false
        combBrkTextLabel.horizontalTextPosition = SwingConstants.CENTER
        combBrkTextLabel.verticalTextPosition = SwingConstants.TOP
        combBrkStatLabel.horizontalAlignment = SwingConstants.CENTER
        combBrkStatLabel.border = BorderFactory.createEtchedBorder()
        combBrkStatLabel.isFocusable = false
        combBrkStatLabel.preferredSize = Dimension(110, 22)
        combBrkPtLabel.horizontalAlignment = SwingConstants.CENTER
        combBrkPtLabel.border = BorderFactory.createEtchedBorder()
        combBrkPtLabel.isFocusable = false
        combBrkPtLabel.preferredSize = Dimension(50, 22)
        combBrkResonanceStatLabel.horizontalAlignment = SwingConstants.CENTER
        combBrkResonanceStatLabel.border = BorderFactory.createEtchedBorder()
        combBrkResonanceStatLabel.isFocusable = false
        combBrkResonanceStatLabel.preferredSize = Dimension(50, 22)
        combBrkPercLabel.horizontalAlignment = SwingConstants.CENTER
        combBrkPercLabel.border = BorderFactory.createEtchedBorder()
        combBrkPercLabel.isFocusable = false
        combBrkPercLabel.preferredSize = Dimension(110, 22)
        val combBrkPanelLayout = GroupLayout(combBrkPanel)
        combBrkPanel.layout = combBrkPanelLayout
        combBrkPanelLayout.setHorizontalGroup(
            combBrkPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combBrkPanelLayout.createSequentialGroup()
                        .addComponent(
                            combBrkTextLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            combBrkPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combBrkStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combBrkPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combBrkPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combBrkResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combBrkPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        combBrkPanelLayout.setVerticalGroup(
            combBrkPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combBrkPanelLayout.createSequentialGroup()
                        .addGroup(
                            combBrkPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combBrkStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combBrkPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combBrkPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combBrkPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combBrkResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
                .addComponent(
                    combBrkTextLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combHitTextLabel.horizontalAlignment = SwingConstants.CENTER
        combHitTextLabel.text = "H"
        combHitTextLabel.isFocusable = false
        combHitTextLabel.horizontalTextPosition = SwingConstants.CENTER
        combHitTextLabel.verticalTextPosition = SwingConstants.TOP
        combHitStatLabel.horizontalAlignment = SwingConstants.CENTER
        combHitStatLabel.border = BorderFactory.createEtchedBorder()
        combHitStatLabel.isFocusable = false
        combHitStatLabel.preferredSize = Dimension(110, 22)
        combHitPtLabel.horizontalAlignment = SwingConstants.CENTER
        combHitPtLabel.border = BorderFactory.createEtchedBorder()
        combHitPtLabel.isFocusable = false
        combHitPtLabel.preferredSize = Dimension(50, 22)
        combHitResonanceStatLabel.horizontalAlignment = SwingConstants.CENTER
        combHitResonanceStatLabel.border = BorderFactory.createEtchedBorder()
        combHitResonanceStatLabel.isFocusable = false
        combHitResonanceStatLabel.preferredSize = Dimension(50, 22)
        combHitPercLabel.horizontalAlignment = SwingConstants.CENTER
        combHitPercLabel.border = BorderFactory.createEtchedBorder()
        combHitPercLabel.isFocusable = false
        combHitPercLabel.preferredSize = Dimension(110, 22)
        val combHitPanelLayout = GroupLayout(combHitPanel)
        combHitPanel.layout = combHitPanelLayout
        combHitPanelLayout.setHorizontalGroup(
            combHitPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combHitPanelLayout.createSequentialGroup()
                        .addComponent(
                            combHitTextLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            combHitPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combHitStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combHitPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combHitPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combHitResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combHitPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        combHitPanelLayout.setVerticalGroup(
            combHitPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, combHitPanelLayout.createSequentialGroup()
                        .addGroup(
                            combHitPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combHitStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combHitPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combHitPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combHitPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combHitResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
                .addComponent(
                    combHitTextLabel,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combRldTextLabel.horizontalAlignment = SwingConstants.CENTER
        combRldTextLabel.text = "R"
        combRldTextLabel.isFocusable = false
        combRldTextLabel.horizontalTextPosition = SwingConstants.CENTER
        combRldTextLabel.verticalTextPosition = SwingConstants.TOP
        combRldStatLabel.horizontalAlignment = SwingConstants.CENTER
        combRldStatLabel.border = BorderFactory.createEtchedBorder()
        combRldStatLabel.isFocusable = false
        combRldStatLabel.preferredSize = Dimension(110, 22)
        combRldPtLabel.horizontalAlignment = SwingConstants.CENTER
        combRldPtLabel.border = BorderFactory.createEtchedBorder()
        combRldPtLabel.isFocusable = false
        combRldPtLabel.preferredSize = Dimension(50, 22)
        combRldResonanceStatLabel.horizontalAlignment = SwingConstants.CENTER
        combRldResonanceStatLabel.border = BorderFactory.createEtchedBorder()
        combRldResonanceStatLabel.isFocusable = false
        combRldResonanceStatLabel.preferredSize = Dimension(50, 22)
        combRldPercLabel.horizontalAlignment = SwingConstants.CENTER
        combRldPercLabel.border = BorderFactory.createEtchedBorder()
        combRldPercLabel.isFocusable = false
        combRldPercLabel.preferredSize = Dimension(110, 22)
        val combRldPanelLayout = GroupLayout(combRldPanel)
        combRldPanel.layout = combRldPanelLayout
        combRldPanelLayout.setHorizontalGroup(
            combRldPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combRldPanelLayout.createSequentialGroup()
                        .addComponent(
                            combRldTextLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            combRldPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combRldStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combRldPercLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combRldPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combRldResonanceStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combRldPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        combRldPanelLayout.setVerticalGroup(
            combRldPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combRldPanelLayout.createSequentialGroup()
                        .addGroup(
                            combRldPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combRldPtLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combRldStatLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addGap(0, 0, 0)
                        .addGroup(
                            combRldPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    combRldPercLabel,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combRldResonanceStatLabel,
                                    GroupLayout.Alignment.TRAILING,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
                .addComponent(
                    combRldTextLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        val combStatPanelLayout = GroupLayout(combStatPanel)
        combStatPanel.layout = combStatPanelLayout
        combStatPanelLayout.setHorizontalGroup(
            combStatPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(combDmgPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(combBrkPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(combHitPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(combRldPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(
                    statButton,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combStatPanelLayout.setVerticalGroup(
            combStatPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combStatPanelLayout.createSequentialGroup()
                        .addComponent(
                            combDmgPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combBrkPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combHitPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combRldPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statButton)
                )
        )
        ticketLabel.horizontalAlignment = SwingConstants.CENTER
        ticketLabel.text = "-"
        ticketLabel.border = BorderFactory.createEtchedBorder()
        ticketLabel.isFocusable = false
        ticketLabel.preferredSize = Dimension(75, 22)
        ticketTextLabel.horizontalAlignment = SwingConstants.RIGHT
        ticketTextLabel.text = "ticket"
        ticketTextLabel.isFocusable = false
        ticketTextLabel.horizontalTextPosition = SwingConstants.LEADING
        xpTextLabel.horizontalAlignment = SwingConstants.RIGHT
        xpTextLabel.text = "enh"
        xpTextLabel.isFocusable = false
        xpLabel.horizontalAlignment = SwingConstants.CENTER
        xpLabel.text = "-"
        xpLabel.border = BorderFactory.createEtchedBorder()
        xpLabel.isFocusable = false
        xpLabel.preferredSize = Dimension(75, 22)
        val combInfoPanelLayout = GroupLayout(combInfoPanel)
        combInfoPanel.layout = combInfoPanelLayout
        combInfoPanelLayout.setHorizontalGroup(
            combInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combInfoPanelLayout.createSequentialGroup()
                        .addGroup(
                            combInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(
                                    ticketTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    xpTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            combInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    xpLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    ticketLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        combInfoPanelLayout.setVerticalGroup(
            combInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combInfoPanelLayout.createSequentialGroup()
                        .addGroup(
                            combInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    ticketTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(ticketLabel, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                            combInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(
                                    xpLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    xpTextLabel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        combInfoPanelLayout.linkSize(SwingConstants.VERTICAL, ticketLabel, xpLabel)
        val combResultPanelLayout = GroupLayout(combResultPanel)
        combResultPanel.layout = combResultPanelLayout
        combResultPanelLayout.setHorizontalGroup(
            combResultPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combResultPanelLayout.createSequentialGroup()
                        .addGroup(
                            combResultPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(
                                    combStatPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combImagePanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combInfoPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combChipListPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        combResultPanelLayout.setVerticalGroup(
            combResultPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combResultPanelLayout.createSequentialGroup()
                        .addComponent(
                            combImagePanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combStatPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combInfoPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
                .addComponent(
                    combChipListPanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combFreqLabel.horizontalAlignment = SwingConstants.CENTER
        combFreqLabel.text = "-"
        combFreqLabel.border = BorderFactory.createEtchedBorder()
        combFreqLabel.isFocusable = false
        combFreqLabel.preferredSize = Dimension(75, 22)
        combFreqListPanel.isFocusable = false
        combFreqListScrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        combFreqListScrollPane.isFocusable = false
        combFreqListScrollPane.preferredSize = Dimension(100, 100)
        combFreqList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        combFreqList.isFocusable = false
        combFreqList.layoutOrientation = JList.HORIZONTAL_WRAP
        combFreqList.visibleRowCount = -1
        combFreqListScrollPane.setViewportView(combFreqList)
        val combFreqListPanelLayout = GroupLayout(combFreqListPanel)
        combFreqListPanel.layout = combFreqListPanelLayout
        combFreqListPanelLayout.setHorizontalGroup(
            combFreqListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combFreqListScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combFreqListPanelLayout.setVerticalGroup(
            combFreqListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combFreqListScrollPane,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    385,
                    Short.MAX_VALUE.toInt()
                )
        )
        combFreqMarkButton.text = "mark"
        combFreqMarkButton.isEnabled = false
        combFreqMarkButton.isFocusable = false
        combFreqMarkButton.margin = Insets(2, 2, 2, 2)
        combFreqTagButton.text = "tag"
        combFreqTagButton.isEnabled = false
        combFreqTagButton.isFocusable = false
        combFreqTagButton.margin = Insets(2, 2, 2, 2)
        val combFreqPanelLayout = GroupLayout(combFreqPanel)
        combFreqPanel.layout = combFreqPanelLayout
        combFreqPanelLayout.setHorizontalGroup(
            combFreqPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combFreqListPanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combFreqLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combFreqTagButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combFreqMarkButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combFreqPanelLayout.setVerticalGroup(
            combFreqPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combFreqPanelLayout.createSequentialGroup()
                        .addComponent(
                            combFreqLabel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combFreqListPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(combFreqMarkButton)
                        .addGap(0, 0, 0)
                        .addComponent(combFreqTagButton)
                )
        )
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(evt: WindowEvent) {
                formWindowClosing(evt)
            }
        })
        poolPanel.isFocusable = false
        poolTPanel.isFocusable = false
        helpButton.isFocusable = false
        helpButton.minimumSize = Dimension(50, 50)
        helpButton.preferredSize = Dimension(50, 50)
        displaySettingButton.isFocusable = false
        displaySettingButton.minimumSize = Dimension(50, 50)
        displaySettingButton.preferredSize = Dimension(50, 50)
        donationButton.text = "<html>Your donation will<br>help me run this app!</html>"
        donationButton.preferredSize = Dimension(200, 41)
        val poolTPanelLayout = GroupLayout(poolTPanel)
        poolTPanel.layout = poolTPanelLayout
        poolTPanelLayout.setHorizontalGroup(
            poolTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    poolTPanelLayout.createSequentialGroup()
                        .addComponent(
                            helpButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            displaySettingButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            donationButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        poolTPanelLayout.setVerticalGroup(
            poolTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    donationButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    helpButton,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    displaySettingButton,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        poolBPanel.isFocusable = false
        poolControlPanel.isFocusable = false
        poolControlPanel.preferredSize = Dimension(274, 50)
        poolRotRButton.isFocusable = false
        poolRotRButton.minimumSize = Dimension(50, 50)
        poolRotRButton.preferredSize = Dimension(50, 50)
        poolRotLButton.isFocusable = false
        poolRotLButton.minimumSize = Dimension(50, 50)
        poolRotLButton.preferredSize = Dimension(50, 50)
        poolSortButton.isFocusable = false
        poolSortButton.minimumSize = Dimension(50, 50)
        poolSortButton.preferredSize = Dimension(50, 50)
        poolColorButton.isFocusable = false
        poolColorButton.margin = Insets(2, 2, 2, 2)
        poolColorButton.preferredSize = Dimension(100, 22)
        poolStarComboBox.isFocusable = false
        poolStarComboBox.preferredSize = Dimension(100, 22)
        val jPanel13Layout = GroupLayout(jPanel13)
        jPanel13.layout = jPanel13Layout
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(poolStarComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
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
        val poolControlPanelLayout = GroupLayout(poolControlPanel)
        poolControlPanel.layout = poolControlPanelLayout
        poolControlPanelLayout.setHorizontalGroup(
            poolControlPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, poolControlPanelLayout.createSequentialGroup()
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
                        .addGap(0, 0, 0)
                        .addComponent(
                            poolSortButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            jPanel13,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        poolControlPanelLayout.linkSize(SwingConstants.HORIZONTAL, poolRotLButton, poolRotRButton, poolSortButton)
        poolControlPanelLayout.setVerticalGroup(
            poolControlPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jPanel13, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(
                    poolSortButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    poolRotLButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    poolRotRButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        poolControlPanelLayout.linkSize(SwingConstants.VERTICAL, poolRotLButton, poolRotRButton, poolSortButton)
        poolListPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        poolListPanel.isFocusable = false
        poolListScrollPane.isFocusable = false
        poolListScrollPane.preferredSize = Dimension(100, 100)
        poolList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        poolList.layoutOrientation = JList.HORIZONTAL_WRAP
        poolList.visibleRowCount = -1
        poolListScrollPane.setViewportView(poolList)
        val poolListPanelLayout = GroupLayout(poolListPanel)
        poolListPanel.layout = poolListPanelLayout
        poolListPanelLayout.setHorizontalGroup(
            poolListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    poolListScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        poolListPanelLayout.setVerticalGroup(
            poolListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    poolListScrollPane,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        imageButton.isFocusable = false
        imageButton.minimumSize = Dimension(50, 50)
        imageButton.preferredSize = Dimension(50, 50)
        proxyButton.isFocusable = false
        proxyButton.minimumSize = Dimension(50, 50)
        proxyButton.preferredSize = Dimension(50, 50)
        val poolReadPanelLayout = GroupLayout(poolReadPanel)
        poolReadPanel.layout = poolReadPanelLayout
        poolReadPanelLayout.setHorizontalGroup(
            poolReadPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    poolReadPanelLayout.createSequentialGroup()
                        .addComponent(
                            imageButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            proxyButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        poolReadPanelLayout.setVerticalGroup(
            poolReadPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    imageButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    proxyButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
        )
        val poolBPanelLayout = GroupLayout(poolBPanel)
        poolBPanel.layout = poolBPanelLayout
        poolBPanelLayout.setHorizontalGroup(
            poolBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    poolListPanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(poolControlPanel, GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE.toInt())
                .addComponent(
                    poolReadPanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        poolBPanelLayout.setVerticalGroup(
            poolBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, poolBPanelLayout.createSequentialGroup()
                        .addComponent(
                            poolReadPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(8, 8, 8)
                        .addComponent(
                            poolListPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            poolControlPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        val poolPanelLayout = GroupLayout(poolPanel)
        poolPanel.layout = poolPanelLayout
        poolPanelLayout.setHorizontalGroup(
            poolPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(poolBPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(
                    poolTPanel,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        poolPanelLayout.setVerticalGroup(
            poolPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    poolPanelLayout.createSequentialGroup()
                        .addComponent(
                            poolTPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            poolBPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        piButtonPanel.isFocusable = false
        poolWindowButton.isFocusable = false
        poolWindowButton.minimumSize = Dimension(50, 50)
        poolWindowButton.preferredSize = Dimension(50, 50)
        addButton.isEnabled = false
        addButton.isFocusable = false
        addButton.minimumSize = Dimension(50, 50)
        addButton.preferredSize = Dimension(50, 50)
        val piButtonPanelLayout = GroupLayout(piButtonPanel)
        piButtonPanel.layout = piButtonPanelLayout
        piButtonPanelLayout.setHorizontalGroup(
            piButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    poolWindowButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(addButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        piButtonPanelLayout.setVerticalGroup(
            piButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    piButtonPanelLayout.createSequentialGroup()
                        .addComponent(
                            poolWindowButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            addButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        invPanel.isFocusable = false
        invTPanel.isFocusable = false
        invNewButton.isFocusable = false
        invNewButton.minimumSize = Dimension(50, 50)
        invNewButton.preferredSize = Dimension(50, 50)
        invOpenButton.isFocusable = false
        invOpenButton.minimumSize = Dimension(50, 50)
        invOpenButton.preferredSize = Dimension(50, 50)
        invSaveButton.isEnabled = false
        invSaveButton.isFocusable = false
        invSaveButton.minimumSize = Dimension(50, 50)
        invSaveButton.preferredSize = Dimension(50, 50)
        invSaveAsButton.isFocusable = false
        invSaveAsButton.minimumSize = Dimension(50, 50)
        invSaveAsButton.preferredSize = Dimension(50, 50)
        fileTAPanel.isFocusable = false
        fileTAPanel.preferredSize = Dimension(50, 50)
        jScrollPane1.border = null
        jScrollPane1.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        jScrollPane1.toolTipText = ""
        jScrollPane1.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        jScrollPane1.isFocusable = false
        fileTextArea.background = SystemColor.control
        fileTextArea.columns = 20
        fileTextArea.lineWrap = true
        fileTextArea.rows = 5
        fileTextArea.isEnabled = false
        fileTextArea.isFocusable = false
        jScrollPane1.setViewportView(fileTextArea)
        val fileTAPanelLayout = GroupLayout(fileTAPanel)
        fileTAPanel.layout = fileTAPanelLayout
        fileTAPanelLayout.setHorizontalGroup(
            fileTAPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE.toInt())
        )
        fileTAPanelLayout.setVerticalGroup(
            fileTAPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE.toInt())
        )
        val invTPanelLayout = GroupLayout(invTPanel)
        invTPanel.layout = invTPanelLayout
        invTPanelLayout.setHorizontalGroup(
            invTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    invTPanelLayout.createSequentialGroup()
                        .addComponent(
                            invNewButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            invOpenButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            invSaveButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            invSaveAsButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fileTAPanel, GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE.toInt())
                )
        )
        invTPanelLayout.setVerticalGroup(
            invTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    invNewButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    invOpenButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    invSaveButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    invSaveAsButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(fileTAPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        invBPanel.isFocusable = false
        jPanel9.isFocusable = false
        jPanel12.isFocusable = false
        filterChipCountLabel.horizontalAlignment = SwingConstants.CENTER
        filterChipCountLabel.isFocusable = false
        invSortTypeComboBox.isFocusable = false
        val jPanel12Layout = GroupLayout(jPanel12)
        jPanel12.layout = jPanel12Layout
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    filterChipCountLabel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    invSortTypeComboBox,
                    GroupLayout.Alignment.TRAILING,
                    0,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel12Layout.createSequentialGroup()
                        .addComponent(
                            invSortTypeComboBox,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            filterChipCountLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        invSortOrderButton.isEnabled = false
        invSortOrderButton.isFocusable = false
        invSortOrderButton.minimumSize = Dimension(50, 50)
        invSortOrderButton.preferredSize = Dimension(50, 50)
        filterButton.isFocusable = false
        filterButton.minimumSize = Dimension(50, 50)
        filterButton.preferredSize = Dimension(50, 50)
        displayTypeButton.isFocusable = false
        displayTypeButton.minimumSize = Dimension(50, 50)
        displayTypeButton.preferredSize = Dimension(50, 50)
        val jPanel9Layout = GroupLayout(jPanel9)
        jPanel9.layout = jPanel9Layout
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel9Layout.createSequentialGroup()
                        .addComponent(
                            filterButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            invSortOrderButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            jPanel12,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            displayTypeButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    displayTypeButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    filterButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(
                    invSortOrderButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
                .addComponent(jPanel12, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        invStatPanel.isFocusable = false
        invApplyButton.text = "apply all"
        invApplyButton.isFocusable = false
        invStarComboBox.isEnabled = false
        invStarComboBox.isFocusable = false
        invStarComboBox.preferredSize = Dimension(100, 22)
        invColorButton.isEnabled = false
        invColorButton.isFocusable = false
        invColorButton.margin = Insets(2, 2, 2, 2)
        invColorButton.preferredSize = Dimension(75, 22)
        val jPanel6Layout = GroupLayout(jPanel6)
        jPanel6.layout = jPanel6Layout
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel6Layout.createSequentialGroup()
                        .addComponent(invStarComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invColorButton,
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
                        .addGap(0, 0, 0)
                        .addGroup(
                            jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    invStarComboBox,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    invColorButton,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                )
        )
        jPanel2.isFocusable = false
        invDmgPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        invDmgPanel.layout = BorderLayout(5, 0)
        invDmgTextLabel.horizontalAlignment = SwingConstants.TRAILING
        invDmgTextLabel.text = "D"
        invDmgTextLabel.isFocusable = false
        invDmgTextLabel.horizontalTextPosition = SwingConstants.LEADING
        invDmgPanel.add(invDmgTextLabel, BorderLayout.LINE_START)
        invDmgComboBox.isEnabled = false
        invDmgComboBox.isFocusable = false
        invDmgComboBox.preferredSize = Dimension(50, 22)
        invDmgPanel.add(invDmgComboBox, BorderLayout.CENTER)
        invDmgPtLabel.horizontalAlignment = SwingConstants.CENTER
        invDmgPtLabel.text = "-"
        invDmgPtLabel.border = BorderFactory.createEtchedBorder()
        invDmgPtLabel.isFocusable = false
        invDmgPtLabel.preferredSize = Dimension(22, 22)
        invDmgPanel.add(invDmgPtLabel, BorderLayout.LINE_END)
        invBrkPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        invBrkPanel.layout = BorderLayout(5, 0)
        invBrkTextLabel.horizontalAlignment = SwingConstants.TRAILING
        invBrkTextLabel.text = "B"
        invBrkTextLabel.isFocusable = false
        invBrkTextLabel.horizontalTextPosition = SwingConstants.LEADING
        invBrkPanel.add(invBrkTextLabel, BorderLayout.LINE_START)
        invBrkComboBox.isEnabled = false
        invBrkComboBox.isFocusable = false
        invBrkComboBox.preferredSize = Dimension(50, 22)
        invBrkPanel.add(invBrkComboBox, BorderLayout.CENTER)
        invBrkPtLabel.horizontalAlignment = SwingConstants.CENTER
        invBrkPtLabel.text = "-"
        invBrkPtLabel.border = BorderFactory.createEtchedBorder()
        invBrkPtLabel.isFocusable = false
        invBrkPtLabel.preferredSize = Dimension(22, 22)
        invBrkPanel.add(invBrkPtLabel, BorderLayout.LINE_END)
        invHitPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        invHitPanel.layout = BorderLayout(5, 0)
        invHitTextLabel.horizontalAlignment = SwingConstants.TRAILING
        invHitTextLabel.text = "H"
        invHitTextLabel.isFocusable = false
        invHitTextLabel.horizontalTextPosition = SwingConstants.LEADING
        invHitPanel.add(invHitTextLabel, BorderLayout.LINE_START)
        invHitComboBox.isEnabled = false
        invHitComboBox.isFocusable = false
        invHitComboBox.preferredSize = Dimension(50, 22)
        invHitPanel.add(invHitComboBox, BorderLayout.CENTER)
        invHitPtLabel.horizontalAlignment = SwingConstants.CENTER
        invHitPtLabel.text = "-"
        invHitPtLabel.border = BorderFactory.createEtchedBorder()
        invHitPtLabel.isFocusable = false
        invHitPtLabel.preferredSize = Dimension(22, 22)
        invHitPanel.add(invHitPtLabel, BorderLayout.LINE_END)
        invRldPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        invRldPanel.layout = BorderLayout(5, 0)
        invRldTextLabel.horizontalAlignment = SwingConstants.TRAILING
        invRldTextLabel.text = "R"
        invRldTextLabel.isFocusable = false
        invRldTextLabel.horizontalTextPosition = SwingConstants.LEADING
        invRldPanel.add(invRldTextLabel, BorderLayout.LINE_START)
        invRldComboBox.isEnabled = false
        invRldComboBox.isFocusable = false
        invRldComboBox.preferredSize = Dimension(50, 22)
        invRldPanel.add(invRldComboBox, BorderLayout.CENTER)
        invRldPtLabel.horizontalAlignment = SwingConstants.CENTER
        invRldPtLabel.text = "-"
        invRldPtLabel.border = BorderFactory.createEtchedBorder()
        invRldPtLabel.isFocusable = false
        invRldPtLabel.preferredSize = Dimension(22, 22)
        invRldPanel.add(invRldPtLabel, BorderLayout.LINE_END)
        val jPanel2Layout = GroupLayout(jPanel2)
        jPanel2.layout = jPanel2Layout
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(invDmgPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(invBrkPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(invHitPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(invRldPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel2Layout.createSequentialGroup()
                        .addComponent(
                            invDmgPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                        .addComponent(
                            invBrkPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                        .addComponent(
                            invHitPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                        .addComponent(
                            invRldPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        enhancementTextLabel.text = "강화"
        enhancementTextLabel.isFocusable = false
        invLevelSlider.majorTickSpacing = 5
        invLevelSlider.maximum = 20
        invLevelSlider.minorTickSpacing = 1
        invLevelSlider.snapToTicks = true
        invLevelSlider.value = 0
        invLevelSlider.isEnabled = false
        invLevelSlider.isFocusable = false
        invLevelSlider.preferredSize = Dimension(100, 22)
        invLevelLabel.horizontalAlignment = SwingConstants.CENTER
        invLevelLabel.text = "-"
        invLevelLabel.border = BorderFactory.createEtchedBorder()
        invLevelLabel.isFocusable = false
        invLevelLabel.preferredSize = Dimension(22, 22)
        val jPanel5Layout = GroupLayout(jPanel5)
        jPanel5.layout = jPanel5Layout
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel5Layout.createSequentialGroup()
                        .addComponent(enhancementTextLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invLevelSlider,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invLevelLabel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel5Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(
                            enhancementTextLabel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addComponent(
                            invLevelSlider,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addComponent(
                            invLevelLabel,
                            GroupLayout.Alignment.TRAILING,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        invRotLButton.isEnabled = false
        invRotLButton.isFocusable = false
        invRotLButton.minimumSize = Dimension(50, 50)
        invRotLButton.preferredSize = Dimension(50, 50)
        invRotRButton.isEnabled = false
        invRotRButton.isFocusable = false
        invRotRButton.minimumSize = Dimension(50, 50)
        invRotRButton.preferredSize = Dimension(50, 50)
        invDelButton.isEnabled = false
        invDelButton.isFocusable = false
        invDelButton.minimumSize = Dimension(50, 50)
        invDelButton.preferredSize = Dimension(50, 50)
        invMarkCheckBox.text = "mark"
        invMarkCheckBox.border = null
        invMarkCheckBox.isEnabled = false
        invMarkCheckBox.isFocusable = false
        invMarkCheckBox.horizontalAlignment = SwingConstants.CENTER
        invMarkCheckBox.horizontalTextPosition = SwingConstants.CENTER
        invMarkCheckBox.verticalTextPosition = SwingConstants.TOP
        val jPanel7Layout = GroupLayout(jPanel7)
        jPanel7.layout = jPanel7Layout
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel7Layout.createSequentialGroup()
                        .addComponent(
                            invRotLButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            invRotRButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            invDelButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invMarkCheckBox,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        jPanel7Layout.linkSize(SwingConstants.HORIZONTAL, invDelButton, invRotLButton, invRotRButton)
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel7Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(invDelButton, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE.toInt())
                        .addComponent(
                            invMarkCheckBox,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addComponent(invRotRButton, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE.toInt())
                        .addComponent(invRotLButton, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE.toInt())
                )
        )
        jPanel7Layout.linkSize(SwingConstants.VERTICAL, invDelButton, invRotLButton, invRotRButton)
        invTagButton.text = "tag"
        invTagButton.isEnabled = false
        invTagButton.isFocusable = false
        invTagButton.margin = Insets(2, 2, 2, 2)
        val invStatPanelLayout = GroupLayout(invStatPanel)
        invStatPanel.layout = invStatPanelLayout
        invStatPanelLayout.setHorizontalGroup(
            invStatPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    invStatPanelLayout.createSequentialGroup()
                        .addGroup(
                            invStatPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    jPanel6,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    jPanel7,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    jPanel5,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            jPanel2,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
                .addComponent(
                    invApplyButton,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(invTagButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        invStatPanelLayout.setVerticalGroup(
            invStatPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    invStatPanelLayout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addGroup(
                            invStatPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(
                                    invStatPanelLayout.createSequentialGroup()
                                        .addComponent(
                                            jPanel6,
                                            GroupLayout.PREFERRED_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.PREFERRED_SIZE
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            jPanel5,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                            jPanel7,
                                            GroupLayout.DEFAULT_SIZE,
                                            GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE.toInt()
                                        )
                                )
                                .addComponent(
                                    jPanel2,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(invTagButton)
                        .addGap(0, 0, 0)
                        .addComponent(invApplyButton)
                )
        )
        invListPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        invListPanel.isFocusable = false
        invListScrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        invListScrollPane.isFocusable = false
        invListScrollPane.preferredSize = Dimension(100, 100)
        invList.dragEnabled = true
        invList.dropMode = DropMode.INSERT
        invList.layoutOrientation = JList.HORIZONTAL_WRAP
        invList.visibleRowCount = -1
        invListScrollPane.setViewportView(invList)
        val invListPanelLayout = GroupLayout(invListPanel)
        invListPanel.layout = invListPanelLayout
        invListPanelLayout.setHorizontalGroup(
            invListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    invListScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        invListPanelLayout.setVerticalGroup(
            invListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    invListScrollPane,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        val jPanel14Layout = GroupLayout(jPanel14)
        jPanel14.layout = jPanel14Layout
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel14Layout.createSequentialGroup()
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                        .addComponent(
                            invListPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                )
        )
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(invListPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        val invBPanelLayout = GroupLayout(invBPanel)
        invBPanel.layout = invBPanelLayout
        invBPanelLayout.setHorizontalGroup(
            invBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jPanel9, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(invStatPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(jPanel14, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        invBPanelLayout.setVerticalGroup(
            invBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    invBPanelLayout.createSequentialGroup()
                        .addComponent(
                            jPanel9,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            jPanel14,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invStatPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        val invPanelLayout = GroupLayout(invPanel)
        invPanel.layout = invPanelLayout
        invPanelLayout.setHorizontalGroup(
            invPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(invTPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(invBPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        invPanelLayout.setVerticalGroup(
            invPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    invPanelLayout.createSequentialGroup()
                        .addComponent(
                            invTPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invBPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        combLeftPanel.isFocusable = false
        combLTPanel.isFocusable = false
        settingButton.isFocusable = false
        settingButton.minimumSize = Dimension(50, 50)
        settingButton.preferredSize = Dimension(50, 50)
        jPanel8.isFocusable = false
        jPanel8.preferredSize = Dimension(100, 50)
        boardNameComboBox.isFocusable = false
        boardNameComboBox.preferredSize = Dimension(100, 21)
        boardStarComboBox.isFocusable = false
        boardStarComboBox.preferredSize = Dimension(100, 21)
        val jPanel8Layout = GroupLayout(jPanel8)
        jPanel8.layout = jPanel8Layout
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(boardNameComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(boardStarComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel8Layout.createSequentialGroup()
                        .addComponent(
                            boardNameComboBox,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            boardStarComboBox,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        val combLTPanelLayout = GroupLayout(combLTPanel)
        combLTPanel.layout = combLTPanelLayout
        combLTPanelLayout.setHorizontalGroup(
            combLTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combLTPanelLayout.createSequentialGroup()
                        .addComponent(
                            jPanel8,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            settingButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        combLTPanelLayout.setVerticalGroup(
            combLTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    jPanel8,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    settingButton,
                    GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE
                )
        )
        combLBPanel.isFocusable = false
        jPanel4.isFocusable = false
        jPanel4.layout = BorderLayout()
        combLabel.horizontalAlignment = SwingConstants.CENTER
        combLabel.text = "0"
        combLabel.border = BorderFactory.createEtchedBorder()
        combLabel.isFocusable = false
        combLabel.preferredSize = Dimension(100, 22)
        jPanel4.add(combLabel, BorderLayout.CENTER)
        combWarningButton.isFocusable = false
        combWarningButton.margin = Insets(0, 0, 0, 0)
        combWarningButton.preferredSize = Dimension(21, 21)
        jPanel4.add(combWarningButton, BorderLayout.WEST)
        combListPanel.border = BorderFactory.createLineBorder(Color(0, 0, 0), 3)
        combListPanel.isFocusable = false
        jScrollPane4.isFocusable = false
        jScrollPane4.preferredSize = Dimension(100, 100)
        combList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        combList.visibleRowCount = -1
        jScrollPane4.setViewportView(combList)
        val combListPanelLayout = GroupLayout(combListPanel)
        combListPanel.layout = combListPanelLayout
        combListPanelLayout.setHorizontalGroup(
            combListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        combListPanelLayout.setVerticalGroup(
            combListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        researchButton.text = "research"
        jPanel18.layout = BorderLayout()
        legendEquippedLabel.horizontalAlignment = SwingConstants.CENTER
        legendEquippedLabel.text = "legend equipped"
        jPanel18.add(legendEquippedLabel, BorderLayout.NORTH)
        legendRotatedLabel.horizontalAlignment = SwingConstants.CENTER
        legendRotatedLabel.text = "legend rotated"
        jPanel18.add(legendRotatedLabel, BorderLayout.SOUTH)
        val combLBPanelLayout = GroupLayout(combLBPanel)
        combLBPanel.layout = combLBPanelLayout
        combLBPanelLayout.setHorizontalGroup(
            combLBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combListPanel,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(
                    researchButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(jPanel18, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        combLBPanelLayout.setVerticalGroup(
            combLBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    GroupLayout.Alignment.TRAILING, combLBPanelLayout.createSequentialGroup()
                        .addComponent(
                            jPanel4,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combListPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            jPanel18,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(researchButton)
                )
        )
        val combLeftPanelLayout = GroupLayout(combLeftPanel)
        combLeftPanel.layout = combLeftPanelLayout
        combLeftPanelLayout.setHorizontalGroup(
            combLeftPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(combLTPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(combLBPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        combLeftPanelLayout.setVerticalGroup(
            combLeftPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combLeftPanelLayout.createSequentialGroup()
                        .addComponent(
                            combLTPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combLBPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        combRightPanel.isFocusable = false
        combRTPanel.isFocusable = false
        combRTPanel.preferredSize = Dimension(300, 50)
        combStopButton.isFocusable = false
        combStopButton.minimumSize = Dimension(50, 50)
        combStopButton.preferredSize = Dimension(50, 50)
        loadingLabel.horizontalAlignment = SwingConstants.CENTER
        loadingLabel.isFocusable = false
        loadingLabel.preferredSize = Dimension(50, 50)
        boardImageLabel.horizontalAlignment = SwingConstants.CENTER
        boardImageLabel.isFocusable = false
        boardImageLabel.preferredSize = Dimension(50, 50)
        showProgImageCheckBox.isFocusable = false
        combStartPauseButton.isFocusable = false
        combStartPauseButton.minimumSize = Dimension(50, 50)
        combStartPauseButton.preferredSize = Dimension(50, 50)
        val combRTPanelLayout = GroupLayout(combRTPanel)
        combRTPanel.layout = combRTPanelLayout
        combRTPanelLayout.setHorizontalGroup(
            combRTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combRTPanelLayout.createSequentialGroup()
                        .addComponent(showProgImageCheckBox)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            boardImageLabel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            combStartPauseButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            combStopButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            loadingLabel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        combRTPanelLayout.linkSize(SwingConstants.HORIZONTAL, boardImageLabel, loadingLabel)
        combRTPanelLayout.setVerticalGroup(
            combRTPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combStopButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(loadingLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(
                    showProgImageCheckBox,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    boardImageLabel,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combStartPauseButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        combRBPanel.isFocusable = false
        jPanel3.isFocusable = false
        combSaveButton.isEnabled = false
        combSaveButton.isFocusable = false
        combSaveButton.minimumSize = Dimension(50, 50)
        combSaveButton.preferredSize = Dimension(50, 50)
        combOpenButton.isFocusable = false
        combOpenButton.minimumSize = Dimension(50, 50)
        combOpenButton.preferredSize = Dimension(50, 50)
        val jPanel3Layout = GroupLayout(jPanel3)
        jPanel3.layout = jPanel3Layout
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel3Layout.createSequentialGroup()
                        .addComponent(
                            combOpenButton,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(
                            combSaveButton,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(
                    combOpenButton,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(
                    combSaveButton,
                    GroupLayout.Alignment.TRAILING,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
        )
        jPanel17.layout = BorderLayout()
        timeLabel.horizontalAlignment = SwingConstants.CENTER
        timeLabel.text = "0:00:00"
        timeLabel.border = BorderFactory.createEtchedBorder()
        timeLabel.isFocusable = false
        timeLabel.preferredSize = Dimension(100, 22)
        jPanel17.add(timeLabel, BorderLayout.CENTER)
        timeWarningButton.preferredSize = Dimension(21, 21)
        jPanel17.add(timeWarningButton, BorderLayout.WEST)
        val combRBPanelLayout = GroupLayout(combRBPanel)
        combRBPanel.layout = combRBPanelLayout
        combRBPanelLayout.setHorizontalGroup(
            combRBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combRBPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                        .addComponent(
                            jPanel3,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
                .addComponent(jPanel17, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(combTabbedPane)
        )
        combRBPanelLayout.setVerticalGroup(
            combRBPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combRBPanelLayout.createSequentialGroup()
                        .addComponent(
                            jPanel17,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(combTabbedPane)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            jPanel3,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                )
        )
        val combRightPanelLayout = GroupLayout(combRightPanel)
        combRightPanel.layout = combRightPanelLayout
        combRightPanelLayout.setHorizontalGroup(
            combRightPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(combRTPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                .addComponent(combRBPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        combRightPanelLayout.setVerticalGroup(
            combRightPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    combRightPanelLayout.createSequentialGroup()
                        .addComponent(
                            combRTPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combRBPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                )
        )
        combProgressBar.isFocusable = false
        tipLabel.text = " "
        tipLabel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        )
        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(
                            poolPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            piButtonPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            invPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combLeftPanel,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combRightPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addContainerGap()
                )
                .addComponent(
                    combProgressBar,
                    GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE,
                    Short.MAX_VALUE.toInt()
                )
                .addComponent(tipLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(
                                    invPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combLeftPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    combRightPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    piButtonPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    poolPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            combProgressBar,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addGap(0, 0, 0)
                        .addComponent(tipLabel)
                )
        )
        pack()
        // </editor-fold>//GEN-END:initComponents
        blinkTimer = Timer(500) { e: ActionEvent? ->
            blink.v = !blink.v
            invList.repaint()
            combFreqList.repaint()
        }
        tml = TipMouseListener(tipLabel)
        init()
    }
}