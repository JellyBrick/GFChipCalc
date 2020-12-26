package main.json;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Bunnyspa
 */
public class TextJson implements Json {

    @NotNull
    private final String data;

    public TextJson(@NotNull String data) {
        this.data = data.trim().replaceAll("^\"|\"$", "");
    }

    @NotNull
    public String getText() {
        return data;
    }

    @Override
    public int getType() {
        return Json.TEXT;
    }

    @NotNull
    @Override
    public String toString() {
        return "\"" + data + "\"";
    }
}
