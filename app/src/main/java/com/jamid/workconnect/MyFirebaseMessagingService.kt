package com.jamid.workconnect

import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {

    private val auth = Firebase.auth

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        if (auth.currentUser != null) {
            if (application != null) {
                val intent = Intent("tokenIntent").apply {
                    putExtra("token", p0)
                }
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage.data.isNotEmpty()) {

            val intent = Intent(NOTIFICATION_INTENT)

            val chatChannelId = remoteMessage.data[CHAT_CHANNEL_ID]
            if (chatChannelId != null) {
                val notificationId = remoteMessage.data[NOTIFICATION_ID]
                if (notificationId != null){
                    val notificationContent = remoteMessage.data[NOTIFICATION_CONTENT]
                    val senderId = remoteMessage.data[SENDER_ID]
                    val senderName = remoteMessage.data[SENDER_NAME]
                    val postId = remoteMessage.data[POST_ID]
                    val requestId = remoteMessage.data[REQUEST_ID]
                    val bundle = Bundle().apply {
                        putString(NOTIFICATION_ID, notificationId)
                        putString(NOTIFICATION_CONTENT, notificationContent)
                        putString(SENDER_ID, senderId)
                        putString(SENDER_NAME, senderName)
                        putString(POST_ID, postId)
                        putString(CHAT_CHANNEL_ID, chatChannelId)
                        putString(REQUEST_ID, requestId)
                    }
                    intent.putExtra(ACCEPT_PROJECT_NOTIFICATION, bundle)
                } else {
                    val message = remoteMessage.data[MESSAGE]
                    val senderId = remoteMessage.data[SENDER_ID]
                    val senderName = remoteMessage.data[SENDER_NAME]
                    val bundle = Bundle().apply {
                        putString(CHAT_CHANNEL_ID, chatChannelId)
                        putString(MESSAGE, message)
                        putString(SENDER_ID, senderId)
                        putString(SENDER_NAME, senderName)
                    }
                    intent.putExtra(CHAT_NOTIFICATION, bundle)
                }
            }

            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

    companion object {
        private const val TAG = "MyFCM"
    }
}