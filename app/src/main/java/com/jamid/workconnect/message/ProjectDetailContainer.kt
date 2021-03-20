package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.ProjectDetailContainerBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.interfaces.PostsLoadStateListener
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.profile.ProfileFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProjectDetailContainer : Fragment(R.layout.project_detail_container), UserItemClickListener, PostItemClickListener, PostsLoadStateListener {

    lateinit var binding: ProjectDetailContainerBinding
    private lateinit var viewModel: ProjectDetailViewModel
    private val mainViewModel: MainViewModel by activityViewModels()
    private val db = Firebase.firestore
    private lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ProjectDetailContainerBinding.bind(view)
        val activity = requireActivity() as MainActivity
        val navHostFragment = childFragmentManager.findFragmentById(R.id.pdc_frag_container) as NavHostFragment
        navController = navHostFragment.navController

        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL)
        val contributor = arguments?.getParcelable<ChatChannelContributor>(ARG_CURRENT_CONTRIBUTOR)

        viewModel = ViewModelProvider(navController.getViewModelStoreOwner(R.id.project_detail_navigation)).get(ProjectDetailViewModel::class.java)

        viewModel.setCurrentChatChannel(chatChannel)
        viewModel.setCurrentContributor(contributor)

        lifecycleScope.launch {
            delay(150)
            activity.mainBinding.primaryAppBar.hide()
        }

        viewModel.currentPost.observe(viewLifecycleOwner) {
            if (it == null) {
                if (chatChannel != null) {
                    db.collection(POSTS).document(chatChannel.postId).get()
                        .addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                val post = doc.toObject(Post::class.java)
                                viewModel.setCurrentPost(post)
                            }
                        }.addOnFailureListener {
                            Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        binding.pdcAppbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            when (verticalOffset) {
                -1 * appBarLayout.totalScrollRange -> binding.pdcToolbar.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_back_24)
                0 -> binding.pdcToolbar.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_back_white_24)
            }
        })

        binding.pdcAppbarImg.setImageURI(chatChannel?.postImage)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.projectDetailFragment -> {
                    binding.pdcTitle.text = ""
                    binding.pdcAppbar.visibility = View.VISIBLE
                    binding.mediaTabs.visibility = View.GONE
                    binding.pdcToolbar.setNavigationOnClickListener {
                        activity.mainBinding.primaryAppBar.show()
                        val frag = activity.currentBottomFragment
                        frag?.let {
                            activity.currentBottomFragment = null
                            activity.supportFragmentManager.beginTransaction()
                                .remove(it)
                                .commit()
                        }
                    }
                    binding.pdcAppbarImg.visibility = View.VISIBLE
                    (binding.pdcFragContainer.layoutParams as CoordinatorLayout.LayoutParams).behavior = AppBarLayout.ScrollingViewBehavior()
                }
                R.id.projectGuidelinesFragment -> {
                    binding.pdcTitle.text = "Project Guidelines"
                    binding.mediaTabs.visibility = View.GONE
                    binding.pdcAppbarImg.visibility = View.GONE
                    binding.pdcToolbar.setNavigationOnClickListener {
                        hideKeyboard()
                        controller.navigateUp()
                    }
                }
                R.id.projectFragment -> {
                    binding.pdcAppbar.visibility = View.VISIBLE
                    (binding.pdcFragContainer.layoutParams as CoordinatorLayout.LayoutParams).behavior = null
                    binding.pdcAppbarImg.visibility = View.GONE
                    binding.pdcToolbar.setNavigationOnClickListener {
                        controller.navigateUp()
                    }
                }
                R.id.blogFragment -> {
                    binding.pdcAppbar.visibility = View.VISIBLE
                    (binding.pdcFragContainer.layoutParams as CoordinatorLayout.LayoutParams).behavior = null
                    binding.pdcAppbarImg.visibility = View.GONE
                    binding.pdcToolbar.setNavigationOnClickListener {
                        controller.navigateUp()
                    }
                }
                R.id.mediaFragment -> {
                    binding.pdcTitle.text = "Media"
                    binding.pdcAppbarImg.visibility = View.GONE
                    binding.mediaTabs.visibility = View.VISIBLE
                    binding.pdcToolbar.setNavigationOnClickListener {
                        controller.navigateUp()
                    }
                }
                R.id.profileFragment -> {
                    binding.pdcAppbar.visibility = View.GONE
                    (binding.pdcFragContainer.layoutParams as CoordinatorLayout.LayoutParams).behavior = null
                }
            }
        }

        mainViewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
//            val windowHeight = getWindowHeight() + bottom + top

//            val params = binding.root.layoutParams as ViewGroup.LayoutParams
//            params.height = windowHeight
//            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            val params1 = binding.pdcToolbar.layoutParams as CollapsingToolbarLayout.LayoutParams
            params1.setMargins(0, top, 0, 0)
            binding.pdcToolbar.layoutParams = params1

//            binding.root.layoutParams = params

        }

    }

    companion object {
        const val TAG = "ProjectDetailContainer"
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
        const val ARG_CURRENT_CONTRIBUTOR = "ARG_CURRENT_CONTRIBUTOR"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel, currentContributor: ChatChannelContributor) = ProjectDetailContainer().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
                putParcelable(ARG_CURRENT_CONTRIBUTOR, currentContributor)
            }
        }
    }

    override fun onItemClick(post: Post) {
        when (post.type) {
            PROJECT -> {
                val bundle = Bundle().apply {
                    putParcelable(ProjectFragment.ARG_POST, post)
                }
                navController.navigate(R.id.projectFragment, bundle)
            }
            BLOG -> {
                val bundle = Bundle().apply {
                    putParcelable(BlogFragment.ARG_POST, post)
                }
                navController.navigate(R.id.blogFragment, bundle)
            }
        }
    }

    override fun onLikePressed(post: Post, prevL: Boolean, prevD: Boolean) {
        mainViewModel.onLikePressed(post, prevL, prevD)
    }

    override fun onDislikePressed(post: Post, prevL: Boolean, prevD: Boolean) {
        mainViewModel.onDislikePressed(post, prevL, prevD)
    }

    override fun onSavePressed(post: Post, prev: Boolean) {
        mainViewModel.onSavePressed(post, prev)
    }

    override fun onFollowPressed(post: Post, prev: Boolean) {
        mainViewModel.onFollowPressed(post.uid, prev)
    }

    override fun onUserPressed(post: Post) {
    }

    /*override fun onUserPressed(post: Post) {
        if (auth.currentUser?.uid != post.uid) {
            val bundle = Bundle().apply {
                putString(ProfileFragment.ARG_UID, post.uid)
            }

            currentNavController?.value?.navigate(R.id.profileFragment, bundle)
        }
    }*/

    override fun onOptionClick(post: Post) {

    }

    override fun onUserPressed(userId: String) {
        val bundle = Bundle().apply {
            putString(ProfileFragment.ARG_UID, userId)
        }
        navController.navigate(R.id.profileFragment, bundle)
    }

    override fun onInitial() {
    }

    override fun onLoadingMore() {
    }

    override fun onLoaded() {
    }

    override fun onFinished() {
    }

    override fun onError() {
    }
}