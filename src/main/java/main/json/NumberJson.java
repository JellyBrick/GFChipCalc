package main.json;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Bunnyspa
 */
public class NumberJson implements Json {

    @NotNull
    private final String data;

    public NumberJson(@NotNull String data) {
        this.data = data.trim();
    }

    public int getInteger() {
        return Integer.parseInt(data);
    }

    public double getDouble() {
        return Double.parseDouble(data);
    }

    @Override
    public int getType() {
        return Json.NUMBER;
    }

    @NotNull
    @Override
    public String toString() {
        return data;
    }
}
