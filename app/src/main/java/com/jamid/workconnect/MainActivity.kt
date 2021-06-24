package com.jamid.workconnect

import android.Manifest
import android.R.attr
import android.R.attr.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.nfc.Tag
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jamid.workconnect.adapter.paging2.PostViewHolderHorizontal
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.adapter.paging3.PostFragmentTest
import com.jamid.workconnect.adapter.paging3.PostViewHolder
import com.jamid.workconnect.auth.InterestFragment
import com.jamid.workconnect.auth.SignInFragment
import com.jamid.workconnect.auth.UserDetailFragment
import com.jamid.workconnect.databinding.ActivityMainBinding
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
import kotlin.collections.ArrayList
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import androidx.annotation.NonNull
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.navigation.NavDestination
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.adapter.paging2.SimpleMessageViewHolder
import com.jamid.workconnect.explore.*


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
    GenericMenuClickListener,
    OnChipClickListener,
    ExploreClickListener {

    // TODO("Saved posts fragment is showing error if the user has no saved posts.")

    private var currentNavController: LiveData<NavController>? = null

    private lateinit var navControllerX: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

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
    var currentViewHolder: Any? = null
    var currentAdapter: Any? = null
    var currentFeedFragment: Fragment? = null

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

    lateinit var navHostFragment: NavHostFragment

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        navHostFragment = supportFragmentManager.findFragmentById(
            R.id.navHostFragment
        ) as NavHostFragment

        navControllerX = navHostFragment.navController

        navControllerX.addOnDestinationChangedListener { controller, destination, arguments ->
            currentFragmentId = destination.id
            updateUI(currentFragmentId)
            /*when (destination.id) {
                R.id.homeFragment, R.id.exploreFragment, R.id.messageFragment, R.id.notificationFragment -> {
                    currentFragmentId = destination.id
                }
            }*/
        }

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        geocoder = Geocoder(this)
        createNotificationChannel()

//        mainBinding.primaryTopBlur.setBlurredView(mainBinding.primaryAppBar)
//        mainBinding.bottomBlur.setBlurredView(mainBinding.bottomCard)

        mContainerId = R.id.navHostFragment

        // Add this function in onCreate Method
        setupBottomNavigationBar()

        viewModel.mediaDownloadResult.observe(this) {
            val downloadResult = it ?: return@observe

            when (downloadResult) {
                is Result.Error -> {
                    viewModel.setCurrentError(downloadResult.exception)
                }
                is Result.Success -> {
                    (currentViewHolder as SimpleMessageViewHolder).bind(downloadResult.data)
                }
            }

            viewModel.mediaDownloadResult.postValue(null)
        }

        if (Build.VERSION.SDK_INT <= 27) {
            // set mode to performance
            // remove blur
            // remove other stuff

            window.navigationBarColor = ContextCompat.getColor(this, R.color.navigationBarColor)
            window.statusBarColor = ContextCompat.getColor(this, R.color.transparent)

        }

        bottomSheetBehavior = setBottomSheet(mainBinding.bottomHelperView)

//        OverScrollDecoratorHelper.setUpOverScroll(mainBinding.horizontalContainer)

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

            mainBinding.bottomNavBackground.updateLayout(bottom)

            viewModel.windowInsets.postValue(Pair(top, bottom))
           /* mainBinding.primaryToolbar.updateLayout(marginTop = top)
            mainBinding.primaryToolbarContent.updateLayout(marginTop = top)
            mainBinding.horizontalContainer.updateLayout(marginTop = top + convertDpToPx(56))
            mainBinding.primaryTabs.updateLayout(marginTop = top + convertDpToPx(56))
            mainBinding.primarySearchLayout.updateLayout(marginTop = top)*/
            insets
        }


//        viewModel.windowInsets.postValue(Pair(statusBarHeight, navigationBarHeight))

        val params1 = mainBinding.bottomCard.layoutParams as CoordinatorLayout.LayoutParams
        params1.height = navigationBarHeight + convertDpToPx(56)
        params1.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
        mainBinding.bottomCard.layoutParams = params1


//        mainBinding.linearPrimaryContainer.setPadding(0, statusBarHeight, 0, 0)

        viewModel.networkErrors.observe(this) {
            if (it != null) {
                Log.e(TAG, it.localizedMessage!!)
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
//                mainBinding.primaryProgressBar.visibility = View.GONE
            }
        }

        viewModel.firebaseUser.observe(this) {
            if (it != null) {
                lifecycleScope.launch {
                    val user = viewModel.getLocalUser(it.uid)
                    if (user == null) {
                        Snackbar.make(mainBinding.root, "You haven't created your account completely. Complete this process to go back.", Snackbar.LENGTH_INDEFINITE).show()
                        navControllerX.navigate(R.id.userDetailFragment)
//                        toFragment(UserDetailFragment.newInstance(), UserDetailFragment.TAG)
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
            Log.d(BUG_TAG, "The user observer in MainActivity is still alive.")
            if (user != null) {
                // the user can login from different fragments, make sure that the search tags appear only
                // when the user is in home fragment
                if (viewModel.currentFragmentTag.value == HomeFragment.TAG) {
//                    mainBinding.horizontalContainer.visibility = View.VISIBLE
                } else {
//                    mainBinding.horizontalContainer.visibility = View.GONE
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

//        mainBinding.primarySearchLayout.setTransitionDuration(150)
        /*mainBinding.cancelSearchBtn.setOnClickListener {
            hideKeyboard(mainBinding.root)
            mainBinding.primaryTabs.visibility = View.GONE
            mainBinding.bottomCard.show()
            mainBinding.primarySearchLayout.transitionToStart()
            mainBinding.primarySearchBar.text.clear()
            mainBinding.cancelSearchBtn.requestFocus()
            onBackPressed()
        }*/

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
        /*mainBinding.userIcon.setImageURI(user?.photo)
        mainBinding.userIcon.setOnClickListener {
            if (user != null) {
                val instance = ProfileFragment.newInstance(user = user)
                toFragment(instance, ProfileFragment.TAG)
            } else {
                val fragment = SignInFragment.newInstance()
                toFragment(fragment, InterestFragment.TAG)
            }
        }*/
    }

    private fun setSearchTags() {

        lifecycleScope.launch {
            val tagsResult = viewModel.getTags(viewModel.user.value)
            when (tagsResult) {
                is Result.Error -> {
                    viewModel.setCurrentError(tagsResult.exception)
                }
                is Result.Success -> {

                    val tags = tagsResult.data

                    /*mainBinding.popularInterestsGroup.removeViews(1, mainBinding.popularInterestsGroup.childCount - 1)
                    tags.forEachIndexed { index, popularInterest ->
                        if (index < 9) {
                            addChip(index, popularInterest)
                        } else {
                            // TODO("Dynamically add numbers of search tags")
                            // currently the number of search tags is static, hence only 9 search tags and one random tag
                            // is used
                        }
                    }*/

                    /*mainBinding.firstChip.isCheckable = true
                    mainBinding.firstChip.isCheckedIconVisible = false

                    mainBinding.firstChip.setOnClickListener {
                        mainBinding.firstChip.isChecked = true
                    }*/
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

//        mainBinding.popularInterestsGroup.addView(chip)
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

                    val options = navOptions {
                        anim {
                            enter = R.anim.slide_in_right
                            exit = R.anim.slide_out_left
                            popEnter = R.anim.slide_in_left
                            popExit = R.anim.slide_out_right
                        }
                    }
                    if (hasPendingTransition) {
                        when (currentFragmentTag) {
                            CreateProjectFragment.TAG -> {
                                navControllerX.navigate(R.id.createProjectFragment, null, options)
//                                toFragment(CreateProjectFragment.newInstance(), CreateProjectFragment.TAG)
                            }
                            EditorFragment.TAG -> {
                                navControllerX.navigate(R.id.editorFragment, null, options)
//                                toFragment(EditorFragment.newInstance(), EditorFragment.TAG)
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

    private fun ViewGroup.setToolbarIfPresent() {
        for (v in this.children) {
            if (v is ViewGroup) {
                if (v is MaterialToolbar) {
                    v.setNavigationOnClickListener {
                        navControllerX.navigateUp()
                    }
                } else {
                    v.setToolbarIfPresent()
                }
            } else {
                if (v is MaterialToolbar) {
                    v.setNavigationOnClickListener {
                        navControllerX.navigateUp()
                    }
                }
            }
        }
    }

    private fun updateUI(fragmentId: Int) {
        when (fragmentId) {
            R.id.homeFragment,
            R.id.exploreFragment,
            R.id.messageFragment,
            R.id.notificationFragment -> {
                mainBinding.bottomNavBackground.visibility = View.GONE
                mainBinding.bottomCard.show()
            }
            R.id.blogFragment,
            R.id.projectFragment,
            R.id.chatFragment,
            R.id.imageViewFragment,
            R.id.createProjectFragment,
            R.id.editorFragment,
            R.id.signInFragment,
            R.id.userDetailFragment -> {
                mainBinding.bottomCard.hide()
                mainBinding.bottomNavBackground.visibility = View.GONE
            }
            R.id.projectDetailFragment,
            R.id.projectGuidelinesFragment -> {
                mainBinding.bottomNavBackground.visibility = View.VISIBLE
                mainBinding.bottomCard.hide()
            }
        }
    }


    fun MaterialButton.update(content: String? = null, enabledState: Boolean = true, visibilityState: Int = View.VISIBLE, @DrawableRes iconId: Int? = null, @ColorInt textColor: Int? = null, @ColorInt backgroundColor: Int? = null, cornerRadiusSize: Int = 18, onClickListener: ((v: MaterialButton) -> Unit)? = null) {
        text = content
        isEnabled = enabledState
        visibility = visibilityState
        icon = if (iconId != null) {
            ContextCompat.getDrawable(this@MainActivity, iconId)
        } else {
            null
        }

        if (content != null) {
            minHeight = convertDpToPx(36)
            minWidth = convertDpToPx(80)
            updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            minHeight = convertDpToPx(36)
            minWidth = convertDpToPx(36)
            updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        if (textColor != null) {
            TODO("Change this fast .. ")
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
        } else {
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.blue_500))
        }


        val states = arrayOf<IntArray>(
            intArrayOf(state_enabled),
            intArrayOf(-state_enabled),
            intArrayOf(state_checked),
            intArrayOf(-state_checked)
        )
        backgroundTintList = if (backgroundColor != null) {
            val colors = intArrayOf(
                backgroundColor,
                ContextCompat.getColor(this@MainActivity, R.color.darkerGrey),
                backgroundColor,
                Color.TRANSPARENT
            )
            ColorStateList(states, colors)
        } else {
            val colors = intArrayOf(
                ContextCompat.getColor(this@MainActivity, R.color.transparent),
                ContextCompat.getColor(this@MainActivity, R.color.darkerGrey),
                ContextCompat.getColor(this@MainActivity, R.color.transparent),
                Color.TRANSPARENT
            )
            ColorStateList(states, colors)
        }

        cornerRadius = convertDpToPx(cornerRadiusSize)

        if (onClickListener != null) {
            setOnClickListener {
                onClickListener(this)
            }
        }

    }


    // Add this member function in MainActivity
    private fun setupBottomNavigationBar() {

        /*val toolbar = findViewById<MaterialToolbar>(R.id.primaryToolbar)
        setSupportActionBar(toolbar)*/

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNavigationView.setupWithNavController(navControllerX)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home_navigation,
                R.id.explore_navigation,
                R.id.message_navigation,
                R.id.notification_navigation
                )
        )

        // List of nav graph for each of the fragment collection
        /*val navGraphIds = listOf(
            R.navigation.home_navigation,
            R.navigation.explore_navigation,
            R.navigation.message_navigation,
            R.navigation.notification_navigation
        )*/

        // Setup the bottom navigation view with a list of navigation graphs
        /*val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.navHostFragment,
            intent = intent
        )*/

        // Whenever the selected controller changes, setup the action bar.
        /*controller.observe(this) { navController ->

//            setupActionBarWithNavController(navController)

            navController.addOnDestinationChangedListener { _, destination, _ ->

            }

        }
        currentNavController = controller*/

        setFragmentManagerListeners()
    }

    private fun setFragmentManagerListeners() {
        supportFragmentManager.addOnBackStackChangedListener {
            viewModel.onFragmentTransactionComplete(supportFragmentManager.backStackEntryCount)
        }
    }

    fun setFragmentTitle(title: String, isPrimary: Boolean = false) {
        if (title.length > 20 ) {
            val ellipsizedTitle = title.substring(0..17) + "..."
//            mainBinding.primaryToolbar.title = ellipsizedTitle
        } else {
            if (isPrimary) {
//                mainBinding.primaryToolbar.title = title.uppercase()
            } else {
//                mainBinding.primaryToolbar.title = title
            }
        }

        if (!isPrimary) {
//            mainBinding.primaryToolbar.setTitleTextAppearance(this@MainActivity, R.style.ToolbarTitleSecondary)
        } else {
//            mainBinding.primaryToolbar.setTitleTextAppearance(this@MainActivity, R.style.ToolbarTitlePrimary)
        }

    }

    /*override fun onBackPressed() {
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
    }*/

    // For back navigation
    override fun onSupportNavigateUp(): Boolean {
        return navControllerX.navigateUp(appBarConfiguration)
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

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val image = it.data?.data
            viewModel.setCurrentImage(image)
            val fragment = ImageCropFragment.newInstance(currentCropConfig)
            showBottomSheet(fragment, ImageCropFragment.TAG)
        }
    }

    private fun selectImage(extras: Bundle?) {
        currentCropConfig = extras
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher.launch(intent)
//        startActivityForResult(intent, REQUEST_GET_IMAGE)
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

   /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_GET_IMAGE -> {
                    val image = data?.data
                    viewModel.setCurrentImage(image)
                    val fragment = ImageCropFragment.newInstance(currentCropConfig)
                    showBottomSheet(fragment, ImageCropFragment.TAG)
                    *//*val fragment = ImageCropFragment.newInstance(4, 3, 400, 300, "RECTANGLE")*//*
                }
                REQUEST_GET_DOCUMENT -> {
                    val doc = data?.data
                    viewModel.setCurrentDoc(doc)
                }
                SignInFragment.RC_SIGN_IN -> {
                    // The Task returned from this call is always completed, no need to attach
                    // a listener.
                    Log.d(TAG, "Signing with google .. reached main act")
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//
                }
            }
        } else {
            if (requestCode == 13) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e)
                }
            } else {
                Toast.makeText(this, "request code is not same - $requestCode", Toast.LENGTH_SHORT).show()
            }
        }
    }*/

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        viewModel.signInWithGoogle(credential)

        /*Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(this,
                OnCompleteListener<AuthResult?> { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = Firebase.auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        updateUI(null)
                    }
                })*/
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

    val requestGoogleSingInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        } else {
            Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show()
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
//        mainBinding.primaryProgressBar.visibility = View.VISIBLE
    }

    override fun onLoadingMore() {
//        mainBinding.primaryProgressBar.visibility = View.VISIBLE
    }

    override fun onLoaded() {
//        mainBinding.primaryProgressBar.visibility = View.GONE
    }

    override fun onFinished() {
//        mainBinding.primaryProgressBar.visibility = View.GONE
    }

    override fun onError() {
//        mainBinding.primaryProgressBar.visibility = View.GONE
    }

    override fun onItemClick(post: Post, viewHolder: Any?) {
        currentViewHolder = viewHolder
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        when (post.type) {
            PROJECT -> {
                navControllerX.navigate(R.id.projectFragment, Bundle().apply { putParcelable(ProjectFragment.ARG_POST, post) }, options)
                /*val instance = ProjectFragment.newInstance(post = post)
                toFragment(instance, ProjectFragment.TAG)*/
            }
            BLOG -> {
                navControllerX.navigate(R.id.blogFragment, Bundle().apply { putParcelable(BlogFragment.ARG_POST, post) }, options)
                /*val instance = BlogFragment.newInstance(post=post)
                toFragment(instance, BlogFragment.TAG)*/
            }
        }
//        toFragment(PostFragmentTest.newInstance(post), PostFragmentTest.TAG)
    }


    // the state before changes
    override fun onLikePressed(post: Post): Post {
        val returnedPost = viewModel.onLikePressed(post)
        when (currentViewHolder) {
            is PostViewHolder -> {
                (currentViewHolder as PostViewHolder).bind(returnedPost)
            }
            is PostViewHolderHorizontal -> {
                (currentViewHolder as PostViewHolderHorizontal).bind(returnedPost)
            }
        }
        return returnedPost
    }


    override fun onDislikePressed(post: Post): Post {
        val returnedPost = viewModel.onDislikePressed(post)
        when (currentViewHolder) {
            is PostViewHolder -> {
                (currentViewHolder as PostViewHolder).bind(returnedPost)
            }
            is PostViewHolderHorizontal -> {
                (currentViewHolder as PostViewHolderHorizontal).bind(returnedPost)
            }
        }
        return returnedPost
    }

    override fun onSavePressed(post: Post): Post {
        val returnedPost = viewModel.onSavePressed(post)
        when (currentViewHolder) {
            is PostViewHolder -> {
                (currentViewHolder as PostViewHolder).bind(returnedPost)
            }
            is PostViewHolderHorizontal -> {
                (currentViewHolder as PostViewHolderHorizontal).bind(returnedPost)
            }
        }
        return returnedPost
    }

    override fun onFollowPressed(post: Post): Post {
        val returnedPost = viewModel.onFollowPressed(post)
//        currentViewHolder?.bind(returnedPost)

        if (currentFeedFragment is PostsFragment) {
            (currentFeedFragment as PostsFragment).getPosts(viewModel.currentHomeTag.value)
        }

      /*  when (currentAdapter) {
            is PostAdapter -> (currentAdapter as PostAdapter).refresh()
            else -> {
                Log.d(TAG, "Could not determine current adapter class.")
            }
        }*/
        return returnedPost
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
        val item4 = GenericMenuItem(tag, "Unfollow ${post.admin.name}", R.drawable.ic_baseline_person_add_disabled_24, 3)
        val item5 = GenericMenuItem(tag, "Delete", R.drawable.ic_baseline_delete_24, 4)

        val fragment = GenericMenuFragment.newInstance(tag, post.title, arrayListOf(item1, item2, item3, item4, item5))
        showBottomSheet(fragment, tag)
    }

    override fun <T> onSearchItemClick(obj: T, clazz: Class<T>) {

        /*mainBinding.primarySearchLayout.transitionToStart()
        mainBinding.primarySearchBar.text.clear()*/

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        when (clazz) {
            User::class.java -> {
                val user = obj as User
                val recentSearch = RecentSearch(user.name, user.id, USER, recentUser = user)
                viewModel.addRecentSearch(recentSearch)

//                val instance = ProfileFragment.newInstance(user = user)
//                toFragment(instance, ProfileFragment.TAG)
                navControllerX.navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, user) }, options)
            }
            Post::class.java -> {
                val post = obj as Post
                if (post.type == PROJECT) {
                    val recentSearch = RecentSearch(post.title, post.id, post.type, recentPost = post)
                    viewModel.addRecentSearch(recentSearch)

//                    toFragment(ProjectFragment.newInstance(post = post), ProjectFragment.TAG)
                    navControllerX.navigate(R.id.projectFragment, Bundle().apply { putParcelable(ProjectFragment.ARG_POST, post) }, options)
                } else {
                    val recentSearch = RecentSearch(post.title, post.id, post.type, recentPost = post)
                    viewModel.addRecentSearch(recentSearch)

//                    toFragment(BlogFragment.newInstance(post = post), BlogFragment.TAG)
                    navControllerX.navigate(R.id.blogFragment, Bundle().apply { putParcelable(BlogFragment.ARG_POST, post) }, options)
                }
            }
        }
    }

    override fun onSearchAdded(text: String) {
        /*mainBinding.primarySearchBar.setText(text)
        mainBinding.primarySearchBar.setSelection(text.length)*/
    }

    override fun onDeleteRecentSearch(query: String) {
        viewModel.deleteRecentSearch(query)
    }

    override fun onUserPressed(userId: String) {
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        navControllerX.navigate(R.id.profileFragment, Bundle().apply { putString(ProfileFragment.ARG_UID, userId) }, options)
//        toFragment(ProfileFragment.newInstance(userId), ProfileFragment.TAG)
    }

    override fun onUserPressed(user: User) {
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        val currentUser = viewModel.user.value
        if (user.id == currentUser?.id) {
            navControllerX.navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, currentUser) }, options)
//            toFragment(ProfileFragment.newInstance(user=currentUser), ProfileFragment.TAG)
        } else {
            navControllerX.navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, user) }, options)
//            toFragment(ProfileFragment.newInstance(user=user), ProfileFragment.TAG)
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
        val extras = FragmentNavigatorExtras(view to message.messageId)

        navControllerX.navigate(
            R.id.imageViewFragment,
            Bundle().apply { putParcelable(ImageViewFragment.ARG_MESSAGE, message) },
            null,
            extras
            )

        /*navHostFragment.childFragmentManager.beginTransaction()
            .add(R.id.navHostFragment, ImageViewFragment.newInstance(message), ImageViewFragment.TAG)
            .setReorderingAllowed(true)
            .addSharedElement(view, message.messageId)
            .addToBackStack(null)
            .commit()*/

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

    override fun onImageDownloadClick(message: SimpleMessage) {
        val externalDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (externalDir != null) {
            viewModel.downloadImage(message, externalDir)
        }
    }

    override fun onUserClick(user: User) {
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        navControllerX.navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, user) }, options)
    }

    override fun onMediaDownloadClick(viewHolder: RecyclerView.ViewHolder, message: SimpleMessage) {
        currentViewHolder = viewHolder
        if (message.type == DOCUMENT) {
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let {
                viewModel.downloadMedia(it, message)
            }
        } else {
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                viewModel.downloadMedia(it, message)
            }
        }
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
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        viewModel.extras[ChatFragment.ARG_CHAT_CHANNEL] = chatChannel
        navControllerX.navigate(R.id.chatFragment, Bundle().apply { putParcelable(ChatFragment.ARG_CHAT_CHANNEL, chatChannel) }, options)
//        toFragment(ChatFragment.newInstance(chatChannel), ChatFragment.TAG)
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
                    .setAction("Open") {
                        mainBinding.bottomNav.selectedItemId = R.id.message_navigation
                        toFragment(ChatFragment.newInstance(chatChannel), ChatFragment.TAG)
                    }
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

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        navControllerX.navigate(R.id.projectFragment, Bundle().apply { putString(ProjectFragment.ARG_POST_ID,  postId)}, options)
//        toFragment(ProjectFragment.newInstance(id = postId), ProjectFragment.TAG)
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
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

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
                navControllerX.navigate(R.id.signInFragment, null, options)
//                toFragment(SignInFragment.newInstance(), SignInFragment.TAG)
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
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
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
            PROFILE_MENU -> {
                when (item.id) {
                    0 -> {
                        navControllerX.navigate(R.id.editFragment, null, options)
                    }
                    1 -> {
                        navControllerX.navigate(R.id.settingsFragment, null, options)
                    }
                    2 -> {
                        navControllerX.navigate(R.id.savedPostsFragment, null, options)
                    }
                }
            }
            OTHER_PROFILE_MENU -> {
                when (item.id) {
                    0, 1, 2, 3 -> {
                        Toast.makeText(this, "Not implemented yet.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        onDismissClick(item.menuTag)
    }

    override fun onChipClick(interest: String) {
        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }
        navControllerX.navigate(R.id.tagPostsFragment, Bundle().apply { putString(TagPostsFragment.ARG_TAG, interest) }, options)
//        toFragment(TagPostsFragment.newInstance(interest), TagFragment.TAG)
    }

    override fun onInterestSelect(interest: String) {
        viewModel.addInterest(interest)
    }

    override fun onInterestRemoved(interest: String) {
        viewModel.removeInterest(interest)
    }

    override fun <T : Any> onSeeMoreClick(clazz: Class<T>, postType: String?) {

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        when (clazz) {
            Post::class.java -> {
                if (postType != null && postType == PROJECT) {
                    navControllerX.navigate(R.id.topProjectsFragment, null, options)
//                    toFragment(TopProjectsFragment.newInstance(), TopProjectsFragment.TAG)
                } else {
                    navControllerX.navigate(R.id.topBlogsFragment, null, options)
//                    toFragment(TopBlogsFragment.newInstance(), TopBlogsFragment.TAG)
                }
            }
            User::class.java -> {
                navControllerX.navigate(R.id.randomUsersFragment, null, options)
//                toFragment(RandomUsersFragment.newInstance(), RandomUsersFragment.TAG)
            }
            TagsHolder::class.java -> {
                navControllerX.navigate(R.id.interestFragment, Bundle().apply { putBoolean(InterestFragment.ARG_IS_SIGN_IN, false) }, options)
//                toFragment(InterestFragment.newInstance(false), InterestFragment.TAG)
            }
        }
    }

}
