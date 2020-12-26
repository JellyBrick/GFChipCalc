package main.json;

import main.App;
import main.puzzle.Shape;
import main.puzzle.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Bunnyspa
 */
public class JsonParser {

    private static final String SIGNKEY = "sign";
    private static final String CHIPKEY_SQUAD = "squad_with_user_info";
    private static final String CHIPKEY_CHIP = "chip_with_user_info";
    private static final String UNKNOWN_HOC = "Unknown HOC";

    @NotNull
    public static List<Chip> readFile(@NotNull String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String data = br.lines().collect(Collectors.joining());
            List<Chip> chips = parseChip(data);
            return chips;
        } catch (IOException ex) {
            App.log(ex);
            return new ArrayList<>();
        }
    }

    public static String parseSign(@NotNull String data) {
        ObjectJson o = Json.getObjectJson(Json.parse(data));
        return Json.getText(o.getValue(SIGNKEY));
    }

    @NotNull
    public static List<Chip> parseChip(@NotNull String data) {
        // Init
        Map<Integer, Tag> squadMap = new HashMap<>();
        List<Chip> chips = new ArrayList<>();
        try {
            // Parse
            ObjectJson o = Json.getObjectJson(Json.parse(data));

            // Squad
            Json squadsJ = o.getValue(CHIPKEY_SQUAD);
            if (squadsJ.getType() == Json.OBJECT) {
                List<String> squadJKeys = Json.getObjectKeys(squadsJ);
                parseChip_squad(squadMap, squadJKeys.stream().map((jKey) -> Json.getObjectJson(Json.getObjectValue(squadsJ, jKey))));
            } else if (squadsJ.getType() == Json.ARRAY) {
                List<Json> squadJs = Json.getList(squadsJ);
                parseChip_squad(squadMap, squadJs.stream().map(Json::getObjectJson));
            }

            // Chip
            Json chipsJ = o.getValue(CHIPKEY_CHIP);
            if (chipsJ.getType() == Json.OBJECT) {
                List<String> chipJKeys = Json.getObjectKeys(chipsJ);
                parseChip_chip(squadMap, chips, chipJKeys.stream().map((jKey) -> Json.getObjectJson(Json.getObjectValue(chipsJ, jKey))));
            } else if (chipsJ.getType() == Json.ARRAY) {
                List<Json> chipJs = Json.getList(chipsJ);
                parseChip_chip(squadMap, chips, chipJs.stream().map(Json::getObjectJson));
            }

            Collections.reverse(chips);
        } catch (Exception ignored) {
        }
        return chips;
    }

    private static void parseChip_squad(@NotNull Map<Integer, Tag> squadMap, @NotNull Stream<ObjectJson> stream) {
        stream.forEach((squadJ) -> {
            int squadID = Integer.parseInt(Json.getText(squadJ.getValue("id")));
            int squadIndex = Integer.parseInt(Json.getText(squadJ.getValue("squad_id"))) - 1;
            squadMap.put(squadID, boardTag(squadIndex));
        });
    }

    private static void parseChip_chip(@NotNull Map<Integer, Tag> squadMap, @NotNull List<Chip> chips, @NotNull Stream<ObjectJson> stream) {
        stream.forEach((chipJ) -> {
            // Raw
            String id = ((TextJson) chipJ.getValue("id")).getText();
            int gridData = Integer.parseInt(((TextJson) chipJ.getValue("grid_id")).getText());
            Shape shape = Shape.byId(gridData);
            int dmg = Integer.parseInt(Json.getText(chipJ.getValue("assist_damage")));
            int brk = Integer.parseInt(Json.getText(chipJ.getValue("assist_def_break")));
            int hit = Integer.parseInt(Json.getText(chipJ.getValue("assist_hit")));
            int rld = Integer.parseInt(Json.getText(chipJ.getValue("assist_reload")));
            int star = Integer.parseInt(Json.getText(chipJ.getValue("chip_id")).substring(0, 1));
            int level = Integer.parseInt(Json.getText(chipJ.getValue("chip_level")));
            int color = Integer.parseInt(Json.getText(chipJ.getValue("color_id"))) - 1;
            int rotation = Integer.parseInt(Json.getText(chipJ.getValue("shape_info")).substring(0, 1));
            int squadID = Integer.parseInt(Json.getText(chipJ.getValue("squad_with_user_id")));

            Stat pt = new Stat(dmg, brk, hit, rld);
            Chip chip = new Chip(id, shape, star, color, pt, level, rotation);
            if (squadMap.containsKey(squadID)) {
                Tag tag = squadMap.get(squadID);
                chip.setTag(tag, true);
            } else if (squadID != 0) {
                Tag tag = boardTag(squadID - 10001);
                chip.setTag(tag, true);
            }
            chips.add(chip);
        });
    }

    @NotNull
    private static Tag boardTag(int index) {
        if (index < Board.NAMES.length) {
            return new Tag(Color.GRAY, Board.NAMES[index]);
        }
        return new Tag(Color.GRAY, UNKNOWN_HOC);
    }
}
