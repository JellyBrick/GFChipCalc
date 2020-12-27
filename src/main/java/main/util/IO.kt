package main.util

import main.App
import main.puzzle.*
import main.puzzle.assembly.CalcExtraSetting
import main.puzzle.assembly.CalcSetting
import main.puzzle.assembly.Progress
import main.puzzle.assembly.ProgressFile
import main.setting.BoardSetting
import main.setting.Setting
import main.ui.resource.AppText
import java.awt.Color
import java.awt.Component
import java.awt.Desktop
import java.awt.Point
import java.io.*
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 *
 * @author Bunnyspa
 */
object IO {
    const val EXT_INVENTORY = "gfci"
    const val EXT_COMBINATION = "gfcc"
    private const val PATH_EX_LANG = "language"
    private const val FILENAME_SETTINGS = "settings.dat"
    private const val FILENAME_UPDATE = "GFChipCalc-Update.jar"
    private const val URL_GITHUB_MAIN = "https://github.com/Bunnyspa/GFChipCalc/releases/latest"
    private const val URL_GITHUB_UPDATE = "https://github.com/Bunnyspa/GFChipCalc-Update/releases/latest"
    private const val URL_DOWNLOAD_UPDATE = "$URL_GITHUB_UPDATE/download/GFChipCalc-Update.jar"
    private fun pre420rotation(shape: Shape): Int {
        return when (shape) {
            Shape._4_I, Shape._5A_I, Shape._5A_Z, Shape._5A_Zm, Shape._5B_F, Shape._6_A, Shape._6_C, Shape._6_D, Shape._6_I, Shape._6_R -> 1
            Shape._4_T, Shape._5A_P, Shape._5A_Pm, Shape._5B_T, Shape._6_Y -> 2
            Shape._4_Lm, Shape._5A_C, Shape._5B_W, Shape._5B_Fm, Shape._6_T -> 3
            else -> 0
        }
    }

    fun loadInventory(fileName: String): List<Chip> {
        val chips: MutableList<Chip> = mutableListOf()
        try {
            BufferedReader(FileReader(fileName)).use { br ->
                val bri = br.lines().iterator()
                if (bri.hasNext()) {
                    val s = bri.next()
                    val v = Version3(s)
                    if (v.isCurrent(5, 3, 0)) {
                        val tags: MutableSet<Tag> = HashSet()
                        bri.forEachRemaining { l: String -> chips.add(parseChip(v, l, tags)) }
                    } else if (v.isCurrent(4, 0, 0)) {
                        // 4.0.0+
                        while (bri.hasNext()) {
                            val c = Chip(v, bri.next().split(";").toTypedArray(), Chip.INVENTORY)
                            // 4.0.0 - 4.1.x
                            if (!v.isCurrent(4, 2, 0)) {
                                c.initRotate(pre420rotation(c.shape))
                            }
                            chips.add(c)
                        }
                    } else {
                        // 1.0.0 - 3.0.0
                        chips.add(Chip(Version3(), s.split(",").toTypedArray(), Chip.INVENTORY))
                        while (bri.hasNext()) {
                            chips.add(Chip(Version3(), bri.next().split(",").toTypedArray(), Chip.INVENTORY))
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            App.log(ex)
        }
        return chips
    }

    fun saveInventory(fileName: String, chips: List<Chip?>) {
        try {
            BufferedWriter(FileWriter(fileName)).use { bw ->
                bw.write(App.VERSION.toData())
                bw.newLine()
                for (c in chips) {
                    bw.write(c!!.toData())
                    bw.newLine()
                }
            }
        } catch (ex: Exception) {
            App.log(ex)
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Combination (Pre 5.3.0)">
    private fun loadCombination(s: String, bri: Iterator<String>): List<Board> {
        val boards: MutableList<Board> = mutableListOf()
        val v = Version3(s)
        if (v.isCurrent(4, 0, 0)) {
            if (bri.hasNext()) {
                val info = bri.next().split(",").toTypedArray()
                val name = info[0]
                val star = info[1].toInt()
                var maxStat: Stat = Board.getMaxStat(name, star)
                if (v.isCurrent(4, 2, 0) && info.size > 2) {
                    maxStat = Stat(info[2].toInt(), info[3].toInt(), info[4].toInt(), info[5].toInt())
                } else if (info.size > 6 && "1" != info[6]) {
                    maxStat = Stat(info[2].toInt(), info[3].toInt(), info[4].toInt(), info[5].toInt())
                }
                while (bri.hasNext()) {
                    // Chips
                    val nChip = bri.next().toInt()
                    val chips: MutableList<Chip> = ArrayList(nChip)
                    val shapes: MutableList<Shape?> = ArrayList(nChip)
                    for (i in 0 until nChip) {
                        val chipInfo = bri.next().split(";").toTypedArray()
                        val c = Chip(v, chipInfo, Chip.COMBINATION)
                        chips.add(c)
                        if (!v.isCurrent(4, 2, 0)) {
                            var r = c.initRotation
                            r += pre420rotation(c.shape)
                            c.initRotation = r
                        }
                        shapes.add(c.shape)
                    }
                    chips.forEach(Consumer { obj: Chip -> obj.setMaxLevel() })

                    // Rotations
                    val rotStrs = bri.next().split(",").toTypedArray()
                    for (i in 0 until nChip) {
                        var r = rotStrs[i].toInt()
                        if (!v.isCurrent(4, 2, 0)) {
                            r += pre420rotation(shapes[i]!!)
                        }
                        chips[i].rotation = r
                    }
                    // Locations
                    val locStrs = bri.next().split(",").toTypedArray()
                    val locations: MutableList<Point> = ArrayList(locStrs.size)
                    for (locStr in locStrs) {
                        locations.add(parsePoint(locStr))
                    }

                    // Generate board
                    val b = Board(name, star, maxStat, chips, locations)
                    boards.add(b)
                }
            }
        } else {
            val info = s.split(",").toTypedArray()
            val name = info[0]
            val star = info[1].toInt()
            var max: Stat = Board.getMaxStat(name, star)
            if (info.size > 6 && "1" != info[6]) {
                max = Stat(info[2].toInt(), info[3].toInt(), info[4].toInt(), info[5].toInt())
            }
            while (bri.hasNext()) {
                // Chips
                val nChip = bri.next().toInt()
                val chips: MutableList<Chip> = mutableListOf()
                for (i in 0 until nChip) {
                    val c = Chip(Version3(), bri.next().split(",").toTypedArray(), Chip.COMBINATION)
                    c.rotate(pre420rotation(c.shape))
                    chips.add(c)
                }
                chips.forEach(Consumer { obj: Chip -> obj.setMaxLevel() })

                // Matrix
                val matrix = PuzzleMatrix(Board.HEIGHT, Board.WIDTH, Board.UNUSED)
                for (row in 0 until Board.HEIGHT) {
                    val rowStrs = bri.next()
                    for (col in 0 until Board.WIDTH) {
                        val rowChar = rowStrs.substring(col, col + 1)
                        if ("-" != rowChar) {
                            matrix[row, col] = Integer.valueOf(rowChar)
                        }
                    }
                }
                val chipLocs = Board.toLocation(matrix)
                if (chips.size == chipLocs.size) {
                    val b = Board(name, star, max, chips, chipLocs)
                    boards.add(b)
                }
            }
        }
        return boards
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Progress">
    fun loadProgressFile(fileName: String, invChips: List<Chip?>): ProgressFile? {
        try {
            BufferedReader(FileReader(fileName)).use { br ->
                val bri = br.lines().iterator()
                if (bri.hasNext()) {
                    val s = bri.next()
                    val v = Version3(s)
                    return if (v.isCurrent(5, 3, 0)) {
                        parseProgressFile(v, bri, invChips)
                    } else {
                        val boards = loadCombination(s, bri)
                        var name = ""
                        var star = 0
                        var stat = Stat()
                        if (boards.isNotEmpty()) {
                            val b = boards[0]
                            name = b.name
                            star = b.star
                            stat = b.getCustomMaxStat()
                        }
                        val chipSet: MutableSet<Chip> = HashSet()
                        boards.forEach(Consumer { b: Board -> b.forEachChip { e: Chip -> chipSet.add(e) } })
                        loadProgress_adjustInits(chipSet, invChips)
                        ProgressFile(
                            CalcSetting(name, star, false, false, false, stat, null),
                            CalcExtraSetting(
                                CalcExtraSetting.CALCMODE_FINISHED,
                                0,
                                false,
                                0,
                                0,
                                0,
                                0,
                                ArrayList(chipSet)
                            ),
                            Progress(0, -1, 1, 1, boards)
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            App.log(ex)
        }
        return null
    }

    fun saveProgressFile(fileName: String, pf: ProgressFile) {
        try {
            BufferedWriter(FileWriter(fileName)).use { bw ->
                bw.write(App.VERSION.toData())
                bw.newLine()
                bw.write(pf.toData())
            }
        } catch (ex: Exception) {
            App.log(ex)
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Setting">
    fun loadSettings(): Setting {
        val lines: MutableList<String> = mutableListOf()
        val sgLines: MutableList<String> = mutableListOf()
        try {
            BufferedReader(FileReader(FILENAME_SETTINGS)).use { br ->
                val bri = br.lines().iterator()
                var section: String = Setting.SECTION_GENERAL
                while (bri.hasNext()) {
                    val s = bri.next().trim { it <= ' ' }
                    if (s.startsWith("[")) {
                        section = s.substring(1, s.indexOf(']'))
                    } else if (section.contains(Setting.SECTION_GENERAL)) {
                        lines.add(s)
                    } else if (section.contains(Setting.SECTION_BOARD)) {
                        sgLines.add(s)
                    }
                }
            }
        } catch (ex: FileNotFoundException) {
            return Setting()
        } catch (ex: Exception) {
            App.log(ex)
        }
        return Setting(lines, sgLines)
    }

    fun saveSettings(settings: Setting) {
        try {
            BufferedWriter(FileWriter(FILENAME_SETTINGS)).use { bw ->
                val s = settings.toData()
                bw.write(s)
            }
        } catch (ex: Exception) {
            App.log(ex)
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Preset">
    fun loadBoardTemplates(name: String, star: Int, isPartial: Boolean): List<BoardTemplate> {
        val out: MutableList<BoardTemplate> = mutableListOf()
        val fileName = "template_" + toFileName(name) + "_" + star + (if (isPartial) "_p" else "") + ".dat"
        val url: URL = App.getResource("template/$fileName")
        try {
            BufferedReader(InputStreamReader(url.openStream())).use { br ->
                val bri = br.lines().iterator()
                while (bri.hasNext()) {
                    val line = bri.next()
                    out.add(loadBoardTemplate(name, star, line))
                }
            }
        } catch (ignored: Exception) {
        }
        return out
    }

    private fun loadBoardTemplate(name: String, star: Int, line: String): BoardTemplate {
        val split = line.split(";").toTypedArray()
        val names = split[0].split(",").toTypedArray()
        val rotations = split[1].split(",").toTypedArray()
        val locations = split[2].split(",").toTypedArray()
        val puzzles: MutableList<Puzzle> = ArrayList(names.size)
        for (i in names.indices) {
            puzzles.add(
                Puzzle(
                    Shape.byId(names[i].toInt()), rotations[i].toInt(),
                    parsePoint(locations[i])
                )
            )
        }
        return if (split.size > 3) {
            // Symmetry
            val symmetry = parseBoolean(split[3])
            BoardTemplate(name, star, puzzles, symmetry)
        } else {
            BoardTemplate(name, star, puzzles)
        }
    }

    fun toFileName(boardName: String): String {
        return boardName.replace("-", "").replace(" ", "").toLowerCase()
    }

    // </editor-fold>
    private fun getNameWOExt(s: String): String {
        val lastIndex = s.lastIndexOf('.')
        return s.substring(0, lastIndex)
    }

    // <editor-fold defaultstate="collapsed" desc="Locales and Properties">
    val internalLocales: List<Locale>
        get() = ArrayList(listOf(*AppText.LOCALES))
    val externalLocales: List<Locale>
        get() {
            val locales: MutableList<Locale> = mutableListOf()
            val folder = File(PATH_EX_LANG)
            if (!folder.exists() || !folder.isDirectory) {
                return locales
            }
            for (file in folder.listFiles()) {
                val name = getNameWOExt(file.name)
                locales.add(Locale.forLanguageTag(name.replace("_", "-")))
            }
            return locales
        }
    val locales: List<Locale>
        get() {
            val locales: MutableList<Locale> = ArrayList(internalLocales)
            externalLocales.forEach(Consumer { locale: Locale ->
                if (!locales.contains(locale)) {
                    locales.add(locale)
                }
            })
            return locales
        }

    fun exportProps(app: App, component: Component?) {
        val folder = File(PATH_EX_LANG)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                JOptionPane.showMessageDialog(
                    component,
                    app.getText(AppText.DISPLAY_EXPORT_FAIL_BODY),
                    app.getText(AppText.DISPLAY_EXPORT_FAIL_TITLE),
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
        try {
            for (locale in internalLocales) {
                val filePath = PATH_EX_LANG + "/" + locale.toLanguageTag() + ".properties"
                val fileContent: String = AppText.getFileContent(locale)
                write(filePath, fileContent)
            }
            JOptionPane.showMessageDialog(
                component,
                app.getText(AppText.DISPLAY_EXPORT_DONE_BODY, PATH_EX_LANG),
                app.getText(AppText.DISPLAY_EXPORT_DONE_TITLE),
                JOptionPane.INFORMATION_MESSAGE
            )
        } catch (ex: IOException) {
            JOptionPane.showMessageDialog(
                component,
                AppText.DISPLAY_EXPORT_FAIL_BODY,
                AppText.DISPLAY_EXPORT_FAIL_TITLE,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun getProp(locale: Locale): Properties {
        val lang = locale.language + "-" + locale.country
        return if (externalLocales.contains(locale)) {
            getProp("$PATH_EX_LANG/$lang.properties")
        } else Properties()
    }

    private fun getProp(filePath: String): Properties {
        try {
            InputStreamReader(FileInputStream(filePath), StandardCharsets.UTF_8).use { r ->
                val props = Properties()
                props.load(r)
                return props
            }
        } catch (ex: Exception) {
            return Properties()
        }
    }

    // </editor-fold>
    @Throws(IOException::class)
    fun write(filePath: String, fileContent: String) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(filePath), StandardCharsets.UTF_8)).use { w ->
            w.write(
                fileContent
            )
        }
    }

    @Throws(IOException::class)
    fun read(filePath: String): List<String> {
        val l: MutableList<String> = mutableListOf()
        BufferedReader(InputStreamReader(FileInputStream(filePath), StandardCharsets.UTF_8)).use { r ->
            val s = r.lines()
            s.forEach { t: String -> l.add(t.trim { it <= ' ' }) }
        }
        return l
    }

    fun checkNewVersion(app: App) {
        val mainLatest = getVersion(URL_GITHUB_MAIN, App.VERSION.toData())
        if (!App.VERSION.isCurrent(mainLatest)) {
            val retval = JOptionPane.showConfirmDialog(
                app.mf,
                app.getText(AppText.NEWVER_CONFIRM_BODY, mainLatest),
                app.getText(AppText.NEWVER_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION
            )
            if (retval == JOptionPane.YES_OPTION) {
                if (!runUpdate(app)) {
                    openWeb(app, app.mf, URL_GITHUB_MAIN)
                }
            }
        }
    }

    private fun getVersion(url: String, defaultVersion: String): String {
        return try {
            var latest: String
            val con = URL(url).openConnection()
            con.connect()
            con.getInputStream().use { `is` ->
                val redirected = con.url.toString()
                latest = redirected.substring(redirected.lastIndexOf("/") + 2)
            }
            latest
        } catch (ex: IOException) {
            defaultVersion
        }
    }

    private fun runUpdate(app: App): Boolean {
        val updateLatest = getVersion(URL_GITHUB_UPDATE, app.setting.updateVersion.toData())
        val path = File("").absolutePath
        try {
            val exePath = path + "\\" + FILENAME_UPDATE
            val exeFile = File(exePath)
            if (!app.setting.updateVersion.isCurrent(updateLatest) || !exeFile.exists()) {
                downloadUpdate()
                app.setting.updateVersion = Version2(updateLatest)
                app.mf.settingFile_save()
            }
            if (exeFile.exists()) {
                val process = ProcessBuilder("java", "-jar", exePath)
                process.directory(File(path + "\\"))
                process.start()
                exitProcess(0)
            }
        } catch (ignored: IOException) {
        }
        return false
    }

    private fun downloadUpdate() {
        try {
            BufferedInputStream(URL(URL_DOWNLOAD_UPDATE).openStream()).use { inputStream ->
                FileOutputStream(
                    FILENAME_UPDATE
                ).use { fileOS ->
                    val data = ByteArray(1024)
                    var byteContent: Int
                    while (inputStream.read(data, 0, 1024).also { byteContent = it } != -1) {
                        fileOS.write(data, 0, byteContent)
                    }
                }
            }
        } catch (ex: Exception) {
            Logger.getLogger(IO::class.java.name).log(Level.SEVERE, null, ex)
        }
    }

    fun openWeb(app: App, c: Component?, link: String) {
        try {
            Desktop.getDesktop().browse(URI(link))
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(
                c,
                app.getText(AppText.NEWVER_ERROR_BODY),
                app.getText(AppText.NEWVER_ERROR_TITLE),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Parsing">
    //========== Boolean ==========//
    fun data(b: Boolean): String {
        return if (b) "1" else "0"
    }

    fun parseBoolean(s: String): Boolean {
        return "1" == s || "true".equals(s, ignoreCase = true)
    }

    //========== String ==========//
    fun data(s: List<String>, delim: String): String {
        return java.lang.String.join(delim, s)
    }

    private fun parseStringList(s: String): List<String> {
        return listOf(*s.split(",").toTypedArray())
    }

    //========== UUID ==========//
    fun data(u: UUID): String {
        return u.toString()
    }

    //========== Point ==========//
    fun data(p: Point): String {
        return p.x.toString() + "." + p.y
    }

    fun parsePoint(s: String): Point {
        val split = s.split("\\.").toTypedArray()
        return Point(split[0].toInt(), split[1].toInt())
    }

    //========== Tag ==========//
    fun parseTag(s: String): Tag {
        return if (s.length > 6) {
            val color = Color.decode("#" + s.substring(0, 6))
            val name = s.substring(6)
            Tag(color, name)
        } else {
            Tag()
        }
    }

    //========== Stat ==========//
    fun parseStat(s: String): Stat {
        val d = s.split(",").toTypedArray()
        val dmg = if (d.isNotEmpty()) d[0].toInt() else 0
        val brk = if (d.size > 1) d[1].toInt() else 0
        val hit = if (d.size > 2) d[2].toInt() else 0
        val rld = if (d.size > 3) d[3].toInt() else 0
        return Stat(dmg, brk, hit, rld)
    }

    //========== Board Setting ==========//
    fun parseBS(data: List<String>, advancedSetting: Boolean): BoardSetting {
        val out = BoardSetting()
        data.stream().map { line: String -> line.split(";").toTypedArray() }
            .forEachOrdered { parts: Array<String> ->
                val name = parts[0]
                if (listOf(*Board.NAMES).contains(name)) {
                    val star = parts[1].toInt()
                    if (advancedSetting) {
                        val mode = parts[2].toInt()
                        out.setMode(name, star, mode)
                    }
                    val stat = parseStat(parts[3])
                    val pt = parseStat(parts[4])
                    out.setStat(name, star, stat)
                    out.setPt(name, star, pt)
                    if (parts.size > 5) {
                        out.setPresetIndex(name, star, parts[5].toInt())
                    }
                }
            }
        return out
    }

    //========== Chip ==========//
    fun parseChip(v: Version3, s: String, tagPool: MutableSet<Tag>): Chip {
        val it: Iterator<String> = listOf(*s.split(";").toTypedArray()).iterator()
        val id = it.next()
        val shape: Shape =
            if (v.isCurrent(7, 0, 0)) Shape.byId(it.next().toInt()) else Shape.byName(it.next())
        val star = it.next().toInt()
        val color = it.next().toInt()
        val pt = parseStat(it.next())
        val initLevel = it.next().toInt()
        val initRotation = it.next().toInt()
        val isMarked = parseBoolean(it.next())
        val tags: MutableSet<Tag> = hashSetOf()
        if (it.hasNext()) {
            parseStringList(it.next()).forEach { tagStr: String ->
                val tag = parseTag(tagStr)
                var added = false
                for (t in tagPool) {
                    if (tag == t) {
                        tags.add(t)
                        added = true
                        break
                    }
                }
                if (!added) {
                    tags.add(tag)
                    tagPool.add(tag)
                }
            }
        }
        return Chip(id, shape, star, color, pt, initLevel, initRotation, isMarked, tags)
    }

    //========== Progress ==========//
    fun parseProgressFile(v: Version3, it: Iterator<String>, invChips: List<Chip?>): ProgressFile {
        val calcMode = it.next().toInt()
        val name = it.next()
        val star = it.next().toInt()
        if (!v.isCurrent(6, 5, 3) && calcMode == CalcExtraSetting.CALCMODE_FINISHED) {
            val stat = parseStat(it.next())
            var nComb = -1
            if (v.isCurrent(6, 4, 0)) {
                nComb = it.next().toInt()
            }
            val chips = parseProgress_chips(v, it, invChips)
            chips.forEach(Consumer { obj: Chip -> obj.setMaxLevel() })
            val boards = parseProgress_boards(name, star, stat, chips, it)
            return ProgressFile(
                CalcSetting(name, star, true, true, false, stat, Stat(0)),
                CalcExtraSetting(calcMode, 0, true, 0, 0, 0, 0, chips),
                Progress(0, nComb, 1, 1, boards)
            )
        }
        val maxLevel = parseBoolean(it.next())
        val matchColor = parseBoolean(it.next())
        val rotation = parseBoolean(it.next())
        var symmetry = false
        if (v.isCurrent(6, 9, 0)) {
            symmetry = parseBoolean(it.next())
        }
        val markMin = it.next().toInt()
        val markMax = it.next().toInt()
        val markType = it.next().toInt()
        val sortType = it.next().toInt()
        val stat = parseStat(it.next())
        val pt = parseStat(it.next())
        val nComb = it.next().toInt()
        val progress = it.next().toInt()
        val progMax = it.next().toInt()
        val tag = it.next().toInt()
        val chips = parseProgress_chips(v, it, invChips)
        if (maxLevel) {
            chips.forEach(Consumer { obj: Chip -> obj.setMaxLevel() })
        }
        val boards = parseProgress_boards(name, star, stat, chips, it)
        return ProgressFile(
            CalcSetting(name, star, maxLevel, rotation, symmetry, stat, pt),
            CalcExtraSetting(calcMode, tag, matchColor, markMin, markMax, markType, sortType, chips),
            Progress(sortType, nComb, progress, progMax, boards)
        )
    }

    private fun parseProgress_chips(v: Version3, it: Iterator<String>, invChips: List<Chip?>): List<Chip> {
        val nChip = it.next().toInt()
        val chips: MutableList<Chip> = mutableListOf()
        val tags: MutableSet<Tag> = HashSet()
        for (i in 0 until nChip) {
            chips.add(parseChip(v, it.next(), tags))
        }
        loadProgress_adjustInits(chips, invChips)
        return chips
    }

    private fun loadProgress_adjustInits(chips: Collection<Chip>, invChips: List<Chip?>) {
        chips.forEach(Consumer { c: Chip ->
            for (ic in invChips) {
                if (c == ic) {
                    c.initRotation = ic.initRotation
                    c.initLevel = ic.initLevel
                    break
                }
            }
        })
    }

    private fun parseProgress_boards(
        name: String,
        star: Int,
        stat: Stat,
        chips: List<Chip>,
        it: Iterator<String>
    ): List<Board> {
        val boards: MutableList<Board> = mutableListOf()
        while (it.hasNext()) {
            val n = it.next().toInt()
            val bChips: MutableList<Chip> = mutableListOf()
            val bLocs: MutableList<Point> = mutableListOf()
            for (k in 0 until n) {
                val split = it.next().split(",").toTypedArray()
                val i = split[0].toInt()
                val r = split[1].toInt()
                val l = parsePoint(split[2])
                val c = Chip(chips[i])
                c.rotation = r
                bChips.add(c)
                bLocs.add(l)
            }
            val board = Board(name, star, stat, bChips, bLocs)
            boards.add(board)
        }
        return boards
    } // </editor-fold>
}