package main.json;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Bunnyspa
 */
public class BooleanJson implements Json {

    private final boolean data;

    public BooleanJson(@NotNull String data) {
        this.data = Boolean.parseBoolean(data.trim());
    }

    public boolean getBoolean() {
        return data;
    }

    @Override
    public int getType() {
        return Json.BOOLEAN;
    }

    @NotNull
    @Override
    public String toString() {
        return String.valueOf(data);
    }
}
