package com.jamid.workconnect

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.adapter.ContributorAdapter
import com.jamid.workconnect.databinding.FragmentProjectBinding
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.message.ProjectDetailContainer
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.profile.ProfileFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.text.SimpleDateFormat
import java.util.*


class ProjectFragment : BasePostFragment(R.layout.fragment_project), UserItemClickListener {

    private lateinit var binding: FragmentProjectBinding
    private lateinit var contributorAdapter: ContributorAdapter
    private val db = Firebase.firestore

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.project_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh_project -> {
                db.collection("posts").document(post.id).get()
                    .addOnSuccessListener {
                        val p = it.toObject(Post::class.java)!!
                        post = p
                    }.addOnFailureListener {
                        viewModel.setCurrentError(it)
                    }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("ShowToast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectBinding.bind(view)

        val postId = arguments?.getString(ARG_POST_ID)
        val postObj = arguments?.getParcelable<Post>(ARG_POST)

        hideKeyboard()

        postObj?.let {
            setMetadata(it, binding.projectMetadata, binding.projectBottomBlur, binding.projectScrollContainer)
        }

        if (postId != null) {
            db.collection(POSTS).document(postId).get()
                .addOnSuccessListener {
                    if (it != null && it.exists()) {
                        val p = it.toObject(Post::class.java)!!
                        setMetadata(p, binding.projectMetadata, binding.projectBottomBlur, binding.projectScrollContainer)
                        initEverything()
                    }
                }.addOnFailureListener {

                }
        } else {
            initEverything()
        }

        /*requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }*/



       /* binding.projectFragmentAppbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            when (verticalOffset) {
                -1 * appBarLayout.totalScrollRange -> binding.projectFragmentToolbar.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_back_24)
                0 -> binding.projectFragmentToolbar.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_back_white_24)
            }
        })*/

        /*binding.projectScrollContainer.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val bigTitleHeight = binding.projectContent.projectTitle.measuredHeight
            val smallTitleHeight = binding.smallTitle.measuredHeight

            if (scrollY <= 170) {
                val factor: Float = smallTitleHeight.toFloat() / bigTitleHeight
                val translation = smallTitleHeight.toFloat() - (scrollY * factor)
                if (translation >= 0 ) {
                    binding.smallTitle.translationY = translation
                }
            } else if (scrollY > 170) {
                if (binding.smallTitle.translationY != 0f) {
                    binding.smallTitle.translationY = 0f
                }
            }
        }*/

        OverScrollDecoratorHelper.setUpOverScroll(binding.projectScrollContainer)

        viewModel.requestSentResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    binding.projectMetadata.postMetaJoinBtn.isEnabled = false
                    Snackbar.make(binding.root, "Sent request to join project!", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.projectData)
                        .show()
                }
                is Result.Error -> {
                    binding.projectMetadata.postMetaJoinBtn.isEnabled = true
                    Toast.makeText(requireContext(), "Something went wrong !", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }

    private fun initEverything() {

        if (auth.currentUser != null && auth.currentUser!!.uid != post.uid) {
            if (post.contributors?.contains(auth.currentUser!!.uid) == true) {
                binding.projectMetadata.postMetaJoinBtn.visibility = View.GONE
            } else {
                val db = Firebase.firestore
                db.collection("posts").document(post.id)
                    .collection("requests")
                    .whereEqualTo("sender", auth.currentUser!!.uid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { requestSnapshots ->
                        binding.projectMetadata.postMetaJoinBtn.isEnabled = requestSnapshots.isEmpty
                    }.addOnFailureListener {
                        viewModel.setCurrentError(it)
                    }
            }
        } else {
            if (auth.currentUser?.uid == post.uid) {
                binding.projectMetadata.postMetaJoinBtn.visibility = View.GONE
            } else {
                binding.projectMetadata.postMetaJoinBtn.isEnabled = true
            }
        }

        binding.projectContent.projectImg.setImageURI(post.thumbnail)

        val name = post.admin["name"] as String
        val photo = post.admin["photo"] as String?

        binding.projectMetadata.apply {
            adminPhoto.setImageURI(photo)
            authorName.text = "$name •"

            postMetaJoinBtn.setOnClickListener {
                if (auth.currentUser != null) {
                    postMetaJoinBtn.isEnabled = false
                    viewModel.joinProject(post)
                } else {
                    findNavController().navigate(R.id.signInFragment)
                }
            }
        }

        binding.projectContent.apply {
            projectTitle.text = post.title
            projectContent.text = post.content + "\n" + post.content
            if (post.location == null) {
                projectMetaText.text = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt)
            } else {
                projectMetaText.text = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt) + " • " + post.location!!.place
            }

            val tags = post.tags
            if (!tags.isNullOrEmpty()) {
                projectTagHeader.visibility = View.VISIBLE
                projectTagsList.visibility = View.VISIBLE

                tags.forEach {
                    addNewChip(it)
                }
            } else {
                projectTagHeader.visibility = View.GONE
                projectTagsList.visibility = View.GONE
            }

            val parent = activity.currentBottomFragment
            contributorAdapter = if (parent is ProjectDetailContainer) {
                ContributorAdapter(parent)
            } else {
                ContributorAdapter(activity)
            }

            val contributors = post.contributors
            if (!contributors.isNullOrEmpty()) {
                projectContributorsHeader.visibility = View.VISIBLE
                projectContributorsList.visibility = View.VISIBLE

                projectContributorsList.apply {
                    layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    adapter = contributorAdapter
                }

                contributorAdapter.submitList(contributors)
            } else {
                projectContributorsHeader.visibility = View.GONE
                projectContributorsList.visibility = View.GONE
            }
        }

        // OBSERVE FOR SIGN IN CHANGES
        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.id == post.uid) {
                    binding.projectMetadata.adminFollowBtn.visibility = View.GONE
                    binding.projectMetadata.authorName.text = name
                } else {
                    binding.projectMetadata.adminFollowBtn.visibility = View.VISIBLE
                    binding.projectMetadata.authorName.text = "$name •"

//                    viewModel.updateFollowingsMap(blog.uid, it.followings.contains(blog.uid))
                }
            }
        }

        val bundle = Bundle().apply {
            putString(ProfileFragment.ARG_UID, post.uid)
        }

        binding.projectMetadata.authorName.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, bundle)
        }

        binding.projectMetadata.adminPhoto.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, bundle)
        }
    }


    fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    private fun addNewChip(s: String) {
        val chip = Chip(requireContext())
        chip.text = s
        binding.projectContent.projectTagsList.addView(chip)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearProjectFragmentResults()
    }

    companion object {

        const val TAG = "ProjectFragment"
        const val ARG_POST = "ARG_POST"
        const val ARG_POST_ID = "ARG_POST_ID"

        @JvmStatic
        fun newInstance(id: String? = null, post: Post? = null) = ProjectFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_POST, post)
                putString(ARG_POST_ID, id)
            }
        }
    }

    override fun onUserPressed(userId: String) {
        /*if (auth.currentUser?.uid != userId) {
            val fragment = UserFragment.newInstance(userId)
            requireActivity().supportFragmentManager.beginTransaction().add(android.R.id.content, fragment, UserFragment.TAG)
                .addToBackStack(UserFragment.TAG)
                .commit()
        } else {
            Toast.makeText(requireContext(), "Not implemented yet", Toast.LENGTH_SHORT).show()
        }*/
    }
}