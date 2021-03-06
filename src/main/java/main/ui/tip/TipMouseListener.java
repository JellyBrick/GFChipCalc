package main.ui.tip;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Bunnyspa
 */
public class TipMouseListener implements MouseListener {

    private final JLabel label;
    @NotNull
    private final Map<Component, String> map;

    public TipMouseListener(JLabel label) {
        map = new HashMap<>();
        this.label = label;
    }

    public void clearTips() {
        map.clear();
    }

    public void setTip(Component c, String s) {
        map.put(c, s);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(@NotNull MouseEvent e) {
        Component c = e.getComponent();
        if (c != null && map.containsKey(c)) {
            label.setText(map.get(c));
        } else {
            label.setText(" ");
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        label.setText(" ");
    }

}
