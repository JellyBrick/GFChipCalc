package main.setting;

import main.puzzle.Board;
import main.puzzle.Stat;
import main.util.DoubleKeyHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Bunnyspa
 */
public class BoardSetting {

    public static final int MAX_DEFAULT = 0;
    public static final int MAX_STAT = 1;
    public static final int MAX_PT = 2;
    public static final int MAX_PRESET = 3;

    @NotNull
    private final DoubleKeyHashMap<String, Integer, Stat> statMap, ptMap;
    @NotNull
    private final DoubleKeyHashMap<String, Integer, Integer> modeMap, presetIndexMap;

    public BoardSetting() {
        this.statMap = new DoubleKeyHashMap<>();
        this.ptMap = new DoubleKeyHashMap<>();
        this.modeMap = new DoubleKeyHashMap<>();
        this.presetIndexMap = new DoubleKeyHashMap<>();

        for (String name : Board.NAMES) {
            for (int star = 1; star <= 5; star++) {
                statMap.put(name, star, Board.getMaxStat(name, star));
                ptMap.put(name, star, Board.getMaxPt(name, star));
                modeMap.put(name, star, star == 5 ? MAX_PRESET : MAX_DEFAULT);
                presetIndexMap.put(name, star, 0);
            }
        }
    }

    @NotNull
    public String toData() {
        List<String> lines = new ArrayList<>();
        for (String name : Board.NAMES) {
            for (int star = 1; star <= 5; star++) {
                String ss = String.join(";",
                        name,
                        String.valueOf(star),
                        String.valueOf(getStatMode(name, star)),
                        getStat(name, star).toData(),
                        getPt(name, star).toData(),
                        String.valueOf(getPresetIndex(name, star))
                );
                lines.add(ss);
            }
        }
        return String.join(System.lineSeparator(), lines);
    }

    public void setStat(String name, int star, Stat stat) {
        statMap.put(name, star, stat);
    }

    public void setPt(String name, int star, Stat pt) {
        ptMap.put(name, star, pt);
    }

    public void setMode(String name, int star, int mode) {
        modeMap.put(name, star, mode);
    }

    public void setPresetIndex(String name, int star, int index) {
        presetIndexMap.put(name, star, index);
    }

    @Nullable
    public Stat getStat(String name, int star) {
        if (statMap.containsKey(name, star)) {
            return statMap.get(name, star);
        }
        return new Stat();
    }

    @Nullable
    public Stat getPt(String name, int star) {
        if (ptMap.containsKey(name, star)) {
            return ptMap.get(name, star);
        }
        return new Stat();
    }

    public int getStatMode(String name, int star) {
        if (modeMap.containsKey(name, star)) {
            return modeMap.get(name, star);
        }
        return MAX_DEFAULT;
    }

    public int getPresetIndex(String name, int star) {
        if (presetIndexMap.containsKey(name, star)) {
            return presetIndexMap.get(name, star);
        }
        return -1;
    }

    public boolean hasDefaultPreset(String name, int star) {
        if (!StatPresetMap.PRESET.containsKey(name, star)) {
            return false;
        }
        if (StatPresetMap.PRESET.size(name, star) != 1) {
            return false;
        }
        return StatPresetMap.PRESET.get(name, star, 0).stat.equals(Board.getMaxStat(name, star));
    }

    public static StatPreset getPreset(String name, int star, int index) {
        return StatPresetMap.PRESET.get(name, star, index);
    }
}
