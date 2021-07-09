package com.jamid.workconnect.interfaces

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.model.SimpleMessage
import com.jamid.workconnect.model.User
import com.jamid.workconnect.views.zoomable.ImageControllerListener

interface MessageItemClickListener {
    fun onImageClick(view: SimpleDraweeView, message: SimpleMessage)
    fun onTextClick(view: View)
    fun onDocumentClick(message: SimpleMessage)
    fun onUserClick(user: User)
    fun onMediaDownloadClick(viewHolder: RecyclerView.ViewHolder, message: SimpleMessage)
    fun onImageSet(message: SimpleMessage, imageControllerListener: ImageControllerListener)
}