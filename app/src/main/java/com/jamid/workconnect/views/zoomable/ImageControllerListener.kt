package com.jamid.workconnect.views.zoomable

import android.graphics.drawable.Animatable
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo

class ImageControllerListener : BaseControllerListener<ImageInfo>() {

    var params = Pair(0, 0)

    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
        imageInfo?.let {
            params = it.width to it.height
        }
    }
}