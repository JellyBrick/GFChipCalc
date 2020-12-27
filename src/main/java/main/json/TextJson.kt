package main.json

/**
 *
 * @author Bunnyspa
 */
class TextJson(data: String) : Json {
    val text: String = data.trim { it <= ' ' }.replace("^\"|\"$".toRegex(), "")
    override val type: Int
        get() = Json.TEXT

    override fun toString(): String {
        return "\"" + text + "\""
    }

}