package com.jamid.workconnect.data

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
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

    private val messageDao = db.messageDao()
    private val chatChannelContributorDao = db.chatChannelContributorDao()
    private val simpleMediaDao = db.simpleMediaDao()
    private val chatChannelDao = db.chatChannelDao()
    private val notificationDao = db.notificationDao()
    private val userDao = db.userDao()
    private val postDao = db.postDao()
    private val requestDao = db.activeRequestDao()
    private val simpleTagsDao = db.simpleTagsDao()
    private val recentSearchDao = db.recentSearchDao()
    var firebaseUtility = FirebaseUtilityImpl(scope, db)

    val declineProjectResult = firebaseUtility.declineRequestResult
    val undoProjectResult = firebaseUtility.undoRequestSent
    val acceptProjectResult = firebaseUtility.acceptRequestResult

    val mediaDownloadResult = firebaseUtility.mediaDownloadResult
    val signInResult: LiveData<Result<FirebaseUser>> = firebaseUtility.signInResult
    val registerResult: LiveData<Result<FirebaseUser>> = firebaseUtility.registerResult
    val currentFirebaseUser: LiveData<FirebaseUser> = firebaseUtility.currentFirebaseUser
    val currentLocalUser = firebaseUtility.currentLocalUser
    val usernameExists: LiveData<Result<Boolean>> = firebaseUtility.usernameExists
    val emailExists: LiveData<Boolean> = firebaseUtility.emailExists
    val profilePhotoUpload = firebaseUtility.profilePhotoUpload
    val postPhotoUpload: LiveData<Result<Uri>> = firebaseUtility.postPhotoUpload
    val postUpload: LiveData<Result<Post>> = firebaseUtility.postUpload
    val requestSent: LiveData<Result<String>> = firebaseUtility.requestSent
    val mediaUploadResult: LiveData<Result<SimpleMessage>> = firebaseUtility.mediaUploadResult
    val updateUser: LiveData<Result<Map<String, Any?>>> = firebaseUtility.updateUser
    val networkErrors: LiveData<Exception> = firebaseUtility.networkErrors
    val guidelinesUpdateResult: LiveData<Post> = firebaseUtility.guidelinesUpdateResult
    val firebaseUserUpdateResult: LiveData<Result<FirebaseUser>> = firebaseUtility.firebaseUserUpdateResult
    val localTagsList = firebaseUtility.localTagsList
    //    val uid: String = firebaseUtility.uid
    val chatChannelsLiveData = chatChannelDao.allChannels()


    val mapOfDocumentSnapshots = mutableMapOf<String, DocumentSnapshot>()

//    val allRecentSearches = recentSearchDao.allRecentSearches()

    // new set
    val currentOtherUserDetail = firebaseUtility.currentOtherUserDetail

    init {
    	/*scope.launch (Dispatchers.IO) {
    	    postDao.clearPosts()
    	}*/
    }


    val uid: String? = firebaseUtility.uid

    suspend fun getChatMessages(chatChannelId: String) : List<SimpleMessage> {
        return messageDao.getChatMessages(chatChannelId)
    }

    suspend fun updateMessage(messages: List<SimpleMessage>) {
//        messageDao.insertMessages(messages)
    }

    fun getChatMessagesAfter(chatChannelId: String, key: Long): List<SimpleMessage> {
        return messageDao.getChatMessagesAfter(chatChannelId, key)
    }

    suspend fun updateMessage(message: SimpleMessage) {
        messageDao.updateItem(message)
    }

    fun channelContributors(chatChannelId: String): LiveData<List<ChatChannelContributor>> {
        return chatChannelContributorDao.getChatChannelContributors(chatChannelId)
    }

    fun channelContributorsLive(channelId: String): LiveData<List<User>> {
        return userDao.getChannelContributorsLive(channelId)
    }

    suspend fun clearChatChannelContributorsAndChannelIds() {
        chatChannelContributorDao.clearEverything()
    }

    suspend fun checkIfDownloaded(messageId: String): SimpleMedia? {
        return simpleMediaDao.getSimpleMedia(messageId)
    }

    suspend fun insertSimpleMedia(simpleMedia: SimpleMedia?) {
        simpleMedia?.let {
            simpleMediaDao.insertSimpleMedia(listOf(it))
        }
    }

    /////////////////////////////////////////////////////////////////////

    fun signIn(email: String, password: String) {
        firebaseUtility.signIn(email, password)
    }

    fun register(email:String, password: String) {
        firebaseUtility.register(email, password)
    }

    fun uploadUser(tags: List<String>? = null) {
        val user = firebaseUtility.createNewUser(tags)
        if (user != null) {
            firebaseUtility.uploadCurrentUser(user)
        }
    }

    fun updateRegistrationToken(token: String) {
        firebaseUtility.updateRegistrationToken(token)
    }

    fun checkIfUsernameExists(username: String) {
        firebaseUtility.checkIfUsernameExists(username)
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

    fun updateUser(userMap: Map<String, Any?>) {
        firebaseUtility.updateCurrentUser(userMap)
    }

    fun signOut() {
        firebaseUtility.signOut()
    }

    fun onLikePressed(post: Post): Post {
        return firebaseUtility.onLikePressedWithoutCaching(post)
    }

    fun onDislikePressed(post: Post): Post {
        return firebaseUtility.onDislikePressedWithoutCaching(post)
    }

    fun onSaved(post: Post): Post {
        return firebaseUtility.onPostSavedWithoutCaching(post)
    }

    fun onFollowPressed(post: Post): Post {
        return firebaseUtility.onFollowPressed(post)
    }

    fun onFollowPressed(currentUser: User, otherUser: User) {
        firebaseUtility.onFollowPressed(currentUser, otherUser)
    }

    fun uploadPost(post: Post) {
        firebaseUtility.uploadPost(post)
    }

    fun joinProject(post: Post) {
        firebaseUtility.joinProject(post)
    }

    fun uploadMessageMedia(message: SimpleMessage, chatChannel: ChatChannel) {
        firebaseUtility.uploadMessageMedia(message, chatChannel)
    }

    fun sendMessage(message: SimpleMessage, chatChannel: ChatChannel) {
        firebaseUtility.sendMessage(message, chatChannel)
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

    fun onNewMessageNotification(chatChannelId: String, onComplete: (chatChannel: ChatChannel) -> Unit) {
        firebaseUtility.onNewMessagesFromBackground(chatChannelId) {
            onComplete(it)
        }
    }

    /*fun getPopularInterests(onComplete: (interests: List<PopularInterest>) -> Unit) {
        firebaseUtility.getPopularInterests {
            onComplete(it)
        }
    }*/

    suspend fun getCurrentUserAsContributor(uid: String, chatChannelId: String) : ChatChannelContributor? {
        return chatChannelContributorDao.getContributor(uid, chatChannelId)
    }

    fun getCachedPost(postId: String) : LiveData<Post> {
        return postDao.getPostLive(postId)
    }

    fun updatePost(post: Post, guidelines: String) {
        post.guidelines = guidelines
        updatePost(post, mapOf("guidelines" to guidelines))
    }

    private fun updatePost(post: Post, map: Map<String, Any?>) {
        firebaseUtility.updatePost(post, map)
    }

    suspend fun getLocalMedia(messageId: String) : SimpleMedia? {
        return simpleMediaDao.getSimpleMedia(messageId)
    }

    fun downloadMedia(externalDir: File?, message: SimpleMessage) {
        firebaseUtility.downloadMedia(externalDir, message)
    }

    fun listenToDownloadProcess(messageId: String): LiveData<SimpleMedia> {
        return simpleMediaDao.getSimpleMediaLive(messageId)
    }

    fun getMedia(chatChannelId: String, messageId: String, onComplete: (simpleMedia: SimpleMedia) -> Unit) {
        firebaseUtility.getMedia(chatChannelId, messageId) {
            onComplete(it)
        }
    }

    fun getContributorsForPost(channelId: String) {
        firebaseUtility.getContributorsForPost(channelId)
    }

    fun getChannelContributors(chatChannelId: String, pageSize: Long, extra: DocumentSnapshot? = null, ahead: Boolean = false, onComplete: (contributors: QuerySnapshot) -> Unit) {
        firebaseUtility.getChannelContributors(chatChannelId, pageSize, extra, ahead) {
            onComplete(it)
        }
    }

    fun getContributorSnapshot(channelId: String, id: String, onComplete: (doc: DocumentSnapshot) -> Unit) {
        firebaseUtility.getContributorSnapshot(channelId, id) {
            onComplete(it)
        }
    }

    fun updateGuidelines(postId: String, guidelines: String) {
        firebaseUtility.updateGuidelines(postId, guidelines)
    }

    fun clearGuideUpdateResult() {
        firebaseUtility.guidelinesUpdateResult.postValue(null)
    }

    fun clearSignInChanges() {
        firebaseUtility.clearSignInChanges()
    }

    fun getPost(postId: String) {
        firebaseUtility.getPost(postId)
    }

    fun <T: Any> getPostsOnStart(query: Query, ofClass: Class<T>, onComplete: (items: List<T>) -> Unit) {
        firebaseUtility.getPostsOnStart(query, ofClass) {
            onComplete(it)
        }
    }

    fun <T: Any> getItems(limit: Long, query: Query, clazz: Class<T>, extras: Map<String, Any?>?, onComplete: (firstDoc: DocumentSnapshot?, lastDoc: DocumentSnapshot?, isEnd: Boolean) -> Unit) {
        firebaseUtility.getItems(limit, query, clazz, extras) { doc1, doc2, isEnd ->
            onComplete(doc1, doc2, isEnd)
        }
    }

    fun getItemsWithoutCaching(query: Query, onComplete: (querySnapshot: QuerySnapshot) -> Unit) {
        firebaseUtility.getItemsWithoutCaching(query) {
            onComplete(it)
        }
    }

    fun <T> getSnapshot(item: T, clazz: Class<T>, onComplete: (doc: DocumentSnapshot) -> Unit) {
        firebaseUtility.getSnapshot(item, clazz) {
            onComplete(it)
        }
    }

    fun getSnapshot(query: DocumentReference, onComplete: (doc: DocumentSnapshot) -> Unit) {
        firebaseUtility.getSnapshot(query) {
            onComplete(it)
        }
    }

    fun <T: Any> getObject(objectId: String, objectClass: Class<T>, onComplete: (obj: T?) -> Unit) {
        Log.d("GET_FIREBASE_OBJECT", "MainRepo")
        firebaseUtility.getObject(objectId, objectClass) {
            Log.d("GET_FIREBASE_OBJECT", "MainRepo - after success")
            onComplete(it)
        }
    }

    inline fun <reified T: Any> getObject(documentReference: DocumentReference, crossinline onComplete: (obj: T) -> Unit) {
        firebaseUtility.getObject<T>(documentReference) {
            onComplete(it)
        }
    }

    fun <T> getObject(documentReference: DocumentReference, clazz: Class<T>, onComplete: (obj: T) -> Unit) {
        firebaseUtility.getObject(documentReference, clazz) {
            onComplete(it)
        }
    }

    fun getOtherUser(userId: String) {
        val docRef = Firebase.firestore.collection(USERS).document(userId).collection("private").document(userId)
        firebaseUtility.getSnapshot(docRef) {
            val userPrivate = it.toObject(UserPrivate::class.java)!!
            currentOtherUserDetail.postValue(userPrivate)
        }
    }

    suspend fun getLocalUser(uid: String) : User? {
        return userDao.getUser(uid)
    }

    fun setProfilePhotoUploadResult(s: String?) {
        profilePhotoUpload.postValue(s?.toUri())
    }

    fun acceptProjectRequest(notification: SimpleNotification) {
        firebaseUtility.acceptProjectRequest(notification)
    }

    fun declineProjectRequest(notification: SimpleNotification) {
        firebaseUtility.denyProjectRequest(notification)
    }

    fun undoProjectRequest(request: SimpleRequest) {
        firebaseUtility.undoProjectRequest(request)
    }

    suspend fun getChannelContributors(channelId: String) : List<User> {
        return userDao.getChannelContributors(channelId)
    }

    /*fun getChannelContributorsFromFirebase(channelId: String) {
        firebaseUtility.getChannelContributors(channelId)
    }
*/
   /* fun getChatChannels() {
        Log.d(ChatChannelFragment.TAG, "Getting chat channels - REP")
        firebaseUtility.getChatChannelsFromFirebase()
    }*/

    fun getLocalPostContributors(channelId: String): LiveData<List<User>> {
        return userDao.getChannelContributorsLive(channelId)
    }

    fun getSavedPosts(initialItemPosition: Int, finalItemPosition: Int) {
        Log.d(BUG_TAG, "Getting saved posts ... RP")
        firebaseUtility.getSavedPosts(initialItemPosition, finalItemPosition)
    }

    suspend fun deleteLocalRequest(notificationId: String, postId: String, requestId: String, chatChannelId: String) {
        Log.d(BUG_TAG, "Delete local request")
        val currentUser = currentLocalUser.value!!
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

//        firebaseUtility.getChatChannelFromFirebase(chatChannelId)
        firebaseUtility.getNotificationFromFirebase(notificationId)

        userDao.updateItem(currentUser)

    }

    fun setNotificationListener() {
        firebaseUtility.setNotificationListener()
    }

    suspend fun clearPosts() {
        postDao.clearPosts()
    }

    fun getNotifications() {
        firebaseUtility.getNotifications()
    }

    fun getMyRequests() {
        firebaseUtility.getMyRequests()
    }

    suspend fun clearRequests() {
        requestDao.clearRequests()
    }

    fun increaseProjectWeightage(cachedPost: Post) {
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
        firebaseUtility.updateOtherUser(otherUser, mapOf<String, Any?>(SEARCH_RANK to userSearchRank))


        // TODO("If the user is followed or following the current user, change the weightage of the user
        //      both in fireStore and locally")
        //      both in fireStore and locally")
    }

    suspend fun insertPosts(posts: List<Post>, source: PostSource) = scope.launch (Dispatchers.IO) {
        val returnedPosts = filterPosts(posts)
        postDao.insertItems(returnedPosts)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun insertMessagesWithFilter(externalImagesDir: File, externalDocumentsDir: File, messages: List<SimpleMessage>, chatChannel: ChatChannel) {

        val lastMessage = messages.first()
        chatChannel.lastMessage = lastMessage

        for (message in messages) {
            if (message.type == DOCUMENT) {
                val f = File(externalDocumentsDir, message.metaData?.originalFileName!!)
                message.isDownloaded = f.exists()
                Log.d(BUG_TAG, message.content + " " + message.isDownloaded)
            } else if (message.type == IMAGE) {
                val f = File(externalImagesDir, message.metaData?.originalFileName!!)
                message.isDownloaded = f.exists()
                Log.d(BUG_TAG, message.content + " " + message.isDownloaded)
            }
        }

        messageDao.insertMessages(messages)
        chatChannelDao.updateItem(chatChannel)

    }


    // TODO("This function actually doesn't require postSource, the filters have to applied always.")
    fun filterPosts(posts: List<Post>): List<Post> {
        val currentUser = currentLocalUser.value
        return filter(currentUser, posts)
    }

    private fun filter(user: User?, posts: List<Post>): List<Post> {
        if (user == null) return posts

        val userDetails = user.userPrivate
//        val interests = userDetails.interests.toSet()
//        val finalSet = mutableSetOf<String>()

        for (post in posts) {

            post.postLocalData.isCreator = userDetails.projectIds.contains(post.id) || userDetails.blogIds.contains(post.id)
            post.postLocalData.isLiked = userDetails.likedPosts.contains(post.id)
            post.postLocalData.isSaved = userDetails.savedPosts.contains(post.id)
            post.postLocalData.isDisliked = userDetails.dislikedPosts.contains(post.id)
            post.postLocalData.isCollaboration = userDetails.collaborationIds.contains(post.id)

//            val tempSet = post.tags.toMutableSet()
//            val intersection = tempSet.intersect(interests)

            /*if (intersection.isNotEmpty()) {
                finalSet.addAll(intersection)
            }*/

            if (!post.postLocalData.isCreator) {
                post.postLocalData.isUserFollowed = userDetails.followings.contains(post.uid)
            }

//            post.postLocalData.inFeed = true

        }

        /*val tags = mutableListOf<SimpleTag>()
        for (item in finalSet) {
            tags.add(SimpleTag(item, 0))
        }

        simpleTagsDao.insertItems(tags)*/
        return posts
    }

    suspend fun <T> getLocalItems(params: PagingSource.LoadParams<String>, clazz: Class<T>): List<T> {
        /*when (clazz) {
            Post::class.java -> {
                val postId = params.key
                if (postId != null) {
                    val anchorPost = postDao.getPost(postId)
                    if (anchorPost != null) {
                        val anchorTime = anchorPost.createdAt
                        val posts = postDao.getPostsBasedOnTime(anchorTime)
                        return posts as List<T>
                    }
                }
            }
        }*/
        TODO("return a list of items from database of generic type")
    }

    suspend fun getLocalPosts(params: PagingSource.LoadParams<String>): List<Post> {
        val postId = params.key
        return if (postId != null) {
            val anchorPost = postDao.getPost(postId)
            if (anchorPost != null) {
                val anchorTime = anchorPost.createdAt
                return postDao.getPostsBasedOnTime(anchorTime, params.loadSize) ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /*suspend fun getPosts(postSource: PostSource, key: PageKey? = null, lim: Int): List<Post> {
        return when (postSource) {
            is PostSource.FeedRandom -> {
                if (key != null) {
                    val endBeforeId = key.endBefore?.id
                    val startAfterId = key.startAfter?.id
                    when {
                        endBeforeId != null -> {
                            val post = postDao.getPost(endBeforeId)
                            return if (post != null) {
                                val endBeforeTime = post.createdAt
                                return postDao.getPostsBefore(endBeforeTime, lim)
                            } else emptyList()
                        }
                        startAfterId != null -> {
                            val post = postDao.getPost(startAfterId)
                            return if (post != null) {
                                val startAfterTime = post.createdAt
                                return postDao.getPostsAfter(startAfterTime, lim)
                            } else listOf()
                        }
                        else -> {
                            return postDao.getPosts(lim)
                        }
                    }
                } else {
                    postDao.getPosts(lim)
                }
            }
            is PostSource.FeedWithFollowings -> {
                if (key != null) {
                    val endBeforeId = key.endBefore?.id
                    val startAfterId = key.startAfter?.id
                    when {
                        endBeforeId != null -> {
                            val post = postDao.getPost(endBeforeId)
                            return if (post != null) {
                                val endBeforeTime = post.createdAt
                                return postDao.getPostsBasedOnUserBefore(postSource.currentUser.id, endBeforeTime, lim)
                            } else emptyList()
                        }
                        startAfterId != null -> {
                            val post = postDao.getPost(startAfterId)
                            return if (post != null) {
                                val startAfterTime = post.createdAt
                                return postDao.getPostsBasedOnUserAfter(postSource.currentUser.id, startAfterTime, lim)
                            } else emptyList()
                        }
                        else -> {
                            return postDao.getPostsBasedOnUserAfter(postSource.currentUser.id, System.currentTimeMillis(), lim)
                        }
                    }
                } else {
                    postDao.getPostsBasedOnUserAfter(postSource.currentUser.id, System.currentTimeMillis(), lim)
                }
            }
            is PostSource.FeedWithOtherUserAndType -> {
                if (key != null) {
                    val endBeforeId = key.endBefore?.id
                    val startAfterId = key.startAfter?.id
                    when {
                        endBeforeId != null -> {
                            val post = postDao.getPost(endBeforeId)
                            return if (post != null) {
                                val endBeforeTime = post.createdAt
                                return postDao.getUserProjectsBefore(postSource.otherUser.id, postSource.type, lim, endBeforeTime)
                            } else emptyList()
                        }
                        startAfterId != null -> {
                            val post = postDao.getPost(startAfterId)
                            return if (post != null) {
                                val startAfterTime = post.createdAt
                                return postDao.getUserProjectsAfter(postSource.otherUser.id, postSource.type, lim, startAfterTime)
                            } else emptyList()
                        }
                        else -> {
                            return postDao.getUserProjects(postSource.otherUser.id, postSource.type, lim)
                        }
                    }
                } else {
                    postDao.getUserProjects(postSource.otherUser.id, postSource.type, lim)
                }
            }
            is PostSource.FeedWithOtherUserCollaborations -> {
                if (key != null) {
                    val endBeforeId = key.endBefore?.id
                    val startAfterId = key.startAfter?.id
                    when {
                        endBeforeId != null -> {
                            val post = postDao.getPost(endBeforeId)
                            return if (post != null) {
                                val endBeforeTime = post.createdAt
                                return postDao.getUserCollaborationsBefore("%${postSource.otherUser.id}%", endBeforeTime, lim)
                            } else emptyList()
                        }
                        startAfterId != null -> {
                            val post = postDao.getPost(startAfterId)
                            return if (post != null) {
                                val startAfterTime = post.createdAt
                                return postDao.getUserCollaborationsAfter("%${postSource.otherUser.id}%", startAfterTime, lim)
                            } else emptyList()
                        }
                        else -> {
                            return postDao.getUserCollaborations(postSource.otherUser.id, lim)
                        }
                    }
                } else {
                    postDao.getUserCollaborations("%${postSource.otherUser.id}%", lim)
                }
            }
            is PostSource.FeedWithTags -> {
                throw Exception("This type of post source is not supported.")
            }
        }
    }*/

    suspend fun getProjectContributors(post: Post): Result<QuerySnapshot> {
        return firebaseUtility.getProjectContributors(post)
    }

    suspend fun getRandomTopUsers(): Result<QuerySnapshot> {
        return firebaseUtility.getRandomTopUsers()
    }

    suspend fun clearMessages(chatChannelId: String) {
        messageDao.clearMessages(chatChannelId)
    }

    suspend fun insertMessages(externalImagesDir: File, externalDocumentsDir: File, messages: List<SimpleMessage>, chatChannel: ChatChannel) {
        insertMessagesWithFilter(externalImagesDir, externalDocumentsDir, messages, chatChannel)
    }

    suspend fun getMessages(chatChannelId: String, key: PageKey? = null, lim: Int): List<SimpleMessage> {
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
    }

	suspend fun clearUsers() {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            userDao.deleteOtherUsers(currentUser.id)
        } else {
            userDao.deleteAllUsers()
        }
	}

    suspend fun insertUsers(users: List<User>, userSource: UserSource) {
        val returnedUsers = filterUsers(users, userSource)
        // TODO("There must be a better way without replace")
        for (user in returnedUsers) {
            val u = userDao.getUser(user.id)
            if (u != null) {
                userDao.updateItem(user)
            } else {
                userDao.insert(user)
            }
        }
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
            is UserSource.Search -> TODO()
        }

        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            val userDetails = currentUser.userPrivate
            for (user in users) {
                user.isUserFollowed = userDetails.followings.contains(user.id)
                user.isUserFollowingMe = userDetails.followers.contains(user.id)
                user.isCurrentUser = currentUser.id == user.id
            }
        }
        return users
    }

    suspend fun getUsers(userSource: UserSource, key: PageKey?, loadSize: Int): List<User> {
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
    }

    suspend fun getTagsFromFirebase(user: User?): Result<List<String>> {
        return firebaseUtility.getTagsFromFirebase(user)
    }

    suspend fun getTopProjects(): Result<QuerySnapshot> {
        return firebaseUtility.getTopProjects()
    }

    suspend fun getTopBlogs(): Result<QuerySnapshot> {
        return firebaseUtility.getTopBlogs()
    }

    fun setChatChannelListeners() {
        firebaseUtility.setChatChannelsListeners()
    }

    fun getContributorsForAllChannels() {
        firebaseUtility.getContributorForAllChannels()
    }

    suspend fun getPagedNotifications(query: Query, limit: Int, startAfter: DocumentSnapshot?): Result<QuerySnapshot> {
        return firebaseUtility.getPagedNotifications(query, limit, startAfter)
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

    fun setChannelContributorsListener(chatChannel: ChatChannel) {
        firebaseUtility.setChannelContributorsListener(chatChannel)
    }

    suspend fun setChannelMessagesListener(externalImagesDir: File, externalDocumentsDir: File, chatChannel: ChatChannel) {
        firebaseUtility.setChannelMessagesListener(externalImagesDir, externalDocumentsDir, chatChannel)
    }

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
        return firebaseUtility.fetchSavedPosts(i, f)
    }

    fun signInWithGoogle(credential: AuthCredential) {
        firebaseUtility.signInWithGoogle(credential)
    }

    fun addInterest(interest: String) {
        firebaseUtility.addInterest(interest)
    }

    fun removeInterest(interest: String) {
        firebaseUtility.removeInterest(interest)
    }

    fun getMessageSender(senderId: String): LiveData<User> {
        return userDao.getMessageSender(senderId)
    }

    fun downloadImage(message: SimpleMessage, externalDir: File) {
        firebaseUtility.downloadImage(message, externalDir)
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
        firebaseUtility = FirebaseUtilityImpl(scope, db)
    }

    suspend fun getChannelsForQuery(query: String): List<ChatChannel> {
        return chatChannelDao.getChannelsForQuery(query)
    }

}