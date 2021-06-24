package com.jamid.workconnect.interfaces

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.model.SimpleMessage
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import java.io.File

interface MessageItemClickListener {
    fun onImageClick(view: SimpleDraweeView, message: SimpleMessage)
    fun onTextClick(view: View)
    fun onDocumentClick(message: SimpleMessage)
    fun onImageDownloadClick(message: SimpleMessage)
    fun onUserClick(user: User)
    fun onMediaDownloadClick(viewHolder: RecyclerView.ViewHolder, message: SimpleMessage)
}