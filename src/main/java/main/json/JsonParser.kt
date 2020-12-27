package main.json

import main.App
import main.puzzle.*
import java.awt.Color
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 *
 * @author Bunnyspa
 */
object JsonParser {
    private const val SIGNKEY = "sign"
    private const val CHIPKEY_SQUAD = "squad_with_user_info"
    private const val CHIPKEY_CHIP = "chip_with_user_info"
    private const val UNKNOWN_HOC = "Unknown HOC"
    fun readFile(filePath: String): List<Chip> {
        try {
            BufferedReader(FileReader(filePath)).use { br ->
                val data = br.lines().collect(Collectors.joining())
                return parseChip(data)
            }
        } catch (ex: IOException) {
            App.log(ex)
            return mutableListOf()
        }
    }

    fun parseSign(data: String): String {
        val o: ObjectJson = Json.getObjectJson(Json.parse(data))
        return Json.getText(o.getValue(SIGNKEY)!!)
    }

    fun parseChip(data: String): List<Chip> {
        // Init
        val squadMap: MutableMap<Int, Tag> = HashMap()
        val chips: MutableList<Chip> = mutableListOf()
        try {
            // Parse
            val o: ObjectJson = Json.getObjectJson(Json.parse(data))

            // Squad
            val squadsJ = o.getValue(CHIPKEY_SQUAD)
            if (squadsJ != null) {
                if (squadsJ.type == Json.OBJECT) {
                    val squadJKeys = Json.getObjectKeys(squadsJ)
                    parseChip_squad(squadMap, squadJKeys.stream().map { jKey: String? ->
                        Json.getObjectJson(
                            Json.getObjectValue(squadsJ, jKey)!!
                        )
                    })
                } else if (squadsJ.type == Json.ARRAY) {
                    val squadJs = Json.getList(squadsJ)
                    parseChip_squad(
                        squadMap,
                        squadJs.stream().map { data: Json? -> Json.getObjectJson(data) }
                    )
                }
            }

            // Chip
            val chipsJ = o.getValue(CHIPKEY_CHIP)
            if (chipsJ?.type == Json.OBJECT) {
                val chipJKeys: List<String?> = Json.getObjectKeys(chipsJ)
                parseChip_chip(squadMap, chips, chipJKeys.stream().map { jKey: String? ->
                    Json.getObjectJson(
                        Json.getObjectValue(chipsJ, jKey)!!
                    )
                })
            } else if (chipsJ?.type == Json.ARRAY) {
                val chipJs: List<Json?> = Json.getList(chipsJ)
                parseChip_chip(
                    squadMap,
                    chips,
                    chipJs.stream().map { data: Json? -> Json.getObjectJson(data) }
                )
            }
            chips.reverse()
        } catch (ignored: Exception) {
        }
        return chips
    }

    private fun parseChip_squad(squadMap: MutableMap<Int, Tag>, stream: Stream<ObjectJson>) {
        stream.forEach { squadJ: ObjectJson ->
            val squadID: Int = Json.getText(squadJ.getValue("id")!!).toInt()
            val squadIndex: Int = Json.getText(squadJ.getValue("squad_id")!!).toInt() - 1
            squadMap[squadID] = boardTag(squadIndex)
        }
    }

    private fun parseChip_chip(squadMap: Map<Int, Tag>, chips: MutableList<Chip>, stream: Stream<ObjectJson>) {
        stream.forEach { chipJ: ObjectJson ->
            // Raw
            val id = (chipJ.getValue("id") as TextJson?)?.text
            val gridData = (chipJ.getValue("grid_id") as TextJson).text.toInt()
            val shape = Shape.byId(gridData)
            val dmg: Int = Json.getText(chipJ.getValue("assist_damage")!!).toInt()
            val brk: Int = Json.getText(chipJ.getValue("assist_def_break")!!).toInt()
            val hit: Int = Json.getText(chipJ.getValue("assist_hit")!!).toInt()
            val rld: Int = Json.getText(chipJ.getValue("assist_reload")!!).toInt()
            val star: Int = Json.getText(chipJ.getValue("chip_id")!!).substring(0, 1).toInt()
            val level: Int = Json.getText(chipJ.getValue("chip_level")!!).toInt()
            val color: Int = Json.getText(chipJ.getValue("color_id")!!).toInt() - 1
            val rotation: Int = Json.getText(chipJ.getValue("shape_info")!!).substring(0, 1).toInt()
            val squadID: Int = Json.getText(chipJ.getValue("squad_with_user_id")!!).toInt()
            val pt = Stat(dmg, brk, hit, rld)
            val chip = Chip(id, shape, star, color, pt, level, rotation)
            if (squadMap.containsKey(squadID)) {
                val tag = squadMap.getValue(squadID)
                chip.setTag(tag, true)
            } else if (squadID != 0) {
                val tag = boardTag(squadID - 10001)
                chip.setTag(tag, true)
            }
            chips.add(chip)
        }
    }

    private fun boardTag(index: Int): Tag {
        return if (index < Board.NAMES.size) {
            Tag(Color.GRAY, Board.NAMES[index])
        } else Tag(
            Color.GRAY,
            UNKNOWN_HOC
        )
    }
}