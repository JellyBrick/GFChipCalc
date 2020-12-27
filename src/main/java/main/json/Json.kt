package main.json

/**
 *
 * @author Bunnyspa
 */
interface Json {
    val type: Int

    companion object {
        fun parse(data: String): Json {
            val dataTrim = data.trim { it <= ' ' }
            return when (dataTrim[0]) {
                '{' -> ObjectJson(data)
                '[' -> ArrayJson(data)
                '"' -> TextJson(data)
                't', 'f' -> BooleanJson(data)
                'n' -> NullJson()
                else -> NumberJson(data)
            }
        }

        fun getEndIndex(data: String, i: Int): Int {
            var i = i
            while (i < data.length) {
                val next = data[i]
                if (!Character.isWhitespace(next)) {
                    return when (next) {
                        '{' -> getBracketEndIndex(data, i, OBJECT)
                        '[' -> getBracketEndIndex(data, i, ARRAY)
                        '"' -> {
                            i++
                            while (i < data.length) {
                                if (data[i] == '"' && (i == 0 || data[i - 1] != '\\')) {
                                    return i
                                }
                                i++
                            }
                            -1
                        }
                        't' -> {
                            if (data.startsWith("true", i)) {
                                i + 4
                            } else -1
                        }
                        'f' -> {
                            if (data.startsWith("false", i)) {
                                i + 5
                            } else -1
                        }
                        'n' -> {
                            if (data.startsWith("null", i)) {
                                i + 4
                            } else -1
                        }
                        else -> {
                            i++
                            while (i < data.length) {
                                val c = data[i]
                                if (c != '.' && (c < '0' || '9' < c)) {
                                    return i - 1
                                }
                                i++
                            }
                            i - 1
                        }
                    }
                }
                i++
            }
            return -1
        }

        private fun getBracketEndIndex(data: String, i: Int, type: Int): Int {
            var i = i
            return try {
                var bracketLevel = 0
                var quoting = false
                val openBracket = if (type == OBJECT) '{' else '['
                val closeBracket = if (type == OBJECT) '}' else ']'
                do {
                    val c = data[i]
                    if (c == '"' && (i == 0 || data[i - 1] != '\\')) {
                        quoting = !quoting
                    }
                    if (!quoting && c == openBracket) {
                        bracketLevel++
                    }
                    if (!quoting && c == closeBracket) {
                        bracketLevel--
                    }
                    i++
                } while (bracketLevel > 0 && i < data.length)
                i - 1
            } catch (ex: Exception) {
                -1
            }
        }

        @Throws(ClassCastException::class)
        fun getText(data: Json): String {
            if (data.type != TEXT) {
                throw ClassCastException(getClassCastExceptionMessage(data, TextJson::class.java.name))
            }
            return (data as TextJson).text
        }

        @Throws(ClassCastException::class)
        fun getList(data: Json): List<Json?> {
            if (data.type != ARRAY) {
                throw ClassCastException(getClassCastExceptionMessage(data, ArrayJson::class.java.name))
            }
            return (data as ArrayJson).list
        }

        @Throws(ClassCastException::class)
        fun getObjectJson(data: Json?): ObjectJson {
            if (data?.type != OBJECT) {
                throw ClassCastException(getClassCastExceptionMessage(data, ObjectJson::class.java.name))
            }
            return data as ObjectJson
        }

        @Throws(ClassCastException::class)
        fun getObjectKeys(data: Json): List<String?> {
            if (data.type != OBJECT) {
                throw ClassCastException(getClassCastExceptionMessage(data, ObjectJson::class.java.name))
            }
            return (data as ObjectJson).keys
        }

        @Throws(ClassCastException::class)
        fun getObjectValue(data: Json, key: String?): Json? {
            if (data.type != OBJECT) {
                throw ClassCastException(getClassCastExceptionMessage(data, ObjectJson::class.java.name))
            }
            return (data as ObjectJson).getValue(key)
        }

        private fun getClassCastExceptionMessage(data: Json?, cast: String?): String {
            val sb = StringBuilder()
            sb.append(Json::class.java.name).append(" cannot be cast to ").append(cast)
                .append("- It should be cast to ")
            when (data?.type) {
                NULL -> sb.append(NullJson::class.java.name)
                BOOLEAN -> sb.append(BooleanJson::class.java.name)
                NUMBER -> sb.append(NumberJson::class.java.name)
                TEXT -> sb.append(TextJson::class.java.name)
                ARRAY -> sb.append(ArrayJson::class.java.name)
                OBJECT -> sb.append(ObjectJson::class.java.name)
                else -> throw AssertionError()
            }
            return sb.toString()
        }

        const val NULL = 0
        const val BOOLEAN = 1
        const val NUMBER = 2
        const val TEXT = 3
        const val ARRAY = 4
        const val OBJECT = 5
    }
}