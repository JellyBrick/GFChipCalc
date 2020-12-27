package main.ui.transfer

import main.ui.MainFrame
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DragSource
import java.awt.event.InputEvent
import java.io.IOException
import java.util.*
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler
import kotlin.math.min

/**
 * https://stackoverflow.com/questions/16586562/reordering-jlist-with-drag-and-drop
 */
class InvListTransferHandler(mf: MainFrame) : TransferHandler() {
    private val mf = mf
    private val localObjectFlavor = DataFlavor(Array<Any>::class.java, "Array of items")
    private lateinit var indices: IntArray
    private var addIndex = -1 // Location where items were added
    private var addCount // Number of items added.
            = 0
    private var exporting = false
    override fun createTransferable(c: JComponent): Transferable {
        val source = c as JList<*>
        c.getRootPane().glassPane.isVisible = true
        indices = source.selectedIndices
        val transferedObjects = source.selectedValuesList.toTypedArray()
        // return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
        return object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> {
                return arrayOf(localObjectFlavor)
            }

            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                return localObjectFlavor == flavor
            }

            @Throws(UnsupportedFlavorException::class)
            override fun getTransferData(flavor: DataFlavor): Any {
                return if (isDataFlavorSupported(flavor)) {
                    transferedObjects
                } else {
                    throw UnsupportedFlavorException(flavor)
                }
            }
        }
    }

    override fun canImport(info: TransferSupport): Boolean {
        return exporting && info.isDrop && info.isDataFlavorSupported(localObjectFlavor)
    }

    override fun getSourceActions(c: JComponent): Int {
        val glassPane = c.rootPane.glassPane
        glassPane.cursor = DragSource.DefaultMoveDrop
        return MOVE // COPY_OR_MOVE;
    }

    override fun importData(info: TransferSupport): Boolean {
        val tdl = info.dropLocation
        if (!canImport(info) || tdl !is JList.DropLocation) {
            return false
        }
        val target = info.component as JList<*>
        val listModel = target.model as DefaultListModel<Any>
        val max = listModel.size
        var index = tdl.index
        index = if (index < 0) max else index // If it is out of range, it is appended to the end
        index = min(index, max)
        addIndex = index
        try {
            val values = info.transferable.getTransferData(localObjectFlavor) as Array<Any>
            for (value in values) {
                val idx = index++
                listModel.add(idx, value)
                target.addSelectionInterval(idx, idx)
            }
            addCount = values.size
            return true
        } catch (ex: UnsupportedFlavorException) {
            //ex.printStackTrace();
        } catch (ex: IOException) {
        }
        return false
    }

    override fun exportAsDrag(comp: JComponent, e: InputEvent, action: Int) {
        exporting = true
        super.exportAsDrag(comp, e, action)
    }

    override fun exportDone(c: JComponent, data: Transferable, action: Int) {
        c.rootPane.glassPane.isVisible = false
        cleanup(c, action == MOVE)
        mf.invListTransferHandler_ExportDone()
    }

    private fun cleanup(c: JComponent, remove: Boolean) {
        if (remove && Objects.nonNull(indices)) {
            if (addCount > 0) {
                // https://github.com/aterai/java-swing-tips/blob/master/DragSelectDropReordering/src/java/example/MainPanel.java
                for (i in indices.indices) {
                    if (indices[i] >= addIndex) {
                        indices[i] += addCount
                    }
                }
            }
            val source = c as JList<*>
            val model = source.model as DefaultListModel<*>
            for (i in indices.indices.reversed()) {
                model.remove(indices[i])
            }
        }
        addCount = 0
        addIndex = -1
        exporting = false
    }

}