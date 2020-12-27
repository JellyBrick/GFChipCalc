package main.json

/**
 *
 * @author Bunnyspa
 */
class BooleanJson(data: String) : Json {
    val boolean: Boolean = java.lang.Boolean.parseBoolean(data.trim { it <= ' ' })
    override val type: Int
        get() = Json.BOOLEAN

    override fun toString(): String {
        return boolean.toString()
    }

}