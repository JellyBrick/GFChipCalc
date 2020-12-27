package main.puzzle

import main.util.Fn
import java.awt.Color
import java.io.Serializable
import java.util.*

/**
 *
 * @author Bunnyspa
 */
class Tag : Serializable, Comparable<Any> {
    var name: String
    var color: Color

    constructor(color: Color, name: String) {
        this.color = color
        this.name = name
    }

    constructor() {
        val random = Random()
        color = Fn.getColor(random.nextFloat())
        name = NAME_DEFAULT
    }

    fun toData(): String {
        val c = color
        return String.format("%02X%02X%02X", c.red, c.green, c.blue) + name
    }

    override fun toString(): String {
        return toData()
    }

    override fun compareTo(o: Any): Int {
        val t = o as Tag
        return name.compareTo(t.name)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null || this.javaClass != obj.javaClass) {
            return false
        }
        val tag = obj as Tag
        return name == tag.name && color.rgb == tag.color.rgb
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 89 * hash + Objects.hashCode(name)
        hash = 89 * hash + Objects.hashCode(color.rgb)
        return hash
    }

    companion object {
        private const val NAME_DEFAULT = "New Tag"
        fun getTags(chips: List<Chip>): List<Tag> {
            val tagSet: MutableSet<Tag> = hashSetOf()
            chips.forEach { c -> tagSet.addAll(c.tags) }
            val list = tagSet.toMutableList()
            list.sort()
            return list
        }
    }
}