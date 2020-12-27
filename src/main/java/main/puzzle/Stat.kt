package main.puzzle

import main.puzzle.Stat
import java.io.Serializable
import kotlin.math.min

/**
 *
 * @author Bunnyspa
 */
class Stat : Comparable<Stat>, Serializable {
    val dmg: Int
    val brk: Int
    val hit: Int
    val rld: Int

    constructor() {
        dmg = 0
        brk = 0
        hit = 0
        rld = 0
    }

    constructor(`val`: Int) {
        dmg = `val`
        brk = `val`
        hit = `val`
        rld = `val`
    }

    constructor(dmg: Int, brk: Int, hit: Int, rld: Int) {
        this.dmg = dmg
        this.brk = brk
        this.hit = hit
        this.rld = rld
    }

    constructor(v: IntArray) {
        dmg = v[0]
        brk = v[1]
        hit = v[2]
        rld = v[3]
    }

    constructor(stats: Collection<Stat?>) {
        val s = IntArray(4)
        for (stat in stats) {
            val array = stat!!.toArray()
            for (i in 0..3) {
                s[i] += array[i]
            }
        }
        dmg = s[0]
        brk = s[1]
        hit = s[2]
        rld = s[3]
    }

    fun allGeq(s: Stat): Boolean {
        return s.dmg <= dmg && s.brk <= brk && s.hit <= hit && s.rld <= rld
    }

    fun allLeq(s: Stat): Boolean {
        return dmg <= s.dmg && brk <= s.brk && hit <= s.hit && rld <= s.rld
    }

    fun limit(max: Stat): Stat {
        val newDmg = min(dmg, max.dmg)
        val newBrk = min(brk, max.brk)
        val newHit = min(hit, max.hit)
        val newRld = min(rld, max.rld)
        return Stat(newDmg, newBrk, newHit, newRld)
    }

    fun toStringSlash(): String {
        return java.lang.String.join("/", dmg.toString(), brk.toString(), hit.toString(), rld.toString())
    }

    fun allZero(): Boolean {
        return dmg == 0 && brk == 0 && hit == 0 && rld == 0
    }

    fun toArray(): IntArray {
        return intArrayOf(dmg, brk, hit, rld)
    }

    fun sum(): Int {
        return dmg + brk + hit + rld
    }

    fun toData(): String {
        return java.lang.String.join(",", dmg.toString(), brk.toString(), hit.toString(), rld.toString())
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 53 * hash + dmg
        hash = 53 * hash + brk
        hash = 53 * hash + hit
        hash = 53 * hash + rld
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (obj.javaClass == Stat::class.java) {
            val fstat = obj as Stat
            return dmg == fstat.dmg && brk == fstat.brk && hit == fstat.hit && rld == fstat.rld
        }
        return false
    }

    override fun compareTo(o: Stat): Int {
        if (dmg != o.dmg) {
            return o.dmg - dmg
        }
        if (brk != o.brk) {
            return o.brk - brk
        }
        if (hit != o.hit) {
            return o.hit - hit
        }
        return if (rld != o.rld) {
            o.rld - rld
        } else 0
    }

    override fun toString(): String {
        return "[" + toData() + "]"
    }

    companion object {
        const val DMG = 0
        const val BRK = 1
        const val HIT = 2
        const val RLD = 3
        fun chipStatSum(chips: Collection<Chip>): Stat {
            var dmg = 0
            var brk = 0
            var hit = 0
            var rld = 0
            for (chip in chips) {
                val s = chip.getStat()
                dmg += s.dmg
                brk += s.brk
                hit += s.hit
                rld += s.rld
            }
            return Stat(dmg, brk, hit, rld)
        }

        fun chipPtSum(chips: Collection<Chip>): Stat {
            var dmg = 0
            var brk = 0
            var hit = 0
            var rld = 0
            for (chip in chips) {
                val s = chip.pt
                dmg += s!!.dmg
                brk += s.brk
                hit += s.hit
                rld += s.rld
            }
            return Stat(dmg, brk, hit, rld)
        }

        fun chipOldStatSum(chips: Collection<Chip>): Stat {
            var dmg = 0
            var brk = 0
            var hit = 0
            var rld = 0
            for (chip in chips) {
                val s = chip.getOldStat()
                dmg += s.dmg
                brk += s.brk
                hit += s.hit
                rld += s.rld
            }
            return Stat(dmg, brk, hit, rld)
        }
    }
}