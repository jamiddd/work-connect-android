package com.jamid.workconnect

import android.app.Application
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.jamid.workconnect.auth.SignInFormResult
import com.jamid.workconnect.data.*
import com.jamid.workconnect.home.BlogItem
import com.jamid.workconnect.model.*
import com.jamid.workconnect.model.ObjectType.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val _signInResult = MutableLiveData<Result<FirebaseUser>>()
    val signInResult: LiveData<Result<FirebaseUser>> = _signInResult

    private val _registerResult = MutableLiveData<Result<FirebaseUser>>()
    val registerResult: LiveData<Result<FirebaseUser>> = _registerResult

    private val _emailExists = MutableLiveData<Boolean>().apply { value = false }
    val emailExists: LiveData<Boolean> = _emailExists

    private val _currentImageUri = MutableLiveData<Uri>()
    val currentImageUri: LiveData<Uri> = _currentImageUri

    private val _currentDocUri = MutableLiveData<Uri>()
    val currentDocUri: LiveData<Uri> = _currentDocUri

    private val _currentCroppedImageUri = MutableLiveData<Uri>()
    val currentCroppedImageUri: LiveData<Uri> = _currentCroppedImageUri

    private val _currentPrimaryFragmentId = MutableLiveData<Int>()
    val currentPrimaryFragmentId: LiveData<Int> = _currentPrimaryFragmentId

    private val _firebaseUser = MutableLiveData<FirebaseUser>()
    val firebaseUser: LiveData<FirebaseUser> = _firebaseUser

    private val _user = MutableLiveData<User>().apply {
        value = null
    }
    val user: LiveData<User> = _user

    private val _networkErrors = MutableLiveData<Exception>()
    val networkErrors: LiveData<Exception> = _networkErrors

    val likesMap = MutableLiveData<MutableMap<String, Boolean>>()
    val dislikesMap = MutableLiveData<MutableMap<String, Boolean>>()
    val savesMap = MutableLiveData<MutableMap<String, Boolean>>()
    val followingsMap = MutableLiveData<MutableMap<String, Boolean>>()
    val likesCountMap = MutableLiveData<MutableMap<String, Long>>()
    val dislikesCountMap = MutableLiveData<MutableMap<String, Long>>()

    val windowInsets = MutableLiveData<Pair<Int, Int>>().apply {
        value = Pair(0, 0)
    }

    val hasConversationsUpdated = MutableLiveData<Boolean>().apply {
        value = false
    }


    fun updateLikesMap(postId: String, res: Boolean) {
        val prevMap = likesMap.value ?: mutableMapOf()
        prevMap[postId] = res
        likesMap.postValue(prevMap)
    }

    fun updateDislikesMap(postId: String, res: Boolean) {
        val prevMap = dislikesMap.value ?: mutableMapOf()
        prevMap[postId] = res
        dislikesMap.postValue(prevMap)
    }

    fun updateSavesMap(postId: String, res: Boolean) {
        val prevMap = savesMap.value ?: mutableMapOf()
        prevMap[postId] = res
        savesMap.postValue(prevMap)
    }

    fun updateFollowingsMap(userId: String, res: Boolean) {
        val prevMap = followingsMap.value ?: mutableMapOf()
        prevMap[userId] = res
        followingsMap.postValue(prevMap)
    }

    fun updateLikesCountMap(postId: String, res: Long) {
        val prevMap = likesCountMap.value ?: mutableMapOf()
        prevMap[postId] = res
        likesCountMap.postValue(prevMap)
    }

    fun updateDislikesCountMap(postId: String, res: Long) {
        val prevMap = dislikesCountMap.value ?: mutableMapOf()
        prevMap[postId] = res
        dislikesCountMap.postValue(prevMap)
    }

    fun setFirebaseUser(user: FirebaseUser?) {
        _firebaseUser.postValue(user)
    }

    private val repo : MainRepository
    private val database: WorkConnectDatabase

    init {
        val currentUser = auth.currentUser
        _firebaseUser.postValue(currentUser)
        /*if (currentUser != null) {
            db.collection(USERS).document(currentUser.uid)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        _networkErrors.postValue(error)
                    }

                    if (value != null && value.exists()) {
                        val user = value.toObject(User::class.java)
                        _user.postValue(user)
                    }
                }
        }*/

        database = WorkConnectDatabase.getInstance(application.applicationContext, viewModelScope)
        repo = MainRepository(database)
    }

    val config = PagedList.Config.Builder()
        .setEnablePlaceholders(false)
        .setPageSize(50)
        .setPrefetchDistance(5)
        .build()

    private val currentChatChannelId = MutableLiveData<String>().apply {
        value = ""
    }

    fun chatMessages(chatChannelId: String) = database.messageDao()
        .getLiveChatMessages(chatChannelId)
        /*toLiveData(50, boundaryCallback = MessageBoundaryCallback(chatChannelId, repo, viewModelScope))*/
        .toLiveData(config, null, MessageBoundaryCallback(chatChannelId, repo, viewModelScope))

    fun createNewUser(interests: List<String>) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val name = currentUser.displayName
            val email = currentUser.email
            val photo: String? = if (currentUser.photoUrl == null) {
                null
            } else {
                currentUser.photoUrl.toString()
            }

            val usernameExists = userNameExists.value
            val username = email!!.split('@')[0]
            val user = if (usernameExists != null) {
                if (usernameExists) {
                    val newUsername = username + System.currentTimeMillis().toString()
                    User(uid, name!!, newUsername, email, null, photo, interests, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis())
                } else {
                    User(uid, name!!, username, email, null, photo, interests, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis())
                }
            } else {
                User(uid, name!!, username, email, null, photo, interests, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis())
            }

            db.collection(USERS).document(uid).set(user)
                .addOnSuccessListener {
                    _user.postValue(user)

                    val userMinimal = UserMinimal(uid, name, email, user.username, photo)

                    val searchRef = db.collection(USERS_SEARCH).document(uid)

                    val listOfSubStrings = arrayListOf<String>()
                    for (i in 1 until name.length) {
                        listOfSubStrings.add(name.substring(0..i))
                    }

                    for (i in 1 until username.length) {
                        listOfSubStrings.add(username.substring(0..i))
                    }

                    val searchMap = SearchResult(uid, listOfSubStrings, 0, user.name, user.photo, null)

                    db.runBatch {
                        it.set(searchRef, searchMap)
                        it.set(db.collection(USER_MINIMALS).document(uid), userMinimal)
                    }.addOnSuccessListener {
                        //
                    }.addOnFailureListener {
                        _networkErrors.postValue(it)
                    }
                }
                .addOnFailureListener {
                    _networkErrors.postValue(it)
                }
        }
    }

    fun setCurrentUser(user: User?) {
        _user.postValue(user)
    }

    fun setCurrentImage(uri: Uri?) {
        _currentImageUri.postValue(uri)
    }

    fun setCurrentCroppedImageUri(uri: Uri?) {
        _currentCroppedImageUri.postValue(uri)
    }

    fun setCurrentFragmentId(id: Int) {
        _currentPrimaryFragmentId.postValue(id)
    }

    private val _signInFormResult = MutableLiveData<SignInFormResult>()
    val signInFormResult: LiveData<SignInFormResult> = _signInFormResult

    fun validateSignInForm(email: String?, password: String?) {
        when {
            email.isNullOrBlank() -> {
                _signInFormResult.postValue(SignInFormResult(emailError = "Email cannot be empty."))
            }
            !email.isValidEmail() -> {
                _signInFormResult.postValue(SignInFormResult(emailError = "Email is not valid."))
            }
            password.isNullOrBlank() -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password cannot be empty."))
            }
            !password.isValidPassword() -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password must contain 1 Capital letter, 1 Small letter, 1 Number, 1 Special character and it must be 8 characters long"))
            }
            else -> {
                _signInFormResult.postValue(SignInFormResult(isValid = true))
            }
        }
    }

    private fun CharSequence?.isValidEmail() =
        !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    private fun CharSequence?.isValidPassword() =
        !isNullOrEmpty() && Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=]).{4,}\$").matcher(this).matches()

    fun checkIfEmailExists(email: String) {
        if (email.isValidEmail()) {
            auth.signInWithEmailAndPassword(email, "12345678")
                .addOnFailureListener {
                    when (it) {
                        is FirebaseAuthEmailException -> {
                            Log.d(TAG, "FirebaseAuthEmailException - ${it.localizedMessage}")
                            _emailExists.postValue(true)
                        }
                        is FirebaseAuthInvalidUserException -> {
                            _emailExists.postValue(false)
                            return@addOnFailureListener
                        }
                        else -> {
                            Log.d(TAG, "Unknown Error - ${it.localizedMessage}")
                            _emailExists.postValue(true)
                        }
                    }
                }
                .addOnCanceledListener {
                    Log.d(TAG, "Canceled sign in.")
                }

        }
    }

    private val _userNameExists = MutableLiveData<Boolean>()
    val userNameExists: LiveData<Boolean> = _userNameExists

    fun checkIfUsernameExists(username: String) {
        val user = user.value ?: return
        if (user.username == username) {
            _userNameExists.postValue(false)
            return
        }

        db.collection("userMinimals").whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener {  qs ->
                if (qs.isEmpty) {
                    _userNameExists.postValue(false)
                } else {
                    _userNameExists.postValue(true)
                }
            }.addOnFailureListener {
                _networkErrors.postValue(it)
            }
    }

    fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (it != null) {
                    val result = Result.Success(it.user!!)
                    _signInResult.postValue(result)
                }
            }
            .addOnFailureListener {
                val result = Result.Error(it)
                _signInResult.postValue(result)
            }
    }

    fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val result = Result.Success(it.user!!)
                _registerResult.postValue(result)
            }
            .addOnFailureListener {
                val result = Result.Error(it)
                _registerResult.postValue(result)
            }
    }

    private val _firebaseUserUpdateResult = MutableLiveData<Result<FirebaseUser>>()
    val firebaseUserUpdateResult: LiveData<Result<FirebaseUser>> = _firebaseUserUpdateResult

    fun updateFirebaseUser(downloadUrl: Uri? = null, fullName: String? = null) {
        val currentUser = firebaseUser.value
        if (currentUser != null) {

            val profileUpdates = userProfileChangeRequest {
                displayName = fullName
                photoUri = downloadUrl
            }

            currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    val user = auth.currentUser
                    if (user != null) {
                        _firebaseUserUpdateResult.postValue(Result.Success(user))
                    }
                }
                .addOnFailureListener {
                    _firebaseUserUpdateResult.postValue(Result.Error(it))
                }
        }
    }


    private val _profilePhotoUploadResult = MutableLiveData<Result<Uri>>()
    val profilePhotoUploadResult: LiveData<Result<Uri>> = _profilePhotoUploadResult

    fun uploadProfilePhoto(path: String?) {
        if (path != null) {
            val stream = FileInputStream(File(path))
            val storageRef = storage.reference
            val randomName = "Image_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val imageRef = storageRef.child("${currentUser.uid}/images/profile/$randomName.jpg")

                imageRef.putStream(stream)
                    .addOnSuccessListener { taskSnapshot ->
                        val data = taskSnapshot.bytesTransferred
                        imageRef.downloadUrl
                            .addOnSuccessListener {
                                _profilePhotoUploadResult.postValue(Result.Success(it))
                            }
                            .addOnFailureListener {
                                _profilePhotoUploadResult.postValue(Result.Error(it))
                            }
                        Log.d(TAG, "Total data transferred - $data")
                    }
                    .addOnFailureListener {
                        _networkErrors.postValue(it)
                    }
            }
        }
    }

    private val _addressList = MutableLiveData<List<String>>()
    val addressList: LiveData<List<String>> = _addressList

    private val _currentPlace = MutableLiveData<String>()
    val currentPlace: LiveData<String> = _currentPlace

    private val _currentLocation = MutableLiveData<SimpleLocation>()
    val currentLocation: LiveData<SimpleLocation> = _currentLocation

    fun setAddressList(list: List<String>) {
        _addressList.postValue(list)
    }

    fun setCurrentPlace(place: String? = null) {
        _currentPlace.postValue(place)
    }

    fun setCurrentLocation(location: SimpleLocation?) {
        _currentLocation.postValue(location)
    }

    private val _tag = MutableLiveData<String>()
    val tag: LiveData<String> = _tag

    fun setTag(tag: String?) {
        _tag.postValue(tag)
    }

    fun clearPostChanges() = viewModelScope.launch (Dispatchers.IO) {
        _postUploadResult.postValue(null)
        _postPhotoUploadResult.postValue(null)
        _currentCroppedImageUri.postValue(null)
        _currentImageUri.postValue(null)
        _currentPlace.postValue(null)
        _tag.postValue(null)
    }

    private val _blogFragmentData = MutableLiveData<ArrayList<BlogItem>>()
    val blogFragmentData: LiveData<ArrayList<BlogItem>> = _blogFragmentData

    fun setBlogFragmentData(blogItemsList: ArrayList<BlogItem>?) {
        _blogFragmentData.postValue(blogItemsList)
    }

    private val _postPhotoUploadResult = MutableLiveData<Result<String>>()
    val postPhotoUploadResult: LiveData<Result<String>> = _postPhotoUploadResult

    fun uploadPostImage(path: String?, type: ObjectType) {
        if (path != null) {
            val stream = FileInputStream(File(path))
            val storageRef = storage.reference
            val randomName = "Image_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())

            val currentUser = auth.currentUser
            if (currentUser != null) {

                val imageRef = when (type) {
                    Blog -> storageRef.child("${currentUser.uid}/images/blog/$randomName.jpg")
                    Project -> storageRef.child("${currentUser.uid}/images/post/$randomName.jpg")
                }
                imageRef.putStream(stream)
                    .addOnSuccessListener { taskSnapshot ->
                        val data = taskSnapshot.bytesTransferred
                        imageRef.downloadUrl
                            .addOnSuccessListener {
                                _postPhotoUploadResult.postValue(Result.Success(it.toString()))
                            }
                            .addOnFailureListener {
                                _postPhotoUploadResult.postValue(Result.Error(it))
                            }
                        Log.d(TAG, "Total data transferred - $data")
                    }
                    .addOnFailureListener {
                        _networkErrors.postValue(it)
                    }
            }
        }
    }


    private val _likePressedListener = MutableLiveData<Post>()
    val likePressedListener: LiveData<Post> = _likePressedListener

    fun onLikePressed(post: Post, prevL: Boolean, prevD: Boolean) {

        updateLikesCountMap(post.id, post.likes)

        val map = if (prevL) {
            updateLikesMap(post.id, false)
            mapOf("likes" to post.likes)
        } else {
            updateLikesMap(post.id, true)
            if (prevD) {
                updateDislikesMap(post.id, false)
                updateDislikesCountMap(post.id, post.dislikes)
                mapOf("likes" to post.likes, "dislikes" to post.dislikes)
            } else {
                mapOf("likes" to post.likes)
            }
        }

        val uid = auth.currentUser?.uid ?: return

        db.runBatch {
            it.update(db.collection("posts").document(post.id), map)
            it.update(db.collection("users").document(uid), "likedPosts", FieldValue.arrayUnion(post.id))
            if (prevD) {
                it.update(db.collection("users").document(uid), "dislikedPosts", FieldValue.arrayRemove(post.id))
            }
        }.addOnFailureListener {
            _networkErrors.postValue(it)
        }.addOnSuccessListener {
            _likePressedListener.postValue(post)
        }

    }

    private val _dislikePressedListener = MutableLiveData<Post>()
    val dislikePressedListener: LiveData<Post> = _dislikePressedListener

    fun onDislikePressed(post: Post, prevL: Boolean, prevD: Boolean) {

        updateDislikesCountMap(post.id, post.dislikes)

        val map = if (prevD) {
            updateDislikesMap(post.id, false)
            mapOf("dislikes" to post.dislikes)
        } else {
            updateDislikesMap(post.id, true)
            if (prevL) {
                updateLikesMap(post.id, false)
                updateLikesCountMap(post.id, post.likes)
                mapOf("likes" to post.likes, "dislikes" to post.dislikes)
            } else {
                mapOf("dislikes" to post.dislikes)
            }
        }

        val uid = auth.currentUser?.uid ?: return

        db.runBatch {
            it.update(db.collection("posts").document(post.id), map)
            it.update(db.collection("users").document(uid), "dislikedPosts", FieldValue.arrayUnion(post.id))
            if (prevL) {
                it.update(db.collection("users").document(uid), "likedPosts", FieldValue.arrayRemove(post.id))
            }
        }.addOnFailureListener {
            _networkErrors.postValue(it)
        }.addOnSuccessListener {
            _dislikePressedListener.postValue(post)
        }
    }

    private val _onSaveListener = MutableLiveData<Post>()
    val onSaveListener: LiveData<Post> = _onSaveListener

    fun onSavePressed(post: Post, prev: Boolean) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val userRef = db.collection("users").document(uid)
            val task = if (prev) {
                updateSavesMap(post.id, false)
                userRef.update("savedPosts", FieldValue.arrayRemove(post.id))
            } else {
                updateSavesMap(post.id, true)
                userRef.update("savedPosts", FieldValue.arrayUnion(post.id))
            }

            task.addOnSuccessListener {
                _onSaveListener.postValue(post)
            }.addOnFailureListener {
                _networkErrors.postValue(it)
            }
        }
    }


    fun onFollowPressed(userId: String, prev: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)
            val otherUserRef = db.collection("users").document(userId)
            val task = if (prev) {
                updateFollowingsMap(userId, false)
                db.runBatch {
                    it.update(userRef, "followings", FieldValue.arrayRemove(userId))
                    it.update(otherUserRef, "followers", FieldValue.arrayRemove(currentUser.uid))
                }
            } else {
                updateFollowingsMap(userId, false)
                db.runBatch {
                    it.update(userRef, "followings", FieldValue.arrayUnion(userId))
                    it.update(otherUserRef, "followers", FieldValue.arrayUnion(currentUser.uid))
                }
            }

            task.addOnFailureListener {
                _networkErrors.postValue(it)
            }

        }
    }

    fun setLikePressedListener(post: Post?) {
        _likePressedListener.postValue(post)
    }

    fun setDislikePressedListener(post: Post?) {
        _dislikePressedListener.postValue(post)
    }


    fun signOut() {
        auth.signOut()
        _firebaseUser.postValue(null)
        _user.postValue(null)
    }

    private val _postUploadResult = MutableLiveData<Result<Post>>()
    val postUploadResult: LiveData<Result<Post>> = _postUploadResult

    fun upload(title: String, content: String? = null, type: ObjectType, image: String? = null, tags: List<String> = emptyList(), links: List<String> = emptyList(), items: List<BlogItem>? = null) {

        val currentUser = user.value ?: return

        val map = mapOf(
            NAME to currentUser.name,
            USERNAME to currentUser.username,
            PHOTO to currentUser.photo
        )

        val ref = db.collection(POSTS).document()
        val postId = ref.id
        val now = System.currentTimeMillis()
        val chatChannelRef = db.collection(CHAT_CHANNELS).document()
        val chatChannelRefId = chatChannelRef.id

        val location = if (currentLocation.value == null) {
            null
        } else {
            if (currentLocation.value?.place.isNullOrBlank()) {
                null
            } else {
                currentLocation.value
            }
        }

        val post = when (type) {
            Blog -> Post(postId, title, null, null, map, location, tags, links, currentUser.id, 0, 0, now, now, chatChannelRefId, "", null, items, "Blog")
            Project -> Post(postId, title, content, image, map, location, tags, links, currentUser.id, 0, 0, now, now, chatChannelRefId, "", null, null, "Project")
        }

        ref.set(post)
            .addOnSuccessListener {
                // add post id to user
                _postUploadResult.postValue(Result.Success(post))

                db.runBatch {

                    val postSearchRef = db.collection(POSTS_SEARCH).document(postId)

                    val listOfSubStrings = arrayListOf<String>()

                    for (i in 1 until title.length) {
                        listOfSubStrings.add(title.substring(0..i))
                    }

                    for (tag in tags) {
                        for (i in 1 until tag.length) {
                            listOfSubStrings.add(tag.substring(0..i))
                        }
                    }

                    val searchMap = if (type == Project) {
                        SearchResult(postId, listOfSubStrings, 0, post.title, post.thumbnail, "Project")
                    } else {
                        SearchResult(postId, listOfSubStrings, 0, post.title, post.thumbnail, "Blog")
                    }

                    it.set(postSearchRef, searchMap)

                    if (type == Project) {
                       it.update(db.collection(USERS).document(currentUser.id), PROJECT_IDS, FieldValue.arrayUnion(postId))
                       it.update(db.collection(USERS).document(currentUser.id), CHAT_CHANNELS, FieldValue.arrayUnion(chatChannelRefId))
                       val chatChannel = ChatChannel(chatChannelRefId, postId, title, image, listOf(currentUser.id), now, now, null)
                       it.set(chatChannelRef, chatChannel)
                       val map1 = mapOf(
                           ID to currentUser.id,
                           NAME to currentUser.name,
                           USERNAME to currentUser.username,
                           PHOTO to currentUser.photo,
                           ADMIN to true
                       )
                       it.set(chatChannelRef.collection(CONTRIBUTORS).document(currentUser.id), map1)
                   } else {
                       it.update(db.collection(USERS).document(currentUser.id), BLOG_IDS, FieldValue.arrayUnion(postId))
                   }
                }.addOnSuccessListener {
                    _postUploadResult.postValue(Result.Success(post))
                }.addOnFailureListener {
                    _postUploadResult.postValue(Result.Error(it))
                }

            }
            .addOnFailureListener {
                _postUploadResult.postValue(Result.Error(it))
            }

    }

    fun setPostUploadResult(result: Result<Post>?) {
        _postUploadResult.postValue(result)
    }

    fun setSignInResult(result: Result<FirebaseUser>?) {
        _signInResult.postValue(result)
    }

    fun setRegisterResult(result: Result<FirebaseUser>?) {
        _registerResult.postValue(result)
    }

    fun setCurrentError(e: Exception?) {
        _networkErrors.postValue(e)
    }

    private val _requestSentResult = MutableLiveData<Result<String>>()
    val requestSentResult: LiveData<Result<String>> = _requestSentResult


    /*
    * PROCEDURE FOR REQUESTING FOR PROJECT
    *
    * 1. Add new request to the post request collection
    * 2. Add new notification request in post-user's notification collection
    * 3. Update activeRequests in current user document
    * */
    fun joinProject(post: Post) {
        val requestRef = db.collection(POSTS).document(post.id).collection(REQUESTS).document()
        val requestId = requestRef.id
        val currentUser = auth.currentUser!!
        val notificationRef = db.collection(USERS).document(post.uid).collection(NOTIFICATIONS).document()
        val notificationId = notificationRef.id
        val currentUserRef = db.collection(USERS).document(currentUser.uid)
        val simpleRequest = SimpleRequest(requestId, post.id, currentUser.uid, post.uid, notificationId, System.currentTimeMillis())
        val notification = SimpleNotification(notificationId, "wants to join your project.", post.id, post.uid, currentUser.uid, post.chatChannelId, requestId, System.currentTimeMillis())

        db.runBatch {
            // 1
            it.set(requestRef, simpleRequest)

            // 2
            it.set(notificationRef, notification)

            // 3
            it.update(currentUserRef, ACTIVE_REQUESTS, FieldValue.arrayUnion(post.id))
        }.addOnSuccessListener {
            _requestSentResult.postValue(Result.Success(requestId))
        }.addOnFailureListener {
            _requestSentResult.postValue(Result.Error(it))
        }
    }

    fun clearProjectFragmentResults() {
        _requestSentResult.postValue(null)
    }

    private val _updateUserResult = MutableLiveData<Result<Map<String, Any?>>>()
    val updateUserResult: LiveData<Result<Map<String, Any?>>> = _updateUserResult

    /*
    * PROCEDURE FOR UPDATING USER (Fan out)
    *
    * 1. Update user document
    * 2. Update user-mini document
    * 3. Update all posts containing user info
    * 4. Update all chat channels contributors collection that has user as contributor
    * 5. Update search index
    *
    * */
    fun updateUser(fullName: String, username: String, about: String, interests: MutableList<String>, imageUri: String?) {
        val userMap = mapOf(
            NAME to fullName,
            USERNAME to username,
            ABOUT to about,
            PHOTO to imageUri,
            INTERESTS to interests
        )
        val currentUser = user.value!!

        val miniMap = mapOf(
            NAME to fullName,
            USERNAME to username,
            PHOTO to imageUri,
        )

        val postMap = mapOf(
            NAME to fullName,
            PHOTO to imageUri,
            USERNAME to username
        )

        val postIds = mutableListOf<String>()
        val chatChannelIds = currentUser.chatChannels
        postIds.addAll(currentUser.projectIds)
        postIds.addAll(currentUser.blogIds)

        val searchRef = db.collection(USERS_SEARCH).document(currentUser.id)

        val listOfSubStrings = arrayListOf<String>()
        for (i in 1 until fullName.length) {
            listOfSubStrings.add(fullName.substring(0..i))
        }

        for (i in 1 until username.length) {
            listOfSubStrings.add(username.substring(0..i))
        }

        val userSearchMap = mapOf<String, Any?>(
            SUBSTRINGS to listOfSubStrings,
            TITLE to fullName,
            IMG to imageUri
        )

        db.runBatch {
            // 1
            it.update(db.collection(USERS).document(currentUser.id), userMap)

            // 2
            it.update(db.collection(USER_MINIMALS).document(currentUser.id), miniMap)

            // 3
            for (postId in postIds) {
                it.update(db.collection(POSTS).document(postId), ADMIN, postMap)
            }

            // 4
            for (chatChannelId in chatChannelIds) {
                it.update(db.collection(CHAT_CHANNELS).document(chatChannelId).collection(
                    CONTRIBUTORS).document(currentUser.id), postMap)
            }

            // 5
            it.update(searchRef, userSearchMap)
        }.addOnSuccessListener {
            _updateUserResult.postValue(Result.Success(userMap))
        }.addOnFailureListener {
            _updateUserResult.postValue(Result.Error(it))
        }

    }

    fun clearEditChanges() {
        _userNameExists.postValue(null)
        _currentImageUri.postValue(null)
        _currentCroppedImageUri.postValue(null)
        _updateUserResult.postValue(null)
        _profilePhotoUploadResult.postValue(null)
    }

    private val _deletePostResult = MutableLiveData<Result<Void>>()
    val deletePostResult: LiveData<Result<Void>> = _deletePostResult

    fun deletePost(post: Post) {
        TODO("Don't delete, It's not complete")
        val contributors = post.contributors ?: listOf()
        val currentUser = user.value ?: return

        db.collection("posts").document().collection("requests").get()
            .addOnSuccessListener {  querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val requests = querySnapshot.toObjects(SimpleRequest::class.java)
                    val batch = db.batch()

                    for (request in requests) {
                        val notificationRef = db.collection("users").document(request.sender).collection("notifications").document()
                        val notificationId = notificationRef.id
                        batch.set(notificationRef, SimpleNotification(notificationId, "has deleted the project. The project no longer exists.", request.postId, request.sender, currentUser.id, null, null, System.currentTimeMillis()))
                        batch.update(db.collection("users").document(request.sender), "activeRequests", FieldValue.arrayRemove(post.id))
                        batch.delete(db.collection("posts").document(post.id).collection("requests").document(request.id))
                    }

                    for (contributor in contributors) {
                        val notificationRef = db.collection("users").document(contributor).collection("notifications").document()
                        val notificationId = notificationRef.id
                        batch.set(notificationRef, SimpleNotification(notificationId, "has deleted the project. The project no longer exists.", post.id, contributor, currentUser.id, null, null, System.currentTimeMillis()))
                        batch.update(db.collection("users").document(contributor), "collaborationIds", FieldValue.arrayRemove(post.id))
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Deleted all requests, contributors and sent notifications to all of them connected to the project.")
                        }.addOnFailureListener {
                            Log.e(TAG, it.message.toString())
                        }

                }
            }.addOnFailureListener {
                Log.e(TAG, "Could not fetch requests from the post - ${it.message}")
            }

        db.runBatch {
            it.delete(db.collection("posts").document(post.id))
            if (post.type == PROJECT) {
                it.update(db.collection("users").document(currentUser.id), "projectIds", FieldValue.arrayRemove(post.id))
            } else {
                it.update(db.collection("users").document(currentUser.id), "blogIds", FieldValue.arrayRemove(post.id))
            }
        }.addOnSuccessListener {
            _deletePostResult.postValue(Result.Success(it))
            Log.d(TAG, "Deleted project")
        }.addOnFailureListener {
            _deletePostResult.postValue(Result.Error(it))
            Log.e(TAG, "Error while deleting projects - ${it.message}")
        }

    }

    fun sendMessage(messagesRef: DocumentReference, messageId: String, msg: String, chatChannelId: String, type: String) = viewModelScope.launch(Dispatchers.IO) {
        val currentUser = user.value!!

        val mediaRef = db.collection(CHAT_CHANNELS).document(chatChannelId).collection(MEDIA).document(messageId)

        val media = SimpleMedia(messageId, type, msg, System.currentTimeMillis())

        val message = SimpleMessage(messageId, chatChannelId, type, msg, currentUser.id, System.currentTimeMillis())
        db.runBatch {
            it.set(messagesRef, message)
            if (type != TEXT) {
                it.set(mediaRef, media)
            }
            val map = mapOf(
                LAST_MESSAGE to message,
                UPDATED_AT to System.currentTimeMillis()
            )
            it.update(db.collection(CHAT_CHANNELS).document(chatChannelId), map)
        }.addOnSuccessListener {
            Log.d(TAG, "Sent message to $chatChannelId - $msg")
            hasConversationsUpdated.postValue(true)
        }.addOnFailureListener {
            setCurrentError(it)
        }
    }

    fun sendMessage(message: SimpleMessage, ref: DocumentReference) {
        val mediaRef = db.collection(CHAT_CHANNELS).document(message.chatChannelId).collection(MEDIA).document(message.messageId)
        val media = SimpleMedia(message.messageId, message.type, message.content, System.currentTimeMillis())

        db.runBatch {
            it.set(ref, message)
            if (message.type != TEXT) {
                it.set(mediaRef, media)
            }
            val map = mapOf(
                LAST_MESSAGE to message,
                UPDATED_AT to System.currentTimeMillis()
            )
            it.update(db.collection(CHAT_CHANNELS).document(message.chatChannelId), map)
        }.addOnSuccessListener {
            hasConversationsUpdated.postValue(true)
        }.addOnFailureListener {
            setCurrentError(it)
        }
    }

    private val _imgMessageUploadResult = MutableLiveData<Result<String>>()
    val imgMessageUploadResult: LiveData<Result<String>> = _imgMessageUploadResult

    fun uploadImgMessage(localPath: Uri, chatChannelId: String, messageId: String) = viewModelScope.launch(Dispatchers.IO) {
        val currentUser = auth.currentUser
        val message = SimpleMessage(messageId, chatChannelId, "Image", localPath.toString(), currentUser!!.uid,System.currentTimeMillis())
        repo.insertMessage(message)
        if (localPath.path != null) {
            val stream = FileInputStream(File(localPath.path!!))
            val storageRef = storage.reference
            val randomName = "Image_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())

            val imageRef = storageRef.child("$chatChannelId/images/messages/$randomName.jpg")
            imageRef.putStream(stream)
                .addOnSuccessListener { taskSnapshot ->
                    val data = taskSnapshot.bytesTransferred
                    imageRef.downloadUrl
                        .addOnSuccessListener {
                            _imgMessageUploadResult.postValue(Result.Success(it.toString()))
                            setCurrentImage(null)
                            setCurrentCroppedImageUri(null)
                        }
                        .addOnFailureListener {
                            _imgMessageUploadResult.postValue(Result.Error(it))
                        }
                    Log.d(TAG, "Total data transferred - $data")
                }
                .addOnFailureListener {
                    _networkErrors.postValue(it)
                }
        }
    }

    fun setImageMessageUploadResult(result: Result<String>?) {
        _imgMessageUploadResult.postValue(result)
    }

    private val _currentQuery = MutableLiveData<String>().apply { value = null }
    val currentQuery: LiveData<String> = _currentQuery

    fun setCurrentQuery(query: String) {
        _currentQuery.postValue(query)
    }

    fun clearSearch() {
        _currentQuery.postValue(null)
    }

    private val _currentHomeTag = MutableLiveData<String>()
    val currentHomeTag: LiveData<String> = _currentHomeTag

    fun setCurrentHomeTag(tag: String?) {
        _currentHomeTag.postValue(tag)
    }

    fun insertMessages(messages: List<SimpleMessage>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertMessages(messages)
    }

    fun clearMessages() = viewModelScope.launch(Dispatchers.IO) {
        database.messageDao().clearMessages()
    }

    fun insertChatChannelContributors(user: User) = viewModelScope.launch (Dispatchers.IO) {
        repo.clearChatChannelContributorsAndChannelIds()
        val chatChannelIds = user.chatChannels
        for (channel in chatChannelIds) {
            Log.d(TAG, channel)
            db.collection(CHAT_CHANNELS).document(channel).collection(CONTRIBUTORS)
                .get()
                .addOnSuccessListener {
                    if (it != null && !it.isEmpty) {
                        val contributors = it.toObjects(ChatChannelContributor::class.java)
                        for (contributor in contributors) {
                            val channelIdObject = ChannelIds(chatChannelId = channel, userId = contributor.id)
                            val existingList = contributor.channelIds.toMutableList()
                            existingList.add(channelIdObject)
                            contributor.channelIds = existingList
                        }
                        insertContributors(contributors)
                    }
                }
        }
    }

    fun channelContributors(chatChannel: ChatChannel) = repo.channelContributors(chatChannel)
    fun insertContributors(contributors: List<ChatChannelContributor>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertContributors(contributors)
    }

    private val _guidelinesUpdateResult = MutableLiveData<Result<String>>()
    val guidelinesUpdateResult: LiveData<Result<String>> = _guidelinesUpdateResult

    fun updateGuidelines(postId: String, guidelines: String) {
        val map = mapOf("guidelines" to guidelines)

        db.collection(POSTS).document(postId)
            .update(map)
            .addOnSuccessListener {
                _guidelinesUpdateResult.postValue(Result.Success(guidelines))
            }.addOnFailureListener {
                _guidelinesUpdateResult.postValue(Result.Error(it))
            }
    }


    fun setGuidelinesUpdateResult(result: Result<String>?) {
        _guidelinesUpdateResult.postValue(result)
    }

    fun setCurrentDoc(doc: Uri?) {
        _currentDocUri.postValue(doc)
    }

    private val _docUploadResult = MutableLiveData<Result<SimpleMessage>>().apply {
        value = null
    }
    val docUploadResult: LiveData<Result<SimpleMessage>> = _docUploadResult

    fun uploadDoc(localPath: Uri, chatChannelId: String, messageId: String, name: String?) = viewModelScope.launch(Dispatchers.IO) {
        val currentUser = auth.currentUser
        val message = SimpleMessage(messageId, chatChannelId, "Document", localPath.toString(), currentUser!!.uid, System.currentTimeMillis())
        repo.insertMessage(message)

        val randomName = "Document_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
        val docRef = storage.reference.child("$chatChannelId/documents/messages/${randomName}_$name")

        docRef.putFile(localPath)
            .addOnSuccessListener {
                docRef.downloadUrl
                    .addOnSuccessListener {
                        message.content = it.toString()
                        _currentDocUri.postValue(null)
                        _docUploadResult.postValue(Result.Success(message))
                    }.addOnFailureListener {
                        _docUploadResult.postValue(Result.Error(it))
                    }
            }.addOnFailureListener {
                _docUploadResult.postValue(Result.Error(it))
            }

    }

    suspend fun checkIfFileDownloaded(messageId: String): SimpleMedia? {
        return repo.checkIfDownloaded(messageId)
    }

    fun insertSimpleMedia(simpleMedia: SimpleMedia?) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertSimpleMedia(simpleMedia)
    }

    companion object {
        const val TAG = "MainViewModel"
    }

}