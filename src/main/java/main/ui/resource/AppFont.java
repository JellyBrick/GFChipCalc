/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.ui.resource;

import main.App;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Bunnyspa
 */
public class AppFont {

    @Nullable
    public static final Font FONT_DIGIT = get("mohave/Mohave-Light.otf");

    @Nullable
    private static Font get(String s) {
        try {
            InputStream is = App.getResourceAsStream("font/" + s);
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(14.0F);
        } catch (@NotNull FontFormatException | IOException ignored) {
        }
        return null;
    }

    @NotNull
    public static Font getDefault() {
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

}
