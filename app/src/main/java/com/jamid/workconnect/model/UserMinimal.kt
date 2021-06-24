package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "user_minimals")
@Parcelize
data class UserMinimal (
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val username: String,
    val photo: String?
): Parcelable {
    constructor(): this("", "", "", "", null)
}