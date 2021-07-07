package com.jamid.workconnect

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.jamid.workconnect.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FirebaseUtilityImpl : FirebaseUtility {

    override val auth = Firebase.auth
    override val db = Firebase.firestore
    override val storage = Firebase.storage

    private val errorPrefix = "Error caused while trying to "
    private val errorSuffix = "Reason : "

    val signInResult = MutableLiveData<Result<FirebaseUser>>().apply { value = null }
    val registerResult = MutableLiveData<Result<FirebaseUser>>().apply { value = null }
    val firebaseUserUpdateResult = MutableLiveData<Result<FirebaseUser>>().apply { value = null }
    val networkErrors = MutableLiveData<Exception>().apply { value = null }
    val usernameExists = MutableLiveData<Result<Boolean>>().apply { value = null }
    val emailExists = MutableLiveData<Boolean>()
    val profilePhotoUpload = MutableLiveData<Uri?>().apply { value = null }
    val postPhotoUpload = MutableLiveData<Result<Uri>>().apply { value = null }
    val postUpload = MutableLiveData<Result<Post>>().apply { value = null }
    val requestSent = MutableLiveData<Result<String>>().apply { value = null }
    val commentSentResult = MutableLiveData<Result<SimpleComment>>().apply { value = null }
    val mediaDownloadResult = MutableLiveData<Result<SimpleMessage>>().apply { value = null }
    val mediaUploadResult = MutableLiveData<Result<SimpleMessage>>().apply { value = null }
    val undoRequestSent = MutableLiveData<Result<SimpleRequest>>().apply { value = null }
    val acceptRequestResult = MutableLiveData<Result<SimpleNotification>>().apply { value = null }
    val declineRequestResult = MutableLiveData<Result<SimpleNotification>>().apply { value = null }
    val guidelinesUpdateResult = MutableLiveData<Post>().apply { value = null }
    val updateUser = MutableLiveData<Result<Map<String, Any?>>>().apply { value = null }
    var currentFirebaseUser: FirebaseUser? = null

    init {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            currentFirebaseUser = firebaseUser
        }
    }

    override suspend fun getToken(): String? {
        return try {
            val task = FirebaseMessaging.getInstance()
                .token

            task.await()
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    override fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { firebaseUser ->
                if (firebaseUser != null) {
                    currentFirebaseUser = firebaseUser.user

                    val result = Result.Success(currentFirebaseUser!!)
                    signInResult.postValue(result)
                }
            }
            .addOnFailureListener {
                val result = Result.Error(it)
                signInResult.postValue(result)
            }
    }

    override fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val result = Result.Success(it.user!!)
                registerResult.postValue(result)
            }
            .addOnFailureListener {
                val result = Result.Error(it)
                registerResult.postValue(result)
            }
    }

    override fun checkIfEmailExists(email: String) {
        auth.signInWithEmailAndPassword(email, "12345678")
            .addOnFailureListener {
                when (it) {
                    is FirebaseAuthEmailException -> {
                        Log.d(
                            MainViewModel.TAG,
                            "FirebaseAuthEmailException - ${it.localizedMessage}"
                        )
                        emailExists.postValue(true)
                    }
                    is FirebaseAuthInvalidUserException -> {
                        emailExists.postValue(false)
                        return@addOnFailureListener
                    }
                    else -> {
                        Log.d(MainViewModel.TAG, "Unknown Error - ${it.localizedMessage}")
                        emailExists.postValue(true)
                    }
                }
            }
            .addOnCanceledListener {
                Log.d(MainViewModel.TAG, "Canceled sign in.")
            }
    }

    override fun signInWithGoogle(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val currentUser = it.user
                if (currentUser != null) {
                    Firebase.firestore.collection(USERS).document(currentUser.uid).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                signInResult.postValue(Result.Success(currentUser))
                                // TODO("The user doc will be accessed twice.")
                            } else {
                                registerResult.postValue(Result.Success(currentUser))
                                networkErrors.postValue(Exception("User document doesn't exist."))
                            }
                        }.addOnFailureListener { e ->
                            networkErrors.postValue(e)
                        }
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = currentFirebaseUser?.uid
        return if (uid != null) {
            val currentUserRef = db.collection(USERS).document(uid)
            val currentUser = getObject<User>(currentUserRef)
            if (currentUser != null) {
                val currentUserOtherInfoRef = currentUserRef.collection(PRIVATE).document(uid)
                val otherInfo = getObject<UserPrivate>(currentUserOtherInfoRef)
                if (otherInfo != null) {
                    currentUser.userPrivate = otherInfo
                    currentUser
                } else {
                    null
                }
            } else {
                return null
            }
        } else {
            null
        }
    }


    override suspend fun checkIfUsernameExists(currentUser: User, username: String?) {
        suspend fun check(u: String) {
            val query = db.collection(USERS).whereEqualTo(USERNAME, u)
            val users = getObjects<User>(query)
            if (users.isEmpty()) {
                usernameExists.postValue(Result.Success(false))
            } else {
                usernameExists.postValue(Result.Success(true))
            }
        }

        if (username != null) {
            if (currentUser.username == username) {
                usernameExists.postValue(Result.Success(false))
                return
            }
            check(username)
        } else {
            val tempUsername = currentUser.email.split('@')[0]
            check(tempUsername)
        }
    }

    override fun createNewUser(tags: List<String>?): User? {
        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            val name = currentUser.displayName ?: return null
            val email = currentUser.email ?: return null
            val photo = if (currentUser.photoUrl == null) null
            else currentUser.photoUrl.toString()
            val username = email.split('@')[0]
            val usernameExists = usernameExists.value
            val userPrivate = UserPrivate(interests = tags ?: emptyList())
            return if (usernameExists != null && usernameExists is Result.Success && usernameExists.data) {
                val newUsername = username + System.currentTimeMillis().toString()
                User(uid, name, newUsername, email, photo = photo, userPrivate = userPrivate)
            } else {
                User(uid, name, username, email, photo = photo, userPrivate = userPrivate)
            }
        } else {
            return null
        }
    }

    override suspend fun uploadCurrentUser(user: User): User? {
        return try {
            val currentUserToken = getToken()
            val currentUserRef = db.collection(USERS).document(user.id)
            val currentUserPrivate = db.collection(USERS)
                .document(user.id)
                .collection(PRIVATE)
                .document(user.id)

            val subStrings = mutableListOf<String>()
            subStrings.addAll(getAllSubStrings(user.name, user.username))
            user.indices = subStrings

            if (currentUserToken != null) {
                user.userPrivate.registrationTokens = listOf(currentUserToken)
            } else {
                user.userPrivate.registrationTokens = emptyList()
            }

            val task = db.runBatch {
                it.set(currentUserRef, user)
                it.set(currentUserPrivate, user.userPrivate)
            }

            task.await()
            user
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    override fun updateFirebaseUser(firebaseUserMap: MutableMap<String, Any?>) {
        val currentUser = auth.currentUser
        if (currentUser != null) {

            val profileUpdates = userProfileChangeRequest {
                displayName = firebaseUserMap["fullName"] as String?
                photoUri = firebaseUserMap["photoUri"] as Uri?
            }

            currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    val user = auth.currentUser
                    if (user != null) {
                        firebaseUserUpdateResult.postValue(Result.Success(user))
                    }
                }
                .addOnFailureListener {
                    firebaseUserUpdateResult.postValue(Result.Error(it))
                }
        }
    }

    @SuppressLint("NullSafeMutableLiveData")
    override fun clearSignInChanges() {
        signInResult.postValue(null)
        registerResult.postValue(null)
        emailExists.postValue(null)
    }

    override suspend fun addInterest(currentUser: User, interest: String): User? {
        return try {
            val existingList = currentUser.userPrivate.interests.toMutableList()
            existingList.add(interest)
            currentUser.userPrivate.interests = existingList

            val task = db.collection(USERS).document(currentUser.id)
                .collection(PRIVATE)
                .document(currentUser.id)
                .update(INTERESTS, FieldValue.arrayUnion(interest))

            task.await()
            currentUser
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun updateCurrentUser(currentUser: User, userMap: Map<String, Any?>): User? {
        return try {
            val userDetail = currentUser.userPrivate
            val fullName = userMap[NAME] as String?
            val username = userMap[USERNAME] as String?
            val interests = userMap[INTERESTS]


            currentUser.userPrivate.interests = interests as List<String>

            val finalMap = userMap.toMutableMap()
            finalMap.remove(INTERESTS)

            val subStrings = mutableListOf<String>()

            val fullNameSubs = fullName?.let {
                currentUser.name = it
                getAllSubStrings(fullName)
            }
            if (fullNameSubs != null) {
                subStrings.addAll(fullNameSubs)
            }

            val usernameSubs = username?.let {
                currentUser.username = it
                getAllSubStrings(username)
            }

            if (usernameSubs != null) {
                subStrings.addAll(usernameSubs)
            }

            finalMap[INDICES] = subStrings
            currentUser.indices = subStrings

            currentUser.photo = userMap[PHOTO] as String?
            currentUser.about = userMap[ABOUT] as String?

            // user doc itself
            val currentUserDocRef = db.collection(USERS).document(currentUser.id)
            val currentUserDocPrivateRef =
                currentUserDocRef.collection(PRIVATE).document(currentUser.id)

            // all the posts ever created by the user
            val postIds = mutableListOf<String>()
            postIds.addAll(userDetail.projectIds)
            postIds.addAll(userDetail.blogIds)


            // all the places where the user has followed somebody
            val followings = userDetail.followings

            // all the places where the user was followed by somebody
            val followers = userDetail.followers

            // all the chat channels where the user has taken part
            val chatChannels = userDetail.chatChannels

            // all requests
            val requests = userDetail.requestIds

            // all notifications
//			val notifications = userDetail.notificationIds
            // TODO("Something wrong here")

            val task = db.runBatch {
                // 1
                it.update(currentUserDocRef, finalMap)

                // 2
                it.update(currentUserDocPrivateRef, mapOf(INTERESTS to interests))

                // 3
                for (postId in postIds) {
                    val postRef = db.collection(POSTS).document(postId)
                    it.update(postRef, ADMIN, currentUser)
                }

                // 4
                for (followingId in followings) {
                    val followingRef = db.collection(USERS)
                        .document(followingId)
                        .collection(FOLLOWERS)
                        .document(followingId)
                        .collection(USERS)
                        .document(currentUser.id)

                    it.update(followingRef, finalMap)
                }

                // 5
                for (followerId in followers) {
                    val followerRef = db.collection(USERS)
                        .document(followerId)
                        .collection(FOLLOWINGS)
                        .document(followerId)
                        .collection(USERS)
                        .document(currentUser.id)

                    it.update(followerRef, finalMap)
                }


                // 6
                for (channelId in chatChannels) {
                    val chatChannelRef = db.collection(CHAT_CHANNELS)
                        .document(channelId)
                        .collection(USERS)
                        .document(currentUser.id)
                    it.update(chatChannelRef, finalMap)
                }

                for (i in userDetail.activeRequests.indices) {
                    val requestRef =
                        db.collection(POSTS).document(userDetail.activeRequests[i])
                            .collection(REQUESTS)
                            .document(requests[i])
                    it.update(requestRef, SENDER, currentUser)
                }
            }

            task.await()

            updateUser.postValue(Result.Success(finalMap))
            currentUser
        } catch (e: Exception) {
            updateUser.postValue(Result.Error(e))
            null
        }

    }

    override fun updateRegistrationToken(currentUser: User, token: String) {
        val userDetail = currentUser.userPrivate
        if (userDetail.registrationTokens.isEmpty() || !userDetail.registrationTokens.contains(
                token
            )
        ) {
            val uid = currentUser.id

            db.runBatch {
                // 1
                it.update(
                    db.collection(USERS).document(uid).collection(PRIVATE).document(uid),
                    REGISTRATION_TOKENS,
                    FieldValue.arrayUnion(token)
                )

                val tokens = mutableListOf<String>()
                tokens.add(token)
                tokens.addAll(userDetail.registrationTokens)

                for (id in userDetail.chatChannels) {
                    it.update(
                        db.collection(CHAT_CHANNELS).document(id),
                        REGISTRATION_TOKENS,
                        FieldValue.arrayUnion(token)
                    )
                }
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "update token to server" + errorSuffix + it.localizedMessage!!
                )
            }
        }
    }

    override fun uploadProfilePhoto(image: Uri?) {
        if (image != null) {
            val storageRef = storage.reference
            val randomName =
                "Image_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val imageRef = storageRef.child("${currentUser.uid}/images/profile/$randomName.jpg")

                imageRef.putFile(image)
                    .addOnSuccessListener { _ ->
//                    val data = taskSnapshot.bytesTransferred
                        imageRef.downloadUrl
                            .addOnSuccessListener {
                                profilePhotoUpload.postValue(it)
                            }
                            .addOnFailureListener {
                                Log.e(
                                    BUG_TAG,
                                    errorPrefix + "get download url of uploaded profile photo." + errorSuffix + it.localizedMessage!!
                                )
                            }
                    }
                    .addOnFailureListener {
                        Log.e(
                            BUG_TAG,
                            errorPrefix + "upload profile photo to server" + errorSuffix + it.localizedMessage!!
                        )
                    }
            }
        } else {
            profilePhotoUpload.postValue(null)
        }
    }

    override fun signOut() {
        // clear everything here
        auth.signOut()
        TODO("Clear everything after signing out.")
    }

    override suspend fun uploadPost(currentUser: User, post: Post): Pair<Post, ChatChannel>? {
        return try {
            val userDetail = currentUser.userPrivate
            val ref = db.collection(POSTS).document()
            val postId = ref.id
            val now = System.currentTimeMillis()

            val chatChannelRef = db.collection(CHAT_CHANNELS).document()
            val chatChannelRefId = chatChannelRef.id

            val commentChannelRef = db.collection(COMMENT_CHANNELS).document()
            val commentChannelId = commentChannelRef.id


            post.id = postId
            post.admin = currentUser
            post.uid = currentUser.id
            post.chatChannelId = chatChannelRefId
            post.commentChannelId = commentChannelId

            val chatChannel = ChatChannel(
                chatChannelRefId,
                postId,
                post.title,
                post.thumbnail,
                1,
                listOf(currentUser.id),
                listOf(currentUser.id),
                userDetail.registrationTokens,
                now,
                now,
                null
            )

            val subStrings = mutableListOf<String>()
            subStrings.addAll(getAllSubStrings(post.title))
            subStrings.addAll(getAllSubStrings(*post.tags.toTypedArray()))

            post.indices = subStrings

            val task = db.runBatch {
                it.set(ref, post)
                val commentChannel = CommentChannel(commentChannelId, postId, post.title)
                it.set(commentChannelRef, commentChannel)

                if (post.type == PROJECT) {
                    it.update(
                        db.collection(USERS).document(currentUser.id).collection(PRIVATE)
                            .document(currentUser.id),
                        PROJECT_IDS,
                        FieldValue.arrayUnion(postId)
                    )
                    it.update(
                        db.collection(USERS).document(currentUser.id).collection(PRIVATE)
                            .document(currentUser.id),
                        USER_CHAT_CHANNELS,
                        FieldValue.arrayUnion(chatChannelRefId)
                    )

                    it.set(chatChannelRef, chatChannel)

                    it.set(
                        chatChannelRef.collection(USERS).document(currentUser.id),
                        currentUser
                    )
                } else {
                    it.update(
                        db.collection(USERS).document(currentUser.id).collection(PRIVATE)
                            .document(currentUser.id),
                        BLOG_IDS,
                        FieldValue.arrayUnion(postId)
                    )
                }
            }

            task.await()
            postUpload.postValue(Result.Success(post))
            post to chatChannel
        } catch (e: Exception) {
            postUpload.postValue(Result.Error(e))
            networkErrors.postValue(e)
            null
        }
    }

    override suspend fun undoProjectRequest(
        currentUser: User,
        request: SimpleRequest
    ): Pair<User, SimpleRequest>? {
        return try {
            val currentUserRef = db.collection(USERS).document(currentUser.id)

            // delete active request from the sender's document
            val senderRef = db.collection(USERS).document(request.sender.id)
            val senderPrivateRef = senderRef.collection(PRIVATE).document(request.sender.id)

            // delete active request from the own document / delete locally
            val currentRequestsRef = currentUserRef.collection(REQUESTS).document(request.id)


            // delete request from post collection
            val postRequestRef = db.collection(POSTS).document(request.post.id).collection(
                REQUESTS
            ).document(request.id)


            // delete notification from own directory / delete locally
            val currentUserNotificationRef =
                currentUserRef.collection(NOTIFICATIONS).document(request.notificationId)

            val existingList = currentUser.userPrivate.requestIds.toMutableList()
            existingList.remove(request.id)
            currentUser.userPrivate.requestIds = existingList

            val existingList1 = currentUser.userPrivate.activeRequests.toMutableList()
            existingList1.remove(request.postId)
            currentUser.userPrivate.activeRequests = existingList1

            val existingList2 = currentUser.userPrivate.notificationIds.toMutableList()
            existingList2.remove(request.notificationId)
            currentUser.userPrivate.notificationIds = existingList2

            val existingList3 = currentUser.userPrivate.notificationReferences.toMutableList()
            existingList3.remove(request.receiverId)
            currentUser.userPrivate.notificationReferences = existingList3

            val task = db.runBatch {
                val map = mapOf(
                    ACTIVE_REQUESTS to FieldValue.arrayRemove(request.post.id),
                    REQUEST_IDS to FieldValue.arrayRemove(request.id),
                    NOTIFICATION_IDS to FieldValue.arrayRemove(request.notificationId),
                    NOTIFICATION_REFERENCES to FieldValue.arrayRemove(request.receiverId)
                )
                it.update(senderPrivateRef, map)

                it.delete(currentRequestsRef)

                it.delete(postRequestRef)

                it.delete(currentUserNotificationRef)
            }

            task.await()
            undoRequestSent.postValue(Result.Success(request))
            currentUser to request

        } catch (e: Exception) {
            networkErrors.postValue(e)
            undoRequestSent.postValue(Result.Error(e))
            null
        }
    }

    override suspend fun acceptProjectRequest(currentUser: User, notification: SimpleNotification) {
        try {
            val currentUserRef = db.collection(USERS).document(currentUser.id)

            // delete sender active requests in sender doc
            val senderDocRef = db.collection(USERS).document(notification.sender.id)
            val senderDocPrivateRef =
                senderDocRef.collection(PRIVATE).document(notification.sender.id)

            // delete sender request
            val senderRequestRef =
                senderDocRef.collection(REQUESTS).document(notification.requestId!!)


            // delete receiver's request
            val postRequestRef =
                db.collection(POSTS).document(notification.post!!.id).collection(REQUESTS)
                    .document(notification.requestId!!)

            // delete receiver's notification
            val receiverNotificationRef =
                currentUserRef.collection(NOTIFICATIONS).document(notification.id)


            // create new success notification for sender
            val senderNotificationRef = senderDocRef.collection(NOTIFICATIONS).document()
            val successNotificationId = senderNotificationRef.id

            val successNotification = SimpleNotification(
                successNotificationId,
                notification.sender.id,
                ACCEPT_PROJECT,
                notification.requestId,
                currentUser,
                post = notification.post
            )


            // add sender id to project contributor list in doc
            val projectRef = db.collection(POSTS).document(notification.post!!.id)


            // add sender to chat channel contributor list in doc
            val chatChannelRef =
                db.collection(CHAT_CHANNELS).document(notification.post!!.chatChannelId!!)

            // add sender to chat channel users collection
            val chatChannelContributorsRef =
                chatChannelRef.collection(USERS).document(notification.sender.id)

            val task = db.runBatch {
                val map = mapOf(
                    ACTIVE_REQUESTS to FieldValue.arrayRemove(notification.post!!.id),
                    REQUEST_IDS to FieldValue.arrayRemove(notification.requestId),
                    NOTIFICATION_REFERENCES to FieldValue.arrayUnion(notification.sender.id),
                    NOTIFICATION_IDS to FieldValue.arrayUnion(successNotificationId),
                    COLLABORATION_IDS to FieldValue.arrayUnion(notification.post!!.id),
                    USER_CHAT_CHANNELS to FieldValue.arrayUnion(notification.post!!.chatChannelId!!)
                )
                it.update(senderDocPrivateRef, map)
                it.delete(senderRequestRef)
                it.delete(postRequestRef)
                it.delete(receiverNotificationRef)
                it.set(senderNotificationRef, successNotification)
                it.update(projectRef, CONTRIBUTORS, FieldValue.arrayUnion(notification.sender.id))
                val map1 = mapOf(
                    CONTRIBUTORS_LIST to FieldValue.arrayUnion(notification.sender.id),
                    CONTRIBUTORS_COUNT to FieldValue.increment(1)
                    // add the sender registration token to chat channel
                    // REGISTRATION_TOKENS to FieldValue.arrayUnion(notification.sender.userPrivate.registrationTokens.last())
                )
                it.update(chatChannelRef, map1)
                it.set(chatChannelContributorsRef, notification.sender)
            }

            task.await()
            acceptRequestResult.postValue(Result.Success(notification))
        } catch (e: Exception) {
            networkErrors.postValue(e)
            acceptRequestResult.postValue(Result.Error(e))
        }
    }


    override suspend fun denyProjectRequest(currentUser: User, notification: SimpleNotification) {
        try {
            val currentUserRef = db.collection(USERS).document(currentUser.id)

            // delete sender active requests in sender doc
            val senderDocRef = db.collection(USERS).document(notification.sender.id)
            val senderDocPrivateRef =
                senderDocRef.collection(PRIVATE).document(notification.sender.id)


            // delete sender request
            val senderRequestRef =
                senderDocRef.collection(REQUESTS).document(notification.requestId!!)


            // delete receiver request
            val postRequestRef =
                db.collection(POSTS).document(notification.post!!.id).collection(REQUESTS)
                    .document(notification.requestId!!)


            // delete receiver's notification
            val receiverNotificationRef =
                currentUserRef.collection(NOTIFICATIONS).document(notification.id)


            // create new success notification for sender
            val senderNotificationRef = senderDocRef.collection(NOTIFICATIONS).document()
            val failureNotificationId = senderNotificationRef.id

            val failureNotification = SimpleNotification(
                failureNotificationId,
                notification.sender.id,
                DECLINE_PROJECT,
                null,
                currentUser,
                post = notification.post
            )

            val task = db.runBatch {
                val map = mapOf(
                    ACTIVE_REQUESTS to FieldValue.arrayRemove(notification.post!!.id),
                    REQUEST_IDS to FieldValue.arrayRemove(notification.requestId),
                    NOTIFICATION_REFERENCES to FieldValue.arrayUnion(notification.sender.id),
                    NOTIFICATION_IDS to FieldValue.arrayUnion(failureNotificationId)
                )
                it.update(senderDocPrivateRef, map)
                it.delete(senderRequestRef)
                it.delete(postRequestRef)
                it.delete(receiverNotificationRef)
                it.set(senderNotificationRef, failureNotification)
            }

            task.await()
            declineRequestResult.postValue(Result.Success(notification))
        } catch (e: Exception) {
            networkErrors.postValue(e)
            declineRequestResult.postValue(Result.Error(e))
        }
    }

    override suspend fun joinProject(currentUser: User, post: Post): Pair<User, SimpleRequest>? {
        return try {
            val requestRef = db.collection(POSTS).document(post.id).collection(REQUESTS).document()
            val requestId = requestRef.id
            val receiverNotificationRef =
                db.collection(USERS).document(post.uid).collection(NOTIFICATIONS).document()
            val senderRequestRef =
                db.collection(USERS).document(currentUser.id).collection(REQUESTS)
                    .document(requestId)
            val notificationId = receiverNotificationRef.id
            val currentUserRef = db.collection(USERS).document(currentUser.id)
            val currentUserPrivateRef = currentUserRef.collection(PRIVATE).document(currentUser.id)

            val microPost = MicroPost(post.id, post.title, post.thumbnail, post.chatChannelId)

            val existingList = currentUser.userPrivate.requestIds.toMutableList()
            existingList.add(requestId)
            currentUser.userPrivate.requestIds = existingList

            val existingList1 = currentUser.userPrivate.activeRequests.toMutableList()
            existingList1.add(post.id)
            currentUser.userPrivate.activeRequests = existingList1

            val existingList2 = currentUser.userPrivate.notificationIds.toMutableList()
            existingList2.add(notificationId)
            currentUser.userPrivate.notificationIds = existingList2

            val existingList3 = currentUser.userPrivate.notificationReferences.toMutableList()
            existingList3.add(post.uid)
            currentUser.userPrivate.notificationReferences = existingList3

            val simpleRequest = SimpleRequest(
                requestId,
                post.id,
                post.uid,
                notificationId,
                currentUser.id,
                currentUser,
                microPost,
                System.currentTimeMillis()
            )

            val notification = SimpleNotification(
                notificationId,
                post.uid,
                JOIN_PROJECT,
                requestId,
                currentUser,
                System.currentTimeMillis(),
                microPost
            )

            val task = db.runBatch {
                // add request to the post
                it.set(requestRef, simpleRequest)

                //
                it.set(senderRequestRef, simpleRequest)

                // add notification to the other user doc
                it.set(receiverNotificationRef, notification)

                // update current user document
                val map = mapOf(
                    ACTIVE_REQUESTS to FieldValue.arrayUnion(post.id),
                    REQUEST_IDS to FieldValue.arrayUnion(requestId),
                    NOTIFICATION_IDS to FieldValue.arrayUnion(notificationId),
                    NOTIFICATION_REFERENCES to FieldValue.arrayUnion(post.uid)
                )
                it.update(currentUserPrivateRef, map)

            }

            task.await()
            requestSent.postValue(Result.Success(requestId))
            currentUser to simpleRequest
        } catch (e: Exception) {
            networkErrors.postValue(e)
            requestSent.postValue(Result.Error(e))
            null
        }
    }

    override fun uploadPostImage(image: Uri, type: String) {
        val storageRef = storage.reference
        val randomName = "Image_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())

        val currentUser = auth.currentUser
        if (currentUser != null) {

            val imageRef = storageRef.child("${currentUser.uid}/images/${type}/$randomName.jpg")
            imageRef.putFile(image)
                .addOnSuccessListener {
                    imageRef.downloadUrl
                        .addOnSuccessListener {
                            postPhotoUpload.postValue(Result.Success(it))
                        }
                        .addOnFailureListener {
                            postPhotoUpload.postValue(Result.Error(it))
                            Log.e(
                                BUG_TAG,
                                errorPrefix + "get download url." + errorSuffix + it.localizedMessage!!
                            )
                        }
                }
                .addOnFailureListener {
                    Log.e(
                        BUG_TAG,
                        errorPrefix + "upload post image to server." + errorSuffix + it.localizedMessage!!
                    )
                }
        }
    }

    // the state before changes
    override suspend fun onLikePressedWithoutCaching(
        currentUser: User,
        post: Post
    ): Pair<User, Post>? {

        return try {
            val userId = currentUser.id
            val userMap = mutableMapOf<String, Any>()

            if (post.postLocalData.isLiked) {
                if (post.likes != ZERO) {
                    post.likes -= 1
                }
                post.postLocalData.isLiked = false

                val existingLikedPosts = currentUser.userPrivate.likedPosts.toMutableList()
                existingLikedPosts.remove(post.id)
                currentUser.userPrivate.savedPosts = existingLikedPosts

                userMap[LIKED_POSTS] = FieldValue.arrayRemove(post.id)
            } else {
                post.likes += 1
                post.postLocalData.isLiked = true

                val existingLikedPosts = currentUser.userPrivate.likedPosts.toMutableList()
                existingLikedPosts.add(post.id)
                currentUser.userPrivate.savedPosts = existingLikedPosts

                userMap[LIKED_POSTS] = FieldValue.arrayUnion(post.id)

                if (post.postLocalData.isDisliked) {

                    val existingDislikedPosts =
                        currentUser.userPrivate.dislikedPosts.toMutableList()
                    existingDislikedPosts.remove(post.id)
                    currentUser.userPrivate.savedPosts = existingDislikedPosts

                    userMap[DISLIKED_POSTS] = FieldValue.arrayRemove(post.id)
                    if (post.dislikes != ZERO) {
                        post.dislikes -= 1
                    }
                    post.postLocalData.isDisliked = false
                }
            }

            val map = mapOf(LIKES to post.likes, DISLIKES to post.dislikes)
            val currentUserPrivateRef =
                db.collection(USERS).document(userId).collection(PRIVATE).document(userId)

            val task = db.runBatch {
                // update the post document in fireStore
                it.update(db.collection(POSTS).document(post.id), map)
                it.update(currentUserPrivateRef, userMap)
            }
            task.await()
            currentUser to post
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    override suspend fun onDislikePressedWithoutCaching(
        currentUser: User,
        post: Post
    ): Pair<User, Post>? {
        return try {
            val userId = currentUser.id
            val userMap = mutableMapOf<String, Any>()

            if (post.postLocalData.isDisliked) {
                if (post.dislikes != ZERO) {
                    post.dislikes -= 1
                }
                post.postLocalData.isDisliked = false

                val existingDislikedPosts = currentUser.userPrivate.dislikedPosts.toMutableList()
                existingDislikedPosts.remove(post.id)
                currentUser.userPrivate.savedPosts = existingDislikedPosts

                userMap[DISLIKED_POSTS] = FieldValue.arrayRemove(post.id)
            } else {
                post.dislikes += 1
                post.postLocalData.isDisliked = true

                val existingDislikedPosts = currentUser.userPrivate.dislikedPosts.toMutableList()
                existingDislikedPosts.add(post.id)
                currentUser.userPrivate.savedPosts = existingDislikedPosts

                userMap[DISLIKED_POSTS] = FieldValue.arrayUnion(post.id)

                if (post.postLocalData.isLiked) {

                    val existingLikedPosts = currentUser.userPrivate.likedPosts.toMutableList()
                    existingLikedPosts.remove(post.id)
                    currentUser.userPrivate.savedPosts = existingLikedPosts


                    userMap[LIKED_POSTS] = FieldValue.arrayRemove(post.id)
                    if (post.likes != ZERO) {
                        post.likes -= 1
                    }
                    post.postLocalData.isLiked = false
                }
            }

            val map = mapOf(LIKES to post.likes, DISLIKES to post.dislikes)
            val currentUserPrivateRef =
                db.collection(USERS).document(userId).collection(PRIVATE).document(userId)

            val task = db.runBatch {
                // update the post document in fireStore
                it.update(db.collection(POSTS).document(post.id), map)
                it.update(currentUserPrivateRef, userMap)
            }

            task.await()

            currentUser to post
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    override suspend fun getPost(postId: String): Post? {
        val postRef = db.collection(POSTS).document(postId)
        return getObject<Post>(postRef)
    }

    override suspend fun onPostSavedWithoutCaching(
        currentUser: User,
        post: Post
    ): Pair<User, Post>? {
        return try {
            val userId = currentUser.id
            val currentUserPrivateRef =
                db.collection(USERS).document(userId).collection(PRIVATE).document(userId)
            val task = if (post.postLocalData.isSaved) {
                post.postLocalData.isSaved = false
                val existingSavedPosts = currentUser.userPrivate.savedPosts.toMutableList()
                existingSavedPosts.remove(post.id)
                currentUser.userPrivate.savedPosts = existingSavedPosts
                currentUserPrivateRef.update(SAVED_POSTS, FieldValue.arrayRemove(post.id))
            } else {
                post.postLocalData.isSaved = true
                val existingSavedPosts = currentUser.userPrivate.savedPosts.toMutableList()
                existingSavedPosts.add(post.id)
                currentUser.userPrivate.savedPosts = existingSavedPosts
                currentUserPrivateRef.update(SAVED_POSTS, FieldValue.arrayUnion(post.id))
            }

            task.await()
            currentUser to post
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    suspend fun onFollowPressed(currentUser: User, otherUser: User): User? {
        return try {
            val currentUserPrivateRef =
                db.collection(USERS).document(currentUser.id).collection(PRIVATE)
                    .document(currentUser.id)
            val otherUserRef =
                db.collection(USERS).document(otherUser.id).collection(PRIVATE)
                    .document(otherUser.id)
            val followingsRef = db.collection(USERS).document(currentUser.id).collection(FOLLOWINGS)
                .document(currentUser.id).collection(
                    USERS
                ).document(otherUser.id)

            val followersRef =
                db.collection(USERS).document(otherUser.id).collection(FOLLOWERS)
                    .document(otherUser.id)
                    .collection(USERS).document(currentUser.id)

            val userDetail = currentUser.userPrivate
            val isUserFollowed = userDetail.followings.contains(otherUser.id)

            val task = if (isUserFollowed) {
                otherUser.weightage -= 0.1
                otherUser.isUserFollowed = false
//			isUserFollowed = false
                val existingList = userDetail.followings.toMutableList()
                existingList.remove(otherUser.id)
                userDetail.followings = existingList

                db.runBatch {
                    it.update(
                        currentUserPrivateRef,
                        FOLLOWINGS,
                        FieldValue.arrayRemove(otherUser.id)
                    )
                    it.update(otherUserRef, FOLLOWERS, FieldValue.arrayRemove(currentUser.id))
                    it.delete(followingsRef)
                    it.delete(followersRef)
                }
            } else {
                otherUser.weightage += 0.1
                otherUser.isUserFollowed = true
//			isUserFollowed = true
                val existingList = userDetail.followings.toMutableList()
                existingList.add(otherUser.id)
                userDetail.followings = existingList

                db.runBatch {
                    it.update(
                        currentUserPrivateRef,
                        FOLLOWINGS,
                        FieldValue.arrayUnion(otherUser.id)
                    )
                    it.update(otherUserRef, FOLLOWERS, FieldValue.arrayUnion(currentUser.id))
                    it.set(followingsRef, otherUser)
                    it.set(followersRef, currentUser)
                }
            }

            task.await()
            currentUser
        } catch (e: Exception) {
            null
        }


    }

    override suspend fun onFollowPressed(currentUser: User, post: Post): Pair<User, Post>? {
        val otherUser = post.admin
        if (post.type == PROJECT) {
            val existingList = otherUser.userPrivate.chatChannels.toMutableList()
            existingList.add(post.chatChannelId!!)
            otherUser.userPrivate.chatChannels = existingList
        }
        post.postLocalData.isUserFollowed = !post.postLocalData.isUserFollowed

        val updatedUser = onFollowPressed(currentUser, otherUser)
        return if (updatedUser != null) {
            updatedUser to post
        } else {
            null
        }
    }


    override suspend fun updatePost(post: Post, postMap: Map<String, Any?>): Post? {
        return try {
            val task = db.collection(POSTS).document(post.id).update(postMap)
            task.await()
            post.guidelines = postMap[GUIDELINES] as String?
            guidelinesUpdateResult.postValue(post)
            post
        } catch (e: Exception) {
            networkErrors.postValue(e)
            guidelinesUpdateResult.postValue(post)
            null
        }
    }


    override suspend fun sendMessage(
        currentUser: User,
        message: SimpleMessage,
        chatChannel: ChatChannel
    ): Pair<ChatChannel, SimpleMessage>? {

        return try {
            message.senderId = currentUser.id
            message.sender = currentUser

            val messagesRef = if (message.messageId.isEmpty()) {
                val ref =
                    db.collection(CHAT_CHANNELS).document(message.chatChannelId)
                        .collection(MESSAGES)
                        .document()
                message.messageId = ref.id
                ref
            } else {
                db.collection(CHAT_CHANNELS).document(message.chatChannelId).collection(MESSAGES)
                    .document(message.messageId)
            }

            val task = db.runBatch {
                it.set(messagesRef, message)
                val map = mapOf(
                    LAST_MESSAGE to message,
                    UPDATED_AT to System.currentTimeMillis()
                )
                it.update(db.collection(CHAT_CHANNELS).document(message.chatChannelId), map)
            }

            task.await()
            chatChannel.lastMessage = message
            chatChannel.updatedAt = System.currentTimeMillis()

            chatChannel to message

        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }

    }

    override suspend fun uploadMessageMedia(
        currentUser: User,
        message: SimpleMessage,
        chatChannel: ChatChannel
    ): Pair<ChatChannel, SimpleMessage>? {
        return try {
            val messageRef = db.collection(CHAT_CHANNELS)
                .document(message.chatChannelId)
                .collection(MESSAGES)
                .document()

            val messageId = messageRef.id
            message.messageId = messageId

            val storageRef = storage.reference

            val randomName: String
            val mediaRef: StorageReference

            when (message.type) {
                IMAGE -> {
                    randomName =
                        "Image_" + SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.UK
                        ).format(message.createdAt)
                    mediaRef =
                        storageRef.child("${message.chatChannelId}/images/messages/$randomName.jpg")
                }
                DOCUMENT -> {
                    randomName = "Document_" + SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.UK
                    ).format(message.createdAt)
                    mediaRef = storage
                        .reference
                        .child("${message.chatChannelId}/documents/messages/${randomName}_${message.metaData?.originalFileName}")
                }
                else -> {
                    throw Exception("Can only send message of media type.")
                }
            }

            chatChannel.lastMessage = message
            chatChannel.updatedAt = System.currentTimeMillis()

            val task = mediaRef.putFile(message.content.toUri())
            task.await()

            val anotherTask = mediaRef.downloadUrl
            val downloadUrl = anotherTask.await()

            message.content = downloadUrl.toString()
            mediaUploadResult.postValue(Result.Success(message))

            sendMessage(currentUser, message, chatChannel)
        } catch (e: Exception) {
            networkErrors.postValue(e)
            mediaUploadResult.postValue(Result.Error(e))
            null
        }
    }

    // preprocess - file, name
    override suspend fun downloadMedia(
        destinationFile: File,
        message: SimpleMessage
    ): SimpleMessage? {
        return try {
            val uri = Uri.parse(message.content)
            val fileRef = uri.lastPathSegment

            val objectRef = storage.reference.child(fileRef!!)
            val task = objectRef.getFile(destinationFile)
            task.await()

            message.isDownloaded = true
            mediaDownloadResult.postValue(Result.Success(message))
            message
        } catch (e: Exception) {
            networkErrors.postValue(e)
            mediaDownloadResult.postValue(Result.Error(e))
            null
        }
    }

    override suspend fun onNewMessagesFromBackground(chatChannelId: String): ChatChannel? {
        val chatChannelRef = db.collection(CHAT_CHANNELS).document(chatChannelId)
        return getObject<ChatChannel>(chatChannelRef)
    }

    private fun getAllSubStrings(vararg strings: String): List<String> {
        // also needs splitting

        val words = mutableListOf<String>()
        for (str in strings) {
            words.addAll(str.split(' '))
        }

        val list = mutableListOf<String>()
        for (word in words) {
            for (i in 1 until word.length) {
                list.add(word.substring(0..i))
            }
        }
        return list
    }

    fun clearUploadResults() {
        postPhotoUpload.postValue(null)
        postUpload.postValue(null)
    }

    fun clearRequestResults() {
        requestSent.postValue(null)
    }

    fun clearEditChanges() {
        usernameExists.postValue(null)
        updateUser.postValue(null)
        profilePhotoUpload.postValue(null)
    }

    suspend inline fun <reified T : Any?> getObject(docRef: DocumentReference): T? {
        return try {
            val task = docRef.get()
            val doc = task.await()
            return doc.toObject(T::class.java)
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    suspend inline fun <reified T : Any?> getObjects(query: Query): List<T> {
        return try {
            val task = query.get()
            val querySnapshot = task.await()
            querySnapshot.toObjects(T::class.java)
        } catch (e: Exception) {
            networkErrors.postValue(e)
            emptyList()
        }
    }


    fun <T : Any> getSnapshot(docId: String, clazz: Class<T>): Result<DocumentSnapshot> {
        return when (clazz) {
            Post::class.java -> {
                try {
                    val task = db.collection(POSTS).document(docId)
                        .get()
                    val result = Tasks.await(task)
                    Result.Success(result)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }
            else -> Result.Error(Exception("Class not found."))
        }
    }

    suspend fun getNotificationFromFirebase(
        currentUser: User,
        notificationId: String
    ): SimpleNotification? {
        val notificationDoc = db.collection(USERS).document(currentUser.id)
            .collection(NOTIFICATIONS)
            .document(notificationId)

        return getObject<SimpleNotification>(notificationDoc)
    }

    suspend fun getNotifications(currentUser: User): List<SimpleNotification> {

        val query = db.collection(USERS).document(currentUser.id)
            .collection(NOTIFICATIONS)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(20)

        return getObjects(query)
    }


    suspend fun updateOtherUser(otherUser: User, map: Map<String, Any?>): User? {

        return try {
            val task = db.collection(USERS).document(otherUser.id)
                .update(map)

            task.await()
            otherUser
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }

    }

    suspend fun getItems(
        userSource: UserSource,
        lim: Int,
        endBefore: DocumentSnapshot? = null,
        startAfter: DocumentSnapshot? = null
    ): Result<QuerySnapshot> {
        return try {
            var finalQuery = when (userSource) {
                is UserSource.Contributor -> {
                    userSource.query
                }
                is UserSource.Follower -> {
                    userSource.query
                }
                is UserSource.Following -> {
                    userSource.query
                }
                is UserSource.Acceptance -> {
                    userSource.query
                }
                is UserSource.Search -> {
                    userSource.query
                }
                is UserSource.Random -> {
                    userSource.query
                }
            }


            finalQuery = when {
                startAfter != null -> {
                    finalQuery.startAfter(startAfter)
                }
                endBefore != null -> {
                    finalQuery.endBefore(endBefore)
                }
                else -> {
                    finalQuery
                }
            }

            finalQuery = finalQuery.limit(lim.toLong())

            val task = finalQuery.get()
            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getComments(
        query: Query,
        endBefore: DocumentSnapshot? = null,
        startAfter: DocumentSnapshot? = null
    ): Result<QuerySnapshot> {
        return try {

            var finalQuery = query.orderBy(POSTED_AT, Query.Direction.DESCENDING)
            finalQuery = when {
                endBefore != null -> {
                    finalQuery.endBefore(endBefore)
                }
                startAfter != null -> {
                    finalQuery.startAfter(startAfter)
                }
                else -> finalQuery
            }

            finalQuery = finalQuery.limit(10)

            val task = finalQuery.get()
            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getItems(
        postSource: PostSource,
        endBefore: DocumentSnapshot? = null,
        startAfter: DocumentSnapshot? = null
    ): Result<QuerySnapshot> {
        return try {
            var finalQuery: Query = when (postSource) {
                is PostSource.FeedRandom -> {
                    postSource.query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                }
                is PostSource.FeedWithFollowings -> {
                    postSource.query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                }
                is PostSource.FeedWithOtherUserAndType -> {
                    postSource.query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                }
                is PostSource.FeedWithOtherUserCollaborations -> {
                    postSource.query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                }
                is PostSource.FeedWithTags -> {
                    postSource.query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                }
                is PostSource.FeedTopPostsWithType -> {
                    postSource.query.orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
                }
                is PostSource.Search -> {
                    postSource.query.orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
                }
                is PostSource.TagPosts -> {
                    postSource.query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                }
            }

            finalQuery = when {
                startAfter != null -> {
                    finalQuery.startAfter(startAfter)
                }
                endBefore != null -> {
                    finalQuery.endBefore(endBefore)
                }
                else -> {
                    finalQuery
                }
            }

            finalQuery = finalQuery.limit(postSource.defaultPageSize)

            val task = finalQuery.get()
            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getProjectContributors(post: Post): Result<QuerySnapshot> {
        return if (post.chatChannelId != null) {
            try {
                val task = db.collection(CHAT_CHANNELS).document(post.chatChannelId!!).collection(
                    USERS
                ).limit(10).get()
                Result.Success(task.await())
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            Result.Error(Exception("Post is not a project."))
        }
    }

    suspend fun getRandomTopUsers(): Result<QuerySnapshot> {
        return try {
            val task = db.collection(USERS).orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
                .limit(15)
                .get()

            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTagsFromFirebase(user: User?): Result<List<String>> {
        return try {
            val task = if (user != null) {
                val interests = user.userPrivate.interests
                db.collection("top_interests")
                    .whereIn(TAG, interests.subList(0, minOf(10, interests.size)))
                    .orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
                    .limit(10).get()
            } else {
                db.collection("top_interests").orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
                    .limit(10).get()
            }

            val result = task.await()

            val simpleTags = result.toObjects(SimpleTag::class.java)

            // it should be guaranteed that the top_interests has more than 10 interests
            if (simpleTags.size < 9) {
                return getTagsFromFirebase(null)
            }

            val tags = mutableListOf<String>()

            for (st in simpleTags) {
                tags.add(st.tag)
            }

            Result.Success(tags)
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    suspend fun getTopProjects(): Result<QuerySnapshot> {
        return try {
            val task = db.collection(POSTS).whereEqualTo(TYPE, PROJECT)
                .orderBy(SEARCH_RANK, Query.Direction.DESCENDING).limit(15).get()
            Result.Success(task.await())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getTopBlogs(): Result<QuerySnapshot> {
        return try {
            val task = db.collection(POSTS).whereEqualTo(TYPE, BLOG)
                .orderBy(SEARCH_RANK, Query.Direction.DESCENDING).limit(15).get()
            Result.Success(task.await())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getMessagesFromFirebase(
        query: Query,
        limit: Int,
        startAfter: DocumentSnapshot? = null
    ): Result<QuerySnapshot> {
        return if (startAfter != null) {
            try {
                val task = query.startAfter(startAfter)
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()

                Result.Success(task.await())
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            try {
                val task = query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()

                Result.Success(task.await())

            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    suspend fun getPagedNotifications(
        query: Query,
        limit: Int,
        startAfter: DocumentSnapshot?
    ): Result<QuerySnapshot> {
        return if (startAfter != null) {
            val task = query.startAfter(startAfter).limit(limit.toLong()).get()
            Result.Success(task.await())
        } else {
            val task = query.limit(limit.toLong()).get()
            Result.Success(task.await())
        }
    }

    suspend fun getPagedRequests(
        query: Query,
        limit: Int,
        startAfter: DocumentSnapshot?
    ): Result<QuerySnapshot> {
        return if (startAfter != null) {
            val task = query.startAfter(startAfter).limit(limit.toLong()).get()
            Result.Success(task.await())
        } else {
            val task = query.limit(limit.toLong()).get()
            Result.Success(task.await())
        }
    }

    val usersMap = mutableMapOf<String, User>()

    suspend fun fetchSavedPosts(currentUser: User, i: Int, f: Int): Result<QuerySnapshot> {
        val savedPostsIds = currentUser.userPrivate.savedPosts

        if (savedPostsIds.isEmpty()) {
            return Result.Error(Exception("No saved posts."))
        }

        return try {
            val task = db.collection(POSTS)
                .whereIn(ID, savedPostsIds.subList(i, minOf(savedPostsIds.size, f)))
                .get()

            Result.Success(task.await())
        } catch (e: Exception) {
            Result.Error(e)
        }

    }

    override suspend fun removeInterest(currentUser: User, interest: String): User? {
        return try {
            val existingList = currentUser.userPrivate.interests.toMutableList()
            existingList.remove(interest)
            currentUser.userPrivate.interests = existingList

            val task = db.collection(USERS).document(currentUser.id)
                .collection(PRIVATE)
                .document(currentUser.id)
                .update(INTERESTS, FieldValue.arrayRemove(interest))

            task.await()
            currentUser
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    fun sendComment(currentUser: User, post: Post, commentText: String) {
        val commentRef =
            db.collection(COMMENT_CHANNELS).document(post.commentChannelId).collection(COMMENTS)
                .document()
        val commentId = commentRef.id
        val postRef = db.collection(POSTS).document(post.id)
        val commentChannelRef = db.collection(COMMENT_CHANNELS).document(post.commentChannelId)
        val threadRef = db.collection(COMMENT_CHANNELS).document()
        val threadId = threadRef.id

        val thread = CommentChannel(threadId, post.commentChannelId, post.title)

        val simpleComment = SimpleComment(
            commentId,
            commentText,
            0,
            currentUser.id,
            post.id,
            post.commentChannelId,
            threadId
        )

        val lastCommentUpdateMap = mapOf(LAST_COMMENT to simpleComment)

        db.runBatch {
            // Creating a new comments
            it.set(commentRef, simpleComment)
            // Increasing the comment count in the respective post
            it.update(postRef, COMMENT_COUNT, FieldValue.increment(1))
            // Creating a new CommentChannel for the newly created Comment
            it.set(threadRef, thread)
            // adding the newly created comment to its CommentChannel
            it.update(commentChannelRef, lastCommentUpdateMap)

        }.addOnSuccessListener {
            commentSentResult.postValue(Result.Success(simpleComment))
        }.addOnFailureListener {
            commentSentResult.postValue(Result.Error(it))
        }
    }

    suspend fun likeComment(currentUser: User, comment: SimpleComment): SimpleComment {
        val ref1 = db.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)

        val ref2 = db.collection(USERS)
            .document(currentUser.id)
            .collection(PRIVATE)
            .document(currentUser.id)

        val task = db.runBatch {

            it.update(ref1, LIKES, FieldValue.increment(1))

            it.update(ref2, "likedComments", FieldValue.arrayUnion(comment.commentId))

        }

        comment.likes = comment.likes + 1
        comment.isLiked = true
        task.await()

        val existingList = currentUser.userPrivate.likedComments.toMutableList()
        existingList.add(comment.commentId)
        currentUser.userPrivate.likedComments = existingList

        return comment
    }

    suspend fun dislikeComment(currentUser: User, comment: SimpleComment): SimpleComment {

        val ref1 = db.collection(COMMENT_CHANNELS)
            .document(comment.commentChannelId)
            .collection(COMMENTS)
            .document(comment.commentId)

        val ref2 = db.collection(USERS)
            .document(currentUser.id)
            .collection(PRIVATE)
            .document(currentUser.id)

        val task = db.runBatch {

            it.update(ref1, LIKES, FieldValue.increment(-1))

            it.update(ref2, "likedComments", FieldValue.arrayRemove(comment.commentId))

        }

        comment.likes = comment.likes - 1
        comment.isLiked = false
        task.await()


        val existingList = currentUser.userPrivate.likedComments.toMutableList()
        existingList.remove(comment.commentId)
        currentUser.userPrivate.likedComments = existingList

        return comment
    }

    fun sendCommentReply(currentUser: User, parentComment: SimpleComment, commentText: String) {
        // new comment ref
        val newCommentDocRef =
            db.collection(COMMENT_CHANNELS).document(parentComment.threadChannelId)
                .collection(COMMENTS).document()

        // post ref
        val postDocRef = db.collection(POSTS).document(parentComment.postId)

        // channel where the replies of the next comment gets added to
        val newThreadDocRef = db.collection(COMMENT_CHANNELS).document()
        val newThreadId = newThreadDocRef.id

        // new comment that needs to be uploaded to the parent comment dedicated thread channel
        val newCommentId = newCommentDocRef.id
        val newComment = SimpleComment(
            newCommentId,
            commentText,
            0,
            currentUser.id,
            parentComment.postId,
            parentComment.threadChannelId,
            newThreadId,
            commentLevel = parentComment.commentLevel + 1
        )

        // current comment document ref
        val parentCommentDocRef = db.collection(COMMENT_CHANNELS)
            .document(parentComment.commentChannelId)
            .collection(COMMENTS)
            .document(parentComment.commentId)

        val lastCommentUpdateMap = mapOf(LAST_COMMENT to newComment)

        // new channel to be added where replies of the next comment will be added
        val newThread =
            CommentChannel(newThreadId, parentComment.commentId, parentComment.postTitle)

        db.runBatch {
            it.set(newCommentDocRef, newComment)
            it.set(newThreadDocRef, newThread)
            it.update(parentCommentDocRef, REPLIES_COUNT, FieldValue.increment(1))
            it.update(postDocRef, COMMENT_COUNT, FieldValue.increment(1))
            it.update(
                db.collection(COMMENT_CHANNELS).document(parentComment.threadChannelId),
                lastCommentUpdateMap
            )
        }.addOnSuccessListener {
            commentSentResult.postValue(Result.Success(newComment))
        }.addOnFailureListener {
            commentSentResult.postValue(Result.Error(it))
        }
    }


/*suspend fun getItemsFromFirebase(
		endBefore: DocumentSnapshot? = null,
		startAfter: DocumentSnapshot? = null,
		query: Query,
		pageSize: Int = 20,
		extras: Map<String, Any?>?
	): Result<QuerySnapshot> = withContext(Dispatchers.IO) {
		val currentUser = currentLocalUser.value
		val tag = extras?.get(WITH_TAG) as String?

		val finalQuery = if (currentUser != null) {
			if (tag != null) {
				query.whereArrayContains(TAGS, tag)
			} else {
				query.whereIn(
					UID,
					currentUser.userPrivate.followings.subList(
						0,
						minOf(
							10,
							currentUser.userPrivate.followings.size
						)
					)
				)
			}
		} else {
			if (tag != null) {
				query.whereArrayContains(TAGS, tag)
			} else {
				query
			}
		}.also {
			it.orderBy(CREATED_AT, Query.Direction.DESCENDING)
		}

		return@withContext when {
			endBefore != null -> {
				try {
					val task =
						finalQuery.endBefore(endBefore)
							.limit(pageSize.toLong())
							.get()
					Result.Success(task.await())
				} catch (e: Exception) {
					Result.Error(e)
				}
			}
			startAfter != null -> {
				try {
					val task =
						finalQuery.startAfter(startAfter)
							.limit(pageSize.toLong())
							.get()
					Result.Success(task.await())
				} catch (e: Exception) {
					Result.Error(e)
				}
			}
			else -> {
				try {
					val task = finalQuery.limit(pageSize.toLong()).get()
					Result.Success(task.await())
				} catch (e: Exception) {
					Result.Error(e)
				}
			}
		}
	}*/

    /*fun fetchItems(
        snapshot: DocumentSnapshot?,
        query: Query,
        pageSize: Int
    ): Result<QuerySnapshot> {
        return if (snapshot == null) {
            try {
                val task = query.limit(pageSize.toLong()).get()
                Result.Success(Tasks.await(task))
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            try {
                val task =
                    query.startAfter(snapshot)
                        .limit(pageSize.toLong())
                        .get()
                Result.Success(Tasks.await(task))
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }*/

    /*fun setChatChannelsListeners() {
        val currentUser = currentLocalUser.value
        if (currentUser != null) {
            chatChannelListenerRegistration?.remove()
            chatChannelListenerRegistration = db.collection(CHAT_CHANNELS).whereArrayContains(CONTRIBUTORS_LIST, currentUser.id)
                .orderBy(UPDATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->

                    if (error != null) {
                        _networkErrors.postValue(error)
                    }

                    if (value != null && !value.isEmpty) {
                        val chatChannels = value.toObjects(ChatChannel::class.java)

                        scope.launch(Dispatchers.IO) {
                            for (chatChannel in chatChannels) {
                                val senderId = chatChannel.lastMessage?.senderId
                                if (senderId != null) {
                                    val localUser = userDao.getUser(senderId)
                                    if (localUser != null) {
                                        chatChannel.lastMessage?.sender = localUser
                                    } else {
                                        try {
                                            val fetchedUserTask = db.collection(USERS).document(senderId).get()
                                            val user = fetchedUserTask.await().toObject(User::class.java)!!
                                            chatChannel.lastMessage?.sender = user
                                        } catch (e: Exception) {
                                            Log.e(BUG_TAG, e.localizedMessage!!)
                                        }
                                    }
                                }
                            }
                            chatChannelDao.insertChatChannels(chatChannels)
                        }
                    }
                }
        }
    }*/

    /*fun getContributorForAllChannels() {
        *//*val currentUser = currentLocalUser.value
		if (currentUser != null) {
			for (channel in currentUser.userPrivate.chatChannels) {
				db.collection(CHAT_CHANNELS).document(channel).collection(USERS).get()
					.addOnSuccessListener {
						val contributors = it.toObjects(User::class.java)
						scope.launch (Dispatchers.IO) {
							insertUsersWithFilter(contributors, mapOf(CHAT_CHANNEL to channel))
						}
					}.addOnFailureListener {
						_networkErrors.postValue(it)
					}
			}
		}*//*
	}*/


    //	private val listOfUsers = mutableListOf<User>()

    /*fun setChannelContributorsListener(chatChannel: ChatChannel) {
        db.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
            .collection(USERS).addSnapshotListener { v, e ->
                if (e != null) {
                    _networkErrors.postValue(e)
                }

                if (v != null && !v.isEmpty) {
                    val contributors = v.toObjects(User::class.java)

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

                    scope.launch(Dispatchers.IO) {
                        insertUsersWithFilter(
                            listOfUsers,
                            chatChannel
                        )
                    }
                }
            }
    }*/


// temp - must be deleted
    /*fun tempFixBlog(post: Post) {
        val ref = db.collection(COMMENT_CHANNELS).document(post.commentChannelId!!)
        val commentChannel = CommentChannel(post.commentChannelId!!, post.id, post.title)

        ref.set(commentChannel)
    }*/


/*inline fun <reified T : Any> getObject(
		reference: DocumentReference,
		crossinline onComplete: (obj: T) -> Unit
	) {
		reference.get().addOnSuccessListener {
			if (it != null && it.exists()) {
				val clazz = T::class.java
				onComplete(it.toObject(clazz)!!)
			} else {
				Log.e(TAG, "Document is either null or doesn't exist at reference : $reference")
			}
		}.addOnFailureListener {
			Log.e(TAG, it.localizedMessage!!)
		}
	}*/

    /*private inline fun <reified T : Any> getObjects(
        query: Query,
        crossinline onComplete: (obz: List<T>) -> Unit,
        crossinline onError: (err: Exception) -> Unit
    ) {
        query.get().addOnSuccessListener {
            if (!it.isEmpty) {
                onComplete(it.toObjects(T::class.java))
            } else {
                onError(Exception("Collection is empty : $query"))
            }
        }.addOnFailureListener {
            onError(it)
        }
    }*/

    /*fun getItemsWithoutCaching(query: Query, onComplete: (querySnapshot: QuerySnapshot) -> Unit) {
        query.get()
            .addOnSuccessListener {
                onComplete(it)
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get items without caching. wtf" + errorSuffix + it.localizedMessage!!
                )
            }
    }*/

    /*fun getSnapshot(
        documentReference: DocumentReference,
        onComplete: (doc: DocumentSnapshot) -> Unit
    ) {
        documentReference.get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    onComplete(it)
                }
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get snapshot. wtf." + errorSuffix + it.localizedMessage!!
                )
            }
    }*/

    /*fun getChatChannelsFromFirebase() {

        Log.d(ChatChannelFragment.TAG, "Getting chat channels -- Fire")

        val currentUser = currentLocalUser.value!!
        db.collection(CHAT_CHANNELS).whereArrayContains(CONTRIBUTORS_LIST, currentUser.id)
            .orderBy(UPDATED_AT, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { channelsSnapshot ->
                val channels = channelsSnapshot.toObjects(ChatChannel::class.java)

                for (channel in channels) {
                    db.collection(CHAT_CHANNELS)
                        .document(channel.chatChannelId)
                        .collection(USERS)
                        .get()
                        .addOnSuccessListener {
                            val contributors = it.toObjects(User::class.java)
                            scope.launch(Dispatchers.IO) {
                                insertUsersWithFilter(contributors, mapOf(CHAT_CHANNEL to channel))
                            }
                        }.addOnFailureListener {
                            Log.e(ChatChannelFragment.TAG, it.localizedMessage!!.toString())
                        }
                }

            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get chat channels based on contributors." + errorSuffix + it.localizedMessage!!
                )
            }
    }*/

    /*suspend fun getSavedPosts(currentUser: User, initialItemPosition: Int, finalItemPosition: Int): List<Post> {
        val savedPostIds = currentUser.userPrivate.savedPosts

        if (savedPostIds.isEmpty()) {
            return emptyList()
        }

        suspend fun downloadSavedPosts(batchCount: Int, initialPos: Int): List<Post> {

            val internalListOfPosts = mutableListOf<Post>()

            var initial = initialPos
            var final = minOf(initial + 10, savedPostIds.size)

            for (j in 0 until batchCount) {
                val query = db.collection(POSTS)
                    .whereArrayContainsAny(ID, savedPostIds.subList(initial, final))

                val posts = getObjects<Post>(query)
                if (posts != null) {
                    internalListOfPosts.addAll(posts)
                }
                *//*if (posts != null) {

				}

				*//*


				scope.launch {
					insertPosts(posts)
				}


				if (final - initial >= 10) {
					initial = final
					final = minOf(initial + 10, savedPostIds.size)
				}
			}

			return internalListOfPosts
		}

		// TODO("Notify to the fragment, when it's the end of the list")
		return when {
			savedPostIds.size <= (finalItemPosition - initialItemPosition) / 2 -> {
				// only one batch
				downloadSavedPosts(1, initialItemPosition)
			}
			savedPostIds.size < finalItemPosition -> {
				// two batch
				// and its the end of the list
				downloadSavedPosts(2, initialItemPosition)
			}
			savedPostIds.size == finalItemPosition -> {
				// exactly two batch and end of list
				downloadSavedPosts(2, initialItemPosition)
			}
			savedPostIds.size > finalItemPosition -> {
				// not the end of the list
				downloadSavedPosts(2, initialItemPosition)
			}
			else -> emptyList()
		}
	}*/

    /*fun getChatChannelFromFirebase(chatChannelId: String) {

        val chatChannelRef = db.collection(CHAT_CHANNELS).document(chatChannelId)

        chatChannelRef.get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val chatChannel = it.toObject(ChatChannel::class.java)!!

                    chatChannelRef.collection(USERS)
                        .get()
                        .addOnSuccessListener { qs ->
                            val contributors = qs.toObjects(User::class.java)
                            scope.launch(Dispatchers.IO) {
                                insertUsersWithFilter(
                                    contributors,
                                    mapOf(CHAT_CHANNEL to chatChannel)
                                )
                            }
                        }.addOnFailureListener { exc ->
                            Log.e(
                                BUG_TAG,
                                errorPrefix + "get single chat channel contributors" + errorSuffix + exc.localizedMessage!!
                            )
                        }

                }
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get single chat channel." + errorSuffix + it.localizedMessage!!
                )
            }

    }*/

    /*fun setNotificationListener() {
        val currentUser = currentLocalUser.value!!
        db.collection(USERS).document(currentUser.id)
            .collection(NOTIFICATIONS)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(
                        BUG_TAG,
                        errorPrefix + "set listener for notifications." + errorSuffix + error.localizedMessage!!
                    )
                }

                if (value != null && !value.isEmpty) {
                    val notifications = value.toObjects(SimpleNotification::class.java)
                    scope.launch(Dispatchers.IO) {
                        notificationDao.insertItems(notifications)
                    }
                }

            }

    }*/

    /*fun getMyRequests() {
        val currentUser = currentLocalUser.value!!
        db.collection(USERS).document(currentUser.id).collection(REQUESTS)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener {
                val requests = it.toObjects(SimpleRequest::class.java)
                scope.launch(Dispatchers.IO) {
                    requestDao.insertItems(requests)
                }
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get post requests." + errorSuffix + it.localizedMessage!!
                )
            }
    }*/


    /*db.collection(USERS).document(otherUser.id)
        .update(map)
        .addOnSuccessListener {
            Log.d(BUG_TAG, "Updated other user -> ${otherUser.name}")
            scope.launch(Dispatchers.IO) {
                if (userDao.getUser(otherUser.id) != null) {
                    userDao.updateItem(otherUser)
                } else {
                    userDao.insert(otherUser)
                }
            }
        }.addOnFailureListener {
            Log.e(
                BUG_TAG,
                errorPrefix + "updating other user doc." + errorSuffix + it.localizedMessage!!
            )
        }*/


    /*private suspend fun filterUsers(newUsers: List<User>): List<User> {
        val currentUser = userDao.getCachedUser()
        if (currentUser != null) {
            newUsers.forEach { otherUser ->
                otherUser.isCurrentUser = currentUser.id == otherUser.id
                if (!otherUser.isCurrentUser) {
                    otherUser.isUserFollowed = currentUser.userPrivate.followings.contains(otherUser.id)
                    otherUser.isUserFollowingMe = currentUser.userPrivate.followers.contains(otherUser.id)
                }
            }
        }
        return newUsers
    }*/

    /*fun setUserListener(onSet: (user: User) -> Unit) {
        val currentUser = currentFirebaseUser
        if (currentUser != null) {
            val currentDocRef = db.collection(USERS).document(currentUser.uid)
            addDocumentListener<User>(currentDocRef){
                onSet(it)
            }
        }
    }*/

    suspend fun getUser(senderId: String): User? {
        val userDoc = db.collection(USERS).document(senderId)
        return getObject<User>(userDoc)
    }


    companion object {
        const val ZERO: Long = 0
        const val TAG = "FirebaseUtility"
    }

    /*currentUserRef.get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val user = it.toObject(User::class.java)!!
                    currentUserRef.collection("private").document(userId)
                        .get()
                        .addOnSuccessListener { userPrivateDoc ->

                            if (userPrivateDoc != null && userPrivateDoc.exists()) {
                                user.userPrivate = userPrivateDoc.toObject(UserPrivate::class.java)!!
                                scope.launch(Dispatchers.IO) {
                                    userDao.insertUser(user)
                                    filterPosts(user)
                                }
                                _currentLocalUser.postValue(user)
                            }

                        }.addOnFailureListener { exc ->
                            _networkErrors.postValue(exc)
                        }

                }
            }.addOnFailureListener {
                _networkErrors.postValue(it)
            }*/

    /*db.collection(USERS).whereEqualTo(USERNAME, u)
                .get()
                .addOnSuccessListener { qs ->
                    if (qs.isEmpty) {
                        usernameExists.postValue(false)
                    } else {
                        usernameExists.postValue(true)
                    }
                }.addOnFailureListener {
                    _networkErrors.postValue(it)
                }*/

    /*
        db.collection(POSTS).document(postId).get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val post = it.toObject(Post::class.java)!!
                    onComplete(post)
                }
            }.addOnFailureListener {
                _networkErrors.postValue(it)
            }*/

    /*db.collection(POSTS).document(post.id)
                .collection(REQUESTS)
                .whereEqualTo(SENDER, user.id)
                .limit(1)
                .get()
                .addOnSuccessListener { requestSnapshots ->
                    onComplete(requestSnapshots.toObjects(SimpleRequest::class.java))
                }.addOnFailureListener {
                    _networkErrors.postValue(it)
                }*/

    /*db.collection(CHAT_CHANNELS).document(chatChannelId).get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    val chatChannel = it.toObject(ChatChannel::class.java)!!

                }
            }.addOnFailureListener {
                _networkErrors.postValue(it)
            }*/

    /*db.collection(USERS).document(firebaseUser.uid)
            .addSnapshotListener { v, e ->
                if (e != null) {
                    _networkErrors.postValue(e)
                    return@addSnapshotListener
                }

                if (v != null && v.exists()) {
                    val user = v.toObject(User::class.java)!!

                    db.collection(USERS).document(firebaseUser.uid).collection("private").document(firebaseUser.uid)
                        .get()
                        .addOnSuccessListener {
                            if (it != null && it.exists()) {
                                val userPrivate = it.toObject(UserPrivate::class.java)!!
                                user.userPrivate = userPrivate
                                scope.launch(Dispatchers.IO) {
                                    setUpChannels(user)
                                    _currentLocalUser.postValue(user)
                                    userDao.insertUser(user)
                                }
                            }
                        }.addOnFailureListener {
                            _networkErrors.postValue(it)
                        }
                } else {
                    _networkErrors.postValue(Exception("User document is exist."))
                }
            }*/

    /*db.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
            .collection(MESSAGES)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(MESSAGE_PAGE_SIZE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _networkErrors.postValue(error)
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {
                    val messages = value.toObjects(SimpleMessage::class.java)

                    val lastMessage = messages.first()
                    chatChannel.lastMessage = lastMessage
                    chatChannel.updatedAt = System.currentTimeMillis()

                    scope.launch(Dispatchers.IO) {
                        messageDao.insertMessages(messages)
                        chatChannelDao.insert(chatChannel)
                    }
                }
            }*/

    /*private inline fun <reified T: Any> onQueryUpdate(querySnapshot: QuerySnapshot) {
        when (val clazz = T::class.java) {
            SimpleMessage::class.java -> {
                val messages = querySnapshot.toObjects(clazz)
                val lastMessage = messages.firstOrNull()

                scope.launch (Dispatchers.IO) {
                    if (lastMessage != null) {
                        chatChannelDao.getChatChannel(lastMessage.chatChannelId)?.let {
                            it.lastMessage = lastMessage
                            it.updatedAt = System.currentTimeMillis()

                            messageDao.insertMessages(messages)
                            chatChannelDao.insert(it)
                        }
                    }
                }
            }
        }
    }*/

    /*
                    filterPosts(user)
                    filterUsersAtStart()
                    _currentLocalUser.postValue(user)
                    _currentFirebaseUser.postValue(auth.currentUser)*/


/*db.runBatch {
			it.set(currentUserRef, user)
			it.set(currentUserPrivate, user.userPrivate)
		}.addOnSuccessListener {


			Log.d(BUG_TAG, "Upload batch run successfully.")

			_currentLocalUser.postValue(user)

			scope.launch(Dispatchers.IO) {
				user.isCurrentUser = true
				userDao.insertCurrentUser(user)
			}

		}.addOnFailureListener {
			Log.e(
				BUG_TAG,
				errorPrefix + "upload token to server" + errorSuffix + it.localizedMessage!!
			)
		}*/

    /*
    val currentUser = currentLocalUser.value!!
    val userDetail = currentUser.userPrivate
    val fullName = userMap[NAME] as String?
    val username = userMap[USERNAME] as String?
    val interests = userMap[INTERESTS]


    currentUser.userPrivate.interests = interests as List<String>

    val finalMap = userMap.toMutableMap()
    finalMap.remove(INTERESTS)

    val subStrings = mutableListOf<String>()

    val fullNameSubs = fullName?.let {
        currentUser.name = it
        getAllSubStrings(fullName)
    }
    if (fullNameSubs != null) {
        subStrings.addAll(fullNameSubs)
    }

    val usernameSubs = username?.let {
        currentUser.username = it
        getAllSubStrings(username)
    }

    if (usernameSubs != null) {
        subStrings.addAll(usernameSubs)
    }

    finalMap[INDICES] = subStrings
    currentUser.indices = subStrings

    currentUser.photo = userMap[PHOTO] as String?
    currentUser.about = userMap[ABOUT] as String?

    // user doc itself
    val currentUserDocRef = db.collection(USERS).document(currentUser.id)
    val currentUserDocPrivateRef =
        currentUserDocRef.collection(PRIVATE).document(currentUser.id)

    // all the posts ever created by the user
    val postIds = mutableListOf<String>()
    postIds.addAll(userDetail.projectIds)
    postIds.addAll(userDetail.blogIds)


    // all the places where the user has followed somebody
    val followings = userDetail.followings

    // all the places where the user was followed by somebody
    val followers = userDetail.followers

    // all the chat channels where the user has taken part
    val chatChannels = userDetail.chatChannels

    // all requests
    val requests = userDetail.requestIds

    // all notifications
    val notifications = userDetail.notificationIds

    db.runBatch {
        // 1
        it.update(currentUserDocRef, finalMap)

        // 2
        it.update(currentUserDocPrivateRef, mapOf(INTERESTS to interests))

        // 3
        for (postId in postIds) {
            val postRef = db.collection(POSTS).document(postId)
            it.update(postRef, ADMIN, currentUser)
        }

        // 4
        for (followingId in followings) {
            val followingRef = db.collection(USERS)
                .document(followingId)
                .collection(FOLLOWERS)
                .document(followingId)
                .collection(USERS)
                .document(currentUser.id)

            it.update(followingRef, finalMap)
        }

        // 5
        for (followerId in followers) {
            val followerRef = db.collection(USERS)
                .document(followerId)
                .collection(FOLLOWINGS)
                .document(followerId)
                .collection(USERS)
                .document(currentUser.id)

            it.update(followerRef, finalMap)
        }


        // 6
        for (channelId in chatChannels) {
            val chatChannelRef = db.collection(CHAT_CHANNELS)
                .document(channelId)
                .collection(USERS)
                .document(currentUser.id)
            it.update(chatChannelRef, finalMap)
        }

        for (i in userDetail.activeRequests.indices) {
            val requestRef =
                db.collection(POSTS).document(userDetail.activeRequests[i]).collection(REQUESTS)
                    .document(requests[i])
            it.update(requestRef, SENDER, currentUser)
        }
    }.addOnSuccessListener {
        _currentLocalUser.postValue(currentUser)

        scope.launch(Dispatchers.IO) {
            userDao.insertCurrentUser(currentUser)
            val lastMessageSenderChats =
                chatChannelDao.getChatChannelsForLastMessage(currentUser.id).toMutableList()
            if (!lastMessageSenderChats.isNullOrEmpty()) {
                for (chat in lastMessageSenderChats) {
                    chat.lastMessage?.sender = currentUser
                }
            }
            chatChannelDao.updateItems(lastMessageSenderChats)
        }

        updateUser.postValue(Result.Success(finalMap))
    }.addOnFailureListener {
        updateUser.postValue(Result.Error(it))
    }*/

    /*
        scope.launch (Dispatchers.IO) {
            when (val postResult = getObject(postRef)) {
                is Result.Success -> {
                    val post = postResult.data.toObject(Post::class.java)!!
                    val posts = filterPosts(listOf(post), currentLocalUser.value)
                    postDao.insertItems(posts)
                }
                is Result.Error -> {
                    _networkErrors.postValue(postResult.exception)
                }
            }
        }*/

    /*task.addOnFailureListener {
            Log.e(BUG_TAG, errorPrefix + "follow a user." + errorSuffix + it.localizedMessage!!)
        }.addOnSuccessListener {
            _currentLocalUser.postValue(currentUser)
        }*/

    /*task.addOnFailureListener {
        Log.e(BUG_TAG, errorPrefix + "follow a user." + errorSuffix + it.localizedMessage!!)
    }.addOnSuccessListener {
        scope.launch(Dispatchers.IO) {
            val otherPostsOfThisUser = postDao.getPostForUser(otherUser.id)
            if (!otherPostsOfThisUser.isNullOrEmpty()) {
                val existingList = otherPostsOfThisUser.toMutableList()
                for (p in existingList) {
                    p.postLocalData.isUserFollowed = isUserFollowed
                }
                postDao.updateItems(existingList)
            }
            _currentLocalUser.postValue(currentUser)
            currentUser.isCurrentUser = true
            userDao.updateItems(listOf(currentUser, otherUser))
        }
    }*/

    /*override suspend fun downloadMedia(externalDir: File?, message: SimpleMessage): SimpleMessage? {
        var name = message.metaData?.originalFileName!!
        var file = File(externalDir, name)
        val uri = Uri.parse(message.content)
        val fileRef = uri.lastPathSegment

        var counter = 0
        val ext = file.extension
        while (file.exists()) {
            counter++
            val actName = name.substring(0, name.length - (ext.length + 1))
            name = "$actName($counter).$ext"
            file = File(externalDir, name)
        }

        if (file.createNewFile()) {
            val objectRef = storage.reference.child(fileRef!!)
            val task = objectRef.getFile(file)
            task.await()

            objectRef.getFile(file).addOnSuccessListener {
                scope.launch(Dispatchers.IO) {
                    message.metaData?.originalFileName = name
                    message.isDownloaded = true
                    mediaDownloadResult.postValue(Result.Success(message))
                    messageDao.updateItem(message)
                }
            }.addOnFailureListener {
                mediaDownloadResult.postValue(Result.Error(it))
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get file from server." + errorSuffix + it.localizedMessage!!
                )
            }
        } else {
            Log.e(
                BUG_TAG,
                errorPrefix + "download a file." + errorSuffix + "Probably file already exists."
            )
        }
    }*/

    /*fun getChannelContributors(chatChannelId: String) {
        db.collection(CHAT_CHANNELS).document(chatChannelId).collection(USERS)
            .get()
            .addOnSuccessListener {
                val contributors = it.toObjects(User::class.java)
                for (contributor in contributors) {
                    contributor.userPrivate.chatChannels = listOf(chatChannelId)
                }
                scope.launch(Dispatchers.IO) {
                    insertUsersWithFilter(contributors, null)
                }
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get chat channel contributors." + errorSuffix + it.localizedMessage!!
                )
            }
    }*/

    /*override suspend fun updateChatChannels(channels: List<ChatChannel>) {
        for (channel in channels) {
            val lastMessage = channel.lastMessage
            if (lastMessage != null) {
                val contributor = userDao.getUser(lastMessage.senderId)
                if (contributor != null) {
                    if (!contributor.userPrivate.chatChannels.contains(channel.chatChannelId)) {
                        val existingList = contributor.userPrivate.chatChannels.toMutableList()
                        existingList.add(channel.chatChannelId)
                        contributor.userPrivate.chatChannels = existingList

                        userDao.updateItem(contributor)
                    }
                    lastMessage.sender = contributor
                    channel.lastMessage = lastMessage
                } else {
                    Log.e(
                        TAG,
                        "While updating chat channel from notification, there was no user corresponding to the last message of this chat channel"
                    )
                }
            }
        }

        chatChannelDao.updateItems(channels)
    }*/

    /*private inline fun <reified T : Any> addDocumentListener(documentReference: DocumentReference, crossinline onSet: (t: T) -> Unit) {
        documentReference.addSnapshotListener { value, error ->
            if (error != null) {
                Log.e(TAG, error.localizedMessage!!)
                return@addSnapshotListener
            }

            if (value != null && value.exists()) {
                val obj = value.toObject(T::class.java)!!
                onSet(obj)
            }
        }
    }*/

    /*private inline fun <reified T: Any> addQueryListener(query: Query) {
        query.addSnapshotListener { value, error ->
            if (error != null) {
                Log.e(TAG, error.localizedMessage!!)
                return@addSnapshotListener
            }

            if (value != null && !value.isEmpty) {
                onQueryUpdate<T>(value)
            }
        }
    }*/

    /*private fun <T : Any> onDocumentUpdate(obj: T) {
        when (obj) {
            is User -> {

                val user = _currentLocalUser.value
                if (user != null) {
                    val private = user.userPrivate
                    obj.userPrivate = private
                    _currentLocalUser.postValue(obj)

                    scope.launch(Dispatchers.IO) {
                        setUpChannels(user)
                        user.isCurrentUser = true
                        userDao.insertCurrentUser(user)
                    }

                }
            }
            is UserPrivate -> {
                val user = currentLocalUser.value
                if (user != null) {
                    user.userPrivate = obj
                    _currentLocalUser.postValue(user)

                    scope.launch(Dispatchers.IO) {
                        setUpChannels(user)
                        user.isCurrentUser = true
                        userDao.insertCurrentUser(user)
                    }
                }
            }
        }
    }*/


    /*override suspend fun setMessagesListener(
        externalImagesDir: File,
        externalDocumentsDir: File,
        chatChannel: ChatChannel
    ) {
        delay(2000)
        if (!channelMessagesListeners.containsKey(chatChannel.chatChannelId)) {
            val lr = db.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
                .collection(MESSAGES)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.e(
                            BUG_TAG,
                            errorPrefix + "set listener for messages." + errorSuffix + error.localizedMessage!!
                        )
                        return@addSnapshotListener
                    }

                    if (value != null && !value.isEmpty) {
                        val messages = value.toObjects(SimpleMessage::class.java)
                        val extras = mapOf(
                            CHAT_CHANNEL to chatChannel,
                            FILE_IMAGES to externalImagesDir,
                            FILE_DOCUMENTS to externalDocumentsDir
                        )

                        insertMessagesWithFilter(messages, extras)
                    }
                }

            channelMessagesListeners[chatChannel.chatChannelId] = lr
        }
    }*/

    /*override fun checkIfAlreadySentRequest(
        post: Post,
        onComplete: (requests: List<SimpleRequest>) -> Unit
    ) {
        val user = currentLocalUser.value
        if (user != null) {
            getObjects<SimpleRequest>(db.collection(POSTS).document(post.id)
                .collection(REQUESTS)
                .whereEqualTo(SENDER, user.id)
                .limit(1), {
                onComplete(it)
            }, {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "check for already sent request." + errorSuffix + it.localizedMessage!!
                )
            })

        }
    }*/

    /*
        scope.launch (Dispatchers.IO) {
            when (val chatChannelResult = getObject(chatChannelRef)) {
                is Result.Success -> {
                    val chatChannel = chatChannelResult.data.toObject(ChatChannel::class.java)!!
                    updateChatChannels(listOf(chatChannel))
                    onComplete(chatChannel)
                }
                is Result.Error -> _networkErrors.postValue(chatChannelResult.exception)
            }
        }*/


    /*private suspend fun filterUsers(
        users: List<User>,
        extras: Map<String, Any?>? = null
    ): List<User> {

        fun lastThing(us: List<User>): List<User> {
            val currentUser = currentLocalUser.value
            if (currentUser != null) {
                val userDetails = currentUser.userPrivate
                for (u in us) {
                    u.isUserFollowed = userDetails.followings.contains(u.id)
                    u.isUserFollowingMe = userDetails.followers.contains(u.id)
                    u.isCurrentUser = currentUser.id == u.id
                }
            }
            return us
        }

        if (extras != null) {
            val otherUser = extras[OTHER_USER] as User?
            if (otherUser != null) {
                val followerTag = extras[FOLLOWERS] as Boolean?
                val followingTag = extras[FOLLOWINGS] as Boolean?
                if (followerTag != null && followerTag) {
                    for (user in users) {
                        val existingList = user.userPrivate.followings.toMutableList()
                        existingList.add(otherUser.id)
                        user.userPrivate.followings = existingList
                    }
                    val us = lastThing(users)
                    userDao.insertItems(us)
                    return us
                }

                if (followingTag != null && followingTag) {
                    for (user in users) {
                        val existingList = user.userPrivate.followers.toMutableList()
                        existingList.add(otherUser.id)
                        user.userPrivate.followers = existingList
                    }
                    val us = lastThing(users)
                    userDao.insertItems(us)
                    return us
                }
            }

            val post = extras[POST] as Post?
            if (post != null && post.type == PROJECT) {
                for (user in users) {
                    val existingList = user.userPrivate.chatChannels.toMutableList()
                    existingList.add(post.chatChannelId!!)
                    user.userPrivate.chatChannels = existingList
                }
                val us = lastThing(users)
                userDao.insertItems(us)
                return us
            }

            val chatChannel = extras[CHAT_CHANNEL] as ChatChannel?
            if (chatChannel != null) {
                for (user in users) {
                    val u = userDao.getUser(user.id)
                    if (u != null) {
                        val existingList = u.userPrivate.chatChannels.toMutableList()
                        existingList.add(chatChannel.chatChannelId)
                        u.userPrivate.chatChannels = existingList
                        val ux = lastThing(listOf(u))
                        userDao.insertUser(ux[0])
                    } else {
                        val existingList = user.userPrivate.chatChannels.toMutableList()
                        existingList.add(chatChannel.chatChannelId)
                        user.userPrivate.chatChannels = existingList
                        val ux = lastThing(listOf(user))
                        userDao.insertUser(ux[0])
                    }

                }
                chatChannel.lastMessage?.sender = users.find {
                    it.id == chatChannel.lastMessage?.senderId
                }!!

                chatChannelDao.updateItem(chatChannel)
                return users
            }
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
        userDao.insertItems(users)
        return users
    }*/


    /*@Suppress("UNCHECKED_CAST")
    private fun insertMessagesWithFilter(
        messages: List<SimpleMessage>,
        extras: Map<String, Any?>?
    ) {

        if (extras != null) {
            val chatChannel = extras[CHAT_CHANNEL] as ChatChannel?
            val externalImagesDir = extras[FILE_IMAGES] as File?
            val externalDocumentsDir = extras[FILE_DOCUMENTS] as File?
            for (message in messages) {
                usersMap[message.senderId]?.let {
                    message.sender = it
                }
            }

            if (chatChannel != null) {
                val lastMessage = messages.first()
                chatChannel.lastMessage = lastMessage

                if (externalImagesDir != null && externalDocumentsDir != null) {
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
                }

                scope.launch(Dispatchers.IO) {
                    messageDao.insertMessages(messages)
                    chatChannelDao.updateItem(chatChannel)
                }

            }
        }
    }*/

    /*private fun filterPosts(
        posts: List<Post>,
        user: User?,
        extras: Map<String, Any?>? = null
    ): List<Post> {
        val blogItems = mutableListOf<BlogItem>()

        val inFeed = extras?.get(HOME_FEED) as Boolean?
        val withTag = extras?.get(WITH_TAG) as String?

        if (user != null) {
            val userDetails = user.userPrivate
            val interests = userDetails.interests.toSet()
            val finalSet = mutableSetOf<String>()
            for (post in posts) {

                post.postLocalData.isCreator =
                    userDetails.projectIds.contains(post.id) || userDetails.blogIds.contains(post.id)
                post.postLocalData.isLiked = userDetails.likedPosts.contains(post.id)
                post.postLocalData.isSaved = userDetails.savedPosts.contains(post.id)
                post.postLocalData.isDisliked = userDetails.dislikedPosts.contains(post.id)
                post.postLocalData.isCollaboration = userDetails.collaborationIds.contains(post.id)

                val tempSet = post.tags.toMutableSet()
                val intersection = tempSet.intersect(interests)
                if (intersection.isNotEmpty()) {
                    finalSet.addAll(intersection)
                }

                if (!post.postLocalData.isCreator) {
                    val isFollowed = userDetails.followings.contains(post.uid)
                    post.postLocalData.isUserFollowed = isFollowed
                }

                if (inFeed != null && inFeed) {
                    post.postLocalData.inFeed = inFeed
                }

            }

            if (withTag == null) {
                val tags = mutableListOf<SimpleTag>()
                for (item in finalSet) {
                    tags.add(SimpleTag(item, 0))
                }
                scope.launch(Dispatchers.IO) {
                    simpleTagsDao.insertItems(tags)
                }
            }
        } else {
            val tagsList = mutableListOf<String>()
            val tags = mutableListOf<SimpleTag>()
            for (post in posts) {
                if (inFeed != null && inFeed) {
                    post.postLocalData.inFeed = inFeed
                }
                tagsList.addAll(post.tags)
            }

            for (item in tagsList) {
                tags.add(SimpleTag(item, 0))
            }

            if (withTag == null) {
                scope.launch(Dispatchers.IO) {
                    simpleTagsDao.insertItems(tags)
                }
            }
        }

        return posts
    }*/

    /*private suspend fun insertPosts(
        posts: List<Post>,
        extras: Map<String, Any?>? = null
    ): List<Post> {
        for (post in posts) {
            Log.d(BUG_TAG, post.id)
        }
        val returnedPosts = filterPosts(posts, currentLocalUser.value, extras)
        for (post in returnedPosts) {
            if (postDao.getPost(post.id) != null) {
                postDao.updateItem(post)
            } else {
                postDao.insert(post)
            }
        }
        return returnedPosts
    }*/

    /*fun <T> getSnapshot(item: Any?, clazz: Class<T>, onComplete: (doc: DocumentSnapshot) -> Unit) {
        when (clazz) {
            Post::class.java -> {
                val post = item as Post
                db.collection(POSTS).document(post.id)
                    .get()
                    .addOnSuccessListener {
                        if (it.exists() && it != null) {
                            onComplete(it)
                        }
                    }.addOnFailureListener {
                        Log.e(
                            BUG_TAG,
                            errorPrefix + "get a snapshot." + errorSuffix + it.localizedMessage!!
                        )
                    }
            }
            SimpleMessage::class.java -> {
                val message = item as SimpleMessage
                db.collection(CHAT_CHANNELS).document(message.chatChannelId).collection(MESSAGES)
                    .document(message.messageId).get()
                    .addOnSuccessListener {
                        if (it.exists() && it != null) {
                            onComplete(it)
                        }
                    }.addOnFailureListener {
                        Log.e(
                            BUG_TAG,
                            errorPrefix + "get messages for a channel." + errorSuffix + it.localizedMessage!!
                        )
                    }
            }
            UserMinimal::class.java -> {
                val userMinimal = item as UserMinimal
                db.collection(USER_MINIMALS).document(userMinimal.id)
                    .get()
                    .addOnSuccessListener {
                        if (it.exists() && it != null) {
                            onComplete(it)
                        }
                    }.addOnFailureListener {
                        Log.e(
                            BUG_TAG,
                            errorPrefix + "get userMinimal. wtf" + errorSuffix + it.localizedMessage!!
                        )
                    }
            }
            User::class.java -> {
                val user = item as User
                db.collection(USERS).document(user.id)
                    .get()
                    .addOnSuccessListener {
                        if (it.exists() && it != null) {
                            onComplete(it)
                        }
                    }.addOnFailureListener {
                        Log.e(
                            BUG_TAG,
                            errorPrefix + "get user." + errorSuffix + it.localizedMessage!!
                        )
                    }
            }
        }
    }*/

    /*fun <T> getObject(
        documentReference: DocumentReference,
        clazz: Class<T>,
        onComplete: (obj: T) -> Unit
    ) {
        documentReference.get()
            .addOnSuccessListener {
                if (it != null && it.exists()) {
                    onComplete(it.toObject(clazz)!!)
                }
            }.addOnFailureListener {
                Log.e(
                    BUG_TAG,
                    errorPrefix + "get an object of type ${clazz::getCanonicalName}." + errorSuffix + it.localizedMessage!!
                )
            }
    }*/


}