package com.jamid.workconnect.interfaces

import android.view.View
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.model.SimpleMessage

interface MessageItemClickListener {
    fun onImageClick(view: SimpleDraweeView, message: SimpleMessage)
    fun onTextClick(view: View)
    fun onDocumentClick(message: SimpleMessage)
}