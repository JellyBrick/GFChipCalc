package main.util

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.EtchedBorder
import javax.swing.plaf.FontUIResource
import kotlin.math.min
import kotlin.math.roundToLong

/**
 *
 * @author Bunnyspa
 */
object Fn {
    // <editor-fold defaultstate="collapsed" desc="GUI Methods"> 
    fun getAllComponents(component: Component?): Set<Component?> {
        val components: MutableSet<Component?> = HashSet()
        components.add(component)
        if (component is Container) {
            for (child in component.components) {
                components.addAll(getAllComponents(child))
            }
        }
        return components
    }

    fun setUIFont(font: Font) {
        val keys = UIManager.getDefaults().keys()
        while (keys.hasMoreElements()) {
            val key = keys.nextElement()
            val value = UIManager.get(key)
            if (value is FontUIResource) {
                UIManager.put(key, FontUIResource(font))
            }
        }
    }

    fun addEscDisposeListener(aDialog: JDialog) {
        getAllComponents(aDialog).forEach(Consumer { c: Component? ->
            c!!.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(evt: KeyEvent) {
                    if (evt.keyCode == KeyEvent.VK_ESCAPE) {
                        aDialog.dispose()
                    }
                }
            })
        })
    }

    fun addEscListener(aDialog: JDialog?, r: Runnable) {
        getAllComponents(aDialog).forEach(Consumer { c: Component? ->
            c!!.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(evt: KeyEvent) {
                    if (evt.keyCode == KeyEvent.VK_ESCAPE) {
                        r.run()
                    }
                }
            })
        })
    }

    fun open(c: Component?, dialog: JDialog) {
        dialog.setLocationRelativeTo(c)
        dialog.isVisible = true
    }

    fun open(c: Component?, frame: JFrame) {
        frame.setLocationRelativeTo(c)
        frame.isVisible = true
    }

    fun getWidth(str: String, font: Font?): Int {
        val c = Canvas()
        return c.getFontMetrics(font).stringWidth(str)
    }

    fun getHeight(font: Font?): Int {
        val c = Canvas()
        return c.getFontMetrics(font).height
    }

    fun popup(comp: JComponent, title: String?, text: String?) {
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = CompoundBorder(EtchedBorder(), EmptyBorder(5, 5, 0, 5))
        val titleLabel = JLabel(title)
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD)
        val textLabel = JLabel(text)
        textLabel.border = EmptyBorder(0, 0, 5, 0)
        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(textLabel, BorderLayout.CENTER)
        val dialog = JDialog()
        dialog.isUndecorated = true
        dialog.addMouseListener(object : MouseAdapter() {
            override fun mouseExited(e: MouseEvent) {
                dialog.dispose()
            }
        })
        dialog.add(panel)
        dialog.pack()
        val p = comp.locationOnScreen
        p.translate((comp.width - dialog.width) / 2, 0)
        dialog.location = p
        dialog.isVisible = true
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="String">
    fun toHTML(s: String): String {
        var out = "<html>"
        out += s.replace("\\r\\n|\\r|\\n".toRegex(), "<br>").trim { it <= ' ' }
        out += "</html>"
        return out
    }

    fun htmlColor(s: String?, c: Color): String {
        return "<font color=" + colorToHexcode(c) + ">" + s + "</font>"
    }

    fun htmlColor(i: Int, c: Color): String {
        return htmlColor(i.toString(), c)
    }

    fun getTime(s: Long): String {
        val hour = s / 3600
        val min = s % 3600 / 60
        val sec = s % 60
        return hour.toString() + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec)
    }

    fun thousandComma(i: Int): String {
        return String.format("%,d", i)
    }

    fun fStr(d: Double, len: Int): String {
        return String.format("%." + len + "f", d)
    }

    fun fPercStr(d: Double): String {
        return String.format("%.2f", d * 100) + "%"
    }

    fun iPercStr(d: Double): String {
        return (d * 100).roundToLong().toString() + "%"
    }

    fun pad(s: String, i: Int): String {
        return s.repeat(kotlin.math.max(0, i))
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Number">
    fun limit(i: Int, min: Int, max: Int): Int {
        return min(kotlin.math.max(i, min), max)
    }

    fun max(vararg ints: Int): Int {
        if (ints.isEmpty()) {
            return 0
        }
        var out = ints[0]
        for (i in ints) {
            out = kotlin.math.max(out, i)
        }
        return out
    }

    fun floor(n: Int, d: Int): Int {
        return n / d
    }

    fun ceil(n: Int, d: Int): Int {
        return floor(n, d) + if (n % d == 0) 0 else 1
    }

    private fun getPerc(value: Double, min: Double, max: Double): Double {
        if (min >= max) {
            return 0.0
        }
        if (value < min) {
            return 0.0
        }
        return if (max < value) {
            1.0
        } else (value - min) / (max - min)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Array"> 
    @SafeVarargs
    fun <T> concatAll(first: Array<T>, vararg rest: Array<T>): Array<T> {
        var totalLength = first.size
        for (array in rest) {
            totalLength += array.size
        }
        val result = Arrays.copyOf(first, totalLength)
        var offset = first.size
        for (array in rest) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }
        return result
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Color">
    fun colorToHexcode(c: Color): String {
        val colorHex = Integer.toHexString(c.rgb)
        return "#" + colorHex.substring(2)
    }

    fun percColor(c1: Color, c2: Color, c3: Color, value: Double, min: Double, max: Double): Color {
        return percColor(c1, c2, c3, getPerc(value, min, max))
    }

    fun percColor(c1: Color, c2: Color, value: Double, min: Double, max: Double): Color {
        return percColor(c1, c2, getPerc(value, min, max))
    }

    fun percColor(c1: Color, c2: Color, d: Double): Color {
        val r1 = c1.red
        val g1 = c1.green
        val b1 = c1.blue
        val r2 = c2.red
        val g2 = c2.green
        val b2 = c2.blue
        val r3 = r1 + ((r2 - r1) * d).roundToLong().toInt()
        val g3 = g1 + ((g2 - g1) * d).roundToLong().toInt()
        val b3 = b1 + ((b2 - b1) * d).roundToLong().toInt()
        return Color(r3, g3, b3)
    }

    private fun percColor(c1: Color, c2: Color, c3: Color, d: Double): Color {
        return if (d < 0.5f) {
            percColor(c1, c2, d * 2)
        } else {
            percColor(c2, c3, (d - 0.5f) * 2)
        }
    }

    fun getColor(hue: Float): Color {
        return Color.getHSBColor(hue, 0.75f, 0.75f)
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Dimension">
    fun isInside(p: Point?, r: Rectangle): Boolean {
        return if (p == null) {
            false
        } else isInside(p.x, p.y, r)
    }

    fun isInside(x: Int, y: Int, r: Rectangle): Boolean {
        return r.x < x && x < r.x + r.width && r.y < y && y < r.y + r.height
    }

    fun isOverlapped(r1: Rectangle, r2: Rectangle): Boolean {
        return r2.x < r1.x + r1.width && r1.x < r2.x + r2.width && r2.y < r1.y + r1.height && r1.y < r2.y + r2.height
    }

    fun fit(width: Int, height: Int, container: Rectangle): Rectangle {
        var newWidth = container.width
        var newHeight = height * container.width / width
        if (container.height < newHeight) {
            newWidth = width * container.height / height
            newHeight = container.height
        }
        val x = container.x + container.width / 2 - newWidth / 2
        val y = container.y + container.height / 2 - newHeight / 2
        return Rectangle(x, y, newWidth, newHeight)
    } // </editor-fold>
}