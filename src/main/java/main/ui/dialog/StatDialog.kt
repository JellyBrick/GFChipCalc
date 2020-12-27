package main.ui.dialog

import main.App
import main.puzzle.Board
import main.puzzle.Stat
import main.ui.resource.AppColor
import main.ui.resource.AppText
import main.util.Fn
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.util.function.Consumer
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 *
 * @author Bunnyspa
 */
class StatDialog private constructor(private val app: App, private val board: Board) : JDialog() {
    private val star: Int
    private fun init() {
        initText()
        versionComboBox.isEnabled = star == 5
        if (star < 5) {
            versionComboBox.addItem(Board.getStarHTML_star(star))
        } else {
            for (v in 10 downTo 0) {
                versionComboBox.addItem(Board.getStarHTML_version(v))
            }
        }
        addListeners()
        preferredSize = app.mf.preferredDialogSize
        pack()
        update()
    }

    private fun initText() {
        title = app.getText(AppText.STAT_TITLE)
        textVersionLabel.text = app.getText(AppText.STAT_VERSION)
        totalLabel.text = Fn.toHTML(
            app.getText(AppText.STAT_TOTAL)
                    + " = " + Fn.htmlColor(app.getText(AppText.STAT_HOC), AppColor.LEVEL)
                    + " + " + app.getText(AppText.STAT_CHIP)
                    + " + " + Fn.htmlColor(
                app.getText(AppText.STAT_RESONANCE),
                AppColor.CHIPS[board.getColor()]!!
            )
                    + " + " + Fn.htmlColor(app.getText(AppText.STAT_VERSION), AppColor.RED_STAR)
        )
        val s = board.getStat()
        val cm = board.getCustomMaxStat()
        val om = board.getOrigMaxStat()
        dmgPanel.border =
            TitledBorder(app.getText(AppText.CHIP_STAT_DMG_LONG) + " " + s.dmg + " / " + cm.dmg + if (cm.dmg == om.dmg) "" else " (" + om.dmg + ")")
        brkPanel.border =
            TitledBorder(app.getText(AppText.CHIP_STAT_BRK_LONG) + " " + s.brk + " / " + cm.brk + if (cm.brk == om.brk) "" else " (" + om.brk + ")")
        hitPanel.border =
            TitledBorder(app.getText(AppText.CHIP_STAT_HIT_LONG) + " " + s.hit + " / " + cm.hit + if (cm.hit == om.hit) "" else " (" + om.hit + ")")
        rldPanel.border =
            TitledBorder(app.getText(AppText.CHIP_STAT_RLD_LONG) + " " + s.rld + " / " + cm.rld + if (cm.rld == om.rld) "" else " (" + om.rld + ")")
        closeButton.text = app.getText(AppText.ACTION_CLOSE)
    }

    private fun update() {
        val version = if (versionComboBox.isEnabled) 10 - versionComboBox.selectedIndex else 0
        val keyLabel: Array<JLabel> = arrayOf(keyDmgLabel, keyBrkLabel, keyHitLabel, keyRldLabel)
        val valueLabel: Array<JLabel> = arrayOf(valueDmgLabel, valueBrkLabel, valueHitLabel, valueRldLabel)
        val hocStat: IntArray = Board.getHOCStat(name)!!.toArray()
        val chipStat = board.getStat().limit(board.getOrigMaxStat()).toArray()
        val resStat = board.getResonance().toArray()
        val verStat: IntArray = Board.getVersionStat(name, version).toArray()
        val totalStat = IntArray(4)
        for (i in 0..3) {
            totalStat[i] = hocStat[i] + chipStat[i] + resStat[i] + verStat[i]
        }
        val totalKey = app.getText(AppText.STAT_TOTAL)
        val keys: MutableList<String?> = mutableListOf()
        val values: MutableList<String> = mutableListOf()
        for (i in 0..3) {
            keys.clear()
            values.clear()
            keys.add(totalKey)
            values.add(
                totalStat[i]
                    .toString() + " = " + Fn.htmlColor(hocStat[i], AppColor.LEVEL)
                        + " + " + chipStat[i]
                        + " + " + Fn.htmlColor(resStat[i], AppColor.CHIPS[board.getColor()]!!)
                        + " + " + Fn.htmlColor(verStat[i], AppColor.RED_STAR)
            )
            when (i) {
                Stat.BRK -> {
                    // Old Stat
                    val oldBrk = board.getOldStat().limit(board.getOrigMaxStat()).brk
                    val oldTotalBrk = hocStat[1] + oldBrk + resStat[1] + verStat[1]
                    val oldTotalKey = app.getText(AppText.STAT_TOTAL_OLD)
                    keys.add(oldTotalKey)
                    values.add(
                        oldTotalBrk
                            .toString() + " = " + Fn.htmlColor(hocStat[i], AppColor.LEVEL)
                                + " + " + oldBrk
                                + " + " + Fn.htmlColor(resStat[i], AppColor.CHIPS[board.getColor()]!!)
                                + " + " + Fn.htmlColor(verStat[i], AppColor.RED_STAR)
                    )
                }
                Stat.RLD -> {
                    // Fire Rate
                    val firerate = rldToFirerate(totalStat[3])
                    keys.add(app.getText(AppText.STAT_RLD_FIRERATE))
                    values.add(firerate.toString())

                    // Frame
                    val frame = firerateToFrame(firerate)
                    val sec = (frame / 30.0f).toDouble()
                    keys.add(app.getText(AppText.STAT_RLD_DELAY))
                    values.add(
                        app.getText(
                            AppText.STAT_RLD_DELAY_FRAME,
                            frame.toString()
                        ) + " = " + app.getText(AppText.STAT_RLD_DELAY_SECOND, Fn.fStr(sec, 4))
                    )
                }
                else -> {
                }
            }
            setData(keyLabel[i], valueLabel[i], keys, values)
        }
    }

    private fun addListeners() {
        versionComboBox.addActionListener { e: ActionEvent? -> update() }
        Fn.addEscDisposeListener(this)
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    private fun closeButtonActionPerformed(evt: ActionEvent) { //GEN-FIRST:event_closeButtonActionPerformed
        dispose()
    } //GEN-LAST:event_closeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private var brkPanel: JPanel
    private var closeButton: JButton
    private var dmgPanel: JPanel
    private var hitPanel: JPanel
    private var jPanel1: JPanel
    private var keyBrkLabel: JLabel
    private var keyDmgLabel: JLabel
    private var keyHitLabel: JLabel
    private var keyRldLabel: JLabel
    private var rldPanel: JPanel
    private var textVersionLabel: JLabel
    private var totalLabel: JLabel
    private var valueBrkLabel: JLabel
    private var valueDmgLabel: JLabel
    private var valueHitLabel: JLabel
    private var valueRldLabel: JLabel
    private var versionComboBox: JComboBox<String> // End of variables declaration//GEN-END:variables

    companion object {
        fun open(app: App, board: Board) {
            val dialog = StatDialog(app, board)
            Fn.open(app.mf, dialog)
        }

        private fun setData(keyLabel: JLabel, valueLabel: JLabel, keys: List<String?>, values: List<String>) {
            if (keys.isEmpty()) {
                keyLabel.text = ""
                valueLabel.text = ""
            } else {
                val keysMod: MutableList<String> = mutableListOf()
                keys.forEach(Consumer { k: String? -> keysMod.add("$k&nbsp;:&nbsp;") })
                keyLabel.text = Fn.toHTML("<div align=right>" + java.lang.String.join("<br>", keysMod) + "</div>")
                valueLabel.text = Fn.toHTML(java.lang.String.join("<br>", values))
            }
        }

        private fun rldToFirerate(rld: Int): Int {
            return Fn.ceil(rld + 300, 30)
        }

        private fun firerateToFrame(firerate: Int): Int {
            return Fn.floor(1500, firerate)
        }
    }

    init {
        name = board.name
        star = board.star
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        closeButton = JButton()
        dmgPanel = JPanel()
        keyDmgLabel = JLabel()
        valueDmgLabel = JLabel()
        brkPanel = JPanel()
        keyBrkLabel = JLabel()
        valueBrkLabel = JLabel()
        hitPanel = JPanel()
        keyHitLabel = JLabel()
        valueHitLabel = JLabel()
        rldPanel = JPanel()
        keyRldLabel = JLabel()
        valueRldLabel = JLabel()
        jPanel1 = JPanel()
        textVersionLabel = JLabel()
        versionComboBox = JComboBox()
        totalLabel = JLabel()
        defaultCloseOperation = DISPOSE_ON_CLOSE
        title = "도움말"
        modalityType = ModalityType.APPLICATION_MODAL
        isResizable = false
        type = Type.UTILITY
        closeButton.text = "닫기"
        closeButton.addActionListener { evt: ActionEvent -> closeButtonActionPerformed(evt) }
        dmgPanel.border = BorderFactory.createTitledBorder("dmg")
        dmgPanel.layout = BorderLayout()
        keyDmgLabel.horizontalAlignment = SwingConstants.RIGHT
        keyDmgLabel.text = "key"
        keyDmgLabel.verticalAlignment = SwingConstants.TOP
        dmgPanel.add(keyDmgLabel, BorderLayout.LINE_START)
        valueDmgLabel.text = "value"
        valueDmgLabel.verticalAlignment = SwingConstants.TOP
        dmgPanel.add(valueDmgLabel, BorderLayout.CENTER)
        brkPanel.border = BorderFactory.createTitledBorder("brk")
        brkPanel.layout = BorderLayout()
        keyBrkLabel.horizontalAlignment = SwingConstants.RIGHT
        keyBrkLabel.text = "key"
        keyBrkLabel.verticalAlignment = SwingConstants.TOP
        brkPanel.add(keyBrkLabel, BorderLayout.LINE_START)
        valueBrkLabel.text = "value"
        valueBrkLabel.verticalAlignment = SwingConstants.TOP
        brkPanel.add(valueBrkLabel, BorderLayout.CENTER)
        hitPanel.border = BorderFactory.createTitledBorder("hit")
        hitPanel.layout = BorderLayout()
        keyHitLabel.horizontalAlignment = SwingConstants.RIGHT
        keyHitLabel.text = "key"
        keyHitLabel.verticalAlignment = SwingConstants.TOP
        hitPanel.add(keyHitLabel, BorderLayout.LINE_START)
        valueHitLabel.text = "value"
        valueHitLabel.verticalAlignment = SwingConstants.TOP
        hitPanel.add(valueHitLabel, BorderLayout.CENTER)
        rldPanel.border = BorderFactory.createTitledBorder("rld")
        rldPanel.layout = BorderLayout()
        keyRldLabel.horizontalAlignment = SwingConstants.RIGHT
        keyRldLabel.text = "key"
        keyRldLabel.verticalAlignment = SwingConstants.TOP
        rldPanel.add(keyRldLabel, BorderLayout.LINE_START)
        valueRldLabel.text = "value"
        valueRldLabel.verticalAlignment = SwingConstants.TOP
        rldPanel.add(valueRldLabel, BorderLayout.CENTER)
        textVersionLabel.text = "version"
        versionComboBox.preferredSize = Dimension(100, 21)
        totalLabel.text = "jLabel1"
        val jPanel1Layout = GroupLayout(jPanel1)
        jPanel1.layout = jPanel1Layout
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel1Layout.createSequentialGroup()
                        .addComponent(textVersionLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            versionComboBox,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(totalLabel, GroupLayout.DEFAULT_SIZE, 657, Short.MAX_VALUE.toInt())
                )
        )
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                    jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(textVersionLabel)
                        .addComponent(
                            versionComboBox,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addComponent(totalLabel, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                )
        )
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
                                    jPanel1,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addGroup(
                                    GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE.toInt())
                                        .addComponent(closeButton)
                                )
                                .addComponent(
                                    rldPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    hitPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    brkPanel,
                                    GroupLayout.DEFAULT_SIZE,
                                    GroupLayout.DEFAULT_SIZE,
                                    Short.MAX_VALUE.toInt()
                                )
                                .addComponent(
                                    dmgPanel,
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
                        .addComponent(
                            jPanel1,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            dmgPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            brkPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            hitPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(
                            rldPanel,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE.toInt()
                        )
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeButton)
                        .addContainerGap()
                )
        )
        pack()
        // </editor-fold>//GEN-END:initComponents
        init()
    }
}