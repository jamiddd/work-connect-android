package com.jamid.workconnect.model

data class SimpleRequest(
    val id: String,
    val postId: String,
    val sender: String,
    val receiver: String,
    val notificationId: String,
    val createdAt: Long
){
    constructor(): this("", "", "", "", "", 0)
}