package main.puzzle;

import main.ui.resource.AppText;
import main.util.Fn;
import main.util.IO;
import main.util.Rational;
import main.util.Version3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Bunnyspa
 */
public class Chip implements Serializable {

    public static final Rational RATE_DMG = new Rational(44, 10);
    public static final Rational RATE_BRK = new Rational(127, 10);
    public static final Rational RATE_HIT = new Rational(71, 10);
    public static final Rational RATE_RLD = new Rational(57, 10);
    public static final Rational[] RATES = {Chip.RATE_DMG, Chip.RATE_BRK, Chip.RATE_HIT, Chip.RATE_RLD};

    public static final int COLOR_NA = -1;
    public static final int COLOR_ORANGE = 0;
    public static final int COLOR_BLUE = 1;

    public static final boolean COUNTERCLOCKWISE = true;
    public static final boolean CLOCKWISE = false;

    public static final int SIZE_MAX = 6;
    public static final int LEVEL_MAX = 20;
    public static final int PT_MAX = 5;

    public static final int STAR_MIN = 2;
    public static final int STAR_MAX = 5;

    @Nullable
    private final String id;
    private final Shape shape;

    @Nullable
    private Stat pt;
    private int initRotation, rotation, initLevel, level, star, color, displayType;
    private int boardIndex = -1;
    private boolean marked;
    private final Set<Tag> tags;

    // Pool init
    public Chip(Shape shape) {
        this.id = null;
        this.shape = shape;
        pt = null;
        tags = new HashSet<>();
    }

    // Pool to inventory init
    public Chip(@NotNull Chip c, int star, int color) {
        this.id = UUID.randomUUID().toString();
        shape = c.shape;
        rotation = c.rotation;
        initRotation = c.initRotation;

        pt = new Stat();
        this.star = star;
        this.color = color;

        tags = new HashSet<>();
    }

    // Chip deep copy
    public Chip(@NotNull Chip c) {
        id = c.getID();
        shape = c.shape;
        star = c.star;
        color = c.color;

        pt = c.pt;

        initLevel = c.initLevel;
        level = c.level;

        initRotation = c.initRotation;
        rotation = c.rotation;

        displayType = c.displayType;
        marked = c.marked;
        tags = new HashSet<>(c.tags);
    }

    public static final int INVENTORY = 0;
    public static final int COMBINATION = 1;

    // Pre 5.3.0
    public Chip(@NotNull Version3 version, @NotNull String[] data, int type) {
        if (version.isCurrent(4, 0, 0)) {
            // 4.0.0+
            int i = 0;
            id = data.length > i ? data[i] : UUID.randomUUID().toString();
            i++;
            shape = data.length > i ? Shape.byName(data[i]) : Shape.NONE;
            i++;

            int dmgPt = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, getMaxPt()) : 0;
            i++;
            int brkPt = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, getMaxPt()) : 0;
            i++;
            int hitPt = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, getMaxPt()) : 0;
            i++;
            int rldPt = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, getMaxPt()) : 0;
            i++;
            pt = new Stat(dmgPt, brkPt, hitPt, rldPt);

            rotation = data.length > i ? Integer.parseInt(data[i]) % shape.getMaxRotation() : 0;
            i++;

            if (type == INVENTORY) {
                initRotation = rotation;
                marked = data.length > i && "1".equals(data[i]);
                boardIndex = -1;
            } else {
                initRotation = data.length > i ? Integer.parseInt(data[i]) % shape.getMaxRotation() : 0;
            }
            i++;

            star = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 2, 5) : 5;
            i++;

            level = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, LEVEL_MAX) : 0;
            i++;
            if (version.isCurrent(4, 7, 0) && type == COMBINATION) {
                initLevel = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, LEVEL_MAX) : level;
                i++;
            } else {
                initLevel = level;
            }

            color = data.length > i ? Fn.limit(Integer.parseInt(data[i]), 0, AppText.TEXT_MAP_COLOR.size()) : 0;
            i++;

            tags = new HashSet<>();
            if (type == INVENTORY && data.length > i) {
                for (String tagStr : data[i].split(",")) {
                    tags.add(IO.parseTag(tagStr));
                }
            }
        } else {
            // 1.0.0 - 3.0.0
            shape = data.length > 0 ? Shape.byName(data[0]) : Shape.NONE;
            rotation = data.length > 1 ? Integer.parseInt(data[1]) % shape.getMaxRotation() : 0;
            if (type == INVENTORY) {
                initRotation = rotation;

                int dmgPt = data.length > 2 ? Fn.limit(Integer.parseInt(data[2]), 0, getMaxPt()) : 0;
                int brkPt = data.length > 3 ? Fn.limit(Integer.parseInt(data[3]), 0, getMaxPt()) : 0;
                int hitPt = data.length > 4 ? Fn.limit(Integer.parseInt(data[4]), 0, getMaxPt()) : 0;
                int rldPt = data.length > 5 ? Fn.limit(Integer.parseInt(data[5]), 0, getMaxPt()) : 0;
                pt = new Stat(dmgPt, brkPt, hitPt, rldPt);

                star = data.length > 6 ? Fn.limit(Integer.parseInt(data[6]), 2, 5) : 5;
                level = data.length > 7 ? Fn.limit(Integer.parseInt(data[7]), 0, LEVEL_MAX) : 0;
                color = data.length > 8 ? Fn.limit(Integer.parseInt(data[8]), 0, AppText.TEXT_MAP_COLOR.size()) : 0;

                marked = data.length > 9 && "1".equals(data[9]);

                boardIndex = -1;
            } else {
                initRotation = data.length > 2 ? Integer.parseInt(data[2]) % shape.getMaxRotation() : 0;

                star = data.length > 3 ? Fn.limit(Integer.parseInt(data[3]), 2, 5) : 5;
                level = data.length > 4 ? Fn.limit(Integer.parseInt(data[4]), 0, LEVEL_MAX) : 0;
                color = data.length > 5 ? Fn.limit(Integer.parseInt(data[5]), 0, AppText.TEXT_MAP_COLOR.size()) : 0;

                int dmgPt = data.length > 6 ? Fn.limit(Integer.parseInt(data[6]), 0, getMaxPt()) : 0;
                int brkPt = data.length > 7 ? Fn.limit(Integer.parseInt(data[7]), 0, getMaxPt()) : 0;
                int hitPt = data.length > 8 ? Fn.limit(Integer.parseInt(data[8]), 0, getMaxPt()) : 0;
                int rldPt = data.length > 9 ? Fn.limit(Integer.parseInt(data[9]), 0, getMaxPt()) : 0;

                pt = new Stat(dmgPt, brkPt, hitPt, rldPt);
            }
            id = data.length > 10 ? data[10] : UUID.randomUUID().toString();
            tags = new HashSet<>();
        }
    }

    // ImageProcessor
    public Chip(@NotNull Shape shape, int star, int color, @Nullable Stat pt,
                int level, int rotation) {
        this.id = UUID.randomUUID().toString();
        this.shape = shape;
        this.star = star;
        this.color = color;

        this.pt = pt;

        this.initLevel = level;
        this.level = level;
        this.rotation = rotation % shape.getMaxRotation();
        this.initRotation = rotation % shape.getMaxRotation();

        this.tags = new HashSet<>();
    }

    // json (Inventory)
    public Chip(@Nullable String id, @NotNull Shape shape, int star, int color, @Nullable Stat pt,
                int level, int rotation) {
        this.id = id;
        this.shape = shape;
        this.star = star;
        this.color = color;

        this.pt = pt;

        this.initLevel = level;
        this.level = level;
        this.rotation = rotation % shape.getMaxRotation();
        this.initRotation = rotation % shape.getMaxRotation();

        this.tags = new HashSet<>();
    }

    public Chip(@Nullable String id,
                @NotNull Shape shape, int star, int color, @Nullable Stat pt,
                int level, int rotation,
                boolean marked, Set<Tag> tags) {
        this.id = id;
        this.shape = shape;
        this.star = star;
        this.color = color;

        this.pt = pt;

        this.initLevel = level;
        this.level = level;
        this.rotation = rotation % shape.getMaxRotation();
        this.initRotation = rotation % shape.getMaxRotation();

        this.marked = marked;
        this.tags = tags;
    }

    // <editor-fold defaultstate="collapsed" desc="ID">
    @Nullable
    public String getID() {
        return id;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name">
    public Shape getShape() {
        return shape;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Size and Type">
    public int getSize() {
        return shape.getSize();
    }

    @NotNull
    public Shape.Type getType() {
        return shape.getType();
    }

    public boolean typeGeq(@NotNull Shape.Type type) {
        return shape.getType().compareTo(type) >= 0;
    }

    @NotNull
    public static Rational getTypeMult(@NotNull Shape.Type type, int star) {
        int a = type.getSize() < 5 ? 16 : 20;
        int b = type.getSize() < 3 || type == Shape.Type._5A ? 4 : 0;
        int c = type.getSize() < 4 || type == Shape.Type._5A ? 4 : 0;
        return new Rational(star * a - b - (3 < star ? c : 0), 100);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Star">
    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Color">
    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Rotation and Ticket">
//    public final int getMaxRotation() {
//        return getMaxRotation(shape);
//    }
//
//    public static int getMaxRotation(Shape shape) {
//        return CHIP_ROTATION_MAP.get(shape);
//    }
    public int getInitRotation() {
        return initRotation;
    }

    public int getRotation() {
        return rotation;
    }

    public void setInitRotation(int r) {
        this.initRotation = r % shape.getMaxRotation();
        this.rotation = initRotation;
    }

    public void setRotation(int r) {
        this.rotation = r % shape.getMaxRotation();
    }

    public void resetRotation() {
        setRotation(initRotation);
    }

    public void initRotate(int i) {
        setInitRotation(rotation + i);
    }

    public void initRotate(boolean direction) {
        initRotate(direction ? 3 : 1);
    }

    public void rotate(int i) {
        setRotation(rotation + i);
    }

    public int getNumTicket() {
        return rotation != initRotation ? getStar() * 10 : 0;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="PT and Stat">
    public final int getMaxPt() {
        return getMaxPt(getSize());
    }

    public static int getMaxPt(int size) {
        return size > 4 ? size - 1 : size;
    }

    private int getTotalPts() {
        return pt.sum();
    }

    public boolean isPtValid() {
        return getTotalPts() == getSize();
    }

    @Nullable
    public Stat getPt() {
        return pt;
    }

    public void setPt(int dmg, int brk, int hit, int rld) {
        pt = new Stat(dmg, brk, hit, rld);
    }

    public boolean anyPtOver(@NotNull Stat ptLimit) {
        return pt.dmg > ptLimit.dmg
                || pt.brk > ptLimit.brk
                || pt.hit > ptLimit.hit
                || pt.rld > ptLimit.rld;
    }

    public static int getPt(@NotNull Rational rate, @NotNull Shape.Type type, int star, int level, int stat) {
        for (int pt = 0; pt < PT_MAX; pt++) {
            if (getStat(rate, type, star, level, pt) == stat) {
                return pt;
            }
        }
        return -1;
    }

    @NotNull
    public Stat getStat() {
        return new Stat(
                getStat(RATE_DMG, this, pt.dmg),
                getStat(RATE_BRK, this, pt.brk),
                getStat(RATE_HIT, this, pt.hit),
                getStat(RATE_RLD, this, pt.rld)
        );
    }

    public static int getStat(@NotNull Rational rate, @NotNull Chip c, int pt) {
        return getStat(rate, c.getType(), c.star, c.level, pt);
    }

    public static int getStat(@NotNull Rational rate, @NotNull Shape.Type type, int star, int level, int pt) {
        int base = new Rational(pt).mult(rate).mult(getTypeMult(type, star)).getIntCeil();
        return getLevelMult(level).mult(base).getIntCeil();
    }

    @NotNull
    public Stat getOldStat() {
        int dmg = getOldStat(RATE_DMG, this, pt.dmg);
        int brk = getOldStat(RATE_BRK, this, pt.brk);
        int hit = getOldStat(RATE_HIT, this, pt.hit);
        int rld = getOldStat(RATE_RLD, this, pt.rld);
        return new Stat(dmg, brk, hit, rld);
    }

    private static int getOldStat(@NotNull Rational rate, @NotNull Chip c, int pt) {
        return getOldStat(rate, c.shape, c.star, c.level, pt);
    }

    private static int getOldStat(@NotNull Rational rate, @NotNull Shape shape, int star, int level, int pt) {
        Rational base = new Rational(pt).mult(rate).mult(getTypeMult(shape.getType(), star));
        return getLevelMult(level).mult(base).getIntCeil();
    }

    @NotNull
    public static Stat getPtMultStat(@NotNull Chip c) {
        return new Stat(
                getStat(Chip.RATE_DMG, c, 1) * c.pt.dmg,
                getStat(Chip.RATE_BRK, c, 1) * c.pt.brk,
                getStat(Chip.RATE_HIT, c, 1) * c.pt.hit,
                getStat(Chip.RATE_RLD, c, 1) * c.pt.rld
        );
    }

    public static int getMaxEffStat(@NotNull Rational rate, int pt) {
        int base = new Rational(pt).mult(rate).getIntCeil();
        return getLevelMult(LEVEL_MAX).mult(base).getIntCeil();
    }

    public static int getMaxEffStat(@NotNull Rational rate, int pt, int level) {
        int base = new Rational(pt).mult(rate).getIntCeil();
        return getLevelMult(level).mult(base).getIntCeil();
    }

    public double getFitness(@NotNull Stat maxSG) {
        double out = 0.0;
        int[] s = getStat().toArray();
        int[] m = maxSG.toArray();

        for (int i = 0; i < 4; i++) {
            out += (double) s[i] * m[i];
        }
        return out / getSize();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Level and XP">
    public int getLevel() {
        return level;
    }

    public int getInitLevel() {
        return initLevel;
    }

    public void resetLevel() {
        this.level = this.initLevel;
    }

    public void setInitLevel(int initLevel) {
        this.initLevel = initLevel;
        this.level = initLevel;
    }

    public void setMinInitLevel() {
        setInitLevel(0);
    }

    public void setMaxLevel() {
        level = LEVEL_MAX;
    }

    public void setMaxInitLevel() {
        setInitLevel(LEVEL_MAX);
    }

    @NotNull
    public static Rational getLevelMult(int level) {
        return level < 10 ? new Rational(level).mult(8, 100).add(1) : new Rational(level).mult(7, 100).add(11, 10);
    }

    public int getCumulXP() {
        int xp = 0;
        for (int l = initLevel + 1; l <= LEVEL_MAX; l++) {
            xp += getXP(star, l);
        }
        return xp;

    }

    private static int getXP(int star, int level) {
        int xp = 150 + (level - 1) * 75 + (6 <= level ? (level - 5) * 75 : 0) + (17 <= level ? 150 : 0) + (20 == level ? 150 : 0);
        return xp * Math.max(0, star - 2) / 3;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Mark">
    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Tag">
    @NotNull
    public Set<Tag> getTags() {
        return new HashSet<>(tags);
    }

    public boolean containsTag(Tag tag) {
        return tags.stream().anyMatch((t) -> (t.equals(tag)));
    }

    public void setTag(Tag t, boolean enabled) {
        if (enabled) {
            addTag(t);
        } else {
            removeTag(t);
        }
    }

    private void addTag(Tag t) {
        tags.add(t);
    }

    private void removeTag(Tag t) {
        tags.remove(t);
    }

    public boolean containsHOCTagName() {
        for (String hoc : Board.NAMES) {
            for (Tag t : tags) {
                if (hoc.equals(t.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Matrix">
    @NotNull
    public static PuzzleMatrix<Boolean> generateMatrix(Shape shape, int rotation) {
        PuzzleMatrix<Boolean> matrix = new PuzzleMatrix<>(Shape.MATRIX_MAP.get(shape));
        matrix.rotate(rotation);
        return matrix;
    }

    @NotNull
    public PuzzleMatrix<Boolean> generateMatrix() {
        return generateMatrix(shape, rotation);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Image">
    public void setDisplayType(int displayType) {
        this.displayType = displayType;
    }

    public int getDisplayType() {
        return displayType;
    }

    public void setBoardIndex(int i) {
        boardIndex = i;
    }

    public int getBoardIndex() {
        return boardIndex;
    }

    public boolean statExists() {
        return null != pt;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Compare">
    public static int compare(@NotNull Chip c1, @NotNull Chip c2) {
        return Shape.compare(c1.shape, c2.shape);
    }

    public static int compareStar(@NotNull Chip c1, @NotNull Chip c2) {
        return c1.getStar() - c2.getStar();
    }

    public static int compareLevel(@NotNull Chip c1, @NotNull Chip c2) {
        return c1.getLevel() - c2.getLevel();
    }
    // </editor-fold>

    @NotNull
    public String toData() {
        String[] s = {
            id,
            String.valueOf(shape.id),
            String.valueOf(star),
            String.valueOf(color),
            pt.toData(),
            String.valueOf(initLevel),
            String.valueOf(initRotation),
            IO.data(marked),
                IO.data(getTags().stream().map(Tag::toData), ",")
        };

        return String.join(";", s);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Chip chip = (Chip) obj;
        return this.id.equals(chip.id);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + this.id.hashCode();
        return hash;
    }

    @NotNull
    @Override
    public String toString() {
        return id == null ? "null" : id;
    }
}
