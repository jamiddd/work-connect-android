package com.jamid.workconnect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.github.mmin18.widget.RealtimeBlurView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jamid.workconnect.adapter.paging3.PostFragmentTest
import com.jamid.workconnect.adapter.paging3.PostViewHolder
import com.jamid.workconnect.auth.InterestFragment
import com.jamid.workconnect.auth.SignInFragment
import com.jamid.workconnect.auth.UserDetailFragment
import com.jamid.workconnect.databinding.ActivityMainBinding
import com.jamid.workconnect.explore.ExploreFragment
import com.jamid.workconnect.home.*
import com.jamid.workconnect.interfaces.*
import com.jamid.workconnect.message.*
import com.jamid.workconnect.model.*
import com.jamid.workconnect.profile.*
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.io.File
import com.jamid.workconnect.search.SearchFragment
import com.jamid.workconnect.settings.SettingsFragment
import com.jamid.workconnect.views.zoomable.ImageViewFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class MainActivity : AppCompatActivity(),
    PostsLoadStateListener,
    PostItemClickListener,
    GenericLoadingStateListener,
    SearchItemClickListener,
    UserItemClickListener,
    MessageItemClickListener,
    ChatChannelClickListener,
    NotificationClickListener,
    RequestItemClickListener,
    GenericDialogListener,
    GenericMenuClickListener {

    private var currentNavController: LiveData<NavController>? = null
    private val viewModel: MainViewModel by viewModels()
    lateinit var mainBinding: ActivityMainBinding
    lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var currentBottomFragment: Fragment? = null
    private var currentFragmentId: Int = 0
    private var hasPendingTransition = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private lateinit var notificationManager: NotificationManager

    val recyclerViewPool = RecyclerView.RecycledViewPool()
    val fragmentStack = Stack<String>()
    val titleStack = Stack<String>()
    var currentViewHolder: PostViewHolder? = null

    var currentImagePosition = 0
    private var currentMessage: SimpleMessage? = null
    private var currentFileName: String? = null
    private var mContainerId = 0
    private var currentFragmentTag = ""

    private var currentCropConfig: Bundle? = null

    private var lastTagList: List<String> = emptyList()

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation

            viewModel.setCurrentLocation(SimpleLocation(location.latitude, location.longitude, ""))

            setAddressList(location)
        }
    }

    /*private val Context.dataStore by preferencesDataStore(name = FLAG_PREFERENCES)

    private object PreferenceKeys {
        val IS_FIRST_TIME = booleanPreferencesKey("IS_FIRST_TIME")

    }*/

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        geocoder = Geocoder(this)
        createNotificationChannel()

        mContainerId = R.id.navHostFragment

        // Add this function in onCreate Method
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }



        if (Build.VERSION.SDK_INT <= 27) {
            // set mode to performance
            // remove blur
            // remove other stuff

            window.navigationBarColor = ContextCompat.getColor(this, R.color.navigationBarColor)
            window.statusBarColor = ContextCompat.getColor(this, R.color.transparent)
            @Suppress("DEPRECATION")
            removeBlurViews(mainBinding.mainRoot)

        }

        bottomSheetBehavior = setBottomSheet(mainBinding.bottomHelperView)

        OverScrollDecoratorHelper.setUpOverScroll(mainBinding.horizontalContainer)

        val statusBarHeight = statusBarHeight()
        val navigationBarHeight = navigationBarHeight()

        mainBinding.root.setOnApplyWindowInsetsListener { v, insets ->
            // should only be less than
            val (top: Int, bottom: Int) = if (Build.VERSION.SDK_INT < 30) {
                @Suppress("DEPRECATION")
                val statusBarSize = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                val navBarSize = insets.systemWindowInsetBottom
                statusBarSize to navBarSize
            } else {
                val statusBar = insets.getInsets(WindowInsets.Type.statusBars())
                val navBar = insets.getInsets(WindowInsets.Type.navigationBars())
                val keyBoard = insets.getInsets(WindowInsets.Type.ime())

                val statusBarSize = statusBar.top - statusBar.bottom
                val navBarSize = navBar.bottom - navBar.top
                val keyBoardSize = keyBoard.bottom - keyBoard.top
                if (keyBoardSize > 0) {
                    statusBarSize to keyBoardSize
                } else {
                    statusBarSize to navBarSize
                }
            }
            Log.d(TAG, Pair(top, bottom).toString())
            viewModel.windowInsets.postValue(Pair(top, bottom))
            insets
        }


//        viewModel.windowInsets.postValue(Pair(statusBarHeight, navigationBarHeight))

        val params1 = mainBinding.bottomCard.layoutParams as CoordinatorLayout.LayoutParams
        params1.height = navigationBarHeight + convertDpToPx(56)
        params1.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
        mainBinding.bottomCard.layoutParams = params1


        mainBinding.linearPrimaryContainer.setPadding(0, statusBarHeight, 0, 0)

        viewModel.networkErrors.observe(this) {
            if (it != null) {
                Log.e(TAG, it.localizedMessage!!)
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                mainBinding.primaryProgressBar.visibility = View.GONE
            }
        }

        viewModel.firebaseUser.observe(this) {
            if (it != null) {
                lifecycleScope.launch {
                    val user = viewModel.getLocalUser(it.uid)
                    if (user == null) {
                        Snackbar.make(mainBinding.root, "You haven't created your account completely. Complete this process to go back.", Snackbar.LENGTH_INDEFINITE).show()
                        toFragment(UserDetailFragment.newInstance(), UserDetailFragment.TAG)
                    }
                }
            }
        }

        viewModel.firebaseErrors.observe(this) {
            if (it != null) {
                viewModel.setCurrentError(it)
            }
        }

        setSearchTags()

        viewModel.user.observe(this) { user ->

            if (user != null) {
                // the user can login from different fragments, make sure that the search tags appear only
                // when the user is in home fragment
                if (viewModel.currentFragmentTag.value == HomeFragment.TAG) {
                    mainBinding.horizontalContainer.visibility = View.VISIBLE
                } else {
                    mainBinding.horizontalContainer.visibility = View.GONE
                }
            } else {
                // TODO("Set up search tags dynamically from firebase database")
                // currently the search tags are predefined, but these need to be fetched from popular interests collection
                // once setup
            }

            // TODO("Move this function and associate functions to the respective fragment")
            // set the search tags

            // set the user icon
            setUserIcon(user)

            if (user != null) {
                if (viewModel.miniUser.value == null) {
                    val userMinimal = UserMinimal(user.id, user.name, user.email, user.username, user.photo)
                    viewModel.setUserMinimal(userMinimal)
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter(
            CHAT_FRAGMENT_INTENT))
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter(
            ACCEPT_PROJECT_INTENT)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(tokenReceiver, IntentFilter("tokenIntent"))

        val extras = intent.extras
        if (extras != null) {

            val notificationId = extras.getString(NOTIFICATION_ID)
            if (notificationId != null) {
                mainBinding.bottomNav.selectedItemId = R.id.notification_navigation
            } else {
                val chatChannelId = extras.get(CHAT_CHANNEL_ID) as String?
                if (chatChannelId != null) {
                    viewModel.onNewMessageNotification(chatChannelId) {
                        // TODO("Use it to navigate using navigation component")
                        // navigateToFragmentUsingPendingIntent()

                        // since navigation component is not being used, cannot use pending intent
                        // this is only a workaround until navigation component start can save fragment
                        // state

                        mainBinding.bottomNav.selectedItemId = R.id.message_navigation
                        toFragment(ChatFragment.newInstance(it), ChatFragment.TAG)

                    }
                }
            }
        }

        mainBinding.primarySearchLayout.setTransitionDuration(150)
        mainBinding.cancelSearchBtn.setOnClickListener {
            hideKeyboard(mainBinding.root)
            mainBinding.primaryTabs.visibility = View.GONE
            mainBinding.bottomCard.show()
            mainBinding.primarySearchLayout.transitionToStart()
            mainBinding.primarySearchBar.text.clear()
            mainBinding.cancelSearchBtn.requestFocus()
            onBackPressed()
        }

        viewModel.currentFragmentTag.observe(this) { currentFragment ->
            if (currentFragment != null) {
                updateUI(currentFragment)
            }
        }

    }

    private fun navigateToFragmentUsingPendingIntent(navGraphId: Int, destinationId: Int, extras: Bundle) {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(navGraphId)
            .setDestination(destinationId)
            .setArguments(extras)
            .createPendingIntent()

        pendingIntent.send()
    }

    private fun setUserIcon(user: User?) {
        mainBinding.userIcon.setImageURI(user?.photo)
        mainBinding.userIcon.setOnClickListener {
            if (user != null) {
                val instance = ProfileFragment.newInstance(user = user)
                toFragment(instance, ProfileFragment.TAG)
            } else {
                val fragment = SignInFragment.newInstance()
                toFragment(fragment, SignInFragment.TAG)
            }
        }
    }

    private fun setSearchTags() {

        mainBinding.popularInterestsGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.first_chip) {
                viewModel.setCurrentHomeTag(null)
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                viewModel.setCurrentHomeTag(chip?.text?.toString())
            }
        }

        viewModel.topTags.observe(this) {
            if (it.isNotEmpty()) {
                mainBinding.popularInterestsGroup.removeViews(1, mainBinding.popularInterestsGroup.childCount - 1)
                it.forEachIndexed { index, popularInterest ->
                    if (index < 9) {
                        addChip(index, popularInterest)
                    } else {
                        // TODO("Dynamically add numbers of search tags")
                        // currently the number of search tags is static, hence only 9 search tags and one random tag
                        // is used
                    }
                }

                mainBinding.firstChip.isCheckable = true
                mainBinding.firstChip.isCheckedIconVisible = false

                mainBinding.firstChip.setOnClickListener {
                    mainBinding.firstChip.isChecked = true
                }
            }
        }
    }

    fun removeBlurViews(viewGroup: ViewGroup) {
        if (Build.VERSION.SDK_INT <= 27) {
            for (child in viewGroup.children) {
                if (child is ViewGroup && child.childCount > 0) {
                    removeBlurViews(child)
                } else {
                    if (child is RealtimeBlurView) {
                        viewGroup.removeView(child)
                        if (viewGroup is CardView || viewGroup is MaterialCardView) {
                            (viewGroup as CardView).setBackgroundColor(Color.WHITE)
                        } else {
                            viewGroup.setBackgroundColor(Color.WHITE)
                        }
                    }
                }
            }
        }
    }

    private fun addChip(index: Int, interest: String) {
        val chip = LayoutInflater.from(this).inflate(R.layout.chip, null) as Chip
        chip.text = interest
        chip.id = index + 1
        chip.isCheckable = true
        chip.isCheckedIconVisible = false

        chip.setOnClickListener {
            chip.isChecked = true
        }

        mainBinding.popularInterestsGroup.addView(chip)
    }

    private fun getCurrentSupportFragmentTag() : String? {
        return if (fragmentStack.isNotEmpty()) {
            fragmentStack.peek()
        } else {
            null
        }
    }

    private fun setBottomSheet(bottomSheetView: ConstraintLayout): BottomSheetBehavior<ConstraintLayout> {
        val behavior = BottomSheetBehavior.from(bottomSheetView)
        behavior.peekHeight = 0
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        behavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                viewModel.setPrimaryBottomSheetState(newState)
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    mainBinding.scrimForBottomSheet.alpha = 1f
                    mainBinding.scrimForBottomSheet.setOnClickListener {
                        if (behavior.isHideable) {
                            behavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                } else if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    mainBinding.scrimForBottomSheet.alpha = 1f
                    mainBinding.scrimForBottomSheet.setOnClickListener {
                        if (behavior.isHideable) {
                            behavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    currentBottomFragment?.let { supportFragmentManager.beginTransaction().remove(it).commit() }
                    mainBinding.scrimForBottomSheet.isClickable = false
                    currentBottomFragment = null

                    if (hasPendingTransition) {
                        when (currentFragmentTag) {
                            CreateProjectFragment.TAG -> {
                                toFragment(CreateProjectFragment.newInstance(), CreateProjectFragment.TAG)
                            }
                            EditorFragment.TAG -> {
                                toFragment(EditorFragment.newInstance(), EditorFragment.TAG)
                            }
                            SIGN_IN_PROMPT -> {

                                val instance = GenericDialogFragment.newInstance(
                                    SIGN_IN_PROMPT,
                                    "Sign in or Register",
                                    "You are not signed in. To do any activity, you must have an account.",
                                    image = R.drawable.ic_authentication,
                                    isProgressing = false,
                                    isActionOn = true,
                                    isCancelable = true)

                                showBottomSheet(instance, SIGN_IN_PROMPT)
                            }
                        }
                        hasPendingTransition = false
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                mainBinding.scrimForBottomSheet.alpha = slideOffset
                mainBinding.scrimForBottomSheet.isClickable = slideOffset > 0
            }

        })

        return behavior
    }

    fun updateUI(tag: String) {
        // TODO("Get viewmodel extras here .. ")
        mainBinding.apply {
            // title
            when (tag) {
                HomeFragment.TAG -> primaryTitle.text = HomeFragment.TITLE
                BlogFragment.TAG, ProjectFragment.TAG -> {
                    val post = viewModel.extras[ProjectFragment.ARG_POST] as Post?
                    if (post != null) {
                        primaryTitle.text = post.title
                    }
                }
                ProfileFragment.TAG -> {
                    val bundle = viewModel.extras
                    val user = bundle[ProfileFragment.ARG_USER] as User?
                    if (user != null) {
                        primaryTitle.text = "@" + user.username
                    }
                }
                SignInFragment.TAG -> primaryTitle.text = "Login or Sign Up"
                SavedPostsFragment.TAG -> primaryTitle.text = SavedPostsFragment.TITLE
                MessageFragment.TAG -> primaryTitle.text  = MessageFragment.TITLE
                ChatFragment.TAG -> {
                    val bundle = viewModel.extras
                    val chatChannel = bundle[ChatFragment.ARG_CHAT_CHANNEL] as ChatChannel?
                    if (chatChannel != null) {
                        primaryTitle.text = chatChannel.postTitle
                    }
                }
                ProjectGuidelinesFragment.TAG -> primaryTitle.text = ProjectGuidelinesFragment.TITLE
                NotificationFragment.TAG -> primaryTitle.text = NotificationFragment.TITLE
                MediaFragment.TAG -> primaryTitle.text = MediaFragment.TITLE
                ImageViewFragment.TAG -> primaryTitle.text = ""
//                ImageViewFragment.TAG -> {
//                    val bundle = viewModel.extras
//                    val message = bundle[ImageViewFragment.ARG_MESSAGE] as SimpleMessage?
//                    if (message != null) {
//                        primaryTitle.text = message.sender.name
//                    }
//                }
                PostFragmentTest.TAG -> primaryTitle.text = "Post"
                CreateProjectFragment.TAG -> primaryTitle.text = CreateProjectFragment.TITLE
                EditorFragment.TAG -> primaryTitle.text = EditorFragment.TITLE
                UserDetailFragment.TAG -> primaryTitle.text = ""
                InterestFragment.TAG -> primaryTitle.text = "Select Interests"
                FollowersFragment.TAG -> primaryTitle.text = "Followers"
                FollowingsFragment.TAG -> primaryTitle.text = "Followings"
                EditFragment.TAG -> primaryTitle.text = "Edit Profile"
                SettingsFragment.TAG -> primaryTitle.text = "Settings"
            }

            // APPBAR
            primaryAppBar.translationY = 0f
            when (tag) {
                ProjectFragment.TAG, BlogFragment.TAG -> {
                    primaryAppBar.alpha = 0f
                    topDivider.alpha = 0f
                    topDivider.visibility = View.GONE
                    primaryAppBar.visibility = View.INVISIBLE
                }
                else -> {
                    primaryAppBar.alpha = 1f
                    topDivider.alpha = 1f
                    topDivider.visibility = View.VISIBLE
                    primaryAppBar.visibility = View.VISIBLE
                }
            }

            // TOOLBAR
            when (tag) {
                ExploreFragment.TAG, SearchFragment.TAG -> primaryToolbarContainer.visibility = View.GONE
                else -> primaryToolbarContainer.visibility = View.VISIBLE
            }

            // USER IMAGE
            when (tag) {
                HomeFragment.TAG, MessageFragment.TAG, NotificationFragment.TAG -> userIcon.visibility = View.VISIBLE
                // as it is the profile won't be there, just to be extra safe
                else -> userIcon.visibility = View.GONE
            }

            // MENU ICON
            primaryMenuBtn.apply {
                when (tag) {
                    HomeFragment.TAG -> {
                        isEnabled = true
                        visibility = View.VISIBLE
                        text = ""
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_add_24)
                        setOnClickListener {
                            val _tag = CREATE_MENU
                            val item1 = GenericMenuItem(_tag, "Create new Blog", R.drawable.ic_baseline_note_24, 0)
                            val item2 = GenericMenuItem(_tag, "Create new Project", R.drawable.ic_baseline_architecture_24, 1)
                            val fragment = GenericMenuFragment.newInstance(_tag, "Create New ...", arrayListOf(item1, item2))
                            showBottomSheet(fragment, _tag)
                        }
                    }
                    ProfileFragment.TAG -> {
                        isEnabled = true
                        visibility = View.VISIBLE
                        text = ""
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_more_horiz_24)

                        val bundle = viewModel.extras
                        val user = bundle[ProfileFragment.ARG_USER] as User?
                        if (user != null) {
                            setOnClickListener {
                                val currentUser = viewModel.user.value
                                if (currentUser != null) {
                                    val popupMenu = PopupMenu(this@MainActivity, it)
                                    if (user.id == currentUser.id) {

                                        popupMenu.inflate(R.menu.profile_menu)

                                        popupMenu.setOnMenuItemClickListener { menuItem ->
                                            when (menuItem.itemId) {
                                                R.id.saved_posts_item -> {
                                                    toFragment(SavedPostsFragment.newInstance(), SavedPostsFragment.TAG)
                                                    true
                                                }
                                                R.id.user_settings -> {
                                                    toFragment(SettingsFragment.newInstance(), SettingsFragment.TAG)
                                                    true
                                                }
                                                else -> true
                                            }
                                        }


                                    } else {
                                        popupMenu.inflate(R.menu.other_user_menu)

                                        popupMenu.setOnMenuItemClickListener {
                                            when (it.itemId) {
                                                R.id.block_user -> {
//                                                    toFragment(SavedPostsFragment.newInstance(), SavedPostsFragment.TAG)
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
                    }
                    ChatFragment.TAG -> {
                        visibility = View.VISIBLE
                        isEnabled = true
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_info_24)
                        text = ""
                        setOnClickListener {
                            val extras = viewModel.extras
                            val chatChannel = extras[ProjectDetailFragment.ARG_CHAT_CHANNEL] as ChatChannel?
                            if (chatChannel != null) {
                                val fragment = ProjectDetailFragment.newInstance(chatChannel)
                                toFragment(fragment, ProjectDetailFragment.TAG)
                            }
                        }
                    }
                    CreateProjectFragment.TAG -> {
                        visibility = View.VISIBLE
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_done_24)
                    }
                    EditorFragment.TAG -> {
                        visibility = View.VISIBLE
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_done_24)
                    }
                    ProjectGuidelinesFragment.TAG -> {
                        visibility = View.VISIBLE
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_done_24)
                        /*setOnClickListener {
                            mainBinding.primaryProgressBar.visibility = View.VISIBLE
                            val post = viewModel.extras[ProjectGuidelinesFragment.ARG_POST] as Post?
                            val text = findViewById<EditText>(R.id.pg_text)?.text
                            if (!text.isNullOrBlank() && post != null) {
                                viewModel.updatePost(post, text.trimEnd().toString())
                            }
                        }*/
                    }
                    UserDetailFragment.TAG -> {
                        visibility = View.VISIBLE
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_done_24)

                        val edittext = findViewById<EditText>(R.id.userFullNameText)

                        isEnabled = edittext != null && edittext.text.length > 2

                        mainBinding.primaryMenuBtn.setOnClickListener {
                            hideKeyboard(mainBinding.root)
                            toFragment(InterestFragment.newInstance(), InterestFragment.TAG)
                        }
                    }
                    ProjectFragment.TAG -> {
                        visibility = View.VISIBLE
                        icon = null
                        text = "Join"
                        val user = viewModel.user.value
                        val bundle = viewModel.extras
                        val post = bundle[ProjectFragment.ARG_POST] as Post?
                        if (user != null) {
                            if (post != null) {
                                if (user.id != post.id) {
                                    when {
                                        user.userPrivate.collaborationIds.contains(post.id) -> {
                                            visibility = View.GONE
                                        }
                                        user.userPrivate.activeRequests.contains(post.id) -> {
                                            isEnabled = false
                                        }
                                        else -> {
                                            visibility = View.VISIBLE
                                            isEnabled = true
                                        }
                                    }
                                } else {
                                    visibility = View.GONE
                                }
                            } else {
                                isEnabled = false
                            }
                        }
                    }
                    InterestFragment.TAG -> {
                        isEnabled = false
                        visibility = View.VISIBLE
                        hideKeyboard(mainBinding.root)
                        icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_baseline_done_24)

                        setOnClickListener {
                            supportFragmentManager.popBackStack(SignInFragment.TAG, 1)
                        }

                    }
                    EditFragment.TAG -> {
                        // it is an end destination, so going back is not much of use, maybe in future

                        isEnabled = true
                        visibility = View.VISIBLE
                        icon = null
                        text = "Save"

                        setOnClickListener {
                            Snackbar.make(mainBinding.root, "Not implemented yet", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        primaryMenuBtn.visibility = View.GONE
                    }
                }
            }

            // interest container
            if (tag == HomeFragment.TAG) {
                horizontalContainer.visibility = View.VISIBLE
            } else {
                horizontalContainer.visibility = View.GONE
            }

            // search bar
            when (tag) {
                ExploreFragment.TAG -> {
                    mainBinding.primarySearchBar.hint = "Search"
                    mainBinding.primarySearchBar.setOnClickListener {

                        for (fragment in supportFragmentManager.fragments) {
                            if (fragment is SearchFragment) {
                                return@setOnClickListener
                            }
                        }
                        val instance = SearchFragment.newInstance()
                        toFragment(instance, SearchFragment.TAG)
                    }
                }
                SearchFragment.TAG -> {
                    mainBinding.primarySearchBar.hint = "Search"
                    mainBinding.primarySearchBar.doAfterTextChanged {
                        if (!it.isNullOrBlank()) {
                            val query = it.toString()
                            viewModel.setCurrentQuery(query)
                        }
                    }
                }
                FollowersFragment.TAG -> {

                    mainBinding.primarySearchBar.setOnClickListener {
                        //
                    }

                    mainBinding.primarySearchBar.doAfterTextChanged {
                        if (!it.isNullOrBlank()) {

                        }
                    }
                }
                FollowingsFragment.TAG -> {

                    mainBinding.primarySearchBar.setOnClickListener {
                        //
                    }

                    mainBinding.primarySearchBar.doAfterTextChanged {
                        if (!it.isNullOrBlank()) {

                        }
                    }
                }
            }

            // search layout
            when (tag) {
                ExploreFragment.TAG -> {
                    primarySearchLayout.visibility = View.VISIBLE
                    primarySearchLayout.transitionToStart()
                }
                SearchFragment.TAG -> {
                    primarySearchLayout.visibility = View.VISIBLE
                    primarySearchLayout.transitionToEnd()
                }
                FollowersFragment.TAG -> {
                    primarySearchLayout.visibility = View.VISIBLE
                }
                FollowingsFragment.TAG -> {
                    primarySearchLayout.visibility = View.VISIBLE
                }
                else -> {
                    primarySearchLayout.visibility = View.GONE
                }
            }

            // tabs
            if (tag == SearchFragment.TAG || tag == NotificationFragment.TAG || tag == MediaFragment.TAG) {
                primaryTabs.visibility = View.VISIBLE
            } else {
                primaryTabs.visibility = View.GONE
            }

            // back navigation
            when (tag) {
                HomeFragment.TAG, ExploreFragment.TAG, MessageFragment.TAG, NotificationFragment.TAG, UserDetailFragment.TAG -> {
                    backNavigationBtn.visibility = View.GONE
                    backNavigationBtn.setOnClickListener {

                    }
                }
                else -> {
                    backNavigationBtn.visibility = View.VISIBLE
                    backNavigationBtn.setOnClickListener {
                        onBackPressed()
                    }
                }
            }

            // bottom navigation
            when (tag) {
                HomeFragment.TAG, ExploreFragment.TAG, MessageFragment.TAG, NotificationFragment.TAG -> {
                    mainBinding.bottomCard.show()
                }
                else -> mainBinding.bottomCard.hide()
            }

            // special cases
            when (tag) {
                ProjectDetailFragment.TAG -> {
                    mainBinding.primaryAppBar.visibility = View.GONE
                    topDivider.visibility = View.GONE
                }
                SearchFragment.TAG -> {
                    mainBinding.primarySearchBar.setCursorVisible(true)
                }
                FollowersFragment.TAG -> {
                    mainBinding.primarySearchBar.setCursorVisible(true)
                }
                FollowingsFragment.TAG -> {
                    mainBinding.primarySearchBar.setCursorVisible(true)
                }
                ExploreFragment.TAG -> {
                    mainBinding.primarySearchBar.setCursorVisible(false)
                    mainBinding.primarySearchBar.requestFocus()
                    mainBinding.primarySearchBar.text.clear()
                }
                else -> {
                    mainBinding.primaryAppBar.visibility = View.VISIBLE
                    topDivider.visibility = View.VISIBLE
                }
            }
        }
    }

    // Override onRestoreInstanceState in MainActivity
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }


    // Add this member function in MainActivity
    private fun setupBottomNavigationBar() {

        val toolbar = findViewById<MaterialToolbar>(R.id.primaryToolbar)
        setSupportActionBar(toolbar)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)

        // List of nav graph for each of the fragment collection
        val navGraphIds = listOf(
            R.navigation.home_navigation,
            R.navigation.explore_navigation,
            R.navigation.message_navigation,
            R.navigation.notification_navigation
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.navHostFragment,
            intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this) { navController ->

            setupActionBarWithNavController(navController)

           /* mainBinding.primaryToolbar.setNavigationOnClickListener {
                hideKeyboard(mainBinding.root)
                navController.navigateUp()
            }*/

            navController.addOnDestinationChangedListener { _, destination, _ ->
                /*if (destination.id == R.id.homeFragment) {


                } else if (destination.id == R.id.exploreFragment) {
                    fragmentStack.push(ExploreFragment.TAG)
                    setFragmentTitle(ExploreFragment.TITLE)
                } else if (destination.id == R.id.messageFragment) {
                    fragmentStack.push(MessageFragment.TAG)
                    setFragmentTitle(MessageFragment.TITLE)
                } else if (destination.id == R.id.notificationFragment) {
                    fragmentStack.push(NotificationFragment.TAG)
                    setFragmentTitle(NotificationFragment.TITLE)
                } else if (destination.id == R.id.chatFragment) {
                    fragmentStack.push(ChatFragment.TAG)
                    updateUI(ChatFragment.TAG)
                }*/
                /*mainBinding.primaryMenuBtn.setOnClickListener {
					  val fragment = CreateOptionFragment.newInstance()
					  showBottomSheet(fragment, CreateOptionFragment.TAG)
				  }*/
                /* fragmentStack.removeIf {
					 (it == ExploreFragment.TAG) || (it == MessageFragment.TAG) || (it == NotificationFragment.TAG)
				 }
				 titleStack.removeIf {
					 (it == ExploreFragment.TITLE) || (it == MessageFragment.TITLE) || (it == NotificationFragment.TITLE)
				 }
				 setFragmentTitle(HomeFragment.TITLE)*/
            }

        }
        currentNavController = controller

        setFragmentManagerListeners()
    }

    private fun setFragmentManagerListeners() {
        /*fragmentStack.push(HomeFragment.TAG)
        titleStack.clear()
        setFragmentTitle(HomeFragment.TITLE)*/
        supportFragmentManager.addOnBackStackChangedListener {
            viewModel.onFragmentTransactionComplete(supportFragmentManager.backStackEntryCount)

            removeBlurViews(mainBinding.mainRoot)

            /*if (fragmentStack.size - 1 > supportFragmentManager.backStackEntryCount) {
                mainBinding.primaryTitle.text = titleStack.peek()
                updateUI(fragmentStack.peek())
            } else if (fragmentStack.size - 1 == supportFragmentManager.backStackEntryCount) {
                mainBinding.primaryTitle.text = titleStack.peek()
                updateUI(fragmentStack.peek())
            } else {

            }*/
            /*if (currentNavController?.value?.currentDestination?.id == R.id.homeFragment) {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    // first transaction - can be (project, blog, profile) fragment
                    for (fragment in supportFragmentManager.fragments) {
                        if (fragment.isVisible) {
                            when {
                                fragment is ProfileFragment -> updateUI(ProfileFragment.TAG)
                                fragment is CreateProjectFragment -> updateUI(CreateProjectFragment.TAG)
                                fragment is EditorFragment -> updateUI(EditorFragment.TAG)
                                fragment is SavedPostsFragment -> updateUI(SavedPostsFragment.TAG)
                                fragment is BlogFragment -> updateUI(BlogFragment.TAG)
                                fragment is ProjectFragment -> updateUI(ProjectFragment.TAG)
                                fragment is SignInFragment -> updateUI(SignInFragment.TAG)
                                fragment is UserDetailFragment -> updateUI(UserDetailFragment.TAG)
                                fragment is InterestFragment -> updateUI(InterestFragment.TAG)
                                fragment is FollowersFragment -> updateUI(FollowersFragment.TAG)
                                else -> {

                                }
                            }
                        }
                    }
                } else {
                    // home fragment
                    updateUI(HomeFragment.TAG)
                }
            } else if (currentNavController?.value?.currentDestination?.id == R.id.exploreFragment){
                if (supportFragmentManager.backStackEntryCount == 1) {
                    // explore fragment
                    updateUI(ExploreFragment.TAG)
                } else if(supportFragmentManager.backStackEntryCount == 2) {
                    for (fragment in supportFragmentManager.fragments) {
                        if (fragment.isVisible) {
                            if (fragment is BlogFragment) {
                                updateUI(BlogFragment.TAG)
                            } else if (fragment is ProjectFragment) {
                                updateUI(ProjectFragment.TAG)
                            } else if (fragment is ProfileFragment) {
                                updateUI(ProfileFragment.TAG)
                            } else if(fragment is SignInFragment) {
                                updateUI(SignInFragment.TAG)
                            } else if (fragment is SearchFragment) {
                                updateUI(SearchFragment.TAG)
                            } else {
//                                updateUI(fragmentStack.peek())
                            }
                        }
                    }
                } else if (supportFragmentManager.backStackEntryCount > 2) {
                    for (fragment in supportFragmentManager.fragments) {
                        if (fragment.isVisible) {
                            when {
                                fragment is SavedPostsFragment -> updateUI(SavedPostsFragment.TAG)
                                fragment is ProfileFragment -> updateUI(ProfileFragment.TAG)
                                fragment is BlogFragment -> updateUI(BlogFragment.TAG)
                                fragment is ProjectFragment -> updateUI(ProjectFragment.TAG)
                                fragment is SearchFragment -> updateUI(SearchFragment.TAG)
                                fragment is FollowersFragment -> updateUI(FollowersFragment.TAG)
                                fragment is SignInFragment -> updateUI(SignInFragment.TAG)
                                else -> {
//                                    if (fragmentStack.isNotEmpty()) {
//                                        updateUI(fragmentStack.peek())
//                                    }
                                }
                            }
                        }
                    }
                } else {

                }
            } else if(currentNavController?.value?.currentDestination?.id == R.id.messageFragment){
                if (supportFragmentManager.backStackEntryCount == 1) {
                    updateUI(MessageFragment.TAG)
                } else {
                    for (fragment in supportFragmentManager.fragments) {
                        if (fragment.isVisible) {
                            when {
                                fragment is ProfileFragment -> updateUI(ProfileFragment.TAG)
                                fragment is BlogFragment -> updateUI(BlogFragment.TAG)
                                fragment is ProjectFragment -> updateUI(ProjectFragment.TAG)
                                fragment is SavedPostsFragment -> updateUI(SavedPostsFragment.TAG)
                                fragment is ChatFragment -> updateUI(ChatFragment.TAG)
                                fragment is MediaFragment -> updateUI(MediaFragment.TAG)
                                fragment is ProjectDetailFragment -> updateUI(ProjectDetailFragment.TAG)
                                fragment is ProjectGuidelinesFragment -> updateUI(ProjectGuidelinesFragment.TAG)
                                fragment is ImageViewFragment -> updateUI(ImageViewFragment.TAG)
                                fragment is SignInFragment -> updateUI(SignInFragment.TAG)
                                fragment is FollowersFragment -> updateUI(FollowersFragment.TAG)
                                else -> {

                                }
                            }
                        }
                    }
                }
            } else if(currentNavController?.value?.currentDestination?.id == R.id.notificationFragment) {
                if (supportFragmentManager.backStackEntryCount == 1) {
                    updateUI(NotificationFragment.TAG)
                } else {
                    for (fragment in supportFragmentManager.fragments) {
                        if (fragment.isVisible) {
                            when {
                                fragment is ProfileFragment -> updateUI(ProfileFragment.TAG)
                                fragment is BlogFragment -> updateUI(BlogFragment.TAG)
                                fragment is ProjectFragment -> updateUI(ProjectFragment.TAG)
                                fragment is SavedPostsFragment -> updateUI(SavedPostsFragment.TAG)
                                fragment is ImageViewFragment -> updateUI(ImageViewFragment.TAG)
                                fragment is SignInFragment -> updateUI(SignInFragment.TAG)
                                else -> {

                                }
                            }
                        }
                    }
                }
            } else {

            }*/
        }
    }

    fun setFragmentTitle(title: String) {
        mainBinding.primaryTitle.text = title
    }

    override fun onBackPressed() {
        if (currentBottomFragment != null) {
            if (currentBottomFragment is GenericDialogFragment) {
                return
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        } else {
            if (viewModel.fragmentTagStack.size < 2) {
                finish()
            } else {
                if (viewModel.fragmentTagStack.peek() == UserDetailFragment.TAG) {
                    Toast.makeText(this, "Finish creating your account!", Toast.LENGTH_LONG).show()
                    return
                }
                viewModel.fragmentTagStack.pop()
                viewModel.setCurrentFragmentTag(viewModel.fragmentTagStack.peek())
                super.onBackPressed()
            }
        }
    }

    // For back navigation
    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    /*override fun onCreateBlog() {
        if (viewModel.user.value == null) {
            showSignInDialog(BLOG)
        } else {
            currentFragmentTag = EditorFragment.TAG
            hasPendingTransition = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onCreateProject() {
        if (viewModel.user.value == null) {
            showSignInDialog(PROJECT)
        } else {
            currentFragmentTag = CreateProjectFragment.TAG
            hasPendingTransition = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }*/

    fun showSignInDialog(origin: String) {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {

            val message = when (origin) {
                BLOG -> {
                    "You are not signed in. To create a blog, you must have an account."
                }
                PROJECT -> {
                    "You are not signed in. To create a project, you must have an account."
                }
                POST -> {
                    "You are not signed in. To interact with a blog or a project, you must have an account."
                }
                FOLLOW -> {
                    "You are not signed in. To follow a person, you must have an account."
                }
                else -> {
                    ""
                }
            }
            val instance = GenericDialogFragment.newInstance(
                SIGN_IN_PROMPT,
                "Sign in or Register",
                message,
                image = R.drawable.ic_authentication,
                isProgressing = false,
                isActionOn = true,
                isCancelable = true)

            showBottomSheet(instance, SIGN_IN_PROMPT)
        } else {
            currentFragmentTag = SIGN_IN_PROMPT
            hasPendingTransition = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun showBottomSheet(fragment: Fragment, tag: String? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, tag)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun invokeLocationFragment() {
        getLocation({
            val fragment = LocationFragment.newInstance()
            showBottomSheet(fragment, LocationFragment.TAG)
        }, {

        })
    }

    private fun selectImage(extras: Bundle?) {
        currentCropConfig = extras
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, REQUEST_GET_IMAGE)
    }

//    override fun onSelectImageFromGallery() {
//        selectImage(bundle)
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//    }
//
//    override fun onCaptureEvent() {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//    }
//
//    override fun onImageRemove() {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_GET_IMAGE -> {
                    val image = data?.data
                    viewModel.setCurrentImage(image)
                    val fragment = ImageCropFragment.newInstance(currentCropConfig)
                    showBottomSheet(fragment, ImageCropFragment.TAG)
                    /*val fragment = ImageCropFragment.newInstance(4, 3, 400, 300, "RECTANGLE")*/
                }
                REQUEST_GET_DOCUMENT -> {
                    val doc = data?.data
                    viewModel.setCurrentDoc(doc)
                }
            }
        }
    }

    private fun setAddressList(location: Location) {

        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 7)
        val list = mutableListOf<String>()

        for (address in addresses) {
            if (address.locality == null) {
                continue
            }

            val name = address.locality + ", " + address.adminArea
            list.add(name)
        }

        viewModel.setAddressList(list)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(
        fusedLocationProviderClient: FusedLocationProviderClient,
        mLocationCallback: LocationCallback
    ) {
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1
        mLocationRequest.setExpirationDuration(10000)
        fusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.getMainLooper()
        )
    }

    val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Location permission granted.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocation(onComplete: () -> Unit, onError: () -> Unit) {
        checkForLocationPermissions({
            if (checkIfLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        viewModel.setCurrentLocation(
                            SimpleLocation(
                                location.latitude,
                                location.longitude,
                                ""
                            )
                        )
                        setAddressList(location)
                        onComplete()
                    } else {
                        requestNewLocationData(fusedLocationProviderClient, mLocationCallback)
                        onError()
                    }
                }
            } else {
                Toast.makeText(this, "Location not enabled.", Toast.LENGTH_LONG).show()
            }
        }, {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_GET_LOCATION)
            onError()
        })
    }

    private fun checkForLocationPermissions(onLocationPermissionGranted: () -> Unit, onLocationPermissionDenied: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                onLocationPermissionGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                MaterialAlertDialogBuilder(this).setTitle("Enable Location Permissions")
                    .setMessage("For locating your device using GPS. This helps us in adding your location to the post so that it can be filtered based on location. ")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            else -> {
                onLocationPermissionDenied()
            }
        }
    }

    private fun checkIfLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLocation({
                        val fragment = LocationFragment.newInstance()
                        showBottomSheet(fragment, LocationFragment.TAG)
                    }, {

                    })
                } else {
                    Toast.makeText(
                        this,
                        "Permission Denied for location.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                return
            }
            else -> {
                // Ignored all other requests.
            }
        }

    }

    override fun onInitial() {
        mainBinding.primaryProgressBar.visibility = View.VISIBLE
    }

    override fun onLoadingMore() {
//        mainBinding.primaryProgressBar.visibility = View.VISIBLE
    }

    override fun onLoaded() {
        mainBinding.primaryProgressBar.visibility = View.GONE
    }

    override fun onFinished() {
        mainBinding.primaryProgressBar.visibility = View.GONE
    }

    override fun onError() {
        mainBinding.primaryProgressBar.visibility = View.GONE
    }

    override fun onItemClick(post: Post, viewHolder: PostViewHolder?) {
        currentViewHolder = viewHolder
        when (post.type) {
            PROJECT -> {
                val instance = ProjectFragment.newInstance(post = post)
                toFragment(instance, ProjectFragment.TAG)
            }
            BLOG -> {
                val instance = BlogFragment.newInstance(post=post)
                toFragment(instance, BlogFragment.TAG)
            }
        }
//        toFragment(PostFragmentTest.newInstance(post), PostFragmentTest.TAG)
    }


    // the state before changes
    override fun onLikePressed(post: Post): Post {
        return viewModel.onLikePressed(post)
    }


    override fun onDislikePressed(post: Post): Post {
        return viewModel.onDislikePressed(post)
    }

    override fun onSavePressed(post: Post): Post {
        return viewModel.onSavePressed(post)
    }

    override fun onFollowPressed(post: Post): Post {
        return viewModel.onFollowPressed(post)
    }

    override fun onNotSignedIn(post: Post) {
        showSignInDialog(POST)
    }

    override fun onUserPressed(post: Post) {
        if (viewModel.uid != post.uid) {
            val instance = ProfileFragment.newInstance(post.uid)
            toFragment(instance, ProfileFragment.TAG)
        }
    }

    override fun onOptionClick(post: Post) {
        val tag = POST_MENU
        val item1 = GenericMenuItem(tag, "Collaborate", R.drawable.ic_baseline_playlist_add_24, 0)
        val item2 = GenericMenuItem(tag, "Share", R.drawable.ic_baseline_share_24, 1)
        val item3 = GenericMenuItem(tag, "Report", R.drawable.ic_baseline_report_24, 2)
        val item4 = GenericMenuItem(tag, "Delete", R.drawable.ic_baseline_delete_24, 3)

        val fragment = GenericMenuFragment.newInstance(tag, post.title, arrayListOf(item1, item2, item3, item4))
        showBottomSheet(fragment, tag)
    }

    override fun <T> onSearchItemClick(obj: T, clazz: Class<T>) {
        mainBinding.primarySearchLayout.transitionToStart()
        mainBinding.primarySearchBar.text.clear()
        when (clazz) {
            User::class.java -> {
                val user = obj as User
                val instance = ProfileFragment.newInstance(user = user)
                toFragment(instance, ProfileFragment.TAG)
            }
            Post::class.java -> {
                val post = obj as Post
                if (post.type == PROJECT) {
                    toFragment(ProjectFragment.newInstance(post = post), ProjectFragment.TAG)
                } else {
                    toFragment(BlogFragment.newInstance(post = post), BlogFragment.TAG)
                }
            }
        }
    }

    override fun onSearchAdded(text: String) {
        mainBinding.primarySearchBar.setText(text)
        mainBinding.primarySearchBar.setSelection(text.length)
    }

    override fun onUserPressed(userId: String) {
        toFragment(ProfileFragment.newInstance(userId), ProfileFragment.TAG)
    }

    override fun onUserPressed(user: User) {
        val currentUser = viewModel.user.value
        if (user.id == currentUser?.id) {
            toFragment(ProfileFragment.newInstance(user=currentUser), ProfileFragment.TAG)
        } else {
            toFragment(ProfileFragment.newInstance(user=user), ProfileFragment.TAG)
        }
    }

    override fun onFollowPressed(otherUser: User) {
        val currentUser = viewModel.user.value
        if (currentUser != null) {
            viewModel.onFollowPressed(currentUser, otherUser)
        } else {
            showSignInDialog(FOLLOW)
        }
    }

//    override fun onImageSelect(bundle: Bundle) {
//        selectImage(bundle)
//    }
//
//    override fun onCameraSelect() {
//
//    }
//
//    override fun onDocumentSelect() {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//        intent.type = "*/*"
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        startActivityForResult(intent, REQUEST_GET_DOCUMENT)
//    }

    override fun onImageClick(
        view: SimpleDraweeView,
        message: SimpleMessage
    ) {

        view.transitionName = message.messageId

        viewModel.extras[ImageViewFragment.ARG_MESSAGE] = message

        val fragment = ImageViewFragment.newInstance(message)
        toFragment(fragment, ImageViewFragment.TAG)

    }

    override fun onTextClick(view: View) {
        view.visibility = View.VISIBLE
        lifecycleScope.launch {
            delay(4000)
            view.visibility = View.GONE
        }
    }

    override fun onDocumentClick(message: SimpleMessage) {
        val externalDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(externalDir, message.metaData?.originalFileName!!)
        openFile(file)
    }

    private fun openFile(file: File) {
        // Get URI and MIME type of file
        val uri = FileProvider.getUriForFile(this, "com.jamid.workconnect.fileprovider", file)
        val mime = contentResolver.getType(uri)

        // Open file with user selected app
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    override fun onChatChannelClick(chatChannel: ChatChannel) {
        viewModel.extras[ChatFragment.ARG_CHAT_CHANNEL] = chatChannel
        toFragment(ChatFragment.newInstance(chatChannel), ChatFragment.TAG)
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                PRIMARY_CHANNEL_ID,
                "Mascot Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notification from Mascot"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun sendNotification(chatChannelId: String, name: String?) {
        viewModel.onNewMessageNotification(chatChannelId) { chatChannel ->
            if (name != null) {
                Snackbar.make(mainBinding.root, "$name has sent a message in ${chatChannel.postTitle}", Snackbar.LENGTH_INDEFINITE)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .setAction("Open", {
                        mainBinding.bottomNav.selectedItemId = R.id.message_navigation
                        toFragment(ChatFragment.newInstance(chatChannel), ChatFragment.TAG)
                    })
                    .setAnchorView(mainBinding.bottomNav)
                    .show()
            }

           /* val notifyBuilder = getNotificationBuilder(it, it.postTitle, content)
            notificationManager.notify(NOTIFICATION_ID, notifyBuilder.build())*/
        }
    }

    private fun getNotificationBuilder(chatChannel: ChatChannel, title: String, content: String): NotificationCompat.Builder {
        val bundle = Bundle().apply {
            putParcelable(CHAT_CHANNEL, chatChannel)
        }

        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.message_navigation)
            .setDestination(R.id.chatFragment)
            .setArguments(bundle)
            .createPendingIntent()

        return NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
    }

    private val notificationReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            // get the current bundle
            val acceptNotificationBundle = intent?.getBundleExtra(ACCEPT_PROJECT_NOTIFICATION)
            val chatNotificationBundle = intent?.getBundleExtra(CHAT_NOTIFICATION)

            if (chatNotificationBundle != null) {

                /*
                * If the current bundle is chatNotificationBundle, this notification is meant for
                * chat
                * */

                val chatChannelId = chatNotificationBundle.get(CHAT_CHANNEL_ID) as String?
                val senderId = chatNotificationBundle.get(SENDER_ID) as String?
                val senderName = chatNotificationBundle.get(SENDER_NAME) as String?

                if (fragmentStack.peek() != ChatFragment.TAG && fragmentStack.peek() != MessageFragment.TAG) {
                    if (senderId != viewModel.uid && chatChannelId != null) {
                        sendNotification(chatChannelId, senderName)
                    }
                }
            } else if (acceptNotificationBundle != null){

                /*
                * If the current bundle is acceptNotificationBundle, this notification is meant for
                * deleting local request of the user, and adding the user to the respective project
                * */

                val notificationId = acceptNotificationBundle.getString(NOTIFICATION_ID)
                val notificationContent = acceptNotificationBundle.getString(NOTIFICATION_CONTENT)
                val postId = acceptNotificationBundle.getString(POST_ID)
                val requestId = acceptNotificationBundle.getString(REQUEST_ID)
                val chatChannelId = acceptNotificationBundle.getString(CHAT_CHANNEL_ID)

                if (notificationContent != null) {
                    Snackbar.make(mainBinding.root, notificationContent, Snackbar.LENGTH_LONG)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setAnchorView(mainBinding.bottomNav)
                        .show()
                }

                if (notificationId != null && postId != null && requestId != null && chatChannelId != null) {
                    deleteLocalRequest(notificationId, postId, requestId, chatChannelId)
                }


            }

        }
    }

    private fun deleteLocalRequest(notificationId: String, postId: String, requestId: String, chatChannelId: String) {
        viewModel.deleteLocalRequest(notificationId, postId, requestId, chatChannelId)
    }

    private val tokenReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val token = intent?.extras?.getString("token")
            if (token != null) {
                viewModel.sendRegistrationTokenToServer(token)
            }
        }
    }

    companion object {
        const val TAG = "MainActivityDebug"
        private const val REQUEST_GET_IMAGE = 112
        private const val REQUEST_GET_LOCATION = 113
        private const val REQUEST_FINE_LOCATION = 114
        private const val REQUEST_GET_DOCUMENT = 115
        private const val CREATE_NEW_DOC = 116

        const val PRIMARY_CHANNEL_ID = "primary_notification_channel"

    }

    override fun onNotificationItemClick(post: Post) {
        toFragment(ProjectFragment.newInstance(post = post), ProjectFragment.TAG)
    }

    override fun onNotificationItemClick(postId: String) {
        toFragment(ProjectFragment.newInstance(id = postId), ProjectFragment.TAG)
    }

    override fun <T> onNotificationPositiveClicked(obj: T, clazz: Class<T>) {
        when (clazz) {
            SimpleNotification::class.java -> {
                val notification = obj as SimpleNotification
                viewModel.acceptProjectRequest(notification)
            }
            SimpleRequest::class.java -> {
                val request = obj as SimpleRequest
                viewModel.undoProjectRequest(request)
            }
        }
    }

    override fun <T> onNotificationNegativeClicked(obj: T, clazz: Class<T>) {
        when (clazz) {
            SimpleNotification::class.java -> {
                val notification = obj as SimpleNotification
                viewModel.declineProjectRequest(notification)
            }
            SimpleRequest::class.java -> {

            }
        }
    }

    fun toFragment(instance: Fragment, tag: String) {
        viewModel.setCurrentFragmentTag(tag)
        supportFragmentManager.beginTransaction()
            .add(mContainerId, instance, tag)
            .addToBackStack(tag)
            .commit()
    }

    override fun onRequestItemClick(post: Post) {
        toFragment(ProjectFragment.newInstance(post = post), ProjectFragment.TAG)
    }

    override fun onPositiveButtonClick(post: Post) {

    }

    override fun onNegativeButtonClick(post: Post) {

    }

    override fun onDelete(postId: String, position: Int) {
        // update list
    }

    override fun onDialogPositiveActionClicked(tag: String) {
        when (tag) {
            CREATING_PROJECT -> {

            }
            REMOVING_LOCATION -> {
                viewModel.setCurrentLocation(null)
                viewModel.setCurrentPlace(null)
            }
            CREATING_BLOG -> {

            }
            SIGN_IN_PROMPT -> {
                toFragment(SignInFragment.newInstance(), SignInFragment.TAG)
            }
        }
        hideBottomSheet()
    }

    override fun onDialogNegativeActionClicked(tag: String) {
        hideBottomSheet()
        /*when (tag) {
            CREATING_PROJECT -> {

            }
            REMOVING_LOCATION -> {

            }
        }*/
    }

    fun hideBottomSheet(pendingAction: Map<String, () -> Unit>? = null) {
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.isDraggable = true

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onDismissClick(menuTag: String) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onMenuItemClick(item: GenericMenuItem, bundle: Bundle?) {
        when (item.menuTag) {
            CREATE_MENU -> {
                when (item.id) {
                    0 -> {
                        if (viewModel.user.value == null) {
                            showSignInDialog(BLOG)
                        } else {
                            currentFragmentTag = EditorFragment.TAG
                            hasPendingTransition = true
                        }
                    }
                    1 -> {
                        if (viewModel.user.value == null) {
                            showSignInDialog(PROJECT)
                        } else {
                            currentFragmentTag = CreateProjectFragment.TAG
                            hasPendingTransition = true
                        }
                    }
                }
            }
            POST_MENU -> {

            }
            SELECT_IMAGE_MENU_USER, SELECT_IMAGE_MENU_POST -> {
                when (item.id) {
                    0 -> {
                        selectImage(bundle)
                    }
                    1 -> {
                        Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        viewModel.setCurrentCroppedImageUri(null)
                    }
                }
            }
            CHAT_MENU -> {
                when (item.id) {
                    0 -> {
                        selectImage(bundle)
                    }
                    1 -> {
                        Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        intent.type = "*/*"
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivityForResult(intent, REQUEST_GET_DOCUMENT)
                    }
                }
            }
        }
        onDismissClick(item.menuTag)
    }


}
