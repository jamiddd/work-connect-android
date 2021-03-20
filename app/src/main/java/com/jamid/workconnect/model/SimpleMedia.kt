package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
@Entity(tableName="simple_media")
data class SimpleMedia(@PrimaryKey val id: String, val type: String, val mediaLocation: String, val createdAt: Long, @Exclude @set: Exclude @get: Exclude var onDiskLocation: String? = null, @Exclude @set: Exclude @get: Exclude var size: Long = 0) :
    Parcelable { constructor(): this("", "", "", 0)}