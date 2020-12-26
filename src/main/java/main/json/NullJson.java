package main.json;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Bunnyspa
 */
public class NullJson implements Json {

    @Override
    public int getType() {
        return Json.NULL;
    }

    @NotNull
    @Override
    public String toString() {
        return "null";
    }
}
