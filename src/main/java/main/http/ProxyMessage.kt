package main.http

import java.util.*
import kotlin.experimental.and

/**
 *
 * @author Bunnyspa
 */
class ProxyMessage {
    var reqMethod: String? = null
    var reqUrl: String? = null
    var reqVersion: String? = null
    private val reqHeader: MutableMap<String?, String>
    val reqBody: MutableList<Byte>
    var resCode = 0
    var resMsg: String? = null
    private val resHeader: MutableMap<String?, String>
    val resBody: MutableList<Byte>

    constructor() {
        reqHeader = HashMap()
        resHeader = HashMap()
        reqBody = mutableListOf()
        resBody = mutableListOf()
    }

    constructor(
        reqMethod: String?, reqUrl: String?, reqVersion: String?,
        reqHeader: Map<String?, String>,
        resCode: Int, resMsg: String?,
        resHeader: Map<String?, String>
    ) {
        this.reqMethod = reqMethod
        this.reqUrl = reqUrl
        this.reqVersion = reqVersion
        this.reqHeader = HashMap(reqHeader)
        reqBody = mutableListOf()
        this.resCode = resCode
        this.resMsg = resMsg
        this.resHeader = HashMap(resHeader)
        resBody = mutableListOf()
    }

    val reqHeaders: Set<String?>
        get() = getHeaders(reqHeader)

    fun addReqHeader(header: String, field: String) {
        addHeader(header, field, reqHeader)
    }

    fun addResHeader(header: String, field: String) {
        addHeader(header, field, resHeader)
    }

    fun containsReqHeader(header: String): Boolean {
        return containsHeader(header, reqHeader)
    }

    fun containsResHeader(header: String): Boolean {
        return containsHeader(header, resHeader)
    }

    fun getReqHeader(header: String?): String? {
        return getHeader(header, reqHeader)
    }

    fun getResHeader(header: String?): String? {
        return getHeader(header, resHeader)
    }

    private fun fixForConnect() {
        if ("CONNECT" == reqMethod && resCode == 0) {
            resCode = 200
            resMsg = "Connection Established"
        }
    }

    val requestHeader: String
        get() {
            var out = reqMethod + " " + reqUrl + " " + reqVersion + System.lineSeparator()
            val headers: MutableList<String> = mutableListOf()
            reqHeader.keys.stream().map { key: String? -> key + ": " + reqHeader[key] }
                .forEach { e: String -> headers.add(e) }
            headers.sort()
            out += java.lang.String.join(System.lineSeparator(), headers)
            return out
        }
    private val request: String
        get() {
            var out = requestHeader
            out += System.lineSeparator() + System.lineSeparator()
            if (reqBody.isEmpty()) {
                out += "(No Body)"
            } else {
                out += "Body Length: " + reqBody.size + System.lineSeparator()
                out += getBodyHex(reqBody)
            }
            return out
        }
    private val repsonse: String
        get() {
            var out = repsonseHeader
            out += System.lineSeparator() + System.lineSeparator()
            if (resBody.isEmpty()) {
                out += "(No Body)"
            } else {
                out += "Body Length: " + resBody.size + System.lineSeparator()
                out += getBodyHex(resBody)
            }
            return out
        }
    val repsonseHeader: String
        get() {
            fixForConnect()
            var out = reqVersion + " " + resCode + " " + resMsg + System.lineSeparator()
            val headers: MutableList<String> = mutableListOf()
            resHeader.keys.stream().map { key: String? -> key + ": " + resHeader[key] }
                .forEach { e: String -> headers.add(e) }
            headers.sort()
            out += java.lang.String.join(System.lineSeparator(), headers)
            return out
        }

    fun toData(): String {
        var out = request + System.lineSeparator()
        out += "-----" + System.lineSeparator()
        out += repsonse
        return out.trim { it <= ' ' }
    }

    companion object {
        private fun getHeaders(headerMap: Map<String?, String>): Set<String?> {
            return headerMap.keys
        }

        private fun addHeader(header: String, field: String, headerMap: MutableMap<String?, String>) {
            headerMap[header] = field
        }

        private fun containsHeader(header: String, headerMap: Map<String?, String>): Boolean {
            return headerMap.keys.stream().map { obj: String? -> obj!!.toLowerCase() }
                .anyMatch { key: String -> key == header.toLowerCase() }
        }

        private fun getHeader(header: String?, headerMap: Map<String?, String>): String? {
            for (key in headerMap.keys) {
                if (key.equals(header, ignoreCase = true)) {
                    return headerMap[key]
                }
            }
            return null
        }

        private fun getBodyHex(byteList: List<Byte>): String {
            val sb = StringBuilder()
            byteList.forEach { b ->
                sb.append(byteToHex(b))
            }
            return sb.toString()
        }

        private fun byteToHex(b: Byte): String {
            return Integer.toHexString((b and 0xFF.toByte()).toInt())
        }

    }
}