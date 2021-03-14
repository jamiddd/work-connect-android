package com.jamid.workconnect.model

import android.os.Parcelable
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.home.BlogItem
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Post(
    var id: String,
    var title: String,
    var content: String?,
    var thumbnail: String?,
    var admin: @RawValue Map<String, Any?>,
    var location: SimpleLocation?,
    var tags: List<String>,
    var links: List<String>,
    var uid: String,
    var likes: Long,
    var dislikes: Long,
    var createdAt: Long,
    var updatedAt: Long,
    var chatChannelId: String,
    var guidelines: String,
    var contributors: List<String>?,
    var items: List<BlogItem>?,
    var type: String
): Parcelable {
    constructor(): this(
    "",
    "",
    null,
    null,
    mapOf(),
    null,
    emptyList(),
    emptyList(),
    "",
    0,
    0,
    0,
    0,
    "",
    "",
    null,
    null,
    ""
    )

    companion object {
        fun getEmptyInstance(type: String = PROJECT) = Post("",
            "",
            null,
            null,
            mapOf(),
            null,
            emptyList(),
            emptyList(),
            "",
            0,
            0,
            0,
            0,
            "",
            "",
            null,
            null,
            "")
    }
}