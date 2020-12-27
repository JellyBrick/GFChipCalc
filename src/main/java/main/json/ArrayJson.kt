package main.json

import java.util.*

/**
 *
 * @author Bunnyspa
 */
class ArrayJson(data: String) : Json {
    private val data: MutableList<Json>
    val list: List<Json>
        get() = ArrayList(data)
    override val type: Int
        get() = Json.ARRAY

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        for (i in data.indices) {
            val d = data[i]
            sb.append(d.toString())
            if (i < data.size - 1) {
                sb.append(",")
            }
        }
        sb.append("]")
        return sb.toString()
    }

    init {
        // Init
        var data = data
        this.data = mutableListOf()
        // Trim
        data = data.trim { it <= ' ' }.replace("^\\[|]$".toRegex(), "")
        var si = 0
        while (si < data.length) {
            val ei: Int = Json.getEndIndex(data, si)
            val eStr = data.substring(si, ei + 1)
            val element: Json = Json.parse(eStr)
            this.data.add(element)
            val ci = data.indexOf(',', ei) + 1
            if (ci == 0) {
                break
            }
            si = ci
        }
    }
}