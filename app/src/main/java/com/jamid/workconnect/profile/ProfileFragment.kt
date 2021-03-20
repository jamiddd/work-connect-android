package com.jamid.workconnect.profile

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentProfileBinding
import com.jamid.workconnect.model.User

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentProfileBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var uid: String? = null
    private var data: User? = null
    private lateinit var activity: MainActivity
    private var hasChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)
/*
leftMenu = requireActivity().findViewById(R.id.leftMenu)
inflater.inflate(R.menu.profile_left_menu, leftMenu.menu)
leftMenu.setOnMenuItemClickListener {
when (it.itemId) {
R.id.notification_item -> {
findNavController().navigate(R.id.notificationFragment)
}
}
true
}
*/
    }

    private fun setFollowButton(state: Boolean) {
        binding.editBtn.isSelected = state
        if (state) {
            binding.editBtn.text = getString(R.string.unfollow_text)
        } else {
            binding.editBtn.text = getString(R.string.follow_text)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.saved_posts_item -> {
                findNavController().navigate(R.id.savedPostsFragment)
                true
            }
            R.id.sign_out -> {
                viewModel.signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)
        activity = requireActivity() as MainActivity

        hideKeyboard()

        uid = arguments?.getString(ARG_UID) ?: auth.currentUser?.uid
        data = arguments?.getParcelable(ARG_USER) ?: viewModel.user.value


        uid?.let {
            db.collection(USERS).document(it).addSnapshotListener { ds, error ->
                if (error != null) {
                    viewModel.setCurrentError(error)
                }

                if (ds != null && ds.exists()) {
                    val user = ds.toObject(User::class.java)!!
                    setUser(user, activity)
                }
            }
        }

        data?.let {
            setUser(it, activity)
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.profileToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setUser(user: User, activity: MainActivity) {
        binding.profilePager.adapter = ProfileFragmentPager(user, activity)

        binding.profileUsername.text = "@${user.username}"

        (binding.profilePager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        TabLayoutMediator(binding.profileTabs, binding.profilePager){ s, w ->
            when (w) {
                0 -> s.text = PROJECTS
                1 -> s.text = COLLABORATIONS
                2 -> s.text = BLOGS
            }
        }.attach()
        binding.apply {
            profileImg.setImageURI(user.photo)
            profileEmail.text = user.email
            profileName.text = user.name

            if (user.about != null) {
                profileAbout.visibility = View.VISIBLE
            } else {
                profileAbout.visibility = View.GONE
            }
            profileAbout.text = user.about

            setUserMetaText(user)

            userMeta.movementMethod = LinkMovementMethod.getInstance()
            userMeta.highlightColor = Color.TRANSPARENT

        }

        if (uid != auth.currentUser?.uid) {
            binding.editBtn.text = "Follow"
            binding.editBtn.setOnClickListener {
                hasChanged = true
                viewModel.onFollowPressed(user.id, binding.editBtn.isSelected)
            }

            viewModel.followingsMap.observe(viewLifecycleOwner) { map ->
                if (map != null && map.containsKey(user.id)) {
                    val isFollowed = map[user.id]!!
                    setFollowButton(isFollowed)
                    if(hasChanged) {
                        Log.d("UserFragment", isFollowed.toString())
                        val newList = if (isFollowed) {
                            val t = mutableListOf<String>().also { list ->
                                list.addAll(user.followers)
                                list.add(auth.currentUser!!.uid)
                            }
                            t
                        } else {
                            val s = mutableListOf<String>().also { list ->
                                list.addAll(user.followers)
                                list.remove(auth.currentUser!!.uid)
                            }
                            s
                        }
                        user.followers = newList
                        setUserMetaText(user)
                    }
                }
            }
        } else {
            binding.editBtn.setOnClickListener {
                findNavController().navigate(R.id.editFragment)
            }
        }



    }

    private fun setUserMetaText(user: User) {
          val projectIds = mutableListOf<String>()
          val blogIds = mutableListOf<String>()
          projectIds.addAll(user.projectIds)
          blogIds.addAll(user.blogIds)

          val followersCount = user.followers.size
          val followingsCount = user.followings.size
          val projectsCount = projectIds.size
          val blogsCount = blogIds.size

          val sample = "$followersCount Followers • $followingsCount Following • $projectsCount Projects • $blogsCount Blogs"
          val sp = SpannableString(sample)
          sp.setSpan(StyleSpan(Typeface.BOLD), 0, followersCount.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
          sp.setSpan(StyleSpan(Typeface.BOLD), followersCount.toString().length + 13, followersCount.toString().length + 13 + followingsCount.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
          sp.setSpan(StyleSpan(Typeface.BOLD), followersCount.toString().length + 26 + followingsCount.toString().length, followersCount.toString().length + 26 + followingsCount.toString().length + projectsCount.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
          val blogStart = followersCount.toString().length + 26 + followingsCount.toString().length + projectsCount.toString().length + 12
          val blogEnd = followersCount.toString().length + 26 + followingsCount.toString().length + projectsCount.toString().length + 12 + blogsCount.toString().length
          sp.setSpan(StyleSpan(Typeface.BOLD), blogStart, blogEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

          val bundle = Bundle().apply {
              putParcelable("user", user)
          }

          val clickableSpan = object: ClickableSpan() {
              override fun onClick(widget: View) {
                  findNavController().navigate(R.id.followingsFragment, bundle)
              }

              override fun updateDrawState(ds: TextPaint) {
                  super.updateDrawState(ds)
                  ds.isUnderlineText = false
              }
          }

          val clickableSpan1 = object: ClickableSpan() {
              override fun onClick(widget: View) {
                  val fragment = FollowersFragment.newInstance(user)
                  activity.showBottomSheet(fragment, "FollowersFragment")
              }

              override fun updateDrawState(ds: TextPaint) {
                  super.updateDrawState(ds)
                  ds.isUnderlineText = false
              }
          }

        sp.setSpan(clickableSpan1, 0, followersCount.toString().length + 13, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(clickableSpan, followersCount.toString().length + 13, followersCount.toString().length + 24, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.userMeta.text = sp
        binding.userMeta.movementMethod = LinkMovementMethod.getInstance()
        binding.userMeta.highlightColor = Color.TRANSPARENT
    }

    companion object {

        const val TAG = "ProfileFragment"
        const val ARG_UID = "ARG_UID"
        const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(uid: String? = null, user: User? = null) = ProfileFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_UID, uid)
                putParcelable(ARG_USER, user)
            }
        }
    }
}