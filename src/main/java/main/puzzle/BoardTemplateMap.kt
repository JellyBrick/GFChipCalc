package main.puzzle

import main.util.DoubleKeyHashMap

/**
 *
 * @author Bunnyspa
 */
class BoardTemplateMap {
    private val data: DoubleKeyHashMap<String, Int, List<BoardTemplate>> = DoubleKeyHashMap()
    private val minTypeMap: DoubleKeyHashMap<String, Int, Shape.Type> = DoubleKeyHashMap()
    fun put(name: String?, star: Int, templates: List<BoardTemplate>, minType: Shape.Type?) {
        if (templates.isNotEmpty()) {
            data.put((name)!!, star, templates)
        }
        minTypeMap.put((name)!!, star, (minType)!!)
    }

    operator fun get(name: String?, star: Int): List<BoardTemplate>? {
        if (containsKey(name, star)) {
            return data[name!!, star]
        }
        return null
    }

    fun getMinType(name: String?, star: Int): Shape.Type {
        if (containsMinTypeKey(name, star)) {
            return minTypeMap.getValue(name!!, star)
        }
        return Shape.Type.NONE
    }

    fun containsKey(name: String?, star: Int): Boolean {
        return data.containsKey((name)!!, star)
    }

    private fun containsMinTypeKey(name: String?, star: Int): Boolean {
        return minTypeMap.containsKey((name)!!, star)
    }
}