package com.jamid.workconnect.data

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class MainRepository(val scope: CoroutineScope, val db: WorkConnectDatabase) {

    val firebaseUtility = FirebaseUtilityImpl()

    private val messageDao = db.messageDao()
    private val chatChannelDao = db.chatChannelDao()
    private val notificationDao = db.notificationDao()
    private val userDao = db.userDao()
    private val postDao = db.postDao()
    private val requestDao = db.activeRequestDao()
    private val recentSearchDao = db.recentSearchDao()

    var currentFirebaseUser = MutableLiveData<FirebaseUser>().apply { value = null }

    val declineProjectResult = firebaseUtility.declineRequestResult
    val undoProjectResult = firebaseUtility.undoRequestSent
    val acceptProjectResult = firebaseUtility.acceptRequestResult
    val commentSentResult = firebaseUtility.commentSentResult
    val mediaDownloadResult = firebaseUtility.mediaDownloadResult
    val mediaUploadResult = firebaseUtility.mediaUploadResult
    val signInResult: LiveData<Result<FirebaseUser>> = firebaseUtility.signInResult
    val registerResult: LiveData<Result<FirebaseUser>> = firebaseUtility.registerResult
    val currentLocalUser: LiveData<User> = userDao.currentUser()
    val usernameExists: LiveData<Result<Boolean>> = firebaseUtility.usernameExists
    val emailExists: LiveData<Boolean> = firebaseUtility.emailExists
    val profilePhotoUpload = firebaseUtility.profilePhotoUpload
    val postPhotoUpload: LiveData<Result<Uri>> = firebaseUtility.postPhotoUpload
    val postUpload: LiveData<Result<Post>> = firebaseUtility.postUpload
    val requestSent: LiveData<Result<String>> = firebaseUtility.requestSent
    val updateUser: LiveData<Result<Map<String, Any?>>> = firebaseUtility.updateUser
    val networkErrors: MutableLiveData<Exception> = firebaseUtility.networkErrors
    val guidelinesUpdateResult: LiveData<Post> = firebaseUtility.guidelinesUpdateResult
    val firebaseUserUpdateResult: LiveData<Result<FirebaseUser>> = firebaseUtility.firebaseUserUpdateResult
    val chatChannelsLiveData = chatChannelDao.allChannels()
    val mapOfDocumentSnapshots = mutableMapOf<String, DocumentSnapshot>()

    init {
        scope.launch {
            val firebaseUser = firebaseUtility.currentFirebaseUser
            if (firebaseUser != null) {
                currentFirebaseUser.postValue(firebaseUser)
                val token = firebaseUtility.getToken()
                if (token != null) {
                    updateRegistrationToken(token)
                }
                // signed in
                val currentUser = firebaseUtility.getCurrentUser()
                val currentCachedUser = userDao.getCachedUser()
                if (currentCachedUser != null) {
                    if (currentUser != null) {
                        if (currentCachedUser == currentUser) {
                            // do nothing
                        } else {
                            insertCurrentUser(currentUser)
                        }
                    } else {
                        setCurrentError("Couldn't retrieve new user data from database.")
                        // report error that the firebase call was an error
                    }
                } else {
                    // the user was never in the cache or somehow was deleted before
                    Log.i(TAG, "The cached user was never in the cache or somehow was deleted before")
                    if (currentUser != null) {
                        insertCurrentUser(currentUser)
                    }
                }
            } else {
                Log.d(TAG, "No firebase account signed in")
                // not signed in
            }
        }
    }

    @Suppress("SameParameterValue")
    fun setCurrentError(msg: String) {
        val e = Exception(msg)
        setCurrentError(e)
    }

    fun setChannelContributorsListener(chatChannel: ChatChannel) {
        Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
            .collection(USERS).addSnapshotListener { v, e ->
                if (e != null) {
                    setCurrentError(e)
                }

                if (v != null && !v.isEmpty) {
                    val contributors = v.toObjects(User::class.java)
                    scope.launch (Dispatchers.IO) {
                        insertChannelContributors(chatChannel, contributors)
                    }
                }
            }
    }

    @Suppress("SameParameterValue")
    fun setCurrentError(e: Exception?) {
        networkErrors.postValue(e)
    }

    private suspend fun insertCurrentUser(newUpdatedUser: User) {
        newUpdatedUser.isCurrentUser = true
        newUpdatedUser.isUserFollowed = false
        newUpdatedUser.isUserFollowingMe = false

        userDao.insertCurrentUser(newUpdatedUser)
    }

    companion object {
        private const val TAG = "MainRepository"
    }

    fun getChatMessagesAfter(chatChannelId: String, key: Long): List<SimpleMessage> {
        return messageDao.getChatMessagesAfter(chatChannelId, key)
    }

    suspend fun updateMessage(message: SimpleMessage) {
        messageDao.updateItem(message)
    }

    fun channelContributorsLive(channelId: String): LiveData<List<User>> {
        return userDao.getChannelContributorsLive(channelId)
    }

    /////////////////////////////////////////////////////////////////////

    fun signIn(email: String, password: String) {
        firebaseUtility.signIn(email, password)
    }

    fun register(email:String, password: String) {
        firebaseUtility.register(email, password)
    }

    suspend fun uploadUser(tags: List<String>? = null) {
        val user = firebaseUtility.createNewUser(tags)
        if (user != null) {
            val uploadedUser = firebaseUtility.uploadCurrentUser(user)
            if (uploadedUser != null)
                insertCurrentUser(uploadedUser)
        }
    }

    fun updateRegistrationToken(token: String) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            firebaseUtility.updateRegistrationToken(currentUser, token)
        }
    }

    suspend fun checkIfUsernameExists(username: String) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            firebaseUtility.checkIfUsernameExists(currentUser, username)
        }
    }

    fun checkIfEmailExists(email: String) {
        firebaseUtility.checkIfEmailExists(email)
    }

    fun updateFirebaseUser(map: MutableMap<String, Any?>) {
        firebaseUtility.updateFirebaseUser(map)
    }

    fun uploadProfilePhoto(image: Uri?) {
        firebaseUtility.uploadProfilePhoto(image)
    }

    fun uploadPostImage(image: Uri, type: String = PROJECT) {
        firebaseUtility.uploadPostImage(image, type)
    }

    suspend fun uploadMultipleImages(images: List<Uri>): List<Uri> {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            firebaseUtility.uploadMultipleProjectImages(currentUser, images)
        } else {
            emptyList()
        }
    }

    suspend fun updateUser(userMap: Map<String, Any?>) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val updatedUser = firebaseUtility.updateCurrentUser(currentUser, userMap)
            if (updatedUser != null) {
                userDao.insertCurrentUser(updatedUser)
                postUpdateChanges(updatedUser)
            }
        }
    }

    private suspend fun postUpdateChanges(currentUser: User) {
        val lastMessageSenderChats =
            chatChannelDao.getChatChannelsForLastMessage(currentUser.id).toMutableList()
        if (!lastMessageSenderChats.isNullOrEmpty()) {
            for (chat in lastMessageSenderChats) {
                chat.lastMessage?.sender = currentUser
            }
        }
        chatChannelDao.updateItems(lastMessageSenderChats)
    }

    suspend fun onLikePressed(post: Post): Post {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            val result = firebaseUtility.onLikePressedWithoutCaching(currentUser, post)
            if (result != null) {
               insertCurrentUser(result.first)
               return result.second
            } else {
                throw Exception("Something went wrong while liking the post")
            }
        } else {
            post
        }
    }

    suspend fun onDislikePressed(post: Post): Post {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            val result = firebaseUtility.onDislikePressedWithoutCaching(currentUser, post)
            if (result != null) {
                insertCurrentUser(result.first)
                return result.second
            } else {
                throw Exception("Something went wrong while disliking the post")
            }
        } else {
            post
        }
    }

    suspend fun onSaved(post: Post): Post {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            val result = firebaseUtility.onPostSavedWithoutCaching(currentUser, post)
            if (result != null) {
                insertCurrentUser(result.first)
                return result.second
            } else {
                throw Exception("Something went wrong while saving the post")
            }
        } else {
            post
        }
    }

    suspend fun onFollowPressed(post: Post): Post {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            val result = firebaseUtility.onFollowPressed(currentUser, post)
            if (result != null) {
                insertCurrentUser(result.first)
                result.second
            } else {
                post
            }
        } else {
            post
        }
    }

    suspend fun onFollowPressed(currentUser: User, otherUser: User) {
        firebaseUtility.onFollowPressed(currentUser, otherUser)
    }

    suspend fun uploadPost(post: Post) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val result = firebaseUtility.uploadPost(currentUser, post)
            if (result != null) {
                insertPosts(listOf(result.first))
                insertChatChannels(listOf(result.second))
            }
        }
    }

    private suspend fun insertChatChannels(channels: List<ChatChannel>) {
        chatChannelDao.insertItems(channels)
    }

    suspend fun joinProject(post: Post) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val result = firebaseUtility.joinProject(currentUser, post)
            if (result != null) {
                insertCurrentUser(result.first)
                requestDao.insert(result.second)
            }
        }
    }

    suspend fun uploadMessageMedia(message: SimpleMessage, chatChannel: ChatChannel) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val result = firebaseUtility.uploadMessageMedia(currentUser, message, chatChannel)
            if (result != null) {
                chatChannelDao.updateItem(result.first)
                messageDao.insert(result.second)
            }
        }
    }

    suspend fun sendMessage(message: SimpleMessage, chatChannel: ChatChannel) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val result = firebaseUtility.sendMessage(currentUser, message, chatChannel)
            if (result != null) {
                chatChannelDao.updateItem(result.first)
                messageDao.insert(result.second)
            }
        }
    }

    fun clearUploadResults() {
        firebaseUtility.clearUploadResults()
    }

    fun clearRequestResult() {
        firebaseUtility.clearRequestResults()
    }

    fun clearEditChanges() {
        firebaseUtility.clearEditChanges()
    }

    /*suspend fun onNewMessageNotification(chatChannelId: String): ChatChannel? {
        return firebaseUtility.onNewMessagesFromBackground(chatChannelId)
    }*/

    fun getCachedPost(postId: String) : LiveData<Post> {
        return postDao.getPostLive(postId)
    }

    suspend fun updatePost(post: Post, guidelines: String) {
        post.guidelines = guidelines
        updatePost(post, mapOf(GUIDELINES to guidelines))
    }

    private suspend fun updatePost(post: Post, map: Map<String, Any?>) {
        val returnedPost = firebaseUtility.updatePost(post, map)
        if (returnedPost != null) {
            postDao.updateItem(returnedPost)
        }
    }

    suspend fun downloadMedia(destinationFile: File, message: SimpleMessage) {
        val returnedMessage = firebaseUtility.downloadMedia(destinationFile, message)
        if (returnedMessage != null) {
            messageDao.updateItem(returnedMessage)
        }
    }

    /*fun getChannelContributors(chatChannelId: String, pageSize: Long, extra: DocumentSnapshot? = null, ahead: Boolean = false, onComplete: (contributors: QuerySnapshot) -> Unit) {

    }*/

    /*fun getContributorSnapshot(channelId: String, id: String, onComplete: (doc: DocumentSnapshot) -> Unit) {
        firebaseUtility.getContributorSnapshot(channelId, id) {
            onComplete(it)
        }
    }*/

    fun clearGuideUpdateResult() {
        firebaseUtility.guidelinesUpdateResult.postValue(null)
    }

    fun clearSignInChanges() {
        firebaseUtility.clearSignInChanges()
    }

    suspend fun getPost(postId: String) {
        val post = firebaseUtility.getPost(postId)
        if (post != null) {
            insertPosts(listOf(post))
        }
    }

    /*fun <T: Any> getItems(limit: Long, query: Query, clazz: Class<T>, extras: Map<String, Any?>?, onComplete: (firstDoc: DocumentSnapshot?, lastDoc: DocumentSnapshot?, isEnd: Boolean) -> Unit) {
        firebaseUtility.getItems(limit, query, clazz, extras) { doc1, doc2, isEnd ->
            onComplete(doc1, doc2, isEnd)
        }
    }*/

    /*fun getItemsWithoutCaching(query: Query, onComplete: (querySnapshot: QuerySnapshot) -> Unit) {
        firebaseUtility.getItemsWithoutCaching(query) {
            onComplete(it)
        }
    }*/

   /* fun <T> getSnapshot(item: T, clazz: Class<T>, onComplete: (doc: DocumentSnapshot) -> Unit) {
       *//* firebaseUtility.getSnapshot(item, clazz) {
            onComplete(it)
        }*//*
    }*/

    /*fun getSnapshot(query: DocumentReference, onComplete: (doc: DocumentSnapshot) -> Unit) {
        firebaseUtility.getSnapshot(query) {
            onComplete(it)
        }
    }*/

    suspend inline fun <reified T: Any?> getObject(docRef: DocumentReference): T? {
        return firebaseUtility.getObject<T>(docRef)
    }

    suspend inline fun <reified T: Any> getObjects(query: Query): List<T> {
        return firebaseUtility.getObjects(query)
    }

    suspend fun getLocalUser(uid: String) : User? {
        return userDao.getUser(uid)
    }

    suspend fun acceptProjectRequest(notification: SimpleNotification) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            firebaseUtility.acceptProjectRequest(currentUser, notification)
            val localSender = notification.sender
            localSender.userPrivate.chatChannels = listOf(notification.post?.chatChannelId ?: "")
            val user = filterUser(localSender)
            userDao.insert(user)

            notificationDao.deleteNotification(notification)

            val chatChannel = chatChannelDao.getChatChannel(notification.post?.chatChannelId ?: "")
            if (chatChannel != null) {
                chatChannel.contributorsCount += 1
                val existingList = chatChannel.contributorsList.toMutableList()
                existingList.add(notification.sender.id)
                chatChannel.contributorsList = existingList

                chatChannelDao.updateItem(chatChannel)
            }

        }
    }

    suspend fun declineProjectRequest(notification: SimpleNotification) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            firebaseUtility.denyProjectRequest(currentUser, notification)
            notificationDao.deleteNotification(notification)
        }

    }

    suspend fun undoProjectRequest(request: SimpleRequest) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val result = firebaseUtility.undoProjectRequest(currentUser, request)
            if (result != null) {
                insertCurrentUser(result.first)
                requestDao.deleteRequest(result.second)
            }
        }
    }

    suspend fun deleteLocalRequest(notificationId: String, postId: String, requestId: String, chatChannelId: String) {
        Log.d(BUG_TAG, "Delete local request")
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val simpleRequest = requestDao.getRequest(requestId)
            if (simpleRequest != null) {
                requestDao.deleteRequest(simpleRequest)
            }

            val existingCollaborationList = currentUser.userPrivate.collaborationIds.toMutableList()
            existingCollaborationList.add(postId)
            currentUser.userPrivate.collaborationIds = existingCollaborationList

            val existingChannels = currentUser.userPrivate.chatChannels.toMutableList()
            existingChannels.add(chatChannelId)
            currentUser.userPrivate.chatChannels = existingChannels

            val existingActiveRequests = currentUser.userPrivate.activeRequests.toMutableList()
            existingActiveRequests.remove(postId)
            currentUser.userPrivate.activeRequests = existingActiveRequests

            val post = postDao.getPost(postId)
            if (post != null) {
                if (post.type == PROJECT) {
                    val existingContributors = post.contributors!!.toMutableList()
                    existingContributors.add(currentUser.id)
                    post.contributors = existingContributors
                    postDao.updateItem(post)
                }
            }

            val notification = firebaseUtility.getNotificationFromFirebase(currentUser, notificationId)
            if (notification != null)
                notificationDao.insert(notification)

            insertCurrentUser(currentUser)
        }
    }

    suspend fun clearPosts() {
        postDao.clearPosts()
    }

    suspend fun getNotifications() {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val notifications = firebaseUtility.getNotifications(currentUser)
            notificationDao.insertItems(notifications)
        }
    }

    suspend fun clearRequests() {
        requestDao.clearRequests()
    }

    /*suspend fun increaseProjectWeightage(cachedPost: Post) {
        val newWeightage = cachedPost.weightage + 0.1
        val newSearchRank = cachedPost.searchRank + 1

        cachedPost.weightage = newWeightage
        cachedPost.searchRank = newSearchRank

        // updated post weightage and search rank
        updatePost(cachedPost, mapOf(WEIGHTAGE to newWeightage, SEARCH_RANK to newSearchRank))

        val userSearchRank = cachedPost.admin.searchRank + 1
        val otherUser = cachedPost.admin
        otherUser.searchRank = userSearchRank

        // updated user search rank
        val returnedOtherUser = firebaseUtility.updateOtherUser(otherUser, mapOf<String, Any?>(SEARCH_RANK to userSearchRank))
        if (returnedOtherUser != null) {
            if (userDao.getUser(otherUser.id) != null) {
                userDao.updateItem(returnedOtherUser)
            } else {
                userDao.insert(returnedOtherUser)
            }
        }

        // TODO("If the user is followed or following the current user, change the weightage of the user
        //      both in fireStore and locally")
        //      both in fireStore and locally")
    }*/

    suspend fun insertPosts(posts: List<Post>) = scope.launch (Dispatchers.IO) {
        val returnedPosts = filterPosts(posts)
        postDao.insertItems(returnedPosts)
    }

    private suspend fun insertMessagesWithFilter(externalImagesDir: File, externalDocumentsDir: File, messages: List<SimpleMessage>, chatChannel: ChatChannel) {

        val lastMessage = messages.first()
        chatChannel.lastMessage = lastMessage

        for (message in messages) {
            val sender = usersMap[message.senderId]
            if (sender != null) {
                message.sender = sender
            }

            if (message.type == DOCUMENT) {
                val f = File(externalDocumentsDir, message.metaData?.originalFileName!!)
                message.isDownloaded = f.exists()
            } else if (message.type == IMAGE) {
                val f = File(externalImagesDir, message.metaData?.originalFileName!!)
                message.isDownloaded = f.exists()
            }
        }

        messageDao.insertMessages(messages)
        chatChannelDao.updateItem(chatChannel)

    }

    fun filterPosts(posts: List<Post>): List<Post> {
        val currentUser = currentLocalUser.value
        return filter(currentUser, posts)
    }

    private fun filter(user: User?, posts: List<Post>): List<Post> {
        if (user == null) return posts

        val userDetails = user.userPrivate
        for (post in posts) {
            post.postLocalData.isCreator = userDetails.projectIds.contains(post.id) || userDetails.blogIds.contains(post.id)
            post.postLocalData.isLiked = userDetails.likedPosts.contains(post.id)
            post.postLocalData.isSaved = userDetails.savedPosts.contains(post.id)
            post.postLocalData.isDisliked = userDetails.dislikedPosts.contains(post.id)
            post.postLocalData.isCollaboration = userDetails.collaborationIds.contains(post.id)

            if (!post.postLocalData.isCreator) {
                post.postLocalData.isUserFollowed = userDetails.followings.contains(post.uid)
            }
        }

        return posts
    }


    suspend fun getProjectContributors(post: Post): Result<List<User>> {
        return firebaseUtility.getProjectContributors(post)
    }

    suspend fun getRandomTopUsers(): Result<QuerySnapshot> {
        return firebaseUtility.getRandomTopUsers()
    }

    suspend fun insertMessages(externalImagesDir: File, externalDocumentsDir: File, messages: List<SimpleMessage>, chatChannel: ChatChannel) {
        insertMessagesWithFilter(externalImagesDir, externalDocumentsDir, messages, chatChannel)
    }

    /*suspend fun getMessages(chatChannelId: String, key: PageKey? = null, lim: Int): List<SimpleMessage> {
        return if (key != null) {
            val startAfterId = key.startAfter?.id
            when {
                startAfterId != null -> {
                    val message = messageDao.getMessage(startAfterId)
                    return if (message != null) {
                        val startAfterTime = message.createdAt
                        return messageDao.getMessagesAfter(chatChannelId, startAfterTime, lim)
                    } else listOf()
                }
                else -> {
                    return listOf()
                }
            }
        } else {
            messageDao.getMessages(chatChannelId, lim)
        }
    }*/

	suspend fun clearUsers() {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            userDao.deleteOtherUsers()
        } else {
            userDao.clearUsers()
        }
	}

    private suspend fun filterUser(user: User): User {
        return filterUsers(listOf(user))[0]
    }

    suspend fun filterUsers(users: List<User>, userSource: UserSource? = null): List<User> {
        when (userSource) {
            is UserSource.Acceptance -> {
                for (user in users) {
                    val existingList = user.userPrivate.chatChannels.toMutableList()
                    existingList.add(userSource.post.chatChannelId!!)
                    user.userPrivate.chatChannels = existingList
                }
            }
            is UserSource.Contributor -> {
                for (user in users) {
                    val existingList = user.userPrivate.chatChannels.toMutableList()
                    existingList.add(userSource.chatChannel.chatChannelId)
                    user.userPrivate.chatChannels = existingList
                }
                userSource.chatChannel.lastMessage?.sender = users.find {
                    it.id == userSource.chatChannel.lastMessage?.senderId
                } ?: throw Exception("${userSource.chatChannel.lastMessage?.senderId} is not valid")

                chatChannelDao.insert(userSource.chatChannel)
            }
            is UserSource.Follower -> {
                for (user in users) {
                    val existingList = user.userPrivate.followers.toMutableList()
                    existingList.add(userSource.otherUser.id)
                    user.userPrivate.followers = existingList
                }
            }
            is UserSource.Following -> {
                for (user in users) {
                    val existingList = user.userPrivate.followers.toMutableList()
                    existingList.add(userSource.otherUser.id)
                    user.userPrivate.followers = existingList
                }
            }
            is UserSource.Search -> {

            }
            else -> {

            }
        }

        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val userDetails = currentUser.userPrivate
            for (user in users) {
                val isCurrentUser = currentUser.id == user.id
                user.isCurrentUser = isCurrentUser
                if (!isCurrentUser) {
                    user.isUserFollowed = userDetails.followings.contains(user.id)
                    user.isUserFollowingMe = userDetails.followers.contains(user.id)
                }
            }
        }
        return users
    }

    /*suspend fun getUsers(userSource: UserSource, key: PageKey?, loadSize: Int): List<User> {
        val userSourceTag = "UserSourceTag"
        Log.d(userSourceTag, "Starting to get users from local database .. ")
        val currentUser = currentLocalUser.value
        return when (userSource) {
            is UserSource.Follower -> {
                Log.d(userSourceTag, "Getting followers .. ")
                val isCurrentUser = currentUser?.id == userSource.otherUser.id
                val query = "%${userSource.searchQuery}%"
                val otherUserId = "%${userSource.otherUser.id}%"
                if (key != null) {
                    Log.d(userSourceTag, "key is not null .. ")
                    val endBeforeId = key.endBefore?.id
                    val startAfterId = key.startAfter?.id
                    when {
                        endBeforeId != null -> {
                            Log.d(userSourceTag, "endBeforeId -> $endBeforeId")
                            val user = userDao.getUser(endBeforeId)
                            return if (user != null) {
                                val endBeforeName = user.name
                                return if (userSource.searchQuery != null) {
                                    if (isCurrentUser) {
                                        userDao.getFollowersForCurrentUserBeforeWithQuery(query, endBeforeName, loadSize)
                                    } else {
                                        userDao.getFollowersBeforeWithQuery(otherUserId, query, endBeforeName, loadSize)
                                    }
                                } else {
                                    if (isCurrentUser) {
                                        userDao.getFollowersForCurrentUserBeforeWithQuery(query, endBeforeName, loadSize)
                                    } else {
                                        userDao.getFollowersBefore(userSource.otherUser.id, endBeforeName, loadSize)
                                    }
                                }
                            } else emptyList()
                        }
                        startAfterId != null -> {
                            Log.d(userSourceTag, "startAfterId -> $startAfterId")
                            val user = userDao.getUser(startAfterId)
                            return if (user != null) {
                                val startAfterName = user.name
                                return if (userSource.searchQuery != null) {
                                    if (isCurrentUser) {
                                        userDao.getFollowersForCurrentUserAfterWithQuery(query, startAfterName, loadSize)
                                    } else {
                                        userDao.getFollowersAfterWithQuery(otherUserId, query, startAfterName, loadSize)
                                    }
                                } else {
                                    if (isCurrentUser) {
                                        Log.d(userSourceTag, "It's for the current user")
                                        userDao.getFollowersForCurrentUserAfter(startAfterName, loadSize)
                                    } else {
                                        Log.d(userSourceTag, "It's for some other user")
                                        userDao.getFollowersAfter(otherUserId, startAfterName, loadSize)
                                    }
                                }
                            } else emptyList()
                        }
                        else -> {
                            Log.d(userSourceTag, "Key is not null, but the content is null")
                            return if (userSource.searchQuery != null) {
                                if (isCurrentUser) {
                                    userDao.getFollowersForCurrentUserWithQuery(userSource.searchQuery, loadSize)
                                } else {
                                    userDao.getFollowersWithQuery(otherUserId, query, loadSize)
                                }
                            } else {
                                if (isCurrentUser) {
                                    Log.d(userSourceTag, "Getting followers for current user ..")
                                    userDao.getFollowersForCurrentUser(loadSize)
                                } else {
                                    Log.d(userSourceTag, "Getting followers for other user ..")
                                    userDao.getFollowers(otherUserId, loadSize)
                                }
                            }
                        }
                    }
                } else {
                    return if (userSource.searchQuery != null) {
                        if (isCurrentUser) {
                            userDao.getFollowersForCurrentUserWithQuery(query, loadSize)
                        } else {
                            userDao.getFollowersWithQuery(otherUserId, query, loadSize)
                        }
                    } else {
                        if (isCurrentUser) {
                            userDao.getFollowersForCurrentUser(loadSize)
                        } else {
                            userDao.getFollowers(otherUserId, loadSize)
                        }
                    }
                }
            }
            is UserSource.Following -> {
                Log.d(userSourceTag, "Getting followings .. ")
                val isCurrentUser = currentUser?.id == userSource.otherUser.id
                val query = "%${userSource.searchQuery}%"
                val otherUserId = "%${userSource.otherUser.id}%"

                if (key != null) {
                    val endBeforeId = key.endBefore?.id
                    val startAfterId = key.startAfter?.id
                    when {
                        endBeforeId != null -> {
                            val user = userDao.getUser(endBeforeId)
                            return if (user != null) {
                                val endBeforeName = user.name
                                if (userSource.searchQuery != null) {
                                    if (isCurrentUser) {
                                        userDao.getFollowingsForCurrentUserBeforeWithQuery(query, endBeforeName, loadSize)
                                    } else {
                                        userDao.getFollowingsWithQueryBefore(otherUserId, query, endBeforeName, loadSize)
                                    }
                                } else {
                                    if (isCurrentUser) {
                                        userDao.getFollowingsForCurrentUserBefore(endBeforeName, loadSize)
                                    } else {
                                        userDao.getFollowingsBefore(otherUserId, endBeforeName, loadSize)
                                    }
                                }
                            } else emptyList()
                        }
                        startAfterId != null -> {
                            val user = userDao.getUser(startAfterId)
                            return if (user != null) {
                                val startAfterName = user.name
                                if (userSource.searchQuery != null) {
                                    if (isCurrentUser) {
                                        userDao.getFollowingsForCurrentUserAfterWithQuery(query, startAfterName, loadSize)
                                    } else {
                                        userDao.getFollowingsWithQueryAfter(otherUserId, query, startAfterName, loadSize)
                                    }
                                } else {
                                    if (isCurrentUser) {
                                        userDao.getFollowingsForCurrentUserAfter(startAfterName, loadSize)
                                    } else {
                                        userDao.getFollowingsAfter(otherUserId, startAfterName, loadSize)
                                    }
                                }
                            } else emptyList()
                        }
                        else -> {
                            return if (userSource.searchQuery != null) {
                                if (isCurrentUser) {
                                    userDao.getFollowingsForCurrentUserWithQuery(query, loadSize)
                                } else {
                                    userDao.getFollowingsWithQuery(query, otherUserId, loadSize)
                                }
                            } else {
                                if (isCurrentUser) {
                                    userDao.getFollowingsForCurrentUser(loadSize)
                                } else {
                                    userDao.getFollowings(otherUserId, loadSize)
                                }
                            }
                        }
                    }
                } else {
                    return if (userSource.searchQuery != null) {
                        if (isCurrentUser) {
                            userDao.getFollowingsForCurrentUserWithQuery(query, loadSize)
                        } else {
                            userDao.getFollowingsWithQuery(query, otherUserId, loadSize)
                        }
                    } else {
                        if (isCurrentUser) {
                            userDao.getFollowingsForCurrentUser(loadSize)
                        } else {
                            userDao.getFollowings(otherUserId, loadSize)
                        }
                    }
                }
            }
            else -> userDao.getAllUsers()
        }
    }*/

    suspend fun getTagsFromFirebase(user: User?): Result<List<String>> {
        return firebaseUtility.getTagsFromFirebase(user)
    }

    suspend fun getTopProjects(): Result<QuerySnapshot> {
        return firebaseUtility.getTopProjects()
    }

    suspend fun getTopBlogs(): Result<QuerySnapshot> {
        return firebaseUtility.getTopBlogs()
    }

    private var chatChannelListenerRegistration: ListenerRegistration? = null

    suspend fun setChatChannelsListeners() {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            chatChannelListenerRegistration?.remove()
            chatChannelListenerRegistration = Firebase.firestore
                .collection(CHAT_CHANNELS)
                .whereArrayContains(CONTRIBUTORS_LIST, currentUser.id)
                .orderBy(UPDATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->

                    if (error != null) {
                        networkErrors.postValue(error)
                    }

                    if (value != null && !value.isEmpty) {
                        val chatChannels = value.toObjects(ChatChannel::class.java)

                        scope.launch(Dispatchers.IO) {
                            for (chatChannel in chatChannels) {
                                val senderId = chatChannel.lastMessage?.senderId
                                if (senderId != null) {
                                    val otherLocalUser = userDao.getUser(senderId)
                                    if (otherLocalUser != null) {
                                        chatChannel.lastMessage?.sender = otherLocalUser
                                    } else {
                                        val otherUser = firebaseUtility.getUser(senderId)
                                        if (otherUser != null) {
                                            chatChannel.lastMessage?.sender = otherUser
                                        }
                                        /*try {
                                            val fetchedUserTask = db.collection(USERS).document(senderId).get()
                                            val user = fetchedUserTask.await().toObject(User::class.java)!!
                                            chatChannel.lastMessage?.sender = user
                                        } catch (e: Exception) {
                                            Log.e(BUG_TAG, e.localizedMessage!!)
                                        }*/
                                    }
                                }
                            }
                            chatChannelDao.insertChatChannels(chatChannels)
                        }
                    }
                }
        }
    }

    suspend fun clearNotifications() {
        notificationDao.clearNotifications()
    }

    suspend fun insertNotifications(notifications: List<SimpleNotification>) {
        notificationDao.insertItems(notifications)
    }

    suspend fun insertRequests(requests: List<SimpleRequest>) {
        requestDao.insertItems(requests)
    }

//    fun setChannelContributorsListener(chatChannel: ChatChannel) {
////        firebaseUtility.setChannelContributorsListener(chatChannel)
//    }

    fun getRecentSearchesByType(type: String): Flow<List<RecentSearch>> {
        return recentSearchDao.getRecentSearchesByType(type)
    }

    suspend fun deleteRecentSearch(query: String) {
        recentSearchDao.deleteRecentSearch(query)
    }

    suspend fun addRecentSearch(recentSearch: RecentSearch) {
        recentSearchDao.insertRecentSearch(recentSearch)
    }

    suspend fun fetchSavedPosts(i: Int, f: Int): Result<QuerySnapshot> {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            firebaseUtility.fetchSavedPosts(currentUser, i, f)
        } else {
            Result.Error(Exception("User is null."))
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        firebaseUtility.signInWithGoogle(credential)
    }

    suspend fun addInterest(interest: String) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val updatedUser = firebaseUtility.addInterest(currentUser, interest)
            if (updatedUser != null) {
                insertCurrentUser(updatedUser)
            }
        }
    }

    suspend fun removeInterest(interest: String) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val updatedUser = firebaseUtility.removeInterest(currentUser, interest)
            if (updatedUser != null) {
                insertCurrentUser(updatedUser)
            }
        }
    }


    suspend fun localMessages(type: String = IMAGE, chatChannelId: String, limit: Int = 0): List<SimpleMessage> {
        return if (limit == 0) {
            messageDao.getDownloadedMessagesByType(type, chatChannelId)
        } else {
            messageDao.getDownloadedMessagesByType(type, chatChannelId, limit)
        }
    }

    suspend fun deleteWholeDatabase() {
        userDao.clearUsers()
        postDao.clearPosts()
        messageDao.clearMessages()
        notificationDao.clearNotifications()
        chatChannelDao.clearChatChannels()
    }

    suspend fun getChannelsForQuery(query: String): List<ChatChannel> {
        return chatChannelDao.getChannelsForQuery(query)
    }

    fun sendComment(post: Post, comment: String) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            firebaseUtility.sendComment(currentUser, post, comment)
        }
    }

    /*fun tempFixBlog(post: Post) {
        firebaseUtility.tempFixBlog(post)
    }*/

    suspend fun likeComment(comment: SimpleComment): SimpleComment {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            firebaseUtility.likeComment(currentUser, comment)
        } else {
            comment
        }
    }

    suspend fun dislikeComment(comment: SimpleComment): SimpleComment {
        val currentUser = currentLocalUser.value
        return if (currentUser != null) {
            firebaseUtility.dislikeComment(currentUser, comment)
        } else {
            comment
        }
    }

    fun sendCommentReply(parentComment: SimpleComment, comment: String) {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            firebaseUtility.sendCommentReply(currentUser, parentComment, comment)
        }
    }

    suspend fun insertPublicUserData(userHalf: User) {
        val cachedUser = userDao.getCachedUser()
        if (cachedUser != null) {
            userHalf.userPrivate = cachedUser.userPrivate
            insertCurrentUser(userHalf)
        } else {
            Log.d(TAG, "New user data cannot be inserted if there is no cached user, as there are two documents - User and UserPrivate mixed together")
        }
    }

    suspend fun insertPrivateUserData(userHalf: UserPrivate) {
        val cachedUser = userDao.getCachedUser()
        if (cachedUser != null) {
            cachedUser.userPrivate = userHalf
            insertCurrentUser(cachedUser)
        } else {
            Log.d(TAG, "New user data cannot be inserted if there is no cached user, as there are two documents - User and UserPrivate mixed together")
        }
    }

    suspend fun fetchCurrentUser(firebaseUser: FirebaseUser) {
        currentFirebaseUser.postValue(firebaseUser)
        val currentUser = firebaseUtility.getCurrentUser()
        if (currentUser != null) {
            insertCurrentUser(currentUser)
        } else {
            Log.w(TAG, "Couldn't fetch current user data.")
        }
    }

    val usersMap = mutableMapOf<String, User>()
    private val listOfUsers = mutableListOf<User>()

    suspend fun insertChannelContributors(chatChannel: ChatChannel, contributors: List<User>) {
        for (contributor in contributors) {
            if (!usersMap.containsKey(contributor.id)) {
                if (currentLocalUser.value?.id == contributor.id) {
                    usersMap[contributor.id] = currentLocalUser.value!!
                } else {
                    val existingList = contributor.userPrivate.chatChannels.toMutableList()
                    existingList.add(chatChannel.chatChannelId)
                    contributor.userPrivate.chatChannels = existingList
                    usersMap[contributor.id] = contributor
                }
            } else {
                val existingUser = usersMap[contributor.id]!!
                val existingList = existingUser.userPrivate.chatChannels.toMutableList()
                existingList.add(chatChannel.chatChannelId)
                existingUser.userPrivate.chatChannels = existingList
                usersMap[contributor.id] = existingUser
            }
        }

        usersMap.forEach {
            listOfUsers.add(it.value)
        }

        chatChannel.lastMessage?.sender = listOfUsers.find {
            it.id == chatChannel.lastMessage?.senderId
        }!!

        chatChannelDao.updateItem(chatChannel)
        userDao.insertItems(filterUsers(listOfUsers))
    }

    fun clearMediaDownloadResult() {
        mediaDownloadResult.postValue(null)
    }

    fun clearMediaUploadResult() {
        mediaUploadResult.postValue(null)
    }

}