package main.ui.component

import main.ui.resource.AppColor
import main.ui.resource.AppColor.Three
import java.awt.*
import javax.swing.JLabel

/**
 *
 * @author Bunnyspa
 */
class ColorLabel : JLabel() {
    private var preset = 0
    fun setColorPreset(preset: Int) {
        this.preset = preset
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g.create() as Graphics2D
        val threeWidths = intArrayOf(0, width / 3, width * 2 / 3, width)
        val multWidths = IntArray(AppColor.Index.colors(preset).size + 1)
        multWidths[0] = 0
        for (i in 1 until multWidths.size) {
            multWidths[i] = width * i / AppColor.Index.colors(preset).size
        }
        val heights = intArrayOf(0, height / 3, height * 2 / 3, height)

        // 1
        g2d.color = Three.orange(preset)
        g2d.fill(Rectangle(threeWidths[0], heights[0], threeWidths[1], heights[1]))
        g2d.color = Three.green(preset)
        g2d.fill(Rectangle(threeWidths[1], heights[0], threeWidths[2], heights[1]))
        g2d.color = Three.blue(preset)
        g2d.fill(Rectangle(threeWidths[2], heights[0], threeWidths[3], heights[1]))

        // 2
        val lgp = LinearGradientPaint(
            Point(0, 0),
            Point(width, 0),
            floatArrayOf(0f, 0.5f, 1f),
            arrayOf(Three.orange(preset), Three.green(preset), Three.blue(preset))
        )
        g2d.paint = lgp
        g2d.fill(Rectangle(0, heights[1], width, heights[2]))

        // 3
        for (i in 0 until multWidths.size - 1) {
            g2d.color = AppColor.Index.colors(preset)[i]
            g2d.fill(Rectangle(multWidths[i], heights[2], multWidths[i + 1], heights[3]))
        }
        g2d.dispose()
        super.paintComponent(g)
    }
}