package main.util

import java.util.*
import kotlin.NoSuchElementException

/**
 *
 * @author Bunnyspa
 * @param <KA>
 * @param <KB>
 * @param <V>
</V></KB></KA> */
open class DoubleKeyHashMap<KA, KB, V> {
    private val data: MutableMap<KA, MutableMap<KB, V>> = HashMap()
    fun put(s: KA, i: KB, value: V) {
        init(s)
        data[s]!![i] = value
    }

    fun put(s: KA, m: Map<KB, V>) {
        init(s)
        data[s]!!.putAll(m)
    }

    operator fun get(s: KA, i: KB): V? {
        return if (containsKey(s, i)) {
            data[s]?.get(i)
        } else null
    }

    fun getValue(s: KA, i: KB): V {
        return if (containsKey(s, i)) {
            data[s]!![i]!!
        } else throw NoSuchElementException()
    }

    fun containsKey(s: KA, i: KB): Boolean {
        return data.containsKey(s) && data[s]?.containsKey(i) ?: false
    }

    fun keySet(s: KA): Set<KB>? {
        return if (data.containsKey(s)) {
            data[s]?.keys
        } else null
    }

    fun size(s: KA): Int {
        return data[s]!!.size
    }

    private fun init(s: KA) {
        if (!data.containsKey(s)) {
            data[s] = HashMap()
        }
    }

    override fun toString(): String {
        return data.toString()
    }
}