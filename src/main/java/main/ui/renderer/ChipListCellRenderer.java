package main.ui.renderer;

import main.App;
import main.puzzle.Chip;
import main.ui.resource.AppImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Bunnyspa
 */
public class ChipListCellRenderer extends DefaultListCellRenderer {

    private final App app;
    private final boolean factored;
    private final double factor;

    public ChipListCellRenderer(App app) {
        super();
        this.app = app;
        this.factored = false;
        this.factor = 1.0;
    }

    public ChipListCellRenderer(App app, double factor) {
        super();
        this.app = app;
        factored = true;
        this.factor = factor;
    }

    @NotNull
    @Override
    public Component getListCellRendererComponent(JList<?> list, @Nullable Object value, int index, boolean isSelected, boolean cellHasFocus) {
        DefaultListCellRenderer cr = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        cr.setHorizontalAlignment(CENTER);
        cr.setText("");

        Chip c = (Chip) value;
        if (value != null) {
            ImageIcon icon = AppImage.Chip.get(app, c);
            if (!factored) {
                cr.setIcon(icon);
            } else {
                Image image = icon.getImage().getScaledInstance((int) (icon.getIconWidth() * factor), (int) (icon.getIconHeight() * factor), Image.SCALE_SMOOTH);
                cr.setIcon(new ImageIcon(image));
            }
            cr.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);
        } else {
            cr.setIcon(null);
            cr.setBackground(Color.WHITE);
        }
        return cr;
    }
}
