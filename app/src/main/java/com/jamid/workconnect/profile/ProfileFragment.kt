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
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.core.view.get
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentProfileBinding
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

        if (currentUser != null) {
            val isSameUser = currentUser.id == otherUser.id
            val isUserFollowed = currentUser.userPrivate.followings.contains(otherUser.id)

            if (isContradiction(isUserFollowed, otherUser, currentUser)) {
                val existingList = otherUser.userPrivate.followers.toMutableList()
                existingList.remove(currentUser.id)
                otherUser.userPrivate.followers = existingList
                otherUserLive.postValue(otherUser)
            }

            if (isSameUser) {
                btn.text = getString(R.string.edit)

                btn.setOnClickListener {
                    val fragment = EditFragment.newInstance()
                    activity.toFragment(fragment, EditFragment.TAG)
                }
            } else {
                fun setFollowText(isUserFollowed: Boolean) {
                    if (isUserFollowed) {
                        btn.text = getString(R.string.unfollow_text)
                    } else {
                        btn.text = getString(R.string.follow_text)
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
            btn.text = getString(R.string.follow_text)
            btn.setOnClickListener {
                activity.showSignInDialog(FOLLOW)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        hideKeyboard()

        val argUid = arguments?.getString(ARG_UID)
        val argOtherUser = arguments?.getParcelable<User>(ARG_USER)

        OverScrollDecoratorHelper.setUpOverScroll((binding.profilePager[0] as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        argUid?.let {
            activity.mainBinding.primaryTitle.text = " "
            activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
            viewModel.getObject(it, User::class.java) { user ->
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
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
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.profileUserSection.setPadding(0, top + convertDpToPx(64), 0, 0)
            binding.profileToolbar.updateLayout(marginTop = top)
        }
    }

    private fun setUser(otherUser: User, activity: MainActivity) {

        viewModel.extras[ARG_USER] = otherUser
        activity.setFragmentTitle( "@" + otherUser.username)

        otherUserLive.postValue(otherUser)

        // TODO("There is a problem with this observer .. ")
        viewModel.user.observe(activity) { currentUser ->
            setFollowButton(binding.editBtn, currentUser, otherUser)
            if (currentUser != null) {
                activity.mainBinding.primaryMenuBtn.setOnClickListener {
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
                }
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
        (vp.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        TabLayoutMediator(tabs, vp){ s, w ->
            when (w) {
                0 -> s.text = PROJECTS
                1 -> s.text = COLLABORATIONS
                2 -> s.text = BLOGS
            }
        }.attach()

        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                activity.mainBinding.primaryTabs.getTabAt(position)?.select()
            }
        })
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

          val clickableSpan = object: ClickableSpan() {
              override fun onClick(widget: View) {
                  val fragment = FollowingsFragment.newInstance(user)
                  activity.toFragment(fragment, FollowingsFragment.TAG)
              }

              override fun updateDrawState(ds: TextPaint) {
                  super.updateDrawState(ds)
                  ds.isUnderlineText = false
              }
          }

          val clickableSpan1 = object: ClickableSpan() {
              override fun onClick(widget: View) {
                  val fragment = FollowersFragment.newInstance(user)
                  activity.toFragment(fragment, FollowersFragment.TAG)
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