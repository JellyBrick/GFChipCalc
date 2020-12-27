package main.puzzle;

import main.util.Fn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.Serializable;
import java.util.List;
import java.util.*;

/**
 *
 * @author Bunnyspa
 */
public class Tag implements Serializable, Comparable<Object> {

    private static final String NAME_DEFAULT = "New Tag";

    private String name;
    private Color color;

    public Tag(Color color, String name) {
        this.color = color;
        this.name = name;
    }

    public Tag() {
        Random random = new Random();
        this.color = Fn.getColor(random.nextFloat());
        this.name = NAME_DEFAULT;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public String toData() {
        Color c = getColor();
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()) + getName();
    }

    @NotNull
    @Override
    public String toString() {
        return toData();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        Tag t = (Tag) o;
        return this.name.compareTo(t.name);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Tag tag = (Tag) obj;
        return this.name.equals(tag.name) && this.color.getRGB() == tag.color.getRGB();

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.color.getRGB());
        return hash;
    }

    @NotNull
    public static List<Tag> getTags(@NotNull Collection<Chip> chips) {
        Set<Tag> tagSet = new HashSet<>();
        chips.forEach((c) -> tagSet.addAll(c.getTags()));
        List<Tag> tagList = new ArrayList<>(tagSet);
        Collections.sort(tagList);
        return tagList;
    }
}
