package main.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Bunnyspa
 */
public class ResearchConnection {

    private static final int TIMEOUT = 1000;
    private static final String URL = "https://gfchipcalc-researchserver.herokuapp.com/";

    // chips;-
    // OR
    // chips;rotaions;locations
    public static void sendResult(@NotNull String data) {
        put("", data);
    }

    // version: version;name;star
    @Nullable
    public static String getVersion() {
        return get("version");
    }

    // progress: prog;total
    @Nullable
    public static String getProgress() {
        return get("progress");
    }

    // task: chips
    @Nullable
    public static String getTask() {
        return get("task");
    }

    private static void put(String path, @NotNull String data) {
        try {
            URL u = new URL(URL + path);
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestMethod("PUT");
            con.setFixedLengthStreamingMode(data.length());
            con.setDoOutput(true);

            try (DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
                os.writeBytes(data);
                os.flush();
            }

//            int code = con.getResponseCode();
//            String msg = con.getResponseMessage(); 
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private static String get(String path) {
        try {
            URL u = new URL(URL + path);
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestMethod("GET");

//            int code = con.getResponseCode();
//            String msg = con.getResponseMessage(); 
            String header_te = con.getHeaderField(Proxy.TRANSFER_ENCODING);
            String header_cl = con.getHeaderField(Proxy.CONTENT_LENGTH);

            InputStream cis = con.getInputStream();
            List<Byte> out = new ArrayList<>();
            try (DataInputStream is = new DataInputStream(cis)) {
                if ("chunked".equalsIgnoreCase(header_te)) {
                    int readLen;
                    byte[] buffer = new byte[Math.max(is.available(), 1)];
                    while ((readLen = is.read(buffer)) != -1) {
                        for (int i = 0; i < readLen; i++) {
                            out.add(buffer[i]);
                        }
                        buffer = new byte[Math.max(is.available(), 1)];
                    }
                } else if (header_cl != null) {
                    int cl = Integer.parseInt(header_cl);
                    while (0 < cl) {
                        byte[] buffer = new byte[cl];
                        int readLen = is.read(buffer);
                        for (int i = 0; i < readLen; i++) {
                            out.add(buffer[i]);
                        }
                        cl -= readLen;
                    }
                }
            }
            if (out.isEmpty()) {
                return "";
            }
            byte[] bytes = new byte[out.size()];
            for (int i = 0; i < out.size(); i++) {
                bytes[i] = out.get(i);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return null;
    }
}
