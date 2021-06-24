package com.jamid.workconnect.profile

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentProfileBinding
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserPrivate
import com.jamid.workconnect.settings.SettingsFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ProfileFragment : SupportFragment(R.layout.fragment_profile, TAG, false) {

    private lateinit var binding: FragmentProfileBinding
    private var otherUserLive = MutableLiveData<User>().apply { value = null}

    // TODO("A very weird case that shouldn't happen but happens.")
    private fun isContradiction(isUserFollowed: Boolean, otherUser: User, currentUser: User) : Boolean {
        return !(otherUser.userPrivate.followers.contains(currentUser.id) && isUserFollowed)
    }


    private fun setFollowButton(btn: Button, currentUser: User?, otherUser: User) {
        if (currentUser != null && context != null) {
            val isSameUser = currentUser.id == otherUser.id
            val isUserFollowed = currentUser.userPrivate.followings.contains(otherUser.id)

            if (isContradiction(isUserFollowed, otherUser, currentUser)) {
                val existingList = otherUser.userPrivate.followers.toMutableList()
                existingList.remove(currentUser.id)
                otherUser.userPrivate.followers = existingList
                otherUserLive.postValue(otherUser)
            }

            val options = navOptions {
                anim {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popEnter = R.anim.slide_in_left
                    popExit = R.anim.slide_out_right
                }
            }

            if (isSameUser) {
                btn.text = getString(R.string.edit)

                btn.setOnClickListener {
                    /*val fragment = EditFragment.newInstance()
                    activity.toFragment(fragment, EditFragment.TAG)*/
                    findNavController().navigate(R.id.editFragment, null, options)
                }
            } else {
                fun setFollowText(isUserFollowed: Boolean) {
                    if (isUserFollowed) {
                        btn.text = activity.getString(R.string.unfollow_text)
                    } else {
                        btn.text = activity.getString(R.string.follow_text)
                    }
                }

                setFollowText(isUserFollowed)

                btn.setOnClickListener {
                    val existingList = otherUser.userPrivate.followers.toMutableList()
                    if (isUserFollowed) {
                        existingList.remove(currentUser.id)
                    } else {
                        existingList.add(currentUser.id)
                    }
                    otherUser.userPrivate.followers = existingList

                    // set changes
                    otherUserLive.postValue(otherUser)

                    setFollowText(!isUserFollowed)
                    viewModel.onFollowPressed(currentUser, otherUser)
                }
            }
        } else {
            btn.text = activity.getString(R.string.follow_text)
            btn.setOnClickListener {
                activity.showSignInDialog(FOLLOW)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        hideKeyboard()

        binding.profileFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val argUid = arguments?.getString(ARG_UID)
        val argOtherUser = arguments?.getParcelable<User>(ARG_USER)

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.profileFragmentToolbar.updateLayout(marginTop = top)
            binding.profileUserSection.updateLayout(marginTop = top)
        }

        OverScrollDecoratorHelper.setUpOverScroll((binding.profilePager[0] as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        argUid?.let {
            activity.setFragmentTitle("")
            viewModel.getObject(it, User::class.java) { user ->
                if (user != null) {
                    setPrefetchedContent(user)
                    setUser(user, activity)
                }
            }
        }

        argOtherUser?.let {

            setPrefetchedContent(it)

            if (it.id == Firebase.auth.currentUser?.uid) {
                viewModel.user.observe(viewLifecycleOwner) { user ->
                    if (user != null) {
                        setUser(user, activity)
                    }
                }
            } else {
                Firebase.firestore.collection(USERS)
                    .document(it.id)
                    .collection("private")
                    .document(it.id)
                    .get()
                    .addOnSuccessListener { doc->
                        val userPrivate = doc.toObject(UserPrivate::class.java)
                        it.userPrivate = userPrivate!!
                        setUser(it, activity)
                    }.addOnFailureListener {
                        //
                    }
            }
        }

    }

    private fun setPrefetchedContent(otherUser: User) {
        binding.apply {
            profileImg.setImageURI(otherUser.photo)
            profileEmail.text = otherUser.email
            profileName.text = otherUser.name

            if (otherUser.about != null) {
                profileAbout.visibility = View.VISIBLE
            } else {
                profileAbout.visibility = View.GONE
            }
            profileAbout.text = otherUser.about
            userMeta.movementMethod = LinkMovementMethod.getInstance()
            userMeta.highlightColor = Color.TRANSPARENT


            if (Build.VERSION.SDK_INT <= 27) {
                val colorBlack = ContextCompat.getColor(activity, R.color.black)
                profileName.setTextColor(colorBlack)
                profileEmail.setTextColor(colorBlack)
                profileAbout.setTextColor(colorBlack)
                userMeta.setTextColor(colorBlack)
            }

        }

    }

    private fun setUser(otherUser: User, activity: MainActivity) {

        binding.profileFragmentToolbar.title = "@${otherUser.username}"

        otherUserLive.postValue(otherUser)

        // TODO("There is a problem with this observer .. ")
        viewModel.user.observe(activity) { currentUser ->
            Log.d(BUG_TAG, "The user observer in profile fragment is alive.")
            setFollowButton(binding.editBtn, currentUser, otherUser)
            if (currentUser != null) {

                if (otherUser.id == currentUser.id) {

                    activity.mainBinding.bottomCard.hide()
                    activity.mainBinding.bottomNavBackground.visibility = View.VISIBLE

                    binding.profileMenuBtn.setOnClickListener {
                        val _tag = PROFILE_MENU
                        val item1 = GenericMenuItem(_tag, "Edit profile", R.drawable.ic_baseline_edit_24, 0)
                        val item2 = GenericMenuItem(_tag, "Settings", R.drawable.ic_baseline_architecture_24, 1)
                        val item3 = GenericMenuItem(_tag, "Saved Posts", R.drawable.ic_round_notes_24, 2)
                        val fragment = GenericMenuFragment.newInstance(_tag, "@" + otherUser.username, arrayListOf(item1, item2, item3))
                        activity.showBottomSheet(fragment, _tag)
                    }
                } else {

                    activity.mainBinding.bottomCard.show()
                    activity.mainBinding.bottomNavBackground.visibility = View.GONE

                    binding.profileMenuBtn.setOnClickListener {
                        val _tag = OTHER_PROFILE_MENU
                        val item1 = GenericMenuItem(_tag, "Block", 0, 0)
                        val item2 = GenericMenuItem(_tag, "Share", R.drawable.ic_baseline_share_24, 1)
                        val item3 = GenericMenuItem(_tag, "Unfollow @${otherUser.username}", R.drawable.ic_baseline_person_add_disabled_24, 2)
                        val item4 = GenericMenuItem(_tag, "Report", R.drawable.ic_baseline_report_24, 3)
                        val fragment = GenericMenuFragment.newInstance(_tag, "@" + otherUser.username, arrayListOf(item1, item2, item3, item4))
                        activity.showBottomSheet(fragment, _tag)
                    }
                }

                /*activity.mainBinding.primaryMenuBtn.setOnClickListener {
                    val popupMenu = PopupMenu(activity, it, Gravity.END)
                    if (currentUser.id == otherUser.id) {
                        popupMenu.inflate(R.menu.profile_menu)

                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.saved_posts_item -> {
                                    activity.toFragment(SavedPostsFragment.newInstance(), SavedPostsFragment.TAG)
                                    true
                                }

                                R.id.user_settings -> {
                                    activity.toFragment(SettingsFragment.newInstance(), SettingsFragment.TAG)
                                    true
                                }
                                else -> true
                            }
                        }
                    } else {
                        popupMenu.inflate(R.menu.other_user_menu)

                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.block_user -> {
                                    true
                                }
                                R.id.report_user -> {
                                    true
                                }
                                R.id.share_user -> {
                                    true
                                }
                                else -> true
                            }
                        }
                    }

                    popupMenu.show()
                }*/
            }
        }

        setViewPager(binding.profilePager, binding.profileTabs, otherUser, activity)

        otherUserLive.observe(activity) {
            if (it != null) {
                setUserMetaText(otherUser)
            }
        }

    }

    private fun setViewPager(vp: ViewPager2, tabs: TabLayout, otherUser: User, activity: MainActivity) {
        vp.adapter = ProfileFragmentPager(otherUser, activity)
        vp.offscreenPageLimit = 1
        (vp.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        TabLayoutMediator(tabs, vp){ s, w ->
            when (w) {
                0 -> s.text = PROJECTS
                1 -> s.text = COLLABORATIONS
                2 -> s.text = BLOGS
            }
        }.attach()

    }


    private fun setUserMetaText(user: User) {
          val projectIds = mutableListOf<String>()
          val blogIds = mutableListOf<String>()

          Log.d(TAG, user.userPrivate.toString())

          projectIds.addAll(user.userPrivate.projectIds)
          blogIds.addAll(user.userPrivate.blogIds)

          val followersCount = user.userPrivate.followers.size
          val followingsCount = user.userPrivate.followings.size
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

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
            popUpTo(findNavController().findDestination(R.id.profileFragment)!!.id) {
                saveState = true
            }
            restoreState = true
        }

          val clickableSpan = object: ClickableSpan() {
              override fun onClick(widget: View) {/*
                  val fragment = FollowingsFragment.newInstance(user)
                  activity.toFragment(fragment, FollowingsFragment.TAG)
                  */
                  findNavController().navigate(R.id.followingsFragment, Bundle().apply { putParcelable(FollowingsFragment.ARG_USER, user) }, options)
              }

              override fun updateDrawState(ds: TextPaint) {
                  super.updateDrawState(ds)
                  ds.isUnderlineText = false
              }
          }

          val clickableSpan1 = object: ClickableSpan() {
              override fun onClick(widget: View) {
                  /*val fragment = FollowersFragment.newInstance(user)
                  activity.toFragment(fragment, FollowersFragment.TAG)*/
                  findNavController().navigate(R.id.followersFragment, Bundle().apply { putParcelable(FollowersFragment.ARG_USER, user) }, options)
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

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(BUG_TAG, "Destroying profile fragment.")
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