package com.jamid.workconnect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="message_key")
data class MessageKey(val prev: String?, @PrimaryKey val current: String, val next: String?)