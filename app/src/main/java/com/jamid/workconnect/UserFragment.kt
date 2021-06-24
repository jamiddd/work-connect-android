package com.jamid.workconnect

import android.os.Bundle
import androidx.fragment.app.DialogFragment

class UserFragment : DialogFragment() {

   /* private lateinit var binding: FragmentUserBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var hasChanged = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false)
        return binding.root
    }

    *//** The system calls this only when creating the layout in a dialog. *//*
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    private fun setFollowButton(state: Boolean) {
      *//*  binding.profileFollowBtn.isSelected = state
        if (state) {
            binding.profileFollowBtn.text = getString(R.string.unfollow_text)
        } else {
            binding.profileFollowBtn.text = getString(R.string.follow_text)
        }*//*
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = Firebase.auth
        val db = Firebase.firestore
        val otherUserId = arguments?.getString(ARG_USER_ID) ?: return

//        binding.profileFragment.editBtn.visibility = View.GONE
*//*

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            dismiss()
        }
*//*

        db.collection(USERS).document(otherUserId).get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val otherUser = it.toObject(User::class.java)!!

                    if (otherUser.about != null) {
                        binding.profileFragment.profileAbout.visibility = View.VISIBLE
                    } else {
                        binding.profileFragment.profileAbout.visibility = View.GONE
                    }
                    binding.profileFragment.profileAbout.text = otherUser.about

                    binding.userFragmentTitle.text = otherUser.name

                    binding.profileCancelBtn.setOnClickListener {
                        dismiss()
                    }

                    viewModel.followingsMap.observe(viewLifecycleOwner) { map ->
                        if (map != null && map.containsKey(otherUser.id)) {
                            val isFollowed = map[otherUser.id]!!
                            setFollowButton(isFollowed)
                            if(hasChanged) {
                                Log.d("UserFragment", isFollowed.toString())
                                val newList = if (isFollowed) {
                                    val t = mutableListOf<String>().also { list ->
                                        list.addAll(otherUser.followers)
                                        list.add(auth.currentUser!!.uid)
                                    }
                                    t
                                } else {
                                    val s = mutableListOf<String>().also { list ->
                                        list.addAll(otherUser.followers)
                                        list.remove(auth.currentUser!!.uid)
                                    }
                                    s
                                }
                                otherUser.followers = newList
                                setUserMetaText(otherUser)
                            }
                        }
                    }

                    binding.profileFollowBtn.setOnClickListener {
                        if (auth.currentUser != null) {
                            hasChanged = true
                            viewModel.onFollowPressed(otherUser.id, binding.profileFollowBtn.isSelected)
                        } else {
                            findNavController().navigate(R.id.signInFragment)
                        }
                    }

                    binding.profileFragment.apply {

                        profileImg.setImageURI(otherUser.photo)
                        profileName.text = otherUser.name
                        profileEmail.text = otherUser.email

                        setUserMetaText(otherUser)

                        profilePager.adapter = ProfileFragmentPager(otherUser, requireActivity())
                        profilePager.setPadding(0, 0, 0, 0)

                        TabLayoutMediator(profileTabs, profilePager) { t, p ->
                            when (p) {
                                0 -> t.text = PROJECTS
                                1 -> t.text = COLLABORATIONS
                                2 -> t.text = BLOGS
                            }
                        }.attach()

                    }
                }
            }.addOnFailureListener {
                viewModel.setCurrentError(it)
            }
    }

    private fun setUserMetaText(otherUser: User) {
      *//*  val projectIds = mutableListOf<String>()
        val blogIds = mutableListOf<String>()
        projectIds.addAll(otherUser.projectIds)
        blogIds.addAll(otherUser.blogIds)

        val followersCount = otherUser.followers.size
        val followingsCount = otherUser.followings.size
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
            putParcelable("user", otherUser)
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
                findNavController().navigate(R.id.followersFragment, bundle)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        sp.setSpan(clickableSpan1, 0, followersCount.toString().length + 13, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(clickableSpan, followersCount.toString().length + 13, followersCount.toString().length + 24, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
*//*
       *//* binding.profileFragment.userMeta.text = sp
        binding.profileFragment.userMeta.movementMethod = LinkMovementMethod.getInstance()
        binding.profileFragment.userMeta.highlightColor = Color.TRANSPARENT*//*
    }*/

    companion object {

        const val TAG = "UserFragment"
        const val ARG_USER_ID = "ARG_USER_ID"

        @JvmStatic
        fun newInstance(userId: String) = UserFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_ID, userId)
            }
        }
    }
}