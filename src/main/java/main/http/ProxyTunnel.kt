package main.http

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 *
 * @author Bunnyspa
 */
class ProxyTunnel
/**
 * Creates Object to Listen to Client and Transmit that data to the server
 *
 * @param is Stream that proxy uses to receive data from client
 * @param os Stream that proxy uses to transmit data to remote server
 */(private val `is`: InputStream, private val os: OutputStream) : Thread() {
    override fun run() {
        try {
            // Read byte by byte from client and send directly to server
            val buffer = ByteArray(4096)
            var read: Int
            do {
                read = `is`.read(buffer)
                if (read > 0) {
                    os.write(buffer, 0, read)
                    if (`is`.available() < 1) {
                        os.flush()
                    }
                }
            } while (read >= 0)
        } catch (ignored: IOException) {
        }
    }
}