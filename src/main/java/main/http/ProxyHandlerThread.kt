package main.http

import java.io.*
import java.net.*
import java.util.*
import java.util.function.Consumer
import kotlin.math.max

/**
 *
 * @author Bunnyspa
 */
class ProxyHandlerThread(proxy: Proxy, private val cpSocket: Socket) : Thread() {
    private enum class SaveType {
        NONE, SIGN, INDEX
    }

    private val proxy: Proxy
    private var ctpIS: DataInputStream? = null
    private val pm: ProxyMessage
    private var saveType = SaveType.NONE
    override fun run() {
        try {
            ctpIS = getDIS(cpSocket.getInputStream())
            try {
                readRequestHeader()
                try {
                    if ("CONNECT" == pm.reqMethod) {
                        handle_connect()
                    } else {
                        handle_default()
                    }
                } catch (ex: UnknownHostException) {
                    handleEx(404)
                } catch (ex: MalformedURLException) {
                    handleEx(404)
                } catch (ex: SocketTimeoutException) {
                    handleEx(504)
                } catch (ignored: InterruptedException) {
                }
            } catch (ignored: SocketTimeoutException) {
            }
            if (ctpIS != null) {
                ctpIS!!.close()
            }
        } catch (ignored: Exception) {
        }
        if (saveType == SaveType.SIGN) {
            proxy.process_sign(pm)
        } else if (saveType == SaveType.INDEX) {
            proxy.process_index(pm)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun handle_connect() {
        // Extract the URL and port of remote
        val pieces = pm.reqUrl!!.split(":").toTypedArray()
        val url = pieces[0]
        val port = pieces[1].toInt()

        // Get actual IP associated with this URL through DNS
        val address = InetAddress.getByName(url)
        Socket(address, port).use { spSocket ->
            getDOS(cpSocket.getOutputStream()).use { ptcOS ->
                spSocket.soTimeout = TIMEOUT
                // Send Connection established to the client
                val line = pm.reqVersion + " 200 Connection Established" + CRLF + CRLF
                ptcOS.writeBytes(line)
                ptcOS.flush()
                val ctsConnectThread: Thread = ProxyTunnel(cpSocket.getInputStream(), spSocket.getOutputStream())
                val stcConnectThread: Thread = ProxyTunnel(spSocket.getInputStream(), cpSocket.getOutputStream())
                ctsConnectThread.start()
                stcConnectThread.start()
                ctsConnectThread.join()
                stcConnectThread.join()
            }
        }
    }

    @Throws(IOException::class)
    private fun handle_default() {
        val connection = connection
        handleRequest(connection)
        handleResponse(connection)
    }

    @Throws(IOException::class)
    private fun readRequestHeader() {
        val requestLine = readLine(ctpIS!!)
        val requestParts = requestLine!!.split(" ").toTypedArray()
        pm.reqMethod = requestParts[0]
        pm.reqUrl = requestParts[1]
        pm.reqVersion = requestParts[2]
        // Header
        var line: String
        while (readLine(ctpIS!!).also { line = it!! } != null && line.isNotEmpty()) {
            val parts = line.split(":").toTypedArray()
            pm.addReqHeader(parts[0].trim { it <= ' ' }, parts[1].trim { it <= ' ' })
        }
        // Save if filtered
        if (pm.reqUrl!!.matches(FILTER_SIGN)) {
            saveType = SaveType.SIGN
        } else if (pm.reqUrl!!.matches(FILTER_INDEX)) {
            saveType = SaveType.INDEX
        }
    }

    @Throws(IOException::class)
    private fun handleRequest(connection: HttpURLConnection) {
        connection.requestMethod = pm.reqMethod
        // Header
        pm.reqHeaders.forEach(Consumer { key: String? -> connection.setRequestProperty(key, pm.getReqHeader(key)) })
        // Body
        val bodyTE = pm.containsReqHeader(Proxy.TRANSFER_ENCODING) && "chunked".equals(
            pm.getReqHeader(
                Proxy.TRANSFER_ENCODING
            ), ignoreCase = true
        )
        val bodyCL = pm.containsReqHeader(Proxy.CONTENT_LENGTH)
        if (bodyTE || bodyCL) {
            connection.doOutput = true
            getDOS(connection.outputStream).use { ptsOS ->
                if (bodyTE) {
                    handleBody_chunkedTransferEncoding(ctpIS!!, ptsOS, pm.reqBody, saveType != SaveType.NONE)
                } else if (bodyCL) {
                    handleBody_contentLength(
                        ctpIS!!,
                        ptsOS,
                        pm.reqBody,
                        saveType != SaveType.NONE,
                        pm.getReqHeader(Proxy.CONTENT_LENGTH)!!
                            .toInt()
                    )
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun handleResponse(connection: HttpURLConnection) {
        pm.resCode = connection.responseCode
        pm.resMsg = connection.responseMessage
        getDOS(cpSocket.getOutputStream()).use { ptcOS ->
            // Header
            ptcOS.writeBytes(pm.reqVersion + " " + pm.resCode + " " + pm.resMsg + CRLF)
            val resHeaderFields = connection.headerFields
            for (key in resHeaderFields.keys) {
                val resHeaderValues = resHeaderFields[key]!!
                if (key != null && resHeaderValues.isNotEmpty()) {
                    val value = java.lang.String.join(", ", resHeaderFields[key])
                    pm.addResHeader(key, value)
                    ptcOS.writeBytes("$key: $value$CRLF")
                }
            }
            ptcOS.writeBytes(CRLF)

            // Body
            val bodyTE = pm.containsResHeader(Proxy.TRANSFER_ENCODING) && "chunked".equals(
                pm.getResHeader(
                    Proxy.TRANSFER_ENCODING
                ), ignoreCase = true
            )
            val bodyCL = pm.containsResHeader(Proxy.CONTENT_LENGTH)
            if (bodyTE || bodyCL) {
                val cis: InputStream = try {
                    connection.inputStream
                } catch (ex: IOException) {
                    connection.errorStream
                }
                try {
                    getDIS(cis).use { stpIS ->
                        if (bodyTE) {
                            handleBody_chunkedTransferEncoding(stpIS, ptcOS, pm.resBody, saveType != SaveType.NONE)
                        } else if (bodyCL) {
                            handleBody_contentLength(
                                stpIS,
                                ptcOS,
                                pm.resBody,
                                saveType != SaveType.NONE,
                                pm.getResHeader(Proxy.CONTENT_LENGTH)!!
                                    .toInt()
                            )
                        }
                    }
                } catch (ignored: NullPointerException) {
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun handleEx(code: Int) {
        pm.resCode = code
        pm.resMsg = STATUS[code]
        getDOS(cpSocket.getOutputStream()).use { dos -> dos.writeBytes(pm.reqVersion + " " + pm.resCode + " " + pm.resMsg + CRLF + CRLF) }
    }

    @get:Throws(IOException::class)
    private val connection: HttpURLConnection
        private get() {
            val remoteURL = URL(pm.reqUrl)
            val connection = remoteURL.openConnection() as HttpURLConnection
            connection.readTimeout = TIMEOUT
            return connection
        }

    companion object {
        private val REGEX_HOST =
            ("(\\.girlfrontline\\.co\\.kr|\\.ppgame\\.com|\\.txwy\\.tw|\\.sunborngame\\.com)").toRegex()
        private val FILTER_SIGN =
            (".*$REGEX_HOST.*/Index/(getDigitalSkyNbUid|getUidTianxiaQueue|getUidEnMicaQueue)").toRegex()
        private val FILTER_INDEX = ".*$REGEX_HOST.*/Index/index".toRegex()
        private const val CRLF = "\r\n"
        private const val TIMEOUT = 10000
        private val STATUS: Map<Int, String> =
            object : HashMap<Int, String>( // <editor-fold defaultstate="collapsed">
            ) {
                init {
                    put(404, "Not Found")
                    put(504, "Gateway Timeout")
                }
            } // </editor-fold>

        @Throws(IOException::class)
        private fun readLine(`is`: DataInputStream): String? {
            val sb = StringBuilder()
            var i1: Int
            var i2: Int
            while (`is`.read().also { i1 = it } != -1) {
                if (i1 == '\r'.toInt()) {
                    when (`is`.read().also { i2 = it }) {
                        -1 -> {
                            sb.append(i1.toChar())
                            return sb.toString()
                        }
                        '\n'.toInt() -> return sb.toString()
                        else -> sb.append(i1.toChar()).append(i2.toChar())
                    }
                }
                sb.append(i1.toChar())
            }
            return if (sb.isEmpty()) {
                null
            } else sb.toString()
        }

        @Throws(IOException::class)
        private fun handleBody_chunkedTransferEncoding(
            `is`: DataInputStream,
            os: DataOutputStream,
            cache: MutableList<Byte>,
            saveEnabled: Boolean
        ) {
            var readLen: Int
            var buffer = ByteArray(max(`is`.available(), 1))
            while (`is`.read(buffer).also { readLen = it } != -1) {
                os.writeBytes(Integer.toHexString(readLen) + CRLF)
                for (i in 0 until readLen) {
                    if (saveEnabled) {
                        cache.add(buffer[i])
                    }
                    os.writeByte(buffer[i].toInt())
                }
                os.writeBytes(CRLF)
                os.flush()
                buffer = ByteArray(max(`is`.available(), 1))
            }
            os.writeBytes("0$CRLF$CRLF")
            os.flush()
        }

        @Throws(IOException::class)
        private fun handleBody_contentLength(
            `is`: DataInputStream,
            os: DataOutputStream,
            cache: MutableList<Byte>,
            saveEnabled: Boolean,
            cl: Int
        ) {
            var cl = cl
            while (0 < cl) {
                val buffer = ByteArray(cl)
                val readLen = `is`.read(buffer)
                for (i in 0 until readLen) {
                    if (saveEnabled) {
                        cache.add(buffer[i])
                    }
                    os.writeByte(buffer[i].toInt())
                }
                os.flush()
                cl -= readLen
            }
            os.flush()
        }

        private fun getDIS(`is`: InputStream): DataInputStream {
            return DataInputStream(`is`)
        }

        private fun getDOS(os: OutputStream): DataOutputStream {
            return DataOutputStream(os)
        }
    }

    init {
        try {
            cpSocket.soTimeout = 5000
        } catch (ignored: IOException) {
        }
        this.proxy = proxy
        pm = ProxyMessage()
    }
}