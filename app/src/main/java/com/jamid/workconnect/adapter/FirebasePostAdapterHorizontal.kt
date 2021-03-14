package com.jamid.workconnect.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.jamid.workconnect.*
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.Post

class FirebasePostAdapterHorizontal(
    options: FirestorePagingOptions<Post>,
    val viewModel: MainViewModel,
    val lifecycleOwner: LifecycleOwner,
    val postItemClickListener: PostItemClickListener,
): FirestorePagingAdapter<Post, FirebasePostAdapterHorizontal.FirebasePostHorizontalHolder>(options) {

    inner class FirebasePostHorizontalHolder(val view: View): RecyclerView.ViewHolder(view) {
        private lateinit var post: Post

        fun bind(postItem: Post?) {
            if (postItem != null) {
                post = postItem

                view.setOnClickListener {
                    postItemClickListener.onItemClick(post)
                }

                if (post.type == PROJECT) {
                    initProject()
                } else {
                    initBlog()
                }

//                initPost()

            }
        }

        private fun initBlog() {

            val blogUserImg = view.findViewById<SimpleDraweeView>(R.id.micro_blog_user_img)
            val blogUserName = view.findViewById<TextView>(R.id.micro_blog_meta)
            val title = view.findViewById<TextView>(R.id.micro_blog_title)
            title.text = post.title

            val photo = post.admin["photo"] as String?

            val name = post.admin["name"] as String?
            blogUserName.text = "$name"/* •*/


            blogUserImg.setImageURI(photo)

            blogUserImg.isClickable = true
            blogUserImg.setOnClickListener {
                postItemClickListener.onUserPressed(post)
            }

            blogUserName.isClickable = true
            blogUserName.setOnClickListener {
                postItemClickListener.onUserPressed(post)
            }

            val items = post.items ?: return

            var hasImage = false
            var firstImgPos = 0

            for (i in items.indices) {
                if (items[i].type == IMAGE) {
                    hasImage = true
                    firstImgPos = i
                    break
                }
            }

            val saveBtn = view.findViewById<Button>(R.id.micro_blog_save_btn)

            setSaveButton(saveBtn)

            val blogImage = view.findViewById<SimpleDraweeView>(R.id.micro_blog_image)

            if (hasImage) {
                blogImage.setImageURI(items[firstImgPos].content)
                blogImage.visibility = View.VISIBLE
            } else {
                blogImage.visibility = View.GONE
            }

            val content = view.findViewById<TextView>(R.id.micro_blog_content)
            // FIND AND SET BLOG CONTENT
            content.text = post.items?.get(0)?.content

            viewModel.user.observe(lifecycleOwner) {
                if (it != null) {
                    viewModel.updateSavesMap(post.id, it.savedPosts.contains(post.id))
                }
            }


            viewModel.savesMap.observe(lifecycleOwner) { map ->
                if (map != null && map.containsKey(post.id)) {
                    val isSaved = map[post.id]!!
                    saveBtn.isSelected = isSaved
                }
            }
        }

        private fun setSaveButton(v: Button) {
            // save button actions
            v.setOnClickListener {
                if (v.isSelected) {
                    postItemClickListener.onSavePressed(post, true)
                } else {
                    postItemClickListener.onSavePressed(post, false)
                }
            }
        }


        /*private fun initPost() {
            // SET CONSTANT DATA

            val time = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt)
            val location = post.location?.place

            val metaText = if (location == null) {
                time
            } else {
                "$time • $location"
            }
        }*/

        private fun initProject() {
            val projectUserImg = view.findViewById<SimpleDraweeView>(R.id.micro_project_user_img)
            val projectUserName = view.findViewById<TextView>(R.id.micro_project_user_name)

            val photo = post.admin["photo"] as String?

            projectUserImg.setImageURI(photo)

            projectUserImg.isClickable = true
            projectUserImg.setOnClickListener {
                postItemClickListener.onUserPressed(post)
            }

            projectUserName.isClickable = true
            projectUserName.setOnClickListener {
                postItemClickListener.onUserPressed(post)
            }

            val projectImg = view.findViewById<SimpleDraweeView>(R.id.micro_project_img)
            projectImg.setImageURI(post.thumbnail)

            val name = post.admin["name"] as String?
            projectUserName.text = "$name"/* •*/

            val title = view.findViewById<TextView>(R.id.micro_project_title)
            title.text = post.title

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FirebasePostHorizontalHolder {
        return if (viewType == 0) {
            FirebasePostHorizontalHolder(LayoutInflater.from(parent.context).inflate(R.layout.micro_project_item, parent, false))
        } else {
            FirebasePostHorizontalHolder(LayoutInflater.from(parent.context).inflate(R.layout.micro_blog_item, parent, false))
        }
    }

    override fun onBindViewHolder(
        holder: FirebasePostHorizontalHolder,
        position: Int,
        model: Post
    ) {
        holder.bind(getItem(position)?.toObject(model::class.java))
    }

    override fun getItemViewType(position: Int): Int {
        val currentItem = getItem(position)?.toObject(Post::class.java) ?: return 0

        return when (currentItem.type) {
            PROJECT -> 0
            BLOG -> 1
            else -> super.getItemViewType(position)
        }
    }

}