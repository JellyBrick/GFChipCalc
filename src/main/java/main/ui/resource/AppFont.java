/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.ui.resource;

import main.App;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Bunnyspa
 */
public class AppFont {

    public static final Font FONT_DIGIT = get("mohave/Mohave-Light.otf");

    private static Font get(String s) {
        try {
            InputStream is = App.getResourceAsStream("font/" + s);
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(14.0F);
        } catch (FontFormatException | IOException ignored) {
        }
        return null;
    }

    public static Font getDefault() {
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

}
