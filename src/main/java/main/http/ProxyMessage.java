package main.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 *
 * @author Bunnyspa
 */
public class ProxyMessage {

    public String reqMethod, reqUrl, reqVersion;
    @NotNull
    private final Map<String, String> reqHeader;
    @NotNull
    public final List<Byte> reqBody;

    public int resCode;
    public String resMsg;
    @NotNull
    private final Map<String, String> resHeader;
    @NotNull
    public final List<Byte> resBody;

    public ProxyMessage() {
        reqHeader = new HashMap<>();
        resHeader = new HashMap<>();
        reqBody = new ArrayList<>();
        resBody = new ArrayList<>();
    }

    public ProxyMessage(
            String reqMethod, String reqUrl, String reqVersion,
            @NotNull Map<String, String> reqHeader,
            int resCode, String resMsg,
            @NotNull Map<String, String> resHeader
    ) {
        this.reqMethod = reqMethod;
        this.reqUrl = reqUrl;
        this.reqVersion = reqVersion;
        this.reqHeader = new HashMap<>(reqHeader);
        this.reqBody = new ArrayList<>();
        this.resCode = resCode;
        this.resMsg = resMsg;
        this.resHeader = new HashMap<>(resHeader);
        this.resBody = new ArrayList<>();
    }

    @NotNull
    public Set<String> getReqHeaders() {
        return getHeaders(reqHeader);
    }

    @NotNull
    public Set<String> getResHeaders() {
        return getHeaders(resHeader);
    }

    public void addReqHeader(String header, String field) {
        addHeader(header, field, reqHeader);
    }

    public void addResHeader(String header, String field) {
        addHeader(header, field, resHeader);
    }

    public boolean containsReqHeader(@NotNull String header) {
        return containsHeader(header, reqHeader);
    }

    public boolean containsResHeader(@NotNull String header) {
        return containsHeader(header, resHeader);
    }

    @Nullable
    public String getReqHeader(String header) {
        return getHeader(header, reqHeader);
    }

    @Nullable
    public String getResHeader(String header) {
        return getHeader(header, resHeader);
    }

    @NotNull
    private static Set<String> getHeaders(@NotNull Map<String, String> headerMap) {
        return headerMap.keySet();
    }

    private static void addHeader(String header, String field, @NotNull Map<String, String> headerMap) {
        headerMap.put(header, field);
    }

    private static boolean containsHeader(@NotNull String header, @NotNull Map<String, String> headerMap) {
        return headerMap.keySet().parallelStream().map(String::toLowerCase).anyMatch((key) -> key.equals(header.toLowerCase()));
    }

    @Nullable
    private static String getHeader(String header, @NotNull Map<String, String> headerMap) {
        for (String key : headerMap.keySet()) {
            if (key.equalsIgnoreCase(header)) {
                return headerMap.get(key);
            }
        }
        return null;
    }

    private void fixForConnect() {
        if ("CONNECT".equals(reqMethod) && resCode == 0) {
            resCode = 200;
            resMsg = "Connection Established";
        }
    }

    @NotNull
    private static String getBodyHex(@NotNull List<Byte> byteList) {
        StringBuilder sb = new StringBuilder();
        byteList.parallelStream().forEach((b) -> sb.append(byteToHex(b)));
        return sb.toString();
    }

    @NotNull
    public String getRequestHeader() {
        String out = reqMethod + " " + reqUrl + " " + reqVersion + System.lineSeparator();
        List<String> headers = new ArrayList<>();
        reqHeader.keySet().parallelStream().map((key) -> key + ": " + reqHeader.get(key)).parallel().forEach(headers::add);
        Collections.sort(headers);
        out += String.join(System.lineSeparator(), headers);
        return out;
    }

    @NotNull
    public String getRequest() {
        String out = getRequestHeader();
        out += System.lineSeparator() + System.lineSeparator();
        if (reqBody.isEmpty()) {
            out += "(No Body)";
        } else {
            out += "Body Length: " + reqBody.size() + System.lineSeparator();
            out += getBodyHex(reqBody);
        }
        return out;
    }

    @NotNull
    public String getRepsonse() {
        String out = getRepsonseHeader();
        out += System.lineSeparator() + System.lineSeparator();
        if (resBody.isEmpty()) {
            out += "(No Body)";
        } else {
            out += "Body Length: " + resBody.size() + System.lineSeparator();
            out += getBodyHex(resBody);
        }
        return out;
    }

    @NotNull
    public static String byteToHex(byte b) {
        int v = b & 0xFF;
        return Integer.toHexString(v);
    }

    @NotNull
    public String getRepsonseHeader() {
        fixForConnect();
        String out = reqVersion + " " + resCode + " " + resMsg + System.lineSeparator();
        List<String> headers = new ArrayList<>();
        resHeader.keySet().parallelStream().map((key) -> key + ": " + resHeader.get(key)).parallel().forEach(headers::add);
        Collections.sort(headers);
        out += String.join(System.lineSeparator(), headers);
        return out;
    }

    @NotNull
    private static String getBodyString(@NotNull List<Byte> byteList) {
        byte[] byteArray = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            byteArray[i] = byteList.get(i);
        }
        return new String(byteArray);
    }

    @NotNull
    public String toData() {
        String out = getRequest() + System.lineSeparator();
        out += "-----" + System.lineSeparator();
        out += getRepsonse();
        return out.trim();
    }
}
