package com.jamid.workconnect.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSavedPostsBinding
import com.jamid.workconnect.databinding.MiniProjectItemBinding
import com.jamid.workconnect.databinding.PagingEndLayoutBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.interfaces.PostMenuClickListener
import com.jamid.workconnect.model.Post
import java.text.SimpleDateFormat
import java.util.*

class SavedPostsFragment : Fragment(), PostItemClickListener, PostMenuClickListener {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var postItemClickListener: PostItemClickListener
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private var end = 10
    private lateinit var savedPostsBinding: FragmentSavedPostsBinding
    private lateinit var savedPostsAdapter: SavedPostsAdapter
    private lateinit var activity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        savedPostsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_saved_posts, container, false)
        // Inflate the layout for this fragment
        return savedPostsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity() as MainActivity

        postItemClickListener = this
        savedPostsBinding.savedProjectsProgress.visibility = View.VISIBLE
        savedPostsAdapter = SavedPostsAdapter()

//        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.primaryToolBar)
//        toolbar.setNavigationOnClickListener {
//            hideKeyboard()
//            findNavController().navigateUp()
//        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {

                if (it.savedPosts.isEmpty()) {
                    savedPostsBinding.savedProjectsProgress.visibility = View.GONE
                    savedPostsBinding.noSavedProjects.visibility = View.VISIBLE
                    return@observe
                }


                val savedPosts = mutableListOf<String>()
                val s = if (it.savedPosts.size >= end) {
                    savedPosts.addAll(it.savedPosts.subList(0, end))
                    end += 10
                    savedPosts.add(DUMMY)
                    savedPosts
                } else {
                    it.savedPosts
                }

                savedPostsBinding.saveProjectsRecycler.apply {
                    adapter = savedPostsAdapter
                    layoutManager = LinearLayoutManager(requireContext())
                }

                savedPostsAdapter.submitList(s)
            }
        }

        savedPostsBinding.saveProjectsRecycler.addOnChildAttachStateChangeListener(object: RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                savedPostsBinding.savedProjectsProgress.visibility = View.GONE
            }

            override fun onChildViewDetachedFromWindow(view: View) {}
        })
    }

    private inner class SavedPostsAdapter() : ListAdapter<String, RecyclerView.ViewHolder>(StringComparator()){

        inner class SavedPostsVH(val binding: MiniProjectItemBinding) : RecyclerView.ViewHolder(binding.root){
            private lateinit var post: Post

            fun bind(postId: String) {

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
                            requireContext(),
                            "Something went wrong !!",
                            Toast.LENGTH_SHORT
                        ).show()
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

        inner class LoadNextVH(val binding: PagingEndLayoutBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind() {
                binding.seeMoreBtn.setOnClickListener {
                    binding.seeMoreBtn.visibility = View.INVISIBLE
                    binding.seeMoreProgress.visibility = View.VISIBLE
                    val user = viewModel.user.value
                    if (user != null) {
                        val list = mutableListOf<String>()
                        if (user.savedPosts.size <= end) {
                            savedPostsAdapter.submitList(user.savedPosts)
                        } else {
                            list.addAll(user.savedPosts.subList(0, end))
                            list.add(DUMMY)
                            savedPostsAdapter.submitList(list)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                val binding = DataBindingUtil.inflate<PagingEndLayoutBinding>(LayoutInflater.from(parent.context), R.layout.paging_end_layout, parent, false)
                LoadNextVH(binding)
            } else {
                val binding = DataBindingUtil.inflate<MiniProjectItemBinding>(LayoutInflater.from(parent.context), R.layout.mini_project_item, parent, false)
                SavedPostsVH(binding)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (getItem(position) == DUMMY) 1 else 0
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = getItem(position)
            if (item == DUMMY) {
                (holder as LoadNextVH).bind()
            } else {
                (holder as SavedPostsVH).bind(item)
            }
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() = SavedPostsFragment()
    }

    override fun onItemClick(post: Post) {
        val bundle = Bundle().apply {
            putParcelable("post", post)
        }

        when (post.type) {
            PROJECT -> {
                findNavController().navigate(R.id.projectFragment, bundle)
            }
            BLOG -> {
                findNavController().navigate(R.id.blogFragment, bundle)
            }
        }
    }

    override fun onLikePressed(post: Post, prevL: Boolean, prevD: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onLikePressed(post, prevL, prevD)
        } else {
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onDislikePressed(post: Post, prevL: Boolean, prevD: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onDislikePressed(post, prevL, prevD)
        } else {
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onSavePressed(post: Post, prev: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onSavePressed(post, prev)
        } else {
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onFollowPressed(post: Post, prev: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onFollowPressed(post.uid, prev)
        } else {
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onUserPressed(post: Post) {
        if (auth.currentUser?.uid != post.uid) {
            val bundle = Bundle().apply {
                putString("userId", post.uid)
            }
            findNavController().navigate(R.id.userFragment, bundle)
        } else {
            Toast.makeText(requireContext(), "Not implemented", Toast.LENGTH_SHORT).show()
            /*val bottomNavigationView: BottomNavigationView = requireActivity().findViewById(R.id.bottomNav)*/
        }
    }

    override fun onOptionClick(post: Post) {
        activity.invokePostMenu(post)
    }

    override fun onCollaborateClick(post: Post) {
        if (auth.currentUser != null) {
            TODO("Show UI changes")
            viewModel.joinProject(post)
        } else {
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onShareClick(post: Post) {

    }

    override fun onDeleteClick(post: Post) {
        TODO("Not implemented yet")
        if (auth.currentUser != null) {
            viewModel.deletePost(post)
        } else {
            findNavController().navigate(R.id.signInFragment)
        }
    }

    override fun onReportClick(post: Post) {

    }

}