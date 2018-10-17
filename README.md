# HydraSocket
Socket implementation for Android  based on okhttp
Kotlin DSL:

        /*
         * Sending heartbeat
         * */
        val socket = HydraSocket(url, heartbeatDataToPush, heartbeatInterval)

        /* 
         * Connecting socket
         */
        socket.connect()

        /*
         * Pushing data
         */
        socket.push(dataToPush)

        /*
         * Getting Callbacks
         */
         
        socket.onMessage(IMessageCallback {
            Log.e(TAG, "message : " + it)
        }).onOpen(ISocketOpenCallback {
            Log.e(TAG, "open")
        }).onClose(ISocketCloseCallback {
            Log.e(TAG, "close")
        }).onError(IErrorCallback {
            Log.e(TAG, "error")
        })
