package main.http

import main.http.AuthCode.decodeWithGzip
import main.json.JsonParser
import main.ui.dialog.ProxyDialog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.*
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream
import javax.swing.SwingUtilities
import kotlin.concurrent.withLock

/**
 *
 * @author Bunnyspa
 */
class Proxy(private val dialog: ProxyDialog) {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val serverSocket = initServerSocket()

    @Throws(IOException::class)
    private fun initServerSocket(): ServerSocket {
        return try {
            ServerSocket(PORT)
        } catch (ex: IOException) {
            ServerSocket(0)
        }
    }

    private val threads: MutableList<Thread> = mutableListOf()
    private var isRunning = true
    private var keyReceived = false
    private var key: String? = null
    private val mainThread = Thread {
        while (isRunning) {
            try {
                val socket = serverSocket.accept()
                val thread = ProxyHandlerThread(this, socket)
                threads.add(thread)
                thread.start()
            } catch (ignored: IOException) {
            }
        }
    }
    val address: String
        get() {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val i = interfaces.nextElement()
                    val addresses = i.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val a = addresses.nextElement()
                        if (a.isSiteLocalAddress) {
                            return a.hostAddress
                        }
                    }
                }
                return InetAddress.getLocalHost().hostAddress
            } catch (ignored: SocketException) {
            } catch (ignored: UnknownHostException) {
            }
            return ""
        }
    val port: Int
        get() = serverSocket.localPort

    fun start() {
        isRunning = true
        mainThread.start()
    }

    fun process_sign(pm: ProxyMessage) {
        val data = getBody(pm)
        try {
            key = JsonParser.parseSign(decode(data, AuthCode.SIGN_KEY))
            keyReceived = true
        } catch (ignored: Exception) {
        }
    }

    fun process_index(pm: ProxyMessage) {
        lock.withLock {
            var data: String? = getBody(pm)
            if (!data!!.contains("{")) {
                try {
                    while (!keyReceived) {
                        condition.await()
                    }
                    val decoded = decode(data, key)
                    data = decoded
                } catch (ignored: Exception) {
                }
            }
            parse(data)
        }

    }

    private fun getBody(pm: ProxyMessage): String {
        val byteList = pm.resBody
        val byteArray = ByteArray(byteList.size)
        for (i in byteList.indices) {
            val b = byteList[i]
            byteArray[i] = b
        }
        if (pm.containsResHeader(CONTENT_ENCODING) && "gzip".equals(
                pm.getResHeader(CONTENT_ENCODING),
                ignoreCase = true
            )
        ) {
            try {
                return decode_gzip(byteArray)
            } catch (ignored: IOException) {
            }
        }
        return String(byteArray)
    }

    private fun parse(s: String?) {
        SwingUtilities.invokeLater { dialog.parse(s!!) }
    }

    fun stop() {
        isRunning = false
        threads.stream()
            .filter { obj: Thread -> obj.isAlive }
            .forEach { obj: Thread -> obj.interrupt() }
        threads.clear()
        try {
            serverSocket.close()
        } catch (ignored: IOException) {
        }
        try {
            mainThread.join()
        } catch (ignored: InterruptedException) {
        }
    }

    companion object {
        const val CONTENT_LENGTH = "Content-Length"
        const val TRANSFER_ENCODING = "Transfer-Encoding"
        const val CONTENT_ENCODING = "Content-Encoding"
        const val PORT = 8080
        private fun decode(text: String?, key: String?): String {
            try {
                if (text == null || text.isEmpty()) {
                    return ""
                }
                if (text.length <= 1) {
                    return text
                }
                if (text.startsWith("#")) {
                    val data = decodeWithGzip(text.substring(1), key)
                    return if (data == null) "" else decode_gzip(data)
                }
                return AuthCode.decode(text, key)
            } catch (ignored: Exception) {
            }
            return ""
        }

        @Throws(IOException::class)
        fun decode_gzip(array: ByteArray): String {
            val gzipIS = GZIPInputStream(ByteArrayInputStream(array))
            val byteOS = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var cl: Int
            while (gzipIS.read(buffer).also { cl = it } != -1) {
                byteOS.write(buffer, 0, cl)
            }
            return byteOS.toString()
        }
    }
}