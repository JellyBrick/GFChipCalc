package main.json

import java.util.*

/**
 *
 * @author Bunnyspa
 */
class ObjectJson(data: String) : Json {
    val keys: MutableList<String?>
    private val data: MutableMap<String?, Json>

    val isEmpty: Boolean
        get() = keys.isEmpty()

    fun containsKey(key: String?): Boolean {
        return keys.contains(key)
    }

    fun getValue(s: String?): Json? {
        return if (keys.contains(s)) {
            data[s]
        } else null
    }

    operator fun set(key: String?, j: Json) {
        if (!keys.contains(key)) {
            keys.add(key)
        }
        data[key] = j
    }

    override val type: Int
        get() = Json.OBJECT

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("{")
        for (i in keys.indices) {
            val key = keys[i]
            val d = data[key]
            sb.append("\"").append(key).append("\":").append(d.toString())
            if (i < keys.size - 1) {
                sb.append(",")
            }
        }
        sb.append("}")
        return sb.toString()
    }

    init {
        // Init
        var data = data
        keys = mutableListOf()
        this.data = HashMap()
        // Trim
        data = data.trim { it <= ' ' }.replace("^\\{|}$".toRegex(), "")
        var si = 0
        while (si < data.length) {
            val ei: Int = Json.getEndIndex(data, si)
            val key = data.substring(si, ei + 1).trim { it <= ' ' }.replace("^\"|\"$".toRegex(), "")
            val vsi = data.indexOf(':', ei) + 1
            val vei: Int = Json.getEndIndex(data, vsi)
            val vStr = data.substring(vsi, vei + 1)
            val value: Json = Json.parse(vStr)
            keys.add(key)
            this.data[key] = value
            val ci = data.indexOf(',', vei) + 1
            if (ci == 0) {
                break
            }
            si = ci
        }
    }
}