package test.test.`in`.hydrasocketexample.hydrasocket

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.io.IOException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class HydraSocket {


    @Throws(IOException::class)
    constructor(endpointUri: String) {
        Log.e(TAG, endpointUri)
        this.endpointUri = endpointUri
        this.timer = Timer("Reconnect Timer for $endpointUri")
    }

    @Throws(IOException::class)
    constructor(endpointUri: String, heartbeatData: String) {
        Log.e(TAG, endpointUri)
        this.endpointUri = endpointUri
        this.heartbeatData = heartbeatData
        this.timer = Timer("Reconnect Timer for $endpointUri")

    }

    @Throws(IOException::class)
    constructor(endpointUri: String,  heartbeatData: String,heartbeatIntervalInMs: Int) {
        Log.e(TAG, endpointUri)
        this.endpointUri = endpointUri
        this.heartbeatInterval = heartbeatIntervalInMs
        this.timer = Timer("Reconnect Timer for $endpointUri")
    }

    private var TAG: String = "HydraSocket"
    private var heartbeatInterval: Int? = 2000   // *** default 2 seconds heartbeat time ****
    private var heartbeatData: String? = null
    private var timer: Timer? = null
    private var reconnectTimerTask: TimerTask? = null
    private var heartbeatTimerTask: TimerTask? = null
    private val RECONNECT_INTERVAL_MS = 5000
    private val sendBuffer = LinkedBlockingQueue<String>()


    private val socketCloseCallbacks = Collections
        .newSetFromMap(HashMap<ISocketCloseCallback, Boolean>())

    private val socketOpenCallbacks = Collections
        .newSetFromMap(HashMap<ISocketOpenCallback, Boolean>())

    private val messageCallbacks = Collections
        .newSetFromMap(HashMap<IMessageCallback, Boolean>())

    private val errorCallbacks = Collections
        .newSetFromMap(HashMap<IErrorCallback, Boolean>())


    /************* endpointUri is the url for the socket************* */
    private var endpointUri: String = ""

    private var webSocket: WebSocket? = null

    private val httpClient = OkHttpClient()

    private val hydraWebSocketListener = HydraWebSocket()
    private var reconnectOnFailure = true


    /**
     * **************Setup Listener ***********
     * **/
    inner class HydraWebSocket : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.e(TAG, "WebSocket onOpen: {}" + "  response:" + response.toString())
            this@HydraSocket.webSocket = webSocket
            cancelReconnectTimer()
            startHeartbeatTimer()

            for (callback in socketOpenCallbacks) {
                callback.onOpen()
            }
            this@HydraSocket.flushSendBuffer()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.e(TAG, "WebSocket onClosed {}/{}" + code + "  " + reason)
            for (callback in socketCloseCallbacks) {
                callback.onClose()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.e(TAG, "WebSocket onClosing {}/{}" + code + "  " + reason)

        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.e(TAG, "WebSocket onFailure {}/{}" + t.toString() + "  " + response)

            // Assume closed on failure
            for (callback in errorCallbacks) {
                callback.onError(t.message)
            }

            if (this@HydraSocket.webSocket != null) {
                try {
                    this@HydraSocket.webSocket!!.close(1001 /*CLOSE_GOING_AWAY*/, "EOF received")
                } finally {
                    this@HydraSocket.webSocket = null
                }
            }
            if (reconnectOnFailure) {
                scheduleReconnectTimer()
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            Log.e(TAG, "WebSocket onMessage {}/{}" + bytes.toString())
            //   onMessage()
            for (callback in messageCallbacks) {
                callback.onMessage(bytes.toString())
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.e(TAG, "WebSocket onMessage {}/{}" + text)
            for (callback in messageCallbacks) {
                callback.onMessage(text)
            }
        }
    }

    /**
     * **************Connect to socket ***********
     * **/
    @Throws(IOException::class)
    public fun connect() {
        Log.e(TAG, "Connect")
        // No support for ws:// or ws:// in okhttp. See https://github.com/square/okhttp/issues/1652
        val httpUrl = this.endpointUri.replaceFirst("^ws:".toRegex(), "http:")
            .replaceFirst("^wss:".toRegex(), "https:")
        val request = Request.Builder().url(httpUrl).build()
        webSocket = httpClient.newWebSocket(request, hydraWebSocketListener)
    }

    /**
     * **************Disconnect to socket ***********
     * **/
    @Throws(IOException::class)
    public fun disconnect() {
        Log.e(TAG, "disconnect")
        if (webSocket != null) {
            webSocket!!.close(1001 /*CLOSE_GOING_AWAY*/, "Disconnected by client")
        }
        cancelHeartbeatTimer()
        cancelReconnectTimer()
    }

    /**
     * @return true if the socket connection is connected
     **/
    fun isConnected(): Boolean {
        return webSocket != null
    }

    /**
     * Sets up and schedules a timer task to make repeated reconnect attempts at configured
     * intervals
     **/
    private fun scheduleReconnectTimer() {
        cancelReconnectTimer()
        cancelHeartbeatTimer()

        this@HydraSocket.reconnectTimerTask = object : TimerTask() {
            override fun run() {
                Log.e(TAG, "reconnectTimerTask run")
                try {
                    this@HydraSocket.connect()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reconnect to " + this@HydraSocket.hydraWebSocketListener + "  : " + e)
                }

            }
        }
        timer!!.schedule(this@HydraSocket.reconnectTimerTask, RECONNECT_INTERVAL_MS.toLong())
    }

    private fun startHeartbeatTimer() {
        this@HydraSocket.heartbeatTimerTask = object : TimerTask() {
            override fun run() {
                Log.e(TAG, "heartbeatTimerTask run")
                if (this@HydraSocket.isConnected()) {
                    try {
                        /********************************************
                         *********** Send Heartbeat here ************
                         ********************************************/
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send heartbeat :  " + e)
                    }

                }
            }
        }

        timer!!.schedule(
            this@HydraSocket.heartbeatTimerTask, this@HydraSocket.heartbeatInterval!!.toLong(),
            this@HydraSocket.heartbeatInterval!!.toLong()
        )
    }

    private fun cancelHeartbeatTimer() {
        if (this@HydraSocket.heartbeatTimerTask != null) {
            this@HydraSocket.heartbeatTimerTask!!.cancel()
        }
    }

    private fun cancelReconnectTimer() {
        if (this@HydraSocket.reconnectTimerTask != null) {
            this@HydraSocket.reconnectTimerTask!!.cancel()
        }
    }

    private fun flushSendBuffer() {
        while (this.isConnected() && !this.sendBuffer.isEmpty()) {
            val body = this.sendBuffer.remove()
            this.webSocket!!.send(body.toString())
        }
    }

    /**
     * Sending/Pushing subscription to backend *
     * **/
    public fun push(text: String) {
        if (this.isConnected()) {
            try {
                webSocket!!.send(text)
            } catch (e: Exception) {
                System.gc()
            }

        } else {
            this.sendBuffer!!.add(text)
        }
    }


    fun onClose(callback: ISocketCloseCallback): HydraSocket {
        this.socketCloseCallbacks.add(callback)
        return this
    }

    fun onError(callback: IErrorCallback): HydraSocket {
        this.errorCallbacks.add(callback)
        return this
    }

    fun onMessage(callback: IMessageCallback): HydraSocket {
        this.messageCallbacks.add(callback)
        return this
    }

    fun onOpen(callback: ISocketOpenCallback): HydraSocket {
        cancelReconnectTimer()
        this.socketOpenCallbacks.add(callback)
        return this
    }

}