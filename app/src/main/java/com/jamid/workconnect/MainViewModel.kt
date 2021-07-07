package com.jamid.workconnect

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.adapter.paging3.CommentPagingSource
import com.jamid.workconnect.auth.SignInFormResult
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.data.WorkConnectDatabase
import com.jamid.workconnect.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.regex.Pattern

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalPagingApi::class)
@SuppressLint("NullSafeMutableLiveData")
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repo: MainRepository
    private val database: WorkConnectDatabase =
        WorkConnectDatabase.getInstance(application.applicationContext, viewModelScope)

    init {
        repo = MainRepository(viewModelScope, database)
    }

    val declineProjectResult: LiveData<Result<SimpleNotification>> = repo.declineProjectResult
    val acceptProjectResult: LiveData<Result<SimpleNotification>> = repo.acceptProjectResult
    val undoProjectResult: LiveData<Result<SimpleRequest>> = repo.undoProjectResult
    val mediaUploadResult: LiveData<Result<SimpleMessage>> = repo.mediaUploadResult

    private val _currentImageUri = MutableLiveData<Uri>()
    val currentImageUri: LiveData<Uri> = _currentImageUri

    private val _currentDocUri = MutableLiveData<Uri>()
    val currentDocUri: LiveData<Uri> = _currentDocUri

    private val _currentCroppedImageUri = MutableLiveData<Uri>()
    val currentCroppedImageUri: LiveData<Uri> = _currentCroppedImageUri

    private val _primaryBottomSheetState = MutableLiveData<Int>().apply { value = null }
    val primaryBottomSheetState: LiveData<Int> = _primaryBottomSheetState

    val commentSentResult = repo.commentSentResult

    fun setPrimaryBottomSheetState(sheetState: Int? = null) {
        _primaryBottomSheetState.postValue(sheetState)
    }


    val networkErrors: LiveData<Exception> = repo.networkErrors

    val windowInsets = MutableLiveData<Pair<Int, Int>>().apply {
        value = Pair(0, 0)
    }

    var extras = mutableMapOf<String, Any?>()

    val chatChannelsLiveData: LiveData<List<ChatChannel>> = repo.chatChannelsLiveData

    val user: LiveData<User> = repo.currentLocalUser

    val firebaseErrors = repo.networkErrors

    val firebaseUser: LiveData<FirebaseUser> = repo.currentFirebaseUser

    val signInResult: LiveData<Result<FirebaseUser>> = repo.signInResult
    val registerResult: LiveData<Result<FirebaseUser>> = repo.registerResult
    val emailExists: LiveData<Boolean> = repo.emailExists

    val mediaDownloadResult: LiveData<Result<SimpleMessage>> = repo.mediaDownloadResult

    fun sendRegistrationTokenToServer(token: String) {
        repo.updateRegistrationToken(token)
    }

    fun uploadUser(tags: List<String>? = null) = viewModelScope.launch (Dispatchers.IO) {
        repo.uploadUser(tags)
    }

    fun setCurrentImage(uri: Uri?) {
        _currentImageUri.postValue(uri)
    }

    fun setCurrentCroppedImageUri(uri: Uri?) {
        _currentCroppedImageUri.postValue(uri)
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
            password.length < 8 -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password must be longer than 7 letters."))
            }
            !password.checkIfContainsCapitalLetter() -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password must contain 1 Capital letter"))
            }
            !password.checkIfContainsSmallLetter() -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password must contain 1 small letter"))
            }
            !password.checkIfContainsNumber() -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password must contain 1 number"))
            }
            !password.isValidPassword() -> {
                _signInFormResult.postValue(SignInFormResult(passwordError = "Password contains invalid letter or symbol."))
            }
            else -> {
                _signInFormResult.postValue(SignInFormResult(isValid = true))
            }
        }
    }

    private fun CharSequence?.checkIfContainsCapitalLetter(): Boolean {
        if (this != null) {
            for (ch in this) {
                val asc = ch.code
                if (asc in 65..90) {
                    return true
                }
            }
        }
        return false
    }

    private fun CharSequence?.checkIfContainsSmallLetter(): Boolean {
        if (this != null) {
            for (ch in this) {
                val asc = ch.code
                if (asc in 65..90) {
                    return true
                }
            }
        }
        return false
    }

    private fun CharSequence?.checkIfContainsNumber(): Boolean {
        if (this != null) {
            for (ch in this) {
                val asc = ch.code
                if (asc in 48..57) {
                    return true
                }
            }
        }
        return false
    }

    private fun CharSequence?.isValidEmail() =
        !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    private fun CharSequence?.isValidPassword() =
        !isNullOrEmpty() && Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{4,}\$")
            .matcher(this).matches()

    fun checkIfEmailExists(email: String) {
        if (email.isValidEmail()) {
            repo.checkIfEmailExists(email)
        }
    }

    val userNameExists: LiveData<Result<Boolean>> = repo.usernameExists

    fun checkIfUsernameExists(username: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.checkIfUsernameExists(username)
    }

    fun signIn(email: String, password: String) {
        repo.signIn(email, password)
    }

    fun register(email: String, password: String) {
        repo.register(email, password)
    }

    val firebaseUserUpdateResult: LiveData<Result<FirebaseUser>> = repo.firebaseUserUpdateResult

    fun updateFirebaseUser(downloadUrl: Uri? = null, fullName: String? = null) {
        val map = mutableMapOf<String, Any?>(
            "photoUri" to downloadUrl,
            "fullName" to fullName
        )
        repo.updateFirebaseUser(map)
    }

    val profilePhotoUploadResult: LiveData<Uri?> = repo.profilePhotoUpload

    fun uploadProfilePhoto(image: Uri?) {
        repo.uploadProfilePhoto(image)
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

    fun clearPostChanges() = viewModelScope.launch(Dispatchers.IO) {
        repo.clearUploadResults()
        _currentCroppedImageUri.postValue(null)
        _currentImageUri.postValue(null)
        _currentPlace.postValue(null)
        _tag.postValue(null)
    }

    val postPhotoUploadResult: LiveData<Result<Uri>> = repo.postPhotoUpload

    fun uploadPostImage(image: Uri?, type: String) {
        if (image != null) {
            repo.uploadPostImage(image, type)
        }
    }

    // the state before changes
    suspend fun onLikePressed(post: Post): Post {
        return repo.onLikePressed(post)
    }

    suspend fun onDislikePressed(post: Post): Post {
        return repo.onDislikePressed(post)
    }

    suspend fun onSavePressed(post: Post): Post {
        return repo.onSaved(post)
    }

    suspend fun onFollowPressed(post: Post): Post {
        return repo.onFollowPressed(post)
    }

    fun onFollowPressed(currentUser: User, otherUser: User) = viewModelScope.launch(Dispatchers.IO) {
        repo.onFollowPressed(currentUser, otherUser)
    }

    val postUploadResult: LiveData<Result<Post>> = repo.postUpload

    fun uploadPost(post: Post) = viewModelScope.launch (Dispatchers.IO) {
        post.location = currentLocation.value
        repo.uploadPost(post)
    }

    fun setCurrentError(e: Exception?) {
       repo.setCurrentError(e)
    }

    val requestSentResult: LiveData<Result<String>> = repo.requestSent

    fun joinProject(post: Post) = viewModelScope.launch(Dispatchers.IO) {
        repo.joinProject(post)
    }

    fun clearProjectFragmentResults() = viewModelScope.launch(Dispatchers.Default) {
        repo.clearRequestResult()
    }

    val updateUserResult: LiveData<Result<Map<String, Any?>>> = repo.updateUser

    fun updateUser(userMap: Map<String, Any?>) = viewModelScope.launch(Dispatchers.IO) {
        repo.updateUser(userMap)
    }

    fun clearEditChanges() {
        _currentImageUri.postValue(null)
        _currentCroppedImageUri.postValue(null)
        repo.clearEditChanges()
    }

    /*fun deletePost(post: Post) {
        *//*
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
        }*//*
    }*/

    fun sendMessage(
        message: SimpleMessage,
        chatChannel: ChatChannel
    ) = viewModelScope.launch(Dispatchers.IO) {
        repo.sendMessage(message, chatChannel)
    }

    fun uploadMessageMedia(message: SimpleMessage, chatChannel: ChatChannel) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.uploadMessageMedia(message, chatChannel)
        }

    private val _currentQuery = MutableLiveData<String>()
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

    fun channelContributorsLive(channelId: String) = repo.channelContributorsLive("%$channelId%")

    val guidelinesUpdateResult: LiveData<Post> = repo.guidelinesUpdateResult

    fun clearGuidelinesUpdateResult() {
        repo.clearGuideUpdateResult()
    }

    fun setCurrentDoc(doc: Uri?) {
        _currentDocUri.postValue(doc)
    }


    /*suspend fun onNewMessageNotification(
        chatChannelId: String
    ): ChatChannel? {
        return repo.onNewMessageNotification(chatChannelId)
    }*/

    fun getCachedPost(postId: String) = repo.getCachedPost(postId)

    fun updatePost(post: Post, guidelines: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.updatePost(post, guidelines)
    }

    fun downloadMedia(destinationFile: File, message: SimpleMessage) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.downloadMedia(destinationFile, message)
        }

    fun clearSignInChanges() {
        _signInFormResult.postValue(null)
        repo.clearSignInChanges()
    }

    fun getPost(postId: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.getPost(postId)
    }

    /*fun initialSetupAfterAuthentication(user: User) {

        // 1. get the chat channels
        val chatChannelIds = user.chatChannels
        for (channel in chatChannelIds) {
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


        // 2. get the contributors for each chat channel
    }*/

    fun chatChannelMessages(
        chatChannel: ChatChannel,
        externalImagesDir: File,
        externalDocumentsDir: File
    ): Flow<PagingData<SimpleMessage>> {
        return Pager(
            PagingConfig(pageSize = 30, initialLoadSize = 50, prefetchDistance = 10),
            remoteMediator = MessagesRemoteMediator(
                repo,
                chatChannel,
                externalImagesDir,
                externalDocumentsDir
            )
        ) {
            database.messageDao().getMessages(chatChannel.chatChannelId)
        }.flow.cachedIn(viewModelScope)
    }


    suspend fun randomTopUsers(): Result<QuerySnapshot> {
        return repo.getRandomTopUsers()
    }


    suspend fun topProjects(): Result<QuerySnapshot> {
        return repo.getTopProjects()
    }

    suspend fun topBlogs(): Result<QuerySnapshot> {
        return repo.getTopBlogs()
    }

    fun searchPosts(query: String, type: String): Flow<PagingData<Post>> {
        val postSource = PostSource.Search(
            Firebase.firestore.collection(POSTS)
                .whereEqualTo(TYPE, type)
                .whereArrayContainsAny(
                    INDICES,
                    listOf(
                        query,
                        query.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                        query.replaceFirstChar { it.lowercase(Locale.ROOT) },
                        query.uppercase(Locale.ROOT), query.lowercase(Locale.ROOT)
                    )
                ), type
        )

        return Pager(PagingConfig(10)) {
            PostPagingSource(postSource, repo)
        }.flow
    }

    fun searchUsers(query: String): Flow<PagingData<User>> {
        val userSource = UserSource.Search(
            Firebase.firestore.collection(
                USERS
            ).whereArrayContainsAny(
                INDICES, listOf(
                    query,
                    query.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    query.replaceFirstChar { it.lowercase(Locale.ROOT) },
                    query.uppercase(Locale.ROOT), query.lowercase(Locale.ROOT)
                )
            )
                .orderBy(SEARCH_RANK, Query.Direction.DESCENDING), searchQuery = query
        )

        return Pager(PagingConfig(20)) {
            UserPagingSource(userSource, repo)
        }.flow
    }

    fun userFollowers(otherUser: User, q: String? = null): Flow<PagingData<User>> {
        return if (q == null) {
            val userSource = UserSource.Follower(
                Firebase.firestore.collection(USERS)
                    .document(otherUser.id)
                    .collection(FOLLOWERS)
                    .document(otherUser.id)
                    .collection(USERS)
                    .orderBy(NAME, Query.Direction.ASCENDING),
                q, otherUser
            )

            Pager(PagingConfig(20)) {
                UserPagingSource(userSource, repo)
            }.flow
        } else {
            val userSource = UserSource.Follower(
                Firebase.firestore.collection(
                    USERS
                ).document(otherUser.id).collection(FOLLOWERS)
                    .document(otherUser.id).collection(USERS)
                    .whereArrayContainsAny(
                        INDICES,
                        listOf(
                            q,
                            q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            q.replaceFirstChar { it.lowercase(Locale.ROOT) },
                            q.uppercase(Locale.ROOT), q.lowercase(Locale.ROOT)
                        )
                    ).orderBy(NAME, Query.Direction.ASCENDING), q, otherUser
            )

            Pager(PagingConfig(20)) {
                UserPagingSource(userSource, repo)
            }.flow

        }
    }

    fun userFollowings(otherUser: User, q: String? = null): Flow<PagingData<User>> {
        return if (q == null) {
            val userSource = UserSource.Following(
                Firebase.firestore.collection(
                    USERS
                ).document(otherUser.id).collection(FOLLOWINGS).document(otherUser.id)
                    .collection(USERS).orderBy(NAME, Query.Direction.ASCENDING), q, otherUser
            )

            Pager(PagingConfig(20)) {
                UserPagingSource(userSource, repo)
            }.flow

        } else {

            val userSource = UserSource.Following(
                Firebase.firestore.collection(
                    USERS
                ).document(otherUser.id).collection(FOLLOWINGS)
                    .document(otherUser.id).collection(USERS)
                    .whereArrayContainsAny(
                        INDICES,
                        listOf(
                            q,
                            q.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            q.replaceFirstChar { it.lowercase(Locale.ROOT) },
                            q.uppercase(Locale.ROOT), q.lowercase(Locale.ROOT)
                        )
                    ).orderBy(NAME, Query.Direction.ASCENDING), q, otherUser
            )

            Pager(PagingConfig(20)) {
                UserPagingSource(userSource, repo)
            }.flow

        }
    }

    suspend fun getLocalUser(uid: String): User? {
        return repo.getLocalUser(uid)
    }

    fun notificationsFlow(): Flow<PagingData<SimpleNotification>> {
        return Pager(PagingConfig(20), remoteMediator = NotificationRemoteMediator(repo)) {
            database.notificationDao().getNotifications()
        }.flow
    }

    fun activeRequests(): Flow<PagingData<SimpleRequest>> {
        return Pager(PagingConfig(15), remoteMediator = SimpleRequestRemoteMediator(repo)) {
            database.activeRequestDao().getActiveRequests()
        }.flow
    }


    fun acceptProjectRequest(notification: SimpleNotification) = viewModelScope.launch (Dispatchers.IO) {
        repo.acceptProjectRequest(notification)
    }

    fun declineProjectRequest(notification: SimpleNotification) = viewModelScope.launch (Dispatchers.IO) {
        repo.declineProjectRequest(notification)
    }

    fun undoProjectRequest(request: SimpleRequest) = viewModelScope.launch(Dispatchers.IO) {
        repo.undoProjectRequest(request)
    }

    fun updateMessage(message: SimpleMessage) = viewModelScope.launch(Dispatchers.IO) {
        repo.updateMessage(message)
    }

    private val _inset = MutableLiveData<Pair<Int, Int>>().apply { value = Pair(0, 0) }
    val inset: LiveData<Pair<Int, Int>> = _inset


    /*fun getSavedPosts(initialItemPosition: Int, finalItemPosition: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(BUG_TAG, "Getting saved posts ... VM")
            repo.getSavedPosts(initialItemPosition, finalItemPosition)
        }*/

    fun deleteLocalRequest(
        notificationId: String,
        postId: String,
        requestId: String,
        chatChannelId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteLocalRequest(notificationId, postId, requestId, chatChannelId)
    }

    fun clearPosts() = viewModelScope.launch(Dispatchers.IO) {
        repo.clearPosts()
    }

    fun getNotifications() = viewModelScope.launch(Dispatchers.IO) {
        repo.getNotifications()
    }

    fun increaseProjectWeight(cachedPost: Post?) = viewModelScope.launch(Dispatchers.Default) {
        if (cachedPost != null && Firebase.auth.currentUser != null) {
            repo.increaseProjectWeightage(cachedPost)
        }
    }

    suspend fun getTags(user: User?) = repo.getTagsFromFirebase(user)

    fun postsFlow(tag: String? = null): Flow<PagingData<Post>> {
        val currentUser = user.value
        return if (tag != null) {
            val tags = listOf(
                tag,
                tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                tag.replaceFirstChar { it.lowercase(Locale.ROOT) },
                tag.uppercase(Locale.ROOT),
                tag.lowercase(
                    Locale.ROOT
                )
            )
            val postSource = PostSource.FeedWithTags(
                Firebase.firestore.collection(POSTS).whereArrayContainsAny(
                    TAGS,
                    tags
                ), tags
            )
            Pager(PagingConfig(20)) {
                PostPagingSource(postSource, repo)
            }.flow
        } else {
            val postSource = if (currentUser != null) {
                val followings = currentUser.userPrivate.followings
                val finalList = mutableListOf<String>()
                finalList.add(currentUser.id)
                finalList.addAll(followings.subList(0, minOf(9, followings.size)))
                if (finalList.size < 2) {
                    PostSource.FeedRandom(Firebase.firestore.collection(POSTS))
                } else {
                    PostSource.FeedWithFollowings(
                        Firebase.firestore.collection(POSTS).whereIn(UID, finalList), currentUser
                    )
                }
            } else {
                PostSource.FeedRandom(Firebase.firestore.collection(POSTS))
            }

            Pager(PagingConfig(20)) {
                PostPagingSource(postSource, repo)
            }.flow
        }
    }

    fun otherUserProjectsFlow(otherUser: User): Flow<PagingData<Post>> {
        val postSource = PostSource.FeedWithOtherUserAndType(
            Firebase.firestore.collection(POSTS)
                .whereEqualTo(UID, otherUser.id)
                .whereEqualTo(TYPE, PROJECT), otherUser, PROJECT
        )
        return Pager(PagingConfig(20)) {
            PostPagingSource(postSource, repo)
        }.flow
    }

    fun otherUserBlogsFlow(otherUser: User): Flow<PagingData<Post>> {
        val postSource = PostSource.FeedWithOtherUserAndType(
            Firebase.firestore.collection(POSTS)
                .whereEqualTo(UID, otherUser.id)
                .whereEqualTo(TYPE, BLOG), otherUser, BLOG
        )
        return Pager(PagingConfig(20)) {
            PostPagingSource(postSource, repo)
        }.flow
    }

    fun otherUserCollaborationsFlow(otherUser: User): Flow<PagingData<Post>> {
        val postSource = PostSource.FeedWithOtherUserCollaborations(
            Firebase.firestore.collection(POSTS)
                .whereArrayContains("contributors", otherUser.id), otherUser
        )
        return Pager(PagingConfig(20)) {
            PostPagingSource(postSource, repo)
        }.flow
    }

    suspend fun getProjectContributors(post: Post): Result<QuerySnapshot> {
        return repo.getProjectContributors(post)
    }

    fun setChatChannelListeners() = viewModelScope.launch(Dispatchers.IO) {
        repo.setChatChannelsListeners()
    }

    /*fun setChannelContributorsListener(chatChannel: ChatChannel) {
        repo.setChannelContributorsListener(chatChannel)
    }*/

    fun getRecentSearchesByType(type: String): Flow<List<RecentSearch>> {
        return repo.getRecentSearchesByType(type)
    }

    fun deleteRecentSearch(query: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteRecentSearch(query)
    }

    fun addRecentSearch(recentSearch: RecentSearch) = viewModelScope.launch(Dispatchers.IO) {
        repo.addRecentSearch(recentSearch)
    }

    suspend fun fetchSavedPosts(i: Int, f: Int): Result<QuerySnapshot> {
        return repo.fetchSavedPosts(i, f)
    }

    fun filterPosts(posts: List<Post>): List<Post> {
        return repo.filterPosts(posts)
    }

    fun getQueryPosts(postsTag: String): Flow<PagingData<Post>> {
        val postSource = PostSource.TagPosts(
            Firebase.firestore.collection(POSTS).whereArrayContains(TAGS, postsTag), postsTag
        )
        return Pager(PagingConfig(20, 5, false, 30)) {
            PostPagingSource(postSource, repo)
        }.flow
    }


    fun signInWithGoogle(credential: AuthCredential) = viewModelScope.launch(Dispatchers.IO) {
        repo.signInWithGoogle(credential)
    }

    suspend fun filterUsers(users: List<User>): List<User> {
        return repo.filterUsers(users)
    }

    fun addInterest(interest: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.addInterest(interest)
    }

    fun removeInterest(interest: String) = viewModelScope.launch(Dispatchers.IO) {
        repo.removeInterest(interest)
    }

    fun topProjectsFlow(): Flow<PagingData<Post>> {
        val postSource = PostSource.FeedRandom(
            Firebase.firestore.collection(POSTS)
                .whereEqualTo(TYPE, PROJECT)
                .orderBy(LIKES, Query.Direction.DESCENDING)
        )
        return Pager(PagingConfig(20)) {
            PostPagingSource(postSource, repo)
        }.flow
    }

    fun topBlogsFlow(): Flow<PagingData<Post>> {
        val postSource = PostSource.FeedRandom(
            Firebase.firestore.collection(POSTS)
                .whereEqualTo(TYPE, BLOG)
                .orderBy(LIKES, Query.Direction.DESCENDING)
        )
        return Pager(PagingConfig(20)) {
            PostPagingSource(postSource, repo)
        }.flow
    }

    fun getRandomUsers(): Flow<PagingData<User>> {
        val userSource = UserSource.Random(
            Firebase.firestore.collection(USERS)
                .orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
        )
        return Pager(PagingConfig(20)) {
            UserPagingSource(userSource, repo)
        }.flow
    }


    suspend fun localMessages(
        type: String = IMAGE,
        chatChannelId: String,
        limit: Int = 0
    ): List<SimpleMessage> {
        return repo.localMessages(type, chatChannelId, limit)
    }

    fun deleteWholeDatabase() = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteWholeDatabase()
    }

    suspend fun getChannelsForQuery(query: String): List<ChatChannel> {
        return repo.getChannelsForQuery(query)
    }

    fun getComments(commentChannelId: String): Flow<PagingData<SimpleComment>> {
        return Pager(PagingConfig(10)) {
            CommentPagingSource(commentChannelId, repo)
        }.flow
    }

    fun sendComment(post: Post, comment: String) {
        repo.sendComment(post, comment)
    }

    /*fun tempFixBlog(post: Post) {
        repo.tempFixBlog(post)
    }*/

    suspend fun likeComment(comment: SimpleComment): SimpleComment {
        return repo.likeComment(comment)
    }

    suspend fun dislikeComment(comment: SimpleComment): SimpleComment {
        return repo.dislikeComment(comment)
    }

    fun sendCommentReply(parentComment: SimpleComment, comment: String) {
        repo.sendCommentReply(parentComment, comment)
    }

    var replyJobStarted = MutableLiveData<SimpleComment>().apply { value = null }


    suspend inline fun <reified T: Any?> getObject(documentReference: DocumentReference): T? {
        return repo.getObject(documentReference)
    }

    suspend inline fun <reified T: Any> getObjects(query: Query): List<T> {
        return repo.getObjects(query)
    }

    fun insertPublicUserData(userHalf: User) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertPublicUserData(userHalf)
    }

    fun insertPrivateUserData(userHalf: UserPrivate) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertPrivateUserData(userHalf)
    }

    fun fetchCurrentUser(currentFirebaseUser: FirebaseUser) = viewModelScope.launch(Dispatchers.IO) {
        repo.fetchCurrentUser(currentFirebaseUser)
    }

    fun insertMessagesWithFilter(
        messages: List<SimpleMessage>,
        chatChannel: ChatChannel,
        externalDocumentsDir: File,
        externalImagesDir: File
    ) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertMessages(externalImagesDir, externalDocumentsDir, messages, chatChannel)
    }

    fun insertChannelContributors(chatChannel: ChatChannel, contributors: List<User>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertChannelContributors(chatChannel, contributors)
    }

    fun clearMediaDownloadResult() {
        repo.clearMediaDownloadResult()
    }

    fun clearMediaUploadResult() {
        repo.clearMediaUploadResult()
    }

    companion object {
        const val TAG = "MainViewModel"
    }

}