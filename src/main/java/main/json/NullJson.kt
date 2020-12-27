package main.json

/**
 *
 * @author Bunnyspa
 */
class NullJson : Json {
    override val type: Int
        get() = Json.NULL

    override fun toString(): String {
        return "null"
    }
}