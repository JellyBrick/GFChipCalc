package main.json

/**
 *
 * @author Bunnyspa
 */
class NumberJson(data: String) : Json {
    private val data: String = data.trim { it <= ' ' }
    val integer: Int
        get() = data.toInt()
    val double: Double
        get() = data.toDouble()
    override val type: Int
        get() = Json.NUMBER

    override fun toString(): String {
        return data
    }

}