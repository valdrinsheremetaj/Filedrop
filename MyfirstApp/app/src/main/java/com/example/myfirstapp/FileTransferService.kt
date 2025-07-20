package com.example.myfirstapp

import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class FileTransferService : Service() {

    companion object {
        const val ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE"
        const val ACTION_RECEIVE_FILE = "com.example.android.wifidirect.RECEIVE_FILE"
        const val EXTRAS_FILE_PATH = "file_url"
        const val EXTRAS_GROUP_OWNER_ADDRESS = "go_host"
        const val EXTRAS_GROUP_OWNER_PORT = "go_port"
        const val EXTRAS_FILE_NAME = "file_name"
        const val EXTRAS_MIME_TYPE = "mime_type"

        private const val SOCKET_TIMEOUT = 5000
        private const val TAG = "FileTransferService"
        private const val SERVER_PORT = 8888
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            intent?.let {
                handleIntent(it)
                // only keep running if trying to receive files
                if (it.action != ACTION_RECEIVE_FILE) {
                    stopSelf(startId)
                }
            }
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SEND_FILE -> handleSendFile(intent)
            ACTION_RECEIVE_FILE -> handleReceiveFile()
        }
    }

    // send file to group owner
    private fun handleSendFile(intent: Intent) {
        val fileUri = intent.getStringExtra(EXTRAS_FILE_PATH)
        val host = intent.getStringExtra(EXTRAS_GROUP_OWNER_ADDRESS)
        val port = intent.getIntExtra(EXTRAS_GROUP_OWNER_PORT, SERVER_PORT)
        val fileName = intent.getStringExtra(EXTRAS_FILE_NAME) ?: "received_file"
        val mimeType = intent.getStringExtra(EXTRAS_MIME_TYPE) ?: "application/octet-stream"

        if (fileUri == null || host == null || port == -1) {
            Log.e(TAG, "Invalid input parameters.")
            return
        }

        val socket = Socket()
        try {
            Log.d(TAG, "Opening client socket")
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)

            Log.d(TAG, "Client socket connected: ${socket.isConnected}")
            val stream = BufferedOutputStream(socket.getOutputStream())
            val writer = BufferedWriter(OutputStreamWriter(stream))

            // Write metadata first: file name + MIME type
            writer.write("$fileName\n")
            writer.write("$mimeType\n")
            writer.flush()

            val cr: ContentResolver = applicationContext.contentResolver
            val inputStream = try {
                cr.openInputStream(Uri.parse(fileUri))
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "File not found: ${e.message}")
                null
            }

            // Send actual file content
            inputStream?.use { input ->
                stream.use { output ->
                    copyFile(input, output)
                }
            }

            Log.d(TAG, "Client: Data written")
        } catch (e: Exception) {
            Log.e(TAG, "Error during file transfer: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleReceiveFile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(SERVER_PORT)
                Log.d(TAG, "Server: Socket opened, waiting for connections...")

                while (true) {
                    val client = serverSocket.accept()
                    Log.d(TAG, "Server: Client connected")

                    val input = client.getInputStream()

                    // First, read metadata (use a separate reader)
                    val buffered = BufferedInputStream(input)
                    val lineBuffer = ByteArrayOutputStream()
                    var byte: Int

                    // Read file name (up to newline)
                    while (buffered.read().also { byte = it } != -1 && byte != '\n'.code) {
                        lineBuffer.write(byte)
                    }
                    val fileName = lineBuffer.toString("UTF-8").trim()
                    lineBuffer.reset()

                    // Read MIME type (up to newline)
                    while (buffered.read().also { byte = it } != -1 && byte != '\n'.code) {
                        lineBuffer.write(byte)
                    }
                    val mimeType = lineBuffer.toString("UTF-8").trim()

                    Log.d(TAG, "Receiving file: $fileName with MIME type: $mimeType")

                    val resolver = applicationContext.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { output ->
                            copyFile(buffered, output)
                        }

                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        Log.d(TAG, "Server: File saved to Downloads as $fileName")
                    } else {
                        Log.e(TAG, "Server: Failed to create MediaStore entry")
                    }

                    client.close()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            }
        }
    }





    override fun onBind(intent: Intent?): IBinder? = null

    // read iput stream bytes and send them to output stream
    private fun copyFile(inputStream: InputStream, out: OutputStream): Boolean {
        return try {
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
            out.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file: ${e.message}")
            false
        }
    }
}