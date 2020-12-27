package main.util

/**
 *
 * @author Bunnyspa
 */
class Version2 {
    val v1: Int
    val v2: Int

    constructor(version: String) {
        val verStrs = version.split("\\.").toTypedArray()
        if (2 == verStrs.size) {
            v1 = verStrs[0].toInt()
            v2 = verStrs[1].toInt()
        } else {
            v1 = 0
            v2 = 0
        }
    }

    constructor(v1: Int, v2: Int) {
        this.v1 = v1
        this.v2 = v2
    }

    fun isCurrent(cv1: Int, cv2: Int): Boolean {
        return if (v1 < cv1) {
            false
        } else !(v1 == cv1 && v2 < cv2)
    }

    fun isCurrent(version: String): Boolean {
        val v = Version2(version)
        return isCurrent(v.v1, v.v2)
    }

    fun toData(): String {
        return java.lang.String.join(".", v1.toString(), v2.toString())
    }

    override fun toString(): String {
        return toData()
    }
}