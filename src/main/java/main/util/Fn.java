package main.util;

import main.puzzle.Chip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Bunnyspa
 */
public class Fn {

    // <editor-fold defaultstate="collapsed" desc="GUI Methods"> 
    @NotNull
    public static Set<Component> getAllComponents(Component component) {
        Set<Component> components = new HashSet<>();
        components.add(component);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                components.addAll(getAllComponents(child));
            }
        }
        return components;
    }

    public static void setUIFont(@NotNull Font font) {
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(font));
            }
        }
    }

    public static void addEscDisposeListener(@NotNull JDialog aDialog) {
        getAllComponents(aDialog).forEach((c) -> c.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(@NotNull KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    aDialog.dispose();
                }
            }
        }));
    }

    public static void addEscListener(JDialog aDialog, @NotNull Runnable r) {
        getAllComponents(aDialog).forEach((c) -> c.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(@NotNull KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    r.run();
                }
            }
        }));
    }

    public static void open(Component c, @NotNull JDialog dialog) {
        dialog.setLocationRelativeTo(c);
        dialog.setVisible(true);
    }

    public static void open(Component c, @NotNull JFrame frame) {
        frame.setLocationRelativeTo(c);
        frame.setVisible(true);
    }

    public static int getWidth(@NotNull String str, Font font) {
        Canvas c = new Canvas();
        return c.getFontMetrics(font).stringWidth(str);
    }

    public static int getHeight(Font font) {
        Canvas c = new Canvas();
        return c.getFontMetrics(font).getHeight();
    }

    public static void popup(@NotNull JComponent comp, String title, String text) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 0, 5)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        JLabel textLabel = new JLabel(text);
        textLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(textLabel, BorderLayout.CENTER);

        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                dialog.dispose();
            }
        });
        dialog.add(panel);
        dialog.pack();

        Point p = comp.getLocationOnScreen();
        p.translate((comp.getWidth() - dialog.getWidth()) / 2, 0);
        dialog.setLocation(p);
        dialog.setVisible(true);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="String">
    @NotNull
    public static String toHTML(@NotNull String s) {
        String out = "<html>";
        out += s.replaceAll("\\r\\n|\\r|\\n", "<br>").trim();
        out += "</html>";
        return out;
    }

    @NotNull
    public static String htmlColor(String s, @NotNull Color c) {
        return "<font color=" + Fn.colorToHexcode(c) + ">" + s + "</font>";
    }

    @NotNull
    public static String htmlColor(int i, @NotNull Color c) {
        return htmlColor(String.valueOf(i), c);
    }

    @NotNull
    public static String getTime(long s) {
        long hour = s / 3600;
        long min = (s % 3600) / 60;
        long sec = s % 60;
        return hour + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec);
    }

    public static String thousandComma(int i) {
        return String.format("%,d", i);
    }

    public static String fStr(double d, int len) {
        return String.format("%." + len + "f", d);
    }

    @NotNull
    public static String fPercStr(double d) {
        return String.format("%.2f", d * 100) + "%";
    }

    @NotNull
    public static String iPercStr(double d) {
        return Math.round(d * 100) + "%";
    }

    @NotNull
    public static String pad(String s, int i) {
        return String.valueOf(s).repeat(Math.max(0, i));
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Number">
    public static int limit(int i, int min, int max) {
        return Math.min(Math.max(i, min), max);
    }

    public static int max(@NotNull int... ints) {
        if (ints.length == 0) {
            return 0;
        }
        int out = ints[0];
        for (int i : ints) {
            out = Math.max(out, i);
        }
        return out;
    }

    public static int floor(int n, int d) {
        return n / d;
    }

    public static int ceil(int n, int d) {
        return floor(n, d) + (n % d == 0 ? 0 : 1);
    }

    public static int sum(@NotNull int... ints) {
        int out = 0;
        for (int i : ints) {
            out += i;
        }
        return out;
    }

    private static double getPerc(double value, double min, double max) {
        if (min >= max) {
            return 0.0;
        }
        if (value < min) {
            return 0.0;
        }
        if (max < value) {
            return 1.0;
        }
        return (value - min) / (max - min);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Array"> 
    @NotNull
    @SafeVarargs
    public static <T> T[] concatAll(@NotNull T[] first, @NotNull T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Color">
    @NotNull
    public static String colorToHexcode(@NotNull Color c) {
        String colorHex = Integer.toHexString(c.getRGB());
        return "#" + colorHex.substring(2);
    }

    @NotNull
    public static Color percColor(@NotNull Color c1, @NotNull Color c2, @NotNull Color c3, double value, double min, double max) {
        return percColor(c1, c2, c3, getPerc(value, min, max));
    }

    @NotNull
    public static Color percColor(@NotNull Color c1, @NotNull Color c2, double value, double min, double max) {
        return percColor(c1, c2, getPerc(value, min, max));
    }

    @NotNull
    public static Color percColor(@NotNull Color c1, @NotNull Color c2, double d) {
        int r1 = c1.getRed();
        int g1 = c1.getGreen();
        int b1 = c1.getBlue();
        int r2 = c2.getRed();
        int g2 = c2.getGreen();
        int b2 = c2.getBlue();

        int r3 = r1 + (int) Math.round((r2 - r1) * d);
        int g3 = g1 + (int) Math.round((g2 - g1) * d);
        int b3 = b1 + (int) Math.round((b2 - b1) * d);

        return new Color(r3, g3, b3);
    }

    @NotNull
    private static Color percColor(@NotNull Color c1, @NotNull Color c2, @NotNull Color c3, double d) {
        if (d < 0.5f) {
            return percColor(c1, c2, d * 2);
        } else {
            return percColor(c2, c3, (d - 0.5f) * 2);
        }
    }

    @NotNull
    public static Color getColor(float hue) {
        return Color.getHSBColor(hue, 0.75f, 0.75f);
    }

    @NotNull
    public static Color getSizeColor(int size) {
        float hue = ((float) 6 - size) / Chip.SIZE_MAX;
        return Color.getHSBColor(hue, 0.5f, 0.75f);
    }

    @NotNull
    public static float[] getHSB(@NotNull Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return hsb;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Dimension">
    public static boolean isInside(@Nullable Point p, @NotNull Rectangle r) {
        if (p == null) {
            return false;
        }
        return isInside(p.x, p.y, r);
    }

    public static boolean isInside(int x, int y, @NotNull Rectangle r) {
        return r.x < x
                && x < r.x + r.width
                && r.y < y
                && y < r.y + r.height;
    }

    public static boolean isOverlapped(@NotNull Rectangle r1, @NotNull Rectangle r2) {
        return r2.x < r1.x + r1.width
                && r1.x < r2.x + r2.width
                && r2.y < r1.y + r1.height
                && r1.y < r2.y + r2.height;
    }

    @NotNull
    public static Rectangle fit(int width, int height, @NotNull Rectangle container) {
        int newWidth = container.width;
        int newHeight = height * container.width / width;
        if (container.height < newHeight) {
            newWidth = width * container.height / height;
            newHeight = container.height;
        }
        int x = container.x + container.width / 2 - newWidth / 2;
        int y = container.y + container.height / 2 - newHeight / 2;
        Rectangle out = new Rectangle(x, y, newWidth, newHeight);
        return out;
    }
    // </editor-fold>
}
