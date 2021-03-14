package com.jamid.workconnect.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.firebase.ui.firestore.paging.LoadingState.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.MiniProjectItemBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.interfaces.PostsLoadStateListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SearchResult
import java.text.SimpleDateFormat
import java.util.*

class PostAdapterAlternative(val viewModel: MainViewModel, val viewLifecycleOwner: LifecycleOwner, val postItemClickListener: PostItemClickListener, val postsLoadStateListener: PostsLoadStateListener, val options: FirestorePagingOptions<SearchResult>) : FirestorePagingAdapter<SearchResult, PostAdapterAlternative.PostViewHolderAlternative>(options){

    private val db = Firebase.firestore

    inner class PostViewHolderAlternative(val binding: MiniProjectItemBinding) : RecyclerView.ViewHolder(binding.root){

        private lateinit var post: Post

        fun bind(searchResult: SearchResult?) {

            if (searchResult != null) {
                val postId = searchResult.id

                db.collection(POSTS).document(postId).get()
                    .addOnSuccessListener {
                        val p = it.toObject(Post::class.java)
                        if (p != null) {
                            post = p

                            binding.root.setOnClickListener {
                                postItemClickListener.onItemClick(post)
                            }

                            binding.projectAdminImg.isClickable = true
                            binding.projectAdminImg.setOnClickListener {
                                postItemClickListener.onUserPressed(post)
                            }

                            binding.projectAdminName.isClickable = true
                            binding.projectAdminName.setOnClickListener {
                                postItemClickListener.onUserPressed(post)
                            }

                            when (post.type) {
                                BLOG -> initBlog()
                                PROJECT -> initProject()
                            }

                            initPost()

                            // LIKE BUTTON
                            setLikeButton()

                            // DISLIKE BUTTON
                            setDislikeButton()

                            // SAVE BUTTON
                            setSaveButton()

                            // FOLLOW BUTTON
                            setFollowButton()

                            viewModel.likesMap.observe(viewLifecycleOwner) { map ->
                                if (map != null && map.containsKey(post.id)) {
                                    val isLiked = map[post.id]!!
                                    binding.projectLikeBtn.isSelected = isLiked
                                }
                            }

                            viewModel.dislikesMap.observe(viewLifecycleOwner) { map ->
                                if (map != null && map.containsKey(post.id)) {
                                    val isDisliked = map[post.id]!!
                                    binding.projectDislikeBtn.isSelected = isDisliked
                                }
                            }

                            viewModel.likesCountMap.observe(viewLifecycleOwner) { map ->
                                if (map != null && map.containsKey(post.id)) {
                                    val count = map[post.id]!!
                                    post.likes = count
                                }
                            }

                            viewModel.dislikesCountMap.observe(viewLifecycleOwner) { map ->
                                if (map != null && map.containsKey(post.id)) {
                                    val count = map[post.id]!!
                                    post.dislikes = count
                                }
                            }

                            viewModel.savesMap.observe(viewLifecycleOwner) { map ->
                                if (map != null && map.containsKey(post.id)) {
                                    val isSaved = map[post.id]!!
                                    binding.projectSaveBtn.isSelected = isSaved
                                }
                            }


                            viewModel.followingsMap.observe(viewLifecycleOwner) { map ->
                                if (map != null && map.containsKey(post.uid)) {
                                    val isFollowing = map[post.uid]!!
                                    Log.d("POST_ADAPTER", isFollowing.toString())
                                    updateFollowButton(isFollowing)
                                }
                            }

                            val name = post.admin["name"] as String

                            binding.projectAdminName.text = "$name •"

                            // OBSERVE FOR SIGN IN CHANGES
                            viewModel.user.observe(viewLifecycleOwner) {
                                if (it != null) {
                                    if (it.id == post.uid) {
                                        binding.projectAdminFollowBtn.visibility = View.GONE
                                        binding.projectAdminName.text = name
                                    } else {
                                        binding.projectAdminFollowBtn.visibility = View.VISIBLE
                                        binding.projectAdminName.text = "$name •"
                                    }


                                    viewModel.updateFollowingsMap(post.uid, it.followings.contains(post.uid))
                                    viewModel.updateLikesMap(post.id, it.likedPosts.contains(post.id))
                                    viewModel.updateDislikesMap(post.id, it.dislikedPosts.contains(post.id))
                                    viewModel.updateSavesMap(post.id, it.savedPosts.contains(post.id))
                                    viewModel.updateLikesCountMap(post.id, post.likes)
                                    viewModel.updateDislikesCountMap(post.id, post.dislikes)
                                } else {
                                    binding.projectAdminName.text = "$name •"
                                }
                            }

                            binding.projectOptionsBtn.setOnClickListener {
                                postItemClickListener.onOptionClick(post)
                            }
                        }

                    }.addOnFailureListener {
                        Toast.makeText(
                            binding.root.context,
                            "Something went wrong !!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        private fun initPost() {
            // SET CONSTANT DATA

            val photo = post.admin["photo"] as String?

            binding.projectTitle.text = post.title
            binding.projectAdminImg.setImageURI(photo)

            val time = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt)
            val location = post.location?.place

            val metaText = if (location == null) {
                time
            } else {
                "$time • $location"
            }

            binding.projectMetaText.text = metaText
        }

        private fun initProject() {
            // CHANGE VISIBILITY OF IMAGE ACCORDINGLY
            // SET CONTENT HERE
            if (post.thumbnail != null) {
                binding.projectThumb.visibility = View.VISIBLE
            } else {
                binding.projectThumb.visibility = View.GONE
            }
            binding.projectThumb.setImageURI(post.thumbnail)
            binding.projectMiniContent.text = post.content
        }

        private fun initBlog() {
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

            if (hasImage) {
                binding.projectBlogImage.setImageURI(items[firstImgPos].content)
                binding.projectBlogImage.visibility = View.VISIBLE
                binding.projectMiniContent.minHeight = convertDpToPx(80, binding.root.context)
            } else {
                binding.projectBlogImage.visibility = View.GONE
            }

            // FIND AND SET BLOG CONTENT
            binding.projectMiniContent.text = post.items?.get(0)?.content
        }

        private fun setLikeButton() {
            // set like button actions
            binding.projectLikeBtn.setOnClickListener {
                if (binding.projectLikeBtn.isSelected) {
                    post.likes = post.likes - 1
                    postItemClickListener.onLikePressed(post, prevL = true, prevD = false)
                } else {
                    post.likes = post.likes + 1
                    if (binding.projectDislikeBtn.isSelected) {
                        binding.projectDislikeBtn.isSelected = false
                        post.dislikes = post.dislikes - 1
                        postItemClickListener.onLikePressed(post, prevL = false, prevD = true)
                    } else {
                        postItemClickListener.onLikePressed(post, prevL = false, prevD = false)
                    }
                }
//                binding.projectLikeBtn.isSelected = !binding.projectLikeBtn.isSelected
            }
        }

        private fun setDislikeButton() {
            // set dislike button actions
            binding.projectDislikeBtn.setOnClickListener {
                if (binding.projectDislikeBtn.isSelected) {
                    post.dislikes = post.dislikes - 1
                    postItemClickListener.onDislikePressed(post, prevL = true, prevD = false)
                } else {
                    post.dislikes = post.dislikes + 1
                    if (binding.projectLikeBtn.isSelected) {
                        binding.projectLikeBtn.isSelected = false
                        post.likes = post.likes - 1
                        postItemClickListener.onDislikePressed(post, prevL = true, prevD = false)
                    } else {
                        postItemClickListener.onDislikePressed(post, prevL = false, prevD = false)
                    }
                }
//                binding.projectDislikeBtn.isSelected = !binding.projectDislikeBtn.isSelected
            }
        }

        private fun setSaveButton() {
            // save button actions
            binding.projectSaveBtn.setOnClickListener {
                if (binding.projectSaveBtn.isSelected) {
                    postItemClickListener.onSavePressed(post, true)
                } else {
                    postItemClickListener.onSavePressed(post, false)
                }
//                binding.projectSaveBtn.isSelected = !binding.projectSaveBtn.isSelected
            }
        }

        private fun updateFollowButton(state: Boolean) {
            binding.projectAdminFollowBtn.isSelected = state
            if (state) {
                binding.projectAdminFollowBtn.text = binding.root.resources.getString(R.string.unfollow_text)
            } else {
                binding.projectAdminFollowBtn.text = binding.root.resources.getString(R.string.follow_text)
            }
        }

        private fun setFollowButton() {
            binding.projectAdminFollowBtn.text = binding.root.resources.getString(R.string.follow_text)

            // set follow button actions
            binding.projectAdminFollowBtn.setOnClickListener {
                if (binding.projectAdminFollowBtn.isSelected) {
                    postItemClickListener.onFollowPressed(post, true)
                } else {
                    postItemClickListener.onFollowPressed(post, false)
                }
//                binding.projectAdminFollowBtn.isSelected = !binding.projectAdminFollowBtn.isSelected
            }
        }
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        when (state) {
            LOADING_INITIAL -> postsLoadStateListener.onInitial()
            LOADING_MORE -> postsLoadStateListener.onLoadingMore()
            LOADED -> postsLoadStateListener.onLoaded()
            FINISHED -> postsLoadStateListener.onFinished()
            ERROR -> postsLoadStateListener.onError()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolderAlternative {
        val binding = DataBindingUtil.inflate<MiniProjectItemBinding>(LayoutInflater.from(parent.context), R.layout.mini_project_item, parent, false)
        return PostViewHolderAlternative(binding)
    }

    override fun onBindViewHolder(
        holder: PostViewHolderAlternative,
        position: Int,
        model: SearchResult
    ) {
        holder.bind(getItem(position)?.toObject(model::class.java))
    }

}