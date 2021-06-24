package com.jamid.workconnect.home

import android.os.Parcelable
import androidx.room.Entity
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.workconnect.END_OF_CAT
import kotlinx.parcelize.Parcelize
import java.util.*

@IgnoreExtraProperties
@Entity(tableName = "blogItems", primaryKeys = ["nid", "postId"])
@Parcelize
data class BlogItem(
    var content: String?,
    var type: String,
    var id: Long = System.currentTimeMillis(),
    var hint: String? = null,
    var spans: ArrayList<String> = arrayListOf(),
    var spanRangesStart: ArrayList<Int> = arrayListOf(),
    var spanRangesEnd: ArrayList<Int> = arrayListOf(),
    @Exclude @set: Exclude @get: Exclude
    var nid: Int = 0,
    @Exclude @set: Exclude @get: Exclude
    var postId: String = ""
): Parcelable {
    constructor(): this("", "Paragraph")

    override fun toString() : String {
        var formed = "[$type]"

        if (spans.isNotEmpty()) {
            formed += "[${spans.size}]"
            for (i in spans.indices) {
                val s = "[${spans[i]},${spanRangesStart[i]},${spanRangesEnd[i]}]"
                formed += s
            }
        } else {
            formed += "[0]"
        }

        if (content != null) {
            formed += END_OF_CAT + content
        }

        return formed
    }
}