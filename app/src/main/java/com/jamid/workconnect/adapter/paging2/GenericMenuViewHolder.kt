package com.jamid.workconnect.adapter.paging2

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageOptions
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.GenericViewHolder
import com.jamid.workconnect.interfaces.GenericMenuClickListener
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleComment

class GenericMenuViewHolder(parent: ViewGroup, @LayoutRes layout: Int, private val baseItem: Any? = null): GenericViewHolder<GenericMenuItem>(parent, layout){

    private val genericMenuClickListener = parent.context as GenericMenuClickListener

    override fun bind(item: GenericMenuItem) {
        val textView: TextView = itemView.findViewById(R.id.menu_item)

        textView.text = item.item
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, item.icon, 0)

        if (Build.VERSION.SDK_INT <= 27) {
            textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
        }

        textView.setOnClickListener {
            when (item.menuTag) {
                SELECT_IMAGE_MENU_USER, SELECT_IMAGE_MENU_BLOG -> {
                    if (baseItem != null) {
                        Log.d(TAG, "Base item is not null")
                        if (baseItem is CropImageOptions) {
                            Log.d(TAG, "Base item is indeed CropImageOptions")
                            val bundle = Bundle().apply {
                                putParcelable("menu", baseItem as CropImageOptions?)
                            }
                            genericMenuClickListener.onMenuItemClick(item, bundle)
                        } else {
                            Log.d(TAG, baseItem.toString())
                        }
                    } else {
                        Log.d(TAG, "Base item is null")
                        genericMenuClickListener.onMenuItemClick(item, null)
                    }
                /*
                val bundle = Bundle().apply {
                        putInt(ImageCropFragment.ARG_X, 1)
                        putInt(ImageCropFragment.ARG_Y, 1)
                        putInt(ImageCropFragment.ARG_WIDTH, 100)
                        putInt(ImageCropFragment.ARG_HEIGHT, 100)
                        putString(ImageCropFragment.ARG_SHAPE, ImageCropFragment.ARG_SHAPE_OVAL)
                    }
                    genericMenuClickListener.onMenuItemClick(item, bundle)
                */
                }
                /*SELECT_IMAGE_MENU_POST -> {
                    val bundle = Bundle().apply {
                        putInt(ImageCropFragment.ARG_X, 4)
                        putInt(ImageCropFragment.ARG_Y, 3)
                        putInt(ImageCropFragment.ARG_WIDTH, 400)
                        putInt(ImageCropFragment.ARG_HEIGHT, 300)
                        putString(ImageCropFragment.ARG_SHAPE, ImageCropFragment.ARG_SHAPE_RECT)
                    }
                    genericMenuClickListener.onMenuItemClick(item, bundle)
                }*/
                CHAT_MENU -> {
                    val bundle = Bundle().apply {
                        putBoolean(ImageCropFragment.ARG_FREE_MODE, true)
                    }
                    genericMenuClickListener.onMenuItemClick(item, bundle)
                }
                POST_MENU -> {
                    if (baseItem != null) {
                        val bundle = Bundle().apply {
                            putParcelable("menu", baseItem as Post?)
                        }
                        genericMenuClickListener.onMenuItemClick(item, bundle)
                    }
                }
                COMMENT_MENU -> {
                    if (baseItem != null) {
                        val bundle = Bundle().apply {
                            putParcelable("menu", baseItem as SimpleComment?)
                        }
                        genericMenuClickListener.onMenuItemClick(item, bundle)
                    }
                }
                else -> genericMenuClickListener.onMenuItemClick(item, null)
            }
        }
    }

    companion object {

        private const val TAG = "GenericMenuHolder"

        @JvmStatic
        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int, baseItem: Any? = null): GenericMenuViewHolder {
            return GenericMenuViewHolder(parent, layout, baseItem)
        }
    }

}