package com.jamid.workconnect

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.auth.AuthFragment
import com.jamid.workconnect.databinding.ActivityMainBinding
import com.jamid.workconnect.home.CreateOptionFragment
import com.jamid.workconnect.home.LocationFragment
import com.jamid.workconnect.home.TagFragment
import com.jamid.workconnect.interfaces.*
import com.jamid.workconnect.message.ChatChannelFragment
import com.jamid.workconnect.message.MessageFragment
import com.jamid.workconnect.message.MessageMenuFragment
import com.jamid.workconnect.message.ProjectDetailContainer
import com.jamid.workconnect.model.*
import com.jamid.workconnect.profile.BlogsFragment
import com.jamid.workconnect.profile.CollaborationsListFragment
import com.jamid.workconnect.profile.ProfileFragment
import com.jamid.workconnect.profile.ProjectListFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper


class MainActivity : AppCompatActivity(),
    CreateMenuListener,
    ImageSelectMenuListener,
    PostMenuClickListener,
    PostsLoadStateListener,
    PostItemClickListener,
    GenericLoadingStateListener,
    SearchItemClickListener,
    UserItemClickListener,
    ChatMenuClickListener,
    MessageItemClickListener {

    private var currentNavController: LiveData<NavController>? = null
    private val viewModel: MainViewModel by viewModels()
    lateinit var mainBinding: ActivityMainBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var initiated = false
    lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var currentBottomFragment: Fragment? = null
    private var currentFragmentId: Int = 0
    private var hasPendingTransition = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation

            viewModel.setCurrentLocation(SimpleLocation(location.latitude, location.longitude, ""))

            setAddressList(location)
        }
    }

    @SuppressLint("VisibleForTests")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        geocoder = Geocoder(this)

        // Add this function in onCreate Method
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }

        bottomSheetBehavior = BottomSheetBehavior.from(mainBinding.bottomHelperView)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN


        bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    mainBinding.scrimForBottomSheet.alpha = 1f
                    mainBinding.scrimForBottomSheet.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                } else if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    mainBinding.scrimForBottomSheet.alpha = 1f
                    mainBinding.scrimForBottomSheet.setOnClickListener {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    currentBottomFragment?.let { supportFragmentManager.beginTransaction().remove(it) }
                    mainBinding.scrimForBottomSheet.isClickable = false

                    if (hasPendingTransition) {
                        currentNavController?.value?.navigate(currentFragmentId)
                        hasPendingTransition = false
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                mainBinding.scrimForBottomSheet.alpha = slideOffset
                mainBinding.scrimForBottomSheet.isClickable = slideOffset > 0
            }

        })

        OverScrollDecoratorHelper.setUpOverScroll(mainBinding.horizontalContainer)
        
        mainBinding.root.setOnApplyWindowInsetsListener { v, insets ->
            // should only be less than
            if (Build.VERSION.SDK_INT <= 30) {
                val statusBarSize = insets.systemWindowInsetTop
                val navBarSize = insets.systemWindowInsetBottom
                viewModel.windowInsets.postValue(Pair(statusBarSize, navBarSize))
                mainBinding.bottomNav.setPadding(0, 0, 0, navBarSize)

                val params1 = mainBinding.bottomBlur.layoutParams as CoordinatorLayout.LayoutParams
                params1.height = navBarSize + convertDpToPx(56)
                params1.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
                mainBinding.bottomBlur.layoutParams = params1
            } else {

            }
            return@setOnApplyWindowInsetsListener insets
        }

        viewModel.networkErrors.observe(this) {
            if (it != null) {
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Network Error => ${it.localizedMessage}")
            }
        }


        db.collection("popular_interests").limit(10)
            .get()
            .addOnSuccessListener {
                val interests = it.toObjects(PopularInterest::class.java)
                interests.forEachIndexed { index, popularInterest ->
                    val chip = mainBinding.popularInterestsGroup.getChildAt(index + 1) as Chip
                    chip.text = popularInterest.interest
                }
            }.addOnFailureListener {
                viewModel.setCurrentError(it)
            }


        mainBinding.primarySearchBar.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                val query = it.toString()
                viewModel.setCurrentQuery(query)
            }
        }

        mainBinding.popularInterestsGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.first_chip) {
                viewModel.setCurrentHomeTag(null)
            } else {
                val child = group.findViewById<Chip>(checkedId)
                val tag = child.text.toString()
                viewModel.setCurrentHomeTag(tag)
            }
        }

        viewModel.firebaseUser.observe(this) {
            if (it != null) {
                db.collection(USERS).document(it.uid)
                    .addSnapshotListener { v, e ->
                        if (e != null) {
                            viewModel.setCurrentError(e)
                            return@addSnapshotListener
                        }

                        if (v != null && v.exists()) {
                            val user = v.toObject(User::class.java)!!
                            viewModel.setCurrentUser(user)
                            Log.d("MAIN_ACTIVITY", "Inserting con")
                            if (!initiated) {
                                Log.d("MAIN_ACTIVITY", "Inserting contributor")
                                viewModel.insertChatChannelContributors(user)
                                initiated = true
                            }
                        }
                    }
            }
        }

        viewModel.user.observe(this) { user ->
            if (user != null) {
                mainBinding.userIcon.setImageURI(user.photo)

                mainBinding.userIcon.setOnClickListener {

                    val bundle = Bundle().apply {
                        putParcelable(ProfileFragment.ARG_USER, user)
                    }
                    currentNavController?.value?.navigate(R.id.profileFragment, bundle)
                /*  val profileFragment = ProfileFragment.newInstance(user = user)
                    supportFragmentManager.beginTransaction().add(android.R.id.content, profileFragment, "ProfileFragment")
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                        .addToBackStack("ProfileFragment")
                        .commit() */
                }
            } else {
                mainBinding.userIcon.setOnClickListener {
                    val fragment = AuthFragment.newInstance()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.dynamicViewHolder, fragment, "AuthFragment")
                        .commit()
                    currentBottomFragment = fragment
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            for (fragment in supportFragmentManager.fragments) {
                if (fragment.isVisible && (fragment is ChatChannelFragment || fragment is MessageFragment)) {
                    if (mainBinding.bottomNav.translationY != 0f) {
                        mainBinding.bottomNav.show(mainBinding.bottomBlur)
                    }
                } else if (fragment.isVisible && (fragment is ProfileFragment || fragment is ProjectListFragment || fragment is CollaborationsListFragment || fragment is BlogsFragment)) {
                    if (mainBinding.bottomNav.translationY != 0f) {
                        mainBinding.bottomNav.show(mainBinding.bottomBlur)
                    }
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
            R.navigation.message_navigation
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

            mainBinding.primaryToolbar.setNavigationOnClickListener {
                hideKeyboard(mainBinding.root)
                navController.navigateUp()
            }

            navController.addOnDestinationChangedListener { controller1, destination, arguments ->
                if (destination.id == R.id.homeFragment || destination.id == R.id.messageFragment || destination.id == R.id.exploreFragment) {
                    viewModel.setCurrentFragmentId(destination.id)
                    mainBinding.userIcon.visibility = View.VISIBLE
                } else {
                    mainBinding.userIcon.visibility = View.GONE
                }

                val autoTransition = AutoTransition()
                autoTransition.duration = 150

                when (destination.id) {
                    R.id.homeFragment -> {
                        if (mainBinding.primaryAppBar.translationY != 0f) {
                            mainBinding.primaryAppBar.show()
                        }
                        mainBinding.primaryTitle.text = "Codesquare"
                        mainBinding.bottomNav.show(mainBinding.bottomBlur)
                        mainBinding.primaryToolbar.visibility = View.VISIBLE
                        mainBinding.primarySearchLayout.visibility = View.GONE
                        mainBinding.horizontalContainer.visibility = View.VISIBLE
                        mainBinding.primaryTabs.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar, autoTransition)
                    }
                    R.id.blogFragment -> {
                        if (mainBinding.primaryAppBar.translationY != 0f) {
                            mainBinding.primaryAppBar.show()
                        }
                        mainBinding.primaryTitle.text = ""
                        if (mainBinding.bottomNav.translationY == 0f) {
                            mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                        }
                        mainBinding.primaryToolbar.visibility = View.VISIBLE
                        mainBinding.primarySearchLayout.visibility = View.GONE
                        mainBinding.horizontalContainer.visibility = View.GONE
                        mainBinding.primaryTabs.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar, autoTransition)
                    }
                    R.id.projectFragment -> {
                        if (mainBinding.primaryAppBar.translationY != 0f) {
                            mainBinding.primaryAppBar.show()
                        }
                        mainBinding.primaryTitle.text = ""
                        if (mainBinding.bottomNav.translationY == 0f) {
                            mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                        }
                        mainBinding.primaryToolbar.visibility = View.VISIBLE
                        mainBinding.primarySearchLayout.visibility = View.GONE
                        mainBinding.horizontalContainer.visibility = View.GONE
                        mainBinding.primaryTabs.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar, autoTransition)
                    }
                    R.id.exploreFragment -> {
                        if (mainBinding.primaryAppBar.translationY != 0f) {
                            mainBinding.primaryAppBar.show()
                        }
                        mainBinding.primaryTitle.text = "Explore"
                        mainBinding.bottomNav.show(mainBinding.bottomBlur)
                        mainBinding.primaryToolbar.visibility = View.GONE
                        mainBinding.primarySearchLayout.visibility = View.VISIBLE
                        mainBinding.horizontalContainer.visibility = View.GONE
                        mainBinding.primaryTabs.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar, autoTransition)
                    }
                    R.id.messageFragment -> {
                        if (mainBinding.primaryAppBar.translationY != 0f) {
                            mainBinding.primaryAppBar.show()
                        }
                        mainBinding.primaryTitle.text = "Messages"
                        mainBinding.bottomNav.show(mainBinding.bottomBlur)
                        mainBinding.primaryToolbar.visibility = View.VISIBLE
                        mainBinding.primarySearchLayout.visibility = View.GONE
                        mainBinding.horizontalContainer.visibility = View.GONE
                        mainBinding.primaryTabs.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar, autoTransition)
                    }
                    R.id.profileFragment -> {
                        mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                        mainBinding.horizontalContainer.visibility = View.GONE
                        mainBinding.primaryAppBar.hide()
                    }
                    R.id.editorFragment -> {
                        mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                        mainBinding.primaryTitle.text = "Create Blog"
                        mainBinding.primaryToolbar.visibility = View.VISIBLE
                        mainBinding.primarySearchLayout.visibility = View.GONE
                        mainBinding.horizontalContainer.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar, autoTransition)
                    }
                    R.id.searchFragment -> {

                    }
                    R.id.createProjectFragment -> {
                        mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                        mainBinding.primaryTitle.text = "Create Project"
                        mainBinding.primaryToolbar.visibility = View.VISIBLE
                        mainBinding.primarySearchLayout.visibility = View.GONE
                        mainBinding.horizontalContainer.visibility = View.GONE
                        TransitionManager.beginDelayedTransition(mainBinding.primaryAppBar)
                    }
                    R.id.editFragment -> {
                        mainBinding.primaryAppBar.show()
                        mainBinding.horizontalContainer.visibility = View.GONE
                        mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                        mainBinding.primaryTitle.text = "Edit Profile"
                    }
                    R.id.chatFragment -> {
                        mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                    }
                    R.id.savedPostsFragment -> {
                        mainBinding.bottomNav.hide(mainBinding.bottomBlur)
                    }
                }
            }

        }
        currentNavController = controller
    }

    // For back navigation
    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    override fun onCreateBlog() {
        if (auth.currentUser == null) {
            showSignInDialog()
        } else {
            currentFragmentId = R.id.editorFragment
            hasPendingTransition = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onCreateProject() {
        if (auth.currentUser == null) {
            showSignInDialog()
        } else {
            currentFragmentId = R.id.createProjectFragment
            hasPendingTransition = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun showSignInDialog() {
        MaterialAlertDialogBuilder(this).setTitle("Sign in or Register")
            .setMessage("You are not signed in. To create a blog, you must have an account.")
            .setPositiveButton("Sign In"){ _, _ ->
                invokeSignIn()
            }.setNegativeButton("Cancel") { d, _ ->
                d.dismiss()
            }.show()
    }


    fun invokeSignIn() {
        val fragment = AuthFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, AuthFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun invokeImageSelectOptions() {
        val fragment = ImageSelectFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, ImageSelectFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun invokeCropFragment(x: Int = 1, y: Int = 1, width: Int = 400, height: Int = 300, shape: String = "OVAL") {
        val fragment = ImageCropFragment.newInstance(x, y, width, height, shape)
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, ImageCropFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun postInvokeLocationFragment() {
        val fragment = LocationFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, LocationFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun invokeLocationFragment() {
        getLocation({
            postInvokeLocationFragment()
        }, {
            //
        })
    }

    fun invokeTagFragment() {
        val fragment = TagFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, TagFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun invokePostMenu(post: Post) {
        val fragment = PostMenuFragment.newInstance(post)
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, PostMenuFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun invokeMessageMenu() {
        val fragment = MessageMenuFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, fragment, PostMenuFragment.TAG)
            .commit()

        currentBottomFragment = fragment
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun invokeCreateOption() {
        val menu = CreateOptionFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.dynamicViewHolder, menu, CreateOptionFragment.TAG)
            .commit()

        currentBottomFragment = menu
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun projectDetailFragment(chatChannel: ChatChannel, currentContributor: ChatChannelContributor) {
        val fragment = ProjectDetailContainer.newInstance(chatChannel, currentContributor)
        supportFragmentManager.beginTransaction()
            .add(R.id.navHostFragment, fragment, ProjectDetailContainer.TAG)
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
            .commit()

        currentBottomFragment = fragment
    }

    private fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, REQUEST_GET_IMAGE)
    }

    override fun onSelectImageFromGallery() {
        selectImage()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onCaptureEvent() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

//    TODO("This function is not implemented yet")
    override fun onImageRemove() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_GET_IMAGE -> {
                    val image = data?.data
                    viewModel.setCurrentImage(image)
                    invokeCropFragment(4, 3, 400, 300, "RECTANGLE")
                }
            }
        } else {
            // TODO("on unsuccessful attempt")
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
            }
        }, {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_GET_LOCATION)
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
                        postInvokeLocationFragment()
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


    override fun onCollaborateClick(post: Post) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onShareClick(post: Post) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onDeleteClick(post: Post) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onReportClick(post: Post) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
        Toast.makeText(this, "Error occurred.", Toast.LENGTH_SHORT).show()
    }

    override fun onItemClick(post: Post) {
        when (post.type) {
            PROJECT -> {
                val bundle = Bundle().apply {
                    putParcelable(ProjectFragment.ARG_POST, post)
                }
                currentNavController?.value?.navigate(R.id.projectFragment, bundle)
            }
            BLOG -> {
                val bundle = Bundle().apply {
                    putParcelable(BlogFragment.ARG_POST, post)
                }
                currentNavController?.value?.navigate(R.id.blogFragment, bundle)
            }
        }
    }

    override fun onLikePressed(post: Post, prevL: Boolean, prevD: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onLikePressed(post, prevL, prevD)
        } else {
            currentNavController?.value?.navigate(R.id.signInFragment)
        }
    }

    override fun onDislikePressed(post: Post, prevL: Boolean, prevD: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onDislikePressed(post, prevL, prevD)
        } else {
            currentNavController?.value?.navigate(R.id.signInFragment)
        }
    }

    override fun onSavePressed(post: Post, prev: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onSavePressed(post, prev)
        } else {
            currentNavController?.value?.navigate(R.id.signInFragment)
        }
    }

    override fun onFollowPressed(post: Post, prev: Boolean) {
        if (auth.currentUser != null) {
            viewModel.onFollowPressed(post.uid, prev)
        } else {
            invokeSignIn()
        }
    }

    override fun onUserPressed(post: Post) {
        if (auth.currentUser?.uid != post.uid) {
            val bundle = Bundle().apply {
                putString(ProfileFragment.ARG_UID, post.uid)
            }

            currentNavController?.value?.navigate(R.id.profileFragment, bundle)
        }
    }

    override fun onOptionClick(post: Post) {
        invokePostMenu(post)
    }

    override fun onSearchItemClick(id: String, type: String?) {
        when (type) {
            null -> {
                val bundle = Bundle().apply {
                    putString(ProfileFragment.ARG_UID, id)
                }
                currentNavController?.value?.navigate(R.id.profileFragment, bundle)
            }
            "Project" -> {
                val bundle = Bundle().apply {
                    putString(ProjectFragment.ARG_POST_ID, id)
                }
                currentNavController?.value?.navigate(R.id.projectFragment, bundle)
            }
            "Blog" -> {
                val bundle = Bundle().apply {
                    putString(BlogFragment.ARG_POST_ID, id)
                }
                currentNavController?.value?.navigate(R.id.blogFragment, bundle)
            }
        }
    }

    override fun onSearchAdded(text: String) {
        mainBinding.primarySearchBar.setText(text)
        mainBinding.primarySearchBar.setSelection(text.length)
    }

    override fun onUserPressed(userId: String) {
        val bundle = Bundle().apply {
            putString(ProfileFragment.ARG_UID, userId)
        }
        currentNavController?.value?.navigate(R.id.profileFragment, bundle)
    }

    companion object {
        const val TAG = "MainActivityDebug"
        private const val REQUEST_GET_IMAGE = 112
        private const val REQUEST_GET_LOCATION = 113
        private const val REQUEST_FINE_LOCATION = 114
    }

    override fun onImageSelect() {
        selectImage()
    }

    override fun onCameraSelect() {

    }

    override fun onDocumentSelect() {

    }

    override fun onImageClick(
        view: SimpleDraweeView,
        actualWidth: Int,
        actualHeight: Int,
        message: SimpleMessage
    ) {

    }

    override fun onTextClick(content: String) {

    }

}
