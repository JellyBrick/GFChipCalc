package main.json;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Bunnyspa
 */
public class ArrayJson implements Json {

    @NotNull
    private final List<Json> data;

    public ArrayJson(String data) {
        // Init
        this.data = new ArrayList<>();
        // Trim
        data = data.trim().replaceAll("^\\[|]$", "");

        int si = 0;
        while (si < data.length()) {
            int ei = Json.getEndIndex(data, si);
            String eStr = data.substring(si, ei + 1);
            Json element = Json.parse(eStr);

            this.data.add(element);

            int ci = data.indexOf(',', ei) + 1;
            if (ci == 0) {
                break;
            }
            si = ci;
        }
    }

    @NotNull
    public List<Json> getList() {
        return new ArrayList<>(data);
    }

    @Override
    public int getType() {
        return Json.ARRAY;
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            Json d = data.get(i);
            sb.append(d.toString());
            if (i < data.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
