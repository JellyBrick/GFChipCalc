package main

import main.App
import main.setting.Filter
import main.setting.Setting
import main.ui.MainFrame
import main.ui.resource.AppColor
import main.ui.resource.AppColor.Three
import main.ui.resource.AppText
import main.util.IO
import main.util.Version3
import java.awt.Color
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

/**
 *
 * @author Bunnyspa
 */
class App private constructor() {
    fun orange(): Color? {
        return Three.orange(setting.colorPreset)
    }

    fun green(): Color? {
        return Three.green(setting.colorPreset)
    }

    fun blue(): Color? {
        return Three.blue(setting.colorPreset)
    }

    fun colors(): Array<Color?> {
        return AppColor.Index.colors(setting.colorPreset)
    }

    val mf: MainFrame
    val setting: Setting = IO.loadSettings()
    val filter: Filter = Filter()
    private val text: AppText = AppText()
    private fun start() {
        mf.isVisible = true
        mf.afterLoad()
    }

    fun getText(key: String): String {
        return text.getText(setting.locale, key)
    }

    fun getText(key: String, vararg replaces: String?): String {
        return text.getText(setting.locale, key, *replaces)
    }

    fun getText(key: String, vararg replaces: Int): String {
        val repStrs: Array<String?> = arrayOfNulls(replaces.size)
        for (i in replaces.indices) {
            repStrs[i] = replaces[i].toString()
        }
        return getText(key, *repStrs)
    }

    companion object {
        const val NAME_KR: String = "소녀전선 칩셋 조합기"
        const val NAME_EN: String = "Girls' Frontline HOC Chip Calculator"
        val VERSION: Version3 = Version3(7, 3, 0)
        private const val RESOURCE_PATH: String = "/resources/"

        // </editor-fold>
        private val LOGGER: Logger = Logger.getLogger("gfchipcalc")

        /**
         * @param args the command line arguments
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Test.test();
            val app: App = App()
            app.start()
        }

        fun log(ex: Exception?) {
            if (LOGGER.handlers.isEmpty()) {
                try {
                    val formatter: SimpleDateFormat = SimpleDateFormat("yyMMdd_HHmmss")
                    val fh: FileHandler = FileHandler("Error_" + formatter.format(Date()) + ".log")
                    fh.formatter = SimpleFormatter()
                    LOGGER.addHandler(fh)
                } catch (ex1: IOException) {
                    Logger.getLogger("GFChipCalc").log(Level.SEVERE, null, ex1)
                } catch (ex1: SecurityException) {
                    Logger.getLogger("GFChipCalc").log(Level.SEVERE, null, ex1)
                }
            }
            LOGGER.log(Level.SEVERE, null, ex)
        }

        fun getResource(path: String): URL {
            return App::class.java.getResource(RESOURCE_PATH + path)
        }

        fun getResourceAsStream(path: String): InputStream {
            return App::class.java.getResourceAsStream(RESOURCE_PATH + path)
        }
    }

    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (ex: ClassNotFoundException) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
            } catch (ex1: ClassNotFoundException) {
                log(ex1)
            } catch (ex1: InstantiationException) {
                log(ex1)
            } catch (ex1: IllegalAccessException) {
                log(ex1)
            } catch (ex1: UnsupportedLookAndFeelException) {
                log(ex1)
            }
        } catch (ex: IllegalAccessException) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
            } catch (ex1: ClassNotFoundException) {
                log(ex1)
            } catch (ex1: InstantiationException) {
                log(ex1)
            } catch (ex1: IllegalAccessException) {
                log(ex1)
            } catch (ex1: UnsupportedLookAndFeelException) {
                log(ex1)
            }
        } catch (ex: InstantiationException) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
            } catch (ex1: ClassNotFoundException) {
                log(ex1)
            } catch (ex1: InstantiationException) {
                log(ex1)
            } catch (ex1: IllegalAccessException) {
                log(ex1)
            } catch (ex1: UnsupportedLookAndFeelException) {
                log(ex1)
            }
        } catch (ex: UnsupportedLookAndFeelException) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
            } catch (ex1: ClassNotFoundException) {
                log(ex1)
            } catch (ex1: InstantiationException) {
                log(ex1)
            } catch (ex1: IllegalAccessException) {
                log(ex1)
            } catch (ex1: UnsupportedLookAndFeelException) {
                log(ex1)
            }
        }
        mf = MainFrame(this)
    }
}