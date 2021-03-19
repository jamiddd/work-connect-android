package com.jamid.workconnect.interfaces

import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.model.SimpleMessage

interface MessageItemClickListener {
    fun onImageClick(/*view: SimpleDraweeView, actualWidth: Int, actualHeight: Int, */view: SimpleDraweeView, message: SimpleMessage)
    fun onTextClick(content: String)
    fun onDocumentClick(simpleMessage: SimpleMessage, name: String, size: Long)
}