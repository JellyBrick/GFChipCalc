/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.ui.resource;

import main.App;
import main.puzzle.Chip;
import main.puzzle.Shape;
import main.setting.Setting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Bunnyspa
 */
public class AppColor {

    public static final Map<Integer, Color> CHIPS = new HashMap<>() {
        {
            put(Chip.COLOR_ORANGE, new Color(240, 107, 65));
            put(Chip.COLOR_BLUE, new Color(111, 137, 218));
        }
    }; // </editor-fold>
    public static final Color LEVEL = new Color(10, 205, 171);
    public static final Color YELLOW_STAR = new Color(255, 170, 0);
    public static final Color RED_STAR = Color.RED;

    static Color getPoolColor(@Nullable App app, @NotNull Chip chip) {
        if (app == null) {
            return Color.GRAY;
        }
        if (chip.getSize() < 5) {
            int i = (chip.getSize() + 1) % 3;
            return i == 0 ? app.orange() : i == 1 ? app.green() : app.blue();
        }
        if (chip.getShape().getType() == Shape.Type._5A) {
            return app.orange();
        }
        if (chip.getShape().getType() == Shape.Type._5B) {
            return app.green();
        }
        return app.blue();
    }

    public static class Index {

        private static final Color[] DEFAULT = { // <editor-fold defaultstate="collapsed">
            new Color(15079755),
            new Color(3978315),
            new Color(16769305),
            new Color(4416472),
            new Color(16089649),
            new Color(9510580),
            new Color(4649200),
            new Color(15741670),
            new Color(12383756),
            new Color(16432830),
            new Color(32896),
            new Color(15122175),
            new Color(10117924),
            new Color(16775880),
            new Color(8388608),
            new Color(11206595),
            new Color(8421376),
            new Color(16767153),
            new Color(117),
            new Color(8421504)
        }; // </editor-fold>
        private static final Color[] CB = { // <editor-fold defaultstate="collapsed">
            new Color(15113984),
            new Color(5682409),
            new Color(40563),
            new Color(15787074),
            new Color(29362),
            new Color(13983232),
            new Color(13400487)
        }; // </editor-fold>

        @NotNull
        public static Color[] colors(int alt) {
            if (alt % Setting.NUM_COLOR == 1) {
                return CB;
            }
            return DEFAULT;
        }
    }

    public static class Three {

        private static final Color[] DEFAULT = {
            new Color(14928556), new Color(12901541), new Color(10335956)
        };
        private static final Color[] CB = {
            Index.CB[0], Index.CB[2], Index.CB[1]
        };

        public static Color blue(int alt) {
            if (alt % Setting.NUM_COLOR == 1) {
                return CB[2];
            }
            return DEFAULT[2];
        }

        public static Color green(int alt) {
            if (alt % Setting.NUM_COLOR == 1) {
                return CB[1];
            }
            return DEFAULT[1];
        }

        public static Color orange(int alt) {
            if (alt % Setting.NUM_COLOR == 1) {
                return CB[0];
            }
            return DEFAULT[0];
        }
    }
}
