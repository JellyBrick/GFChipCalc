package main.json;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *
 * @author Bunnyspa
 */
public interface Json {

    int NULL = 0;
    int BOOLEAN = 1;
    int NUMBER = 2;
    int TEXT = 3;
    int ARRAY = 4;
    int OBJECT = 5;

    int getType();

    @NotNull
    static Json parse(@NotNull String data) {
        String dataTrim = data.trim();
        switch (dataTrim.charAt(0)) {
            // Object
            case '{':
                return new ObjectJson(data);
            // Array
            case '[':
                return new ArrayJson(data);
            // Text
            case '"':
                return new TextJson(data);
            // Boolean
            case 't':
            case 'f':
                return new BooleanJson(data);
            // Null
            case 'n':
                return new NullJson();
            // Number
            default:
                return new NumberJson(data);
        }
    }

    static int getEndIndex(@NotNull String data, int i) {
        while (i < data.length()) {
            char next = data.charAt(i);
            if (!Character.isWhitespace(next)) {
                switch (next) {
                    // Object
                    case '{':
                        return getBracketEndIndex(data, i, OBJECT);
                    // Array
                    case '[':
                        return getBracketEndIndex(data, i, ARRAY);
                    // Text
                    case '"':
                        i++;
                        while (i < data.length()) {
                            if (data.charAt(i) == '"' && (i == 0 || data.charAt(i - 1) != '\\')) {
                                return i;
                            }
                            i++;
                        }
                        return -1;
                    // Boolean - True
                    case 't':
                        if (data.startsWith("true", i)) {
                            return i + 4;
                        }
                        return -1;
                    // Boolean - False
                    case 'f':
                        if (data.startsWith("false", i)) {
                            return i + 5;
                        }
                        return -1;
                    // Null
                    case 'n':
                        if (data.startsWith("null", i)) {
                            return i + 4;
                        }
                        return -1;
                    // Number
                    default:
                        i++;
                        while (i < data.length()) {
                            char c = data.charAt(i);
                            if (c != '.' && (c < '0' || '9' < c)) {
                                return i - 1;
                            }
                            i++;
                        }
                        return i - 1;
                }
            }
            i++;
        }
        return -1;
    }

    static int getBracketEndIndex(@NotNull String data, int i, int type) {
        try {
            int bracketLevel = 0;
            boolean quoting = false;

            char openBracket = type == OBJECT ? '{' : '[';
            char closeBracket = type == OBJECT ? '}' : ']';

            do {
                char c = data.charAt(i);
                if (c == '"' && (i == 0 || data.charAt(i - 1) != '\\')) {
                    quoting = !quoting;
                }
                if (!quoting && c == openBracket) {
                    bracketLevel++;
                }
                if (!quoting && c == closeBracket) {
                    bracketLevel--;
                }
                i++;
            } while (bracketLevel > 0 && i < data.length());
            return i - 1;
        } catch (Exception ex) {
            return -1;
        }
    }

    static boolean getBoolean(@NotNull Json data) throws ClassCastException {
        if (data.getType() != BOOLEAN) {
            throw new ClassCastException(getClassCastExceptionMessage(data, BooleanJson.class.getName()));
        }
        return ((BooleanJson) data).getBoolean();
    }

    static int getInteger(@NotNull Json data) throws ClassCastException {
        if (data.getType() != NUMBER) {
            throw new ClassCastException(getClassCastExceptionMessage(data, NumberJson.class.getName()));
        }
        return ((NumberJson) data).getInteger();
    }

    static double getDouble(@NotNull Json data) throws ClassCastException {
        if (data.getType() != NUMBER) {
            throw new ClassCastException(getClassCastExceptionMessage(data, NumberJson.class.getName()));
        }
        return ((NumberJson) data).getDouble();
    }

    @NotNull
    static String getText(@NotNull Json data) throws ClassCastException {
        if (data.getType() != TEXT) {
            throw new ClassCastException(getClassCastExceptionMessage(data, TextJson.class.getName()));
        }
        return ((TextJson) data).getText();
    }

    @NotNull
    static List<Json> getList(@NotNull Json data) throws ClassCastException {
        if (data.getType() != ARRAY) {
            throw new ClassCastException(getClassCastExceptionMessage(data, ArrayJson.class.getName()));
        }
        return ((ArrayJson) data).getList();
    }

    @NotNull
    static ObjectJson getObjectJson(@NotNull Json data) throws ClassCastException {
        if (data.getType() != OBJECT) {
            throw new ClassCastException(getClassCastExceptionMessage(data, ObjectJson.class.getName()));
        }
        return (ObjectJson) data;
    }

    @NotNull
    static List<String> getObjectKeys(@NotNull Json data) throws ClassCastException {
        if (data.getType() != OBJECT) {
            throw new ClassCastException(getClassCastExceptionMessage(data, ObjectJson.class.getName()));
        }
        return ((ObjectJson) data).getKeys();
    }

    @Nullable
    static Json getObjectValue(@NotNull Json data, String key) throws ClassCastException {
        if (data.getType() != OBJECT) {
            throw new ClassCastException(getClassCastExceptionMessage(data, ObjectJson.class.getName()));
        }
        return ((ObjectJson) data).getValue(key);
    }

    @NotNull
    static String getClassCastExceptionMessage(@NotNull Json data, String cast) {
        StringBuilder sb = new StringBuilder();
        sb.append(Json.class.getName()).append(" cannot be cast to ").append(cast).append("- It should be cast to ");
        switch (data.getType()) {
            case NULL:
                sb.append(NullJson.class.getName());
                break;
            case BOOLEAN:
                sb.append(BooleanJson.class.getName());
                break;
            case NUMBER:
                sb.append(NumberJson.class.getName());
                break;
            case TEXT:
                sb.append(TextJson.class.getName());
                break;
            case ARRAY:
                sb.append(ArrayJson.class.getName());
                break;
            case OBJECT:
                sb.append(ObjectJson.class.getName());
                break;
            default:
                throw new AssertionError();
        }
        return sb.toString();
    }
}
