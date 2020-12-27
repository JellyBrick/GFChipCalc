package main.util

/**
 *
 * @author Bunnyspa
 */
class Version3 {
    val v1: Int
    val v2: Int
    val v3: Int

    constructor() {
        v1 = 0
        v2 = 0
        v3 = 0
    }

    constructor(version: String) {
        val verStrs = version.split("\\.").toTypedArray()
        if (3 == verStrs.size) {
            v1 = verStrs[0].toInt()
            v2 = verStrs[1].toInt()
            v3 = verStrs[2].toInt()
        } else {
            v1 = 0
            v2 = 0
            v3 = 0
        }
    }

    constructor(v1: Int, v2: Int, v3: Int) {
        this.v1 = v1
        this.v2 = v2
        this.v3 = v3
    }

    fun isCurrent(cv1: Int, cv2: Int, cv3: Int): Boolean {
        if (v1 < cv1) {
            return false
        }
        return if (v1 == cv1 && v2 < cv2) {
            false
        } else !(v1 == cv1 && v2 == cv2 && v3 < cv3)
    }

    fun isCurrent(version: String): Boolean {
        val v = Version3(version)
        return isCurrent(v.v1, v.v2, v.v3)
    }

    fun toData(): String {
        return java.lang.String.join(".", v1.toString(), v2.toString(), v3.toString())
    }

    override fun toString(): String {
        return toData()
    }
}