package main.http

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.math.max

/**
 *
 * @author Bunnyspa
 */
object ResearchConnection {
    private const val TIMEOUT = 1000
    private const val URL = "https://gfchipcalc-researchserver.herokuapp.com/"

    // chips;-
    // OR
    // chips;rotaions;locations
    fun sendResult(data: String) {
        put("", data)
    }

    // version: version;name;star
    val version: String?
        get() = ResearchConnection["version"]

    // progress: prog;total
    val progress: String?
        get() = ResearchConnection["progress"]

    // task: chips
    val task: String?
        get() = ResearchConnection["task"]

    private fun put(path: String, data: String) {
        try {
            val u = URL(URL + path)
            val con = u.openConnection() as HttpURLConnection
            con.readTimeout = TIMEOUT
            con.requestMethod = "PUT"
            con.setFixedLengthStreamingMode(data.length)
            con.doOutput = true
            DataOutputStream(con.outputStream).use { os ->
                os.writeBytes(data)
                os.flush()
            }

//            int code = con.getResponseCode();
//            String msg = con.getResponseMessage(); 
        } catch (ignored: Exception) {
        }
    }

    private operator fun get(path: String): String? {
        try {
            val u = URL(URL + path)
            val con = u.openConnection() as HttpURLConnection
            con.readTimeout = TIMEOUT
            con.requestMethod = "GET"

//            int code = con.getResponseCode();
//            String msg = con.getResponseMessage(); 
            val header_te: String = con.getHeaderField(Proxy.TRANSFER_ENCODING)
            val header_cl: String = con.getHeaderField(Proxy.CONTENT_LENGTH)
            val cis = con.inputStream
            val out: MutableList<Byte> = mutableListOf()
            DataInputStream(cis).use { `is` ->
                if ("chunked".equals(header_te, ignoreCase = true)) {
                    var readLen: Int
                    var buffer = ByteArray(max(`is`.available(), 1))
                    while (`is`.read(buffer).also { readLen = it } != -1) {
                        for (i in 0 until readLen) {
                            out.add(buffer[i])
                        }
                        buffer = ByteArray(max(`is`.available(), 1))
                    }
                } else if (header_cl != null) {
                    var cl = header_cl.toInt()
                    while (0 < cl) {
                        val buffer = ByteArray(cl)
                        val readLen = `is`.read(buffer)
                        for (i in 0 until readLen) {
                            out.add(buffer[i])
                        }
                        cl -= readLen
                    }
                }
            }
            if (out.isEmpty()) {
                return ""
            }
            val bytes = ByteArray(out.size)
            for (i in out.indices) {
                bytes[i] = out[i]
            }
            return String(bytes, StandardCharsets.UTF_8)
        } catch (ignored: Exception) {
        }
        return null
    }
}