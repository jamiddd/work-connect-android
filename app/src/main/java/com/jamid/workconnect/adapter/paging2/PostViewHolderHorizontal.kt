package com.jamid.workconnect.adapter.paging2

import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.BlogItemConverter
import com.jamid.workconnect.model.Post
import java.text.SimpleDateFormat
import java.util.*

class PostViewHolderHorizontal(val view: View): RecyclerView.ViewHolder(view) {

    private val postItemClickListener = view.context as PostItemClickListener
    private val auth = Firebase.auth
    private lateinit var post: Post

    fun bind(p: Post?) {
        if (p != null) {
            post = p
            setStaticContent()
            view.setOnClickListener {
                postItemClickListener.onItemClick(post, this)
            }
        }
    }

    private fun setSaveButton(saveBtn: Button) {
        saveBtn.isSelected = post.postLocalData.isSaved

        saveBtn.setOnClickListener {
            if (auth.currentUser != null) {
                saveBtn.isSelected = !saveBtn.isSelected
                postItemClickListener.onSavePressed(post)
            } else {
                postItemClickListener.onNotSignedIn(post)
            }
        }
    }

    private fun setStaticContent() {
        val colorBlack = ContextCompat.getColor(view.context, R.color.black)
        // different for different types
        if (post.type == PROJECT) {
            val title = view.findViewById<TextView>(R.id.micro_project_title)
            val userImage = view.findViewById<SimpleDraweeView>(R.id.micro_project_user_img)
            val userName = view.findViewById<TextView>(R.id.micro_project_user_name)
            val dateLocationText = view.findViewById<TextView>(R.id.micro_project_time)

            if (Build.VERSION.SDK_INT <= 27) {
                title.setTextColor(colorBlack)
                userName.setTextColor(colorBlack)
            }

            // same for both project and blog
            title.text = post.title
            userName.text = post.admin.name
            userImage.setImageURI(post.admin.photo)

            val projectImage = view.findViewById<SimpleDraweeView>(R.id.micro_project_img)
            projectImage.setImageURI(post.thumbnail)

            val time = SimpleDateFormat("hh:mm a, dd MMM", Locale.UK).format(post.updatedAt)

            val metaText = if (post.location?.place == null) {
                time
            } else {
                "$time • ${post.location!!.place}"
            }

            dateLocationText.text = metaText

        } else {
            val title = view.findViewById<TextView>(R.id.micro_blog_title)
            val userImage = view.findViewById<SimpleDraweeView>(R.id.micro_blog_user_img)
            val meta = view.findViewById<TextView>(R.id.micro_blog_meta)
            val content = view.findViewById<TextView>(R.id.micro_blog_content)
            val blogImage = view.findViewById<SimpleDraweeView>(R.id.micro_blog_image)

            if (Build.VERSION.SDK_INT <= 27) {
                title.setTextColor(colorBlack)
                meta.setTextColor(colorBlack)
                content.setTextColor(colorBlack)
            }

            title.text = post.title
            userImage.setImageURI(post.admin.photo)

            val time = SimpleDateFormat("hh:mm a, dd MMM", Locale.UK).format(post.updatedAt)

            val metaText = post.admin.name + " • " + time
            meta.text = metaText

            var hasImage = false
            var firstImgPos = 0

            val items = post.items

            if (!items.isNullOrEmpty()) {
                post.items = items
                for (i in items.indices) {
                    val blogItem = BlogItemConverter(items[i])

                    if (blogItem.type == IMAGE) {
                        hasImage = true
                        firstImgPos = i
                        break
                    }
                }

                if (hasImage) {
                    blogImage.setImageURI(BlogItemConverter(items[firstImgPos]).content)
                    blogImage.visibility = View.VISIBLE
                } else {
                    blogImage.visibility = View.GONE
                }

                content.text = BlogItemConverter(items[0]).content
            }

            val saveBtn = view.findViewById<Button>(R.id.micro_blog_save_btn)
            setSaveButton(saveBtn)
        }

    }
}