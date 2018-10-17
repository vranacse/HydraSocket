package test.test.`in`.hydrasocketexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import test.test.`in`.hydrasocketexample.hydrasocket.*

class MainActivity : AppCompatActivity() {

    private var TAG: String = "MainActivity"

    /*
    *
    * dataToPush
    *
    */
    private var dataToPush: String = ""

    /*
    *
    * URL for Socket
    *
    */
    private var url: String = ""

    /*
      *
      * HeartBeat dataToPush
      *
      */
    private var heartbeatDataToPush: String = " "

    /*
      *
      * heartbeatInterval  will send heartbeat after each interval
      *
      */
    private var heartbeatInterval: Int = 10000 //*** In milliseconds ***

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /*
         *
         * Sending heartbeat
         *
         * *****/
        val socket = HydraSocket(url, heartbeatDataToPush, heartbeatInterval)

        /*
         *
         * Connecting socket
         *
         */
        socket.connect()

        /*
         *
         * Pushing data
         *
         */
        socket.push(dataToPush)

        /*
         *
         * Getting Callbacks
         *
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

    }
}
