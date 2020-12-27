package main.ui.resource

import main.App
import main.puzzle.Chip
import main.puzzle.Shape
import main.util.IO
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer

/**
 *
 * @author Bunnyspa
 */
class AppText {
    // <editor-fold defaultstate="collapsed" desc="Resources">
    private var prop: Properties? = null
    private var propTag = ""
    fun getText(locale: Locale, key: String, vararg replaces: String?): String {
        var value = getValue(locale, key)
        for (i in replaces.indices) {
            replaces[i]?.let {
                value = value.replace("{$i}", it)
            }
        }
        try {
            if (EN_US == locale && KEYS_PLURAL.contains(key) && (replaces[0] != null && replaces[0]!!.toInt() > 1)) {
                value += "s"
            }
        } catch (ignored: Exception) {
        }
        if (KEYS_HTML_CENTER.contains(key)) {
            return "<html><center>$value</center></html>"
        }
        if (KEYS_HTML.contains(key)) {
            return "<html>$value</html>"
        }
        return if (key == DISPLAY_LANGUAGE_PREVIEW) {
            value + (if (value.isEmpty()) "" else " ") + DISPLAY_LANGUAGE_PREVIEW_DEFAULT
        } else value
    }

    private fun getValue(locale: Locale, key: String): String {
        if (propTag != locale.toLanguageTag()) {
            prop = IO.getProp(locale)
            propTag = locale.toLanguageTag()
        }
        if (prop!!.containsKey(key)) {
            return prop!!.getProperty(key)
        }
        if (KO_KR == locale && LANGMAP.getValue(KO_KR).containsKey(key)) {
            return LANGMAP[KO_KR]!!.getProperty(key)
        }
        return if (JA_JP == locale && LANGMAP.getValue(JA_JP).containsKey(key)) {
            LANGMAP[JA_JP]!!.getProperty(key)
        } else LANGMAP[EN_US]!!.getProperty(key)
    }

    companion object {
        val TEXT_MAP_COLOR: Map<Int, String> =
            object : HashMap<Int, String>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(Chip.COLOR_ORANGE, AppText.CHIP_COLOR_ORANGE)
                    put(Chip.COLOR_BLUE, AppText.CHIP_COLOR_BLUE)
                }
            } // </editor-fold>

        fun textType(app: App, type: Shape.Type): String {
            return when (type.id) {
                6 -> app.getText(UNIT_CELLTYPE, "5", "B")
                5 -> app.getText(UNIT_CELLTYPE, "5", "A")
                7, 4, 3, 2, 1 -> app.getText(UNIT_CELL, type.id)
                else -> ""
            }
        }

        const val TEXT_STAR_FULL = "★"
        const val TEXT_STAR_EMPTY = "☆"
        val KO_KR: Locale = Locale.forLanguageTag("ko-KR")
        val EN_US: Locale = Locale.forLanguageTag("en-US")
        val JA_JP: Locale = Locale.forLanguageTag("ja-JP")
        val LOCALES = arrayOf(KO_KR, EN_US, JA_JP)
        private val LANGMAP: Map<Locale, Properties> =
            object : HashMap<Locale, Properties>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    for (locale in LOCALES) {
                        val url: URL =
                            App.getResource("language/" + locale.toLanguageTag() + ".properties")
                        try {
                            InputStreamReader(url.openStream(), StandardCharsets.UTF_8).use { r ->
                                val props = Properties()
                                props.load(r)
                                put(locale, props)
                            }
                        } catch (ignored: Exception) {
                        }
                    }
                }
            } // </editor-fold>

        // <editor-fold defaultstate="collapsed" desc="Map">
        // ACTION
        const val ACTION_OK = "ACTION_OK"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val ACTION_APPLY = "ACTION_APPLY"
        const val ACTION_CLOSE = "ACTION_CLOSE"
        const val ACTION_ADD = "ACTION_ADD"
        const val ACTION_DEL = "ACTION_DEL"

        // UNIT
        const val UNIT_PT = "UNIT_PT"
        const val UNIT_COUNT = "UNIT_COUNT"
        const val UNIT_STAR = "UNIT_STAR"
        const val UNIT_STAR_SHORT = "UNIT_STAR_SHORT"
        const val UNIT_CELL = "UNIT_CELL"
        const val UNIT_CELLTYPE = "UNIT_CELLTYPE"
        const val UNIT_LEVEL = "UNIT_LEVEL"

        // NEWVER
        const val NEWVER_CONFIRM_TITLE = "NEWVER_CONFIRM_TITLE"
        const val NEWVER_CONFIRM_BODY = "NEWVER_CONFIRM_BODY"
        const val NEWVER_ERROR_TITLE = "NEWVER_ERROR_TITLE"
        const val NEWVER_ERROR_BODY = "NEWVER_ERROR_BODY"

        // TIP
        const val TIP_DISPLAY = "TIP_DISPLAY"
        const val TIP_HELP = "TIP_HELP"
        const val TIP_IMAGE = "TIP_IMAGE"
        const val TIP_PROXY = "TIP_PROXY"
        const val TIP_POOL = "TIP_POOL"
        const val TIP_POOL_ROTATE_LEFT = "TIP_POOL_ROTATE_LEFT"
        const val TIP_POOL_ROTATE_RIGHT = "TIP_POOL_ROTATE_RIGHT"
        const val TIP_POOL_SORT_ORDER = "TIP_POOL_SORT_ORDER"
        const val TIP_POOL_STAR = "TIP_POOL_STAR"
        const val TIP_POOL_COLOR = "TIP_POOL_COLOR"
        const val TIP_POOLWINDOW = "TIP_POOLWINDOW"
        const val TIP_ADD = "TIP_ADD"
        const val TIP_INV = "TIP_INV"
        const val TIP_INV_NEW = "TIP_INV_NEW"
        const val TIP_INV_OPEN = "TIP_INV_OPEN"
        const val TIP_INV_SAVE = "TIP_INV_SAVE"
        const val TIP_INV_SAVEAS = "TIP_INV_SAVEAS"
        const val TIP_INV_SORT_ORDER = "TIP_INV_SORT_ORDER"
        const val TIP_INV_SORT_TYPE = "TIP_INV_SORT_TYPE"
        const val TIP_INV_FILTER = "TIP_INV_FILTER"
        const val TIP_INV_STAT = "TIP_INV_STAT"
        const val TIP_INV_APPLY = "TIP_INV_APPLY"
        const val TIP_INV_STAR = "TIP_INV_STAR"
        const val TIP_INV_COLOR = "TIP_INV_COLOR"
        const val TIP_INV_ENHANCEMENT = "TIP_INV_ENHANCEMENT"
        const val TIP_INV_ROTATE_LEFT = "TIP_INV_ROTATE_LEFT"
        const val TIP_INV_ROTATE_RIGHT = "TIP_INV_ROTATE_RIGHT"
        const val TIP_INV_DELETE = "TIP_INV_DELETE"
        const val TIP_INV_MARK = "TIP_INV_MARK"
        const val TIP_INV_TAG = "TIP_INV_TAG"
        const val TIP_BOARD_NAME = "TIP_BOARD_NAME"
        const val TIP_BOARD_STAR = "TIP_BOARD_STAR"
        const val TIP_RESEARCH_OLD = "TIP_RESEARCH_OLD"
        const val TIP_COMB_LIST = "TIP_COMB_LIST"
        const val TIP_COMB_CHIPLIST = "TIP_COMB_CHIPLIST"
        const val TIP_COMB_FREQLIST = "TIP_COMB_FREQLIST"
        const val TIP_COMB_SETTING = "TIP_COMB_SETTING"
        const val TIP_COMB_SHOWPROGIMAGE = "TIP_COMB_SHOWPROGIMAGE"
        const val TIP_COMB_START = "TIP_COMB_START"
        const val TIP_COMB_STAT = "TIP_COMB_STAT"
        const val TIP_COMB_OPEN = "TIP_COMB_OPEN"
        const val TIP_COMB_SAVE = "TIP_COMB_SAVE"
        const val TIP_COMB_MARK = "TIP_COMB_MARK"
        const val TIP_COMB_TAG = "TIP_COMB_TAG"

        // HELP
        const val HELP_TITLE = "HELP_TITLE"
        const val HELP_CHIP = "HELP_CHIP"
        const val HELP_CHIP_INFO_POINT_TITLE = "HELP_CHIP_INFO_POINT_TITLE"
        const val HELP_CHIP_INFO_POINT_BODY = "HELP_CHIP_INFO_POINT_BODY"
        const val HELP_CHIP_INFO_EFFICIENCY_TITLE = "HELP_CHIP_INFO_EFFICIENCY_TITLE"
        const val HELP_CHIP_INFO_EFFICIENCY_BODY = "HELP_CHIP_INFO_EFFICIENCY_BODY"
        const val HELP_CHIP_INFO_COLOR_TITLE = "HELP_CHIP_INFO_COLOR_TITLE"
        const val HELP_CHIP_INFO_COLOR_BODY = "HELP_CHIP_INFO_COLOR_BODY"
        const val HELP_CHIP_INFO_CALC_TITLE = "HELP_CHIP_INFO_CALC_TITLE"
        const val HELP_CHIP_INFO_CALC_BODY = "HELP_CHIP_INFO_CALC_BODY"
        const val HELP_CHIP_COL_CELL = "HELP_CHIP_COL_CELL"
        const val HELP_CHIP_COL_LEVEL = "HELP_CHIP_COL_LEVEL"
        const val HELP_CHIP_COLOR_SECTION = "HELP_CHIP_COLOR_SECTION"
        const val HELP_CHIP_COLOR_CUMULATIVE = "HELP_CHIP_COLOR_CUMULATIVE"
        const val HELP_CHIP_CALC_DESC = "HELP_CHIP_CALC_DESC"
        const val HELP_CHIP_CALC_STAT = "HELP_CHIP_CALC_STAT"
        const val HELP_CHIP_CALC_LOSS = "HELP_CHIP_CALC_LOSS"
        const val HELP_APP_IMPORT = "HELP_APP_IMPORT"
        const val HELP_APP_IMPORT_PROXY = "HELP_APP_IMPORT_PROXY"
        const val HELP_APP_IMPORT_IMAGESCAN = "HELP_APP_IMPORT_IMAGESCAN"
        const val HELP_APP_IMPORT_OPEN = "HELP_APP_IMPORT_OPEN"
        const val HELP_APP_OPTIMIZE = "HELP_APP_OPTIMIZE"
        const val HELP_APP_OPTIMIZE_FILTER = "HELP_APP_OPTIMIZE_FILTER"
        const val HELP_APP_OPTIMIZE_SETTING = "HELP_APP_OPTIMIZE_SETTING"
        const val HELP_APP_OPTIMIZE_MARK = "HELP_APP_OPTIMIZE_MARK"
        const val HELP_PROGRAM = "HELP_PROGRAM"
        const val HELP_PROXY = "HELP_PROXY"
        const val HELP_PROXY_DESC = "HELP_PROXY_DESC"
        const val HELP_CHANGELOG = "HELP_CHANGELOG"
        const val HELP_ABOUT = "HELP_ABOUT"

        // DISPLAY
        const val DISPLAY_TITLE = "DISPLAY_TITLE"
        const val DISPLAY_LANGUAGE_TITLE = "DISPLAY_LANGUAGE_TITLE"
        const val DISPLAY_LANGUAGE_PREVIEW = "DISPLAY_LANGUAGE_PREVIEW"
        const val DISPLAY_HOWTO_ADDLANGUAGE = "DISPLAY_HOWTO_ADDLANGUAGE"
        const val DISPLAY_EXPORT = "DISPLAY_EXPORT"
        const val DISPLAY_EXPORT_DONE_TITLE = "DISPLAY_EXPORT_DONE_TITLE"
        const val DISPLAY_EXPORT_DONE_BODY = "DISPLAY_EXPORT_DONE_BODY"
        const val DISPLAY_EXPORT_FAIL_TITLE = "DISPLAY_EXPORT_FAIL_TITLE"
        const val DISPLAY_EXPORT_FAIL_BODY = "DISPLAY_EXPORT_FAIL_BODY"
        const val DISPLAY_COLORFONT_TITLE = "DISPLAY_COLORFONT_TITLE"
        const val DISPLAY_COLOR_NORMAL = "DISPLAY_COLOR_NORMAL"
        const val DISPLAY_COLOR_COLORBLIND = "DISPLAY_COLOR_COLORBLIND"
        const val DISPLAY_FONT_RESET = "DISPLAY_FONT_RESET"

        // IMAGE
        const val IMAGE_TITLE = "IMAGE_TITLE"
        const val IMAGE_OPEN = "IMAGE_OPEN"
        const val IMAGE_OPEN_EXT = "IMAGE_OPEN_EXT"
        const val IMAGE_OVERLAPPED_TITLE = "IMAGE_OVERLAPPED_TITLE"
        const val IMAGE_OVERLAPPED_BODY = "IMAGE_OVERLAPPED_BODY"

        // PROXY
        const val PROXY_TITLE = "PROXY_TITLE"
        const val PROXY_WARNING = "PROXY_WARNING"
        const val PROXY_STAGE1_INST = "PROXY_STAGE1_INST"
        const val PROXY_STAGE1_INFO = "PROXY_STAGE1_INFO"
        const val PROXY_STAGE2_INST = "PROXY_STAGE2_INST"
        const val PROXY_STAGE2_INFO = "PROXY_STAGE2_INFO"
        const val PROXY_ERROR_INST = "PROXY_ERROR_INST"
        const val PROXY_ERROR_INFO = "PROXY_ERROR_INFO"

        // SORT
        const val SORT_CUSTOM = "SORT_CUSTOM"
        const val SORT_CELL = "SORT_CELL"
        const val SORT_ENHANCEMENT = "SORT_ENHANCEMENT"
        const val SORT_STAR = "SORT_STAR"

        // FILTER
        const val FILTER_TITLE = "FILTER_TITLE"
        const val FILTER_GROUP_STAR = "FILTER_GROUP_STAR"
        const val FILTER_GROUP_COLOR = "FILTER_GROUP_COLOR"
        const val FILTER_GROUP_CELL = "FILTER_GROUP_CELL"
        const val FILTER_GROUP_MARK = "FILTER_GROUP_MARK"
        const val FILTER_GROUP_PT = "FILTER_GROUP_PT"
        const val FILTER_GROUP_ENHANCEMENT = "FILTER_GROUP_ENHANCEMENT"
        const val FILTER_GROUP_TAG_INCLUDE = "FILTER_GROUP_TAG_INCLUDE"
        const val FILTER_GROUP_TAG_EXCLUDE = "FILTER_GROUP_TAG_EXCLUDE"
        const val FILTER_PRESET = "FILTER_PRESET"
        const val FILTER_RESET = "FILTER_RESET"
        const val FILTER_TAG_RESET = "FILTER_TAG_RESET"

        // APPLY
        const val APPLY_TITLE = "APPLY_TITLE"
        const val APPLY_MARK_ALL = "APPLY_MARK_ALL"
        const val APPLY_MARK_NONE = "APPLY_MARK_NONE"
        const val APPLY_CONFIRM_DESC = "APPLY_CONFIRM_DESC"
        const val APPLY_TAG_DESC = "APPLY_TAG_DESC"

        //CHIP
        const val CHIP_COLOR_ORANGE = "CHIP_COLOR_ORANGE"
        const val CHIP_COLOR_BLUE = "CHIP_COLOR_BLUE"
        const val CHIP_STAT_DMG = "CHIP_STAT_DMG"
        const val CHIP_STAT_BRK = "CHIP_STAT_BRK"
        const val CHIP_STAT_HIT = "CHIP_STAT_HIT"
        const val CHIP_STAT_RLD = "CHIP_STAT_RLD"
        const val CHIP_STAT_DMG_LONG = "CHIP_STAT_DMG_LONG"
        const val CHIP_STAT_BRK_LONG = "CHIP_STAT_BRK_LONG"
        const val CHIP_STAT_HIT_LONG = "CHIP_STAT_HIT_LONG"
        const val CHIP_STAT_RLD_LONG = "CHIP_STAT_RLD_LONG"
        const val CHIP_COLOR = "CHIP_COLOR"
        const val CHIP_TAG = "CHIP_TAG"
        const val CHIP_MARK = "CHIP_MARK"
        const val CHIP_TICKET = "CHIP_TICKET"
        const val CHIP_XP = "CHIP_XP"
        const val CHIP_LEVEL = "CHIP_LEVEL"

        // LEGEND
        const val LEGEND_EQUIPPED = "LEGEND_EQUIPPED"
        const val LEGEND_ROTATED = "LEGEND_ROTATED"

        // STAT
        const val STAT_TITLE = "STAT_TITLE"
        const val STAT_TOTAL = "STAT_TOTAL"
        const val STAT_TOTAL_OLD = "STAT_TOTAL_OLD"
        const val STAT_HOC = "STAT_HOC"
        const val STAT_CHIP = "STAT_CHIP"
        const val STAT_RESONANCE = "STAT_RESONANCE"
        const val STAT_VERSION = "STAT_VERSION"
        const val STAT_RLD_FIRERATE = "STAT_RLD_FIRERATE"
        const val STAT_RLD_DELAY = "STAT_RLD_DELAY"
        const val STAT_RLD_DELAY_FRAME = "STAT_RLD_DELAY_FRAME"
        const val STAT_RLD_DELAY_SECOND = "STAT_RLD_DELAY_SECOND"

        // FILTER
        const val FILTER_ENABLED = "FILTER_ENABLED"
        const val FILTER_DISABLED = "FILTER_DISABLED"

        // TAG
        const val TAG_NONE = "TAG_NONE"
        const val TAG_TITLE = "TAG_TITLE"
        const val TAG_DESC = "TAG_DESC"

        // CSET
        const val CSET_TITLE = "CSET_TITLE"
        const val CSET_ADVANCED_MODE = "CSET_ADVANCED_MODE"
        const val CSET_GROUP_STAT = "CSET_GROUP_STAT"
        const val CSET_GROUP_MARK = "CSET_GROUP_MARK"
        const val CSET_GROUP_SORT = "CSET_GROUP_SORT"
        const val CSET_GROUP_MISC = "CSET_GROUP_MISC"
        const val CSET_DEFAULT_STAT = "CSET_DEFAULT_STAT"
        const val CSET_STAT = "CSET_STAT"
        const val CSET_PT = "CSET_PT"
        const val CSET_PRESET = "CSET_PRESET"
        const val CSET_PRESET_OPTION = "CSET_PRESET_OPTION"
        const val CSET_MARK_CELL = "CSET_MARK_CELL"
        const val CSET_MARK_CHIP = "CSET_MARK_CHIP"
        const val CSET_MARK_DESC = "CSET_MARK_DESC"
        const val CSET_SORT_TICKET = "CSET_SORT_TICKET"
        const val CSET_SORT_XP = "CSET_SORT_XP"
        const val CSET_MAXLEVEL_DESC = "CSET_MAXLEVEL_DESC"
        const val CSET_COLOR_DESC = "CSET_COLOR_DESC"
        const val CSET_ROTATION_DESC = "CSET_ROTATION_DESC"
        const val CSET_SYMMETRY_DESC = "CSET_SYMMETRY_DESC"
        const val CSET_CONFIRM_FILTER_TITLE = "CSET_CONFIRM_FILTER_TITLE"
        const val CSET_CONFIRM_FILTER_BODY = "CSET_CONFIRM_FILTER_BODY"

        // RESEARCH
        const val RESEARCH_TITLE = "RESEARCH_TITLE"
        const val RESEARCH_WTF = "RESEARCH_WTF"
        const val RESEARCH_THREAD = "RESEARCH_THREAD"
        const val RESEARCH_START = "RESEARCH_START"
        const val RESEARCH_STOP = "RESEARCH_STOP"
        const val RESEARCH_READY = "RESEARCH_READY"
        const val RESEARCH_EMPTY = "RESEARCH_EMPTY"
        const val RESEARCH_WAITING = "RESEARCH_WAITING"
        const val RESEARCH_WORKING = "RESEARCH_WORKING"

        // WARNING
        const val WARNING_HOCMAX = "WARNING_HOCMAX"
        const val WARNING_HOCMAX_DESC = "WARNING_HOCMAX_DESC"
        const val WARNING_TIME = "WARNING_TIME"
        const val WARNING_TIME_DESC = "WARNING_TIME_DESC"

        // COMB
        const val COMB_REMAINING = "COMB_REMAINING"
        const val COMB_DESC = "COMB_DESC"
        const val COMB_NONEFOUND = "COMB_NONEFOUND"
        const val COMB_TAB_RESULT = "COMB_TAB_RESULT"
        const val COMB_TAB_FREQ = "COMB_TAB_FREQ"
        const val COMB_MARK_CONTINUE_TITLE = "COMB_MARK_CONTINUE_TITLE"
        const val COMB_MARK_CONTINUE_BODY = "COMB_MARK_CONTINUE_BODY"
        const val COMB_DNE_TITLE = "COMB_DNE_TITLE"
        const val COMB_DNE_BODY = "COMB_DNE_BODY"
        const val COMB_OPTION_M2_0 = "COMB_OPTION_M2_0"
        const val COMB_OPTION_M2_1 = "COMB_OPTION_M2_1"
        const val COMB_OPTION_M2_2 = "COMB_OPTION_M2_2"
        const val COMB_OPTION_M2_DESC = "COMB_OPTION_M2_DESC"
        const val COMB_OPTION_DEFAULT_0 = "COMB_OPTION_DEFAULT_0"
        const val COMB_OPTION_DEFAULT_1 = "COMB_OPTION_DEFAULT_1"
        const val COMB_OPTION_DEFAULT_DESC = "COMB_OPTION_DEFAULT_DESC"
        const val COMB_OPTION_FILTER_DESC = "COMB_OPTION_FILTER_DESC"
        const val COMB_OPTION_TITLE = "COMB_OPTION_TITLE"
        const val COMB_ERROR_STAT_TITLE = "COMB_ERROR_STAT_TITLE"
        const val COMB_ERROR_STAT_BODY = "COMB_ERROR_STAT_BODY"

        // FILE
        const val FILE_EXT_INV_OPEN = "FILE_EXT_INV_OPEN"
        const val FILE_EXT_INV_SAVE = "FILE_EXT_INV_SAVE"
        const val FILE_EXT_COMB = "FILE_EXT_COMB"
        const val FILE_SAVE_TITLE = "FILE_SAVE_TITLE"
        const val FILE_SAVE_BODY = "FILE_SAVE_BODY"
        const val FILE_OVERWRITE_TITLE = "FILE_OVERWRITE_TITLE"
        const val FILE_OVERWRITE_BODY = "FILE_OVERWRITE_BODY"

        // JSON
        const val JSON_TITLE = "JSON_TITLE"
        const val JSON_FILTER_STAR = "JSON_FILTER_STAR"
        const val JSON_FILTER_SIZE = "JSON_FILTER_SIZE"
        const val JSON_MARK = "JSON_MARK"

        // </editor-fold>
        private val KEYS_HTML = listOf(
            DISPLAY_HOWTO_ADDLANGUAGE,
            HELP_CHIP_INFO_POINT_BODY,
            HELP_CHIP_INFO_EFFICIENCY_BODY,
            HELP_CHIP_INFO_COLOR_BODY,
            HELP_CHIP_INFO_CALC_BODY,
            HELP_CHIP_CALC_DESC,
            HELP_APP_IMPORT_PROXY,
            HELP_APP_IMPORT_IMAGESCAN,
            HELP_APP_IMPORT_OPEN,
            HELP_APP_OPTIMIZE,
            HELP_APP_OPTIMIZE_FILTER,
            HELP_APP_OPTIMIZE_SETTING,
            HELP_APP_OPTIMIZE_MARK,
            HELP_PROXY_DESC,
            PROXY_WARNING,
            PROXY_STAGE1_INFO,
            PROXY_STAGE2_INFO,
            APPLY_TAG_DESC,
            WARNING_HOCMAX_DESC,
            WARNING_TIME_DESC,
            COMB_OPTION_M2_DESC,
            COMB_OPTION_DEFAULT_DESC,
            COMB_OPTION_FILTER_DESC,
            RESEARCH_WTF
        )
        private val KEYS_HTML_CENTER = listOf(
            COMB_DESC
        )
        private val KEYS_PLURAL = listOf(
            UNIT_STAR, UNIT_CELL
        )
        private const val DISPLAY_LANGUAGE_PREVIEW_DEFAULT = "ABC abc 1234567890 ★"
        fun getFileContent(locale: Locale): String {
            val prop: Properties? = if (LANGMAP.containsKey(locale)) {
                LANGMAP[locale]
            } else {
                return ""
            }
            val sb = StringBuilder()
            sb.append("# ").append(App.VERSION.toData()).append(System.lineSeparator())
            val keyList: MutableList<String> = mutableListOf()
            prop!!.keys.forEach(Consumer { k: Any -> keyList.add(k.toString()) })
            keyList.sort()
            var prevKeyPrefix = ""
            for (key in keyList) {
                val keyPrefix = key.substring(0, key.indexOf('_'))
                if (prevKeyPrefix.isNotEmpty() && prevKeyPrefix != keyPrefix) {
                    sb.append(System.lineSeparator())
                }
                val value = prop.getProperty(key)
                sb.append(key).append("=").append(value).append(System.lineSeparator())
                prevKeyPrefix = keyPrefix
            }
            return sb.toString()
        } // </editor-fold>
    }
}