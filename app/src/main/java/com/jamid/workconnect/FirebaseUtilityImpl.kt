package com.jamid.workconnect

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
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
import com.jamid.workconnect.data.WorkConnectDatabase
import com.jamid.workconnect.home.BlogItem
import com.jamid.workconnect.message.ChatChannelFragment
import com.jamid.workconnect.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FirebaseUtilityImpl(private val scope: CoroutineScope, database: WorkConnectDatabase) :
	FirebaseUtility {

	private val auth = Firebase.auth
	val db = Firebase.firestore
	private val storage = Firebase.storage
	private val userDao = database.userDao()
	private val messageDao = database.messageDao()
	private val chatChannelDao = database.chatChannelDao()
	private val userMinimalDao = database.userMinimalDao()
	private val chatChannelContributorDao = database.chatChannelContributorDao()
	private val postDao = database.postDao()
	private val requestDao = database.activeRequestDao()
	private val notificationDao = database.notificationDao()
	private val simpleTagsDao = database.simpleTagsDao()

	private val errorPrefix = "Error caused while trying to "
	private val errorSuffix = "Reason : "

	private val _currentFirebaseUser =
		MutableLiveData<FirebaseUser>().apply { value = null }

	val currentFirebaseUser: LiveData<FirebaseUser> = _currentFirebaseUser
	val signInResult = MutableLiveData<Result<FirebaseUser>>().apply { value = null }

	val registerResult = MutableLiveData<Result<FirebaseUser>>().apply { value = null }
	val firebaseUserUpdateResult = MutableLiveData<Result<FirebaseUser>>().apply { value = null }

	private val _currentLocalUser = MutableLiveData<User>().apply { value = null }
	val currentLocalUser: LiveData<User> = _currentLocalUser

	private val _networkErrors = MutableLiveData<Exception>().apply { value = null }
	val networkErrors: LiveData<Exception> = _networkErrors

	val usernameExists = MutableLiveData<Result<Boolean>>().apply { value = null }
	val emailExists = MutableLiveData<Boolean>().apply { value = false }
	val profilePhotoUpload = MutableLiveData<Uri?>().apply { value = null }
	val postPhotoUpload = MutableLiveData<Result<Uri>>().apply { value = null }
	val postUpload = MutableLiveData<Result<Post>>().apply { value = null }
	val requestSent = MutableLiveData<Result<String>>().apply { value = null }

	val undoRequestSent = MutableLiveData<Result<SimpleRequest>>().apply { value = null }
	val acceptRequestResult = MutableLiveData<Result<SimpleNotification>>().apply { value = null }
	val declineRequestResult = MutableLiveData<Result<SimpleNotification>>().apply { value = null }

	val guidelinesUpdateResult = MutableLiveData<Post>().apply { value = null }

	val mediaUploadResult = MutableLiveData<Result<SimpleMessage>>().apply { value = null }
	val updateUser = MutableLiveData<Result<Map<String, Any?>>>().apply { value = null }

	val localTagsList = MutableLiveData<List<String>>().apply { value = null }

	// new set
	val currentOtherUserDetail = MutableLiveData<UserPrivate>().apply { value = null }

	var uid: String? = auth.uid

	init {
		val firebaseUser = auth.currentUser
		if (firebaseUser != null) {
			_currentFirebaseUser.postValue(firebaseUser)
			addUserListener(firebaseUser)
			getCurrentUser(firebaseUser.uid)
			getToken()
		}
	}

	private fun addUserListener(firebaseUser: FirebaseUser) {
		val currentUserRef = db.collection(USERS).document(firebaseUser.uid)
		addDocumentListener<User>(currentUserRef)
		addDocumentListener<UserPrivate>(currentUserRef.collection(PRIVATE).document(firebaseUser.uid))
	}

	override fun getToken() {
		FirebaseMessaging.getInstance()
			.token.addOnSuccessListener {
				updateRegistrationToken(it)
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get identity token." + errorSuffix + it.localizedMessage!!)
			}
	}

	override fun signIn(email: String, password: String) {
		auth.signInWithEmailAndPassword(email, password)
			.addOnSuccessListener {
				if (it != null) {
					val result = Result.Success(it.user!!)
					signInResult.postValue(result)
					getCurrentUser(it.user!!.uid)
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
//				getCurrentUser(it.user!!.uid)
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

	override fun signInWithGoogle() {

	}

	override fun getCurrentUser(userId: String) {
		val currentUserRef = db.collection(USERS).document(userId)
		getObject<User>(currentUserRef) { user ->
			getObject<UserPrivate>(currentUserRef.collection(PRIVATE).document(userId)) { userData ->
				user.userPrivate = userData
				scope.launch(Dispatchers.IO) {
					setUpChannels(user)
					user.isCurrentUser = true
					userDao.insertCurrentUser(user)
					filterPosts(user)
					_currentLocalUser.postValue(user)
					_currentFirebaseUser.postValue(auth.currentUser)
					filterUsersAtStart()
				}
			}
		}
	}

	private suspend fun filterUsersAtStart() {
		val otherUsers = userDao.getAllUsers()
		if (otherUsers.isNotEmpty()) {
			filterUsers(otherUsers)
		}
	}

	fun checkIfUsernameExists(username: String? = null) {
		fun check(u: String) {
			getObjects<User>(db.collection(USERS).whereEqualTo(USERNAME, u), {
				if (it.isEmpty()) {
					usernameExists.postValue(Result.Success(false))
				} else {
					usernameExists.postValue(Result.Success(true))
				}
			}, {
				usernameExists.postValue(Result.Error(it))
			})
		}

		val user = auth.currentUser ?: return
		if (username != null) {
			val localUser = currentLocalUser.value
			if (localUser != null) {
				if (localUser.username == username) {
					usernameExists.postValue(Result.Success(false))
					return
				}
			}
			check(username)
		} else {
			val tempUsername = user.email?.split('@')?.get(0)
			if (tempUsername != null) {
				check(tempUsername)
			}
		}
	}

	fun createNewUser(tags: List<String>? = null): User? {
		val currentUser = auth.currentUser
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

	override fun uploadCurrentUser(user: User) {
		FirebaseMessaging.getInstance()
			.token.addOnSuccessListener { token ->
//				updateRegistrationToken(it)
				val currentUserRef = db.collection(USERS).document(user.id)
				val currentUserPrivate = db.collection(USERS)
					.document(user.id)
					.collection(PRIVATE)
					.document(user.id)

				val subStrings = mutableListOf<String>()
				subStrings.addAll(getAllSubStrings(user.name))
				subStrings.addAll(getAllSubStrings(user.username))
				user.indices = subStrings

				user.userPrivate.registrationTokens = listOf(token)

				db.runBatch {
					it.set(currentUserRef, user)
					it.set(currentUserPrivate, user.userPrivate)
				}.addOnSuccessListener {

					_currentLocalUser.postValue(user)

					scope.launch(Dispatchers.IO) {
						user.isCurrentUser = true
						userDao.insertCurrentUser(user)
					}

				}.addOnFailureListener {
					Log.e(BUG_TAG, errorPrefix + "upload token to server" + errorSuffix + it.localizedMessage!!)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get token from server" + errorSuffix + it.localizedMessage!!)
			}

	}

	override fun uploadCurrentUser(userMap: MutableMap<String, Any?>) {
		TODO()
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

	override fun clearSignInChanges() {
		signInResult.postValue(null)
		registerResult.postValue(null)
	}

	override fun updateCurrentUser(userMap: Map<String, Any?>) {
		val currentUser = currentLocalUser.value!!
		val userDetail = currentUser.userPrivate
		val fullName = userMap[NAME] as String?
		val username = userMap[USERNAME] as String?
		val interests = userMap[INTERESTS]

		@Suppress("UNCHECKED_CAST")
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
		val currentUserDocPrivateRef = currentUserDocRef.collection(PRIVATE).document(currentUser.id)

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
				val requestRef = db.collection(POSTS).document(userDetail.activeRequests[i]).collection(REQUESTS).document(requests[i])
				it.update(requestRef, SENDER, currentUser)
			}

			for (i in notifications.indices) {
				val notificationRef = db.collection(USERS).document(userDetail.notificationReferences[i]).collection(
					NOTIFICATIONS).document(notifications[i])

				it.update(notificationRef, SENDER, currentUser)
			}

		}.addOnSuccessListener {
			_currentLocalUser.postValue(currentUser)

			scope.launch (Dispatchers.IO) {
				userDao.insertCurrentUser(currentUser)
				val lastMessageSenderChats = chatChannelDao.getChatChannelsForLastMessage(currentUser.id).toMutableList()
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
		}
	}


	// add registration tokens to the following places (for notifications)
	// 1. User document
	// 2. All chat channel documents (for group notifications)

	override fun updateRegistrationToken(token: String) {
		val user = currentLocalUser.value
		if (user != null) {
			val userDetail = user.userPrivate
			if (userDetail.registrationTokens.isEmpty() || !userDetail.registrationTokens.contains(
					token
				)
			) {
				val uid = user.id

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
					Log.e(BUG_TAG, errorPrefix + "update token to server" + errorSuffix + it.localizedMessage!!)
				}
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
								Log.e(BUG_TAG, errorPrefix + "get download url of uploaded profile photo." + errorSuffix + it.localizedMessage!!)
							}
					}
					.addOnFailureListener {
						Log.e(BUG_TAG, errorPrefix + "upload profile photo to server" + errorSuffix + it.localizedMessage!!)
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

	override fun uploadPost(post: Post) {
		val currentUser = currentLocalUser.value!!
		val userDetail = currentUser.userPrivate

		val ref = db.collection(POSTS).document()
		val postId = ref.id
		val now = System.currentTimeMillis()
		val chatChannelRef = db.collection(CHAT_CHANNELS).document()
		val chatChannelRefId = chatChannelRef.id

		post.id = postId
		post.admin = currentUser
		post.uid = currentUser.id
		post.chatChannelId = chatChannelRefId

		var chatChannel: ChatChannel? = null

		val subStrings = mutableListOf<String>()
		subStrings.addAll(getAllSubStrings(post.title))
		for (tag in post.tags) {
			subStrings.addAll(getAllSubStrings(tag))
		}

		post.indices = subStrings

		db.runBatch {
			it.set(ref, post)

			if (post.type == PROJECT) {
				it.update(
					db.collection(USERS).document(currentUser.id).collection(PRIVATE).document(currentUser.id),
					PROJECT_IDS,
					FieldValue.arrayUnion(postId)
				)
				it.update(
					db.collection(USERS).document(currentUser.id).collection(PRIVATE).document(currentUser.id),
					USER_CHAT_CHANNELS,
					FieldValue.arrayUnion(chatChannelRefId)
				)
				chatChannel = ChatChannel(
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
				it.set(chatChannelRef, chatChannel!!)

				it.set(
					chatChannelRef.collection(USERS).document(currentUser.id),
					currentUser
				)
			} else {
				it.update(
					db.collection(USERS).document(currentUser.id).collection(PRIVATE).document(currentUser.id),
					BLOG_IDS,
					FieldValue.arrayUnion(postId)
				)
			}
		}.addOnSuccessListener {
			scope.launch(Dispatchers.IO) {
				insertPosts(listOf(post))
				chatChannelDao.insert(chatChannel!!)
			}
			postUpload.postValue(Result.Success(post))
		}.addOnFailureListener {
			postUpload.postValue(Result.Error(it))
		}
	}

	fun undoProjectRequest(request: SimpleRequest) {
		val currentUser = currentLocalUser.value!!
		val currentUserRef = db.collection(USERS).document(currentUser.id)

		// delete active request from the sender's document
		val senderRef = db.collection(USERS).document(request.sender.id)
		val senderPrivateRef = senderRef.collection(PRIVATE).document(request.sender.id)

		// delete active request from the own document / delete locally
		val currentRequestsRef = currentUserRef.collection(REQUESTS).document(request.id)


		// delete request from post collection
		val postRequestRef = db.collection(POSTS).document(request.post.id).collection(
			REQUESTS).document(request.id)


		// delete notification from own directory / delete locally
		val currentUserNotificationRef = currentUserRef.collection(NOTIFICATIONS).document(request.notificationId)

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

		db.runBatch {
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
		}.addOnSuccessListener {
			undoRequestSent.postValue(Result.Success(request))
			scope.launch (Dispatchers.IO) {
				requestDao.deleteRequest(request)
				_currentLocalUser.postValue(currentUser)
				userDao.insertCurrentUser(currentUser)
			}
		}.addOnFailureListener {
			undoRequestSent.postValue(Result.Error(it))
		}

	}

	fun acceptProjectRequest(notification: SimpleNotification) {
		val currentUser = currentLocalUser.value!!
		val currentUserRef = db.collection(USERS).document(currentUser.id)

		// delete sender active requests in sender doc
		val senderDocRef = db.collection(USERS).document(notification.sender.id)
		val senderDocPrivateRef = senderDocRef.collection(PRIVATE).document(notification.sender.id)

		// delete sender request
		val senderRequestRef = senderDocRef.collection(REQUESTS).document(notification.requestId!!)


		// delete receiver request
		val postRequestRef = db.collection(POSTS).document(notification.post!!.id).collection(REQUESTS).document(notification.requestId!!)

		// delete receiver's notification
		val receiverNotificationRef = currentUserRef.collection(NOTIFICATIONS).document(notification.id)


		// create new success notification for sender
		val senderNotificationRef = senderDocRef.collection(NOTIFICATIONS).document()
		val successNotificationId = senderNotificationRef.id

		val successNotification = SimpleNotification(successNotificationId, notification.sender.id, ACCEPT_PROJECT, notification.requestId, currentUser, post = notification.post)


		// add sender id to project contributor list in doc
		val projectRef = db.collection(POSTS).document(notification.post!!.id)


		// add sender to chat channel contributor list in doc
		val chatChannelRef = db.collection(CHAT_CHANNELS).document(notification.post!!.chatChannelId!!)

		// add sender to chat channel users collection
		val chatChannelContributorsRef = chatChannelRef.collection(USERS).document(notification.sender.id)

		db.runBatch {
			val map = mapOf(
				ACTIVE_REQUESTS to FieldValue.arrayRemove(notification.post!!.id),
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
		}.addOnSuccessListener {
			acceptRequestResult.postValue(Result.Success(notification))
			val localSender = notification.sender
			localSender.userPrivate.chatChannels = listOf(notification.post!!.chatChannelId!!)
			scope.launch (Dispatchers.IO) {
				insertUsersWithFilter(listOf(localSender), null)
				notificationDao.deleteNotification(notification)

				val chatChannel = chatChannelDao.getChatChannel(notification.post!!.chatChannelId!!)
				if (chatChannel != null) {
					chatChannel.contributorsCount += 1
					val existingList = chatChannel.contributorsList.toMutableList()
					existingList.add(notification.sender.id)
					chatChannel.contributorsList = existingList

					chatChannelDao.updateItem(chatChannel)
				}

			}
		}.addOnFailureListener {
			acceptRequestResult.postValue(Result.Error(it))
		}

	}


	fun denyProjectRequest(notification: SimpleNotification) {
		val currentUser = currentLocalUser.value!!
		val currentUserRef = db.collection(USERS).document(currentUser.id)

		// delete sender active requests in sender doc
		val senderDocRef = db.collection(USERS).document(notification.sender.id)
		val senderDocPrivateRef = senderDocRef.collection(PRIVATE).document(notification.sender.id)


		// delete sender request
		val senderRequestRef = senderDocRef.collection(REQUESTS).document(notification.requestId!!)


		// delete receiver request
		val postRequestRef = db.collection(POSTS).document(notification.post!!.id).collection(REQUESTS).document(notification.requestId!!)


		// delete receiver's notification
		val receiverNotificationRef = currentUserRef.collection(NOTIFICATIONS).document(notification.id)


		// create new success notification for sender
		val senderNotificationRef = senderDocRef.collection(NOTIFICATIONS).document()
		val failureNotificationId = senderNotificationRef.id

		val failureNotification = SimpleNotification(failureNotificationId, notification.sender.id, DECLINE_PROJECT, null, currentUser, post = notification.post)

		db.runBatch {
			val map = mapOf(
				ACTIVE_REQUESTS to FieldValue.arrayRemove(notification.post!!.id),
				NOTIFICATION_REFERENCES to FieldValue.arrayUnion(notification.sender.id),
				NOTIFICATION_IDS to FieldValue.arrayUnion(failureNotificationId)
			)
			it.update(senderDocPrivateRef, map)
			it.delete(senderRequestRef)
			it.delete(postRequestRef)
			it.delete(receiverNotificationRef)
			it.set(senderNotificationRef, failureNotification)
		}.addOnSuccessListener {
			declineRequestResult.postValue(Result.Success(notification))
			scope.launch (Dispatchers.IO) {
				notificationDao.deleteNotification(notification)
			}
		}.addOnFailureListener {
			declineRequestResult.postValue(Result.Error(it))
		}
	}

	override fun joinProject(post: Post) {
		val requestRef = db.collection(POSTS).document(post.id).collection(REQUESTS).document()
		val requestId = requestRef.id
		val currentUser = currentLocalUser.value!!
		val receiverNotificationRef = db.collection(USERS).document(post.uid).collection(NOTIFICATIONS).document()
		val senderRequestRef = db.collection(USERS).document(currentUser.id).collection(REQUESTS).document(requestId)
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

		db.runBatch {
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

		}.addOnSuccessListener {
			requestSent.postValue(Result.Success(requestId))
			scope.launch (Dispatchers.IO) {
				_currentLocalUser.postValue(currentUser)
				requestDao.insert(simpleRequest)
				userDao.insertCurrentUser(currentUser)
			}
		}.addOnFailureListener {
			requestSent.postValue(Result.Error(it))
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
							Log.e(BUG_TAG, errorPrefix + "get download url." + errorSuffix + it.localizedMessage!!)
						}
				}
				.addOnFailureListener {
					Log.e(BUG_TAG, errorPrefix + "upload post image to server." + errorSuffix + it.localizedMessage!!)
				}
		}
	}

	// the state before changes
	override fun onLikePressed(post: Post): Post {
		val userId = currentLocalUser.value!!.id
		val userMap = mutableMapOf<String, Any>()

		if (post.postLocalData.isLiked) {
			if (post.likes != ZERO) {
				post.likes -= 1
			}
			post.postLocalData.isLiked = false
			userMap[LIKED_POSTS] = FieldValue.arrayRemove(post.id)
		} else {
			post.likes += 1
			post.postLocalData.isLiked = true
			userMap[LIKED_POSTS] = FieldValue.arrayUnion(post.id)

			if (post.postLocalData.isDisliked) {
				userMap[DISLIKED_POSTS] = FieldValue.arrayRemove(post.id)
				if (post.dislikes != ZERO) {
					post.dislikes -= 1
				}
				post.postLocalData.isDisliked = false
			}
		}

		val map = mapOf(LIKES to post.likes, DISLIKES to post.dislikes)
		val currentUserPrivateRef = db.collection(USERS).document(userId).collection(PRIVATE).document(userId)

		db.runBatch {
			// update the post document in fireStore
			it.update(db.collection(POSTS).document(post.id), map)
			it.update(currentUserPrivateRef, userMap)
		}.addOnFailureListener {
			Log.e(BUG_TAG, errorPrefix + "like a post." + errorSuffix + it.localizedMessage!!)
		}.addOnSuccessListener {
			scope.launch {
				postDao.updateItem(post)
			}
		}
		return post
	}


	override fun onDislikePressed(post: Post): Post {
		val userId = currentLocalUser.value!!.id
		val userMap = mutableMapOf<String, Any>()

		if (post.postLocalData.isDisliked) {
			if (post.dislikes != ZERO) {
				post.dislikes -= 1
			}
			post.postLocalData.isDisliked = false
			userMap[DISLIKED_POSTS] = FieldValue.arrayRemove(post.id)
		} else {
			post.dislikes += 1
			post.postLocalData.isDisliked = true
			userMap[DISLIKED_POSTS] = FieldValue.arrayUnion(post.id)

			if (post.postLocalData.isLiked) {
				userMap[LIKED_POSTS] = FieldValue.arrayRemove(post.id)
				if (post.likes != ZERO) {
					post.likes -= 1
				}
				post.postLocalData.isLiked = false
			}
		}

		val map = mapOf(LIKES to post.likes, DISLIKES to post.dislikes)
		val currentUserPrivateRef = db.collection(USERS).document(userId).collection(PRIVATE).document(userId)

		db.runBatch {
			// update the post document in fireStore
			it.update(db.collection(POSTS).document(post.id), map)
			it.update(currentUserPrivateRef, userMap)
		}.addOnFailureListener {
			Log.e(BUG_TAG, errorPrefix + "dislike a post." + errorSuffix + it.localizedMessage!!)
		}.addOnSuccessListener {
			scope.launch (Dispatchers.IO) {
				postDao.updateItem(post)
			}
		}
		return post
	}

	override fun getPost(postId: String) {
		getObject<Post>(db.collection(POSTS).document(postId)) {
			scope.launch (Dispatchers.IO) {
				val posts = filterPosts(listOf(it), currentLocalUser.value)
				postDao.insertItems(posts)
			}
		}
	}

	override fun checkIfAlreadySentRequest(
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
				Log.e(BUG_TAG, errorPrefix + "check for already sent request." + errorSuffix + it.localizedMessage!!)
					})

		}
	}


	override fun onPostSaved(post: Post): Post {
		val userId = currentLocalUser.value!!.id
		val currentUserPrivateRef = db.collection(USERS).document(userId).collection(PRIVATE).document(userId)
		val task = if (post.postLocalData.isSaved) {
			post.postLocalData.isSaved = false
			currentUserPrivateRef.update(SAVED_POSTS, FieldValue.arrayRemove(post.id))
		} else {
			post.postLocalData.isSaved = true
			currentUserPrivateRef.update(SAVED_POSTS, FieldValue.arrayUnion(post.id))
		}

		task.addOnSuccessListener {
			scope.launch (Dispatchers.IO) {
				postDao.updateItem(post)
			}
		}.addOnFailureListener {
			Log.e(BUG_TAG, errorPrefix + "save a post." + errorSuffix + it.localizedMessage!!)
		}
		return post
	}

	fun onFollowPressed(currentUser: User, otherUser: User) {
		val currentUserPrivateRef = db.collection(USERS).document(currentUser.id).collection(PRIVATE).document(currentUser.id)
		val otherUserRef = db.collection(USERS).document(otherUser.id).collection(PRIVATE).document(otherUser.id)
		val followingsRef = db.collection(USERS).document(currentUser.id).collection(FOLLOWINGS).document(currentUser.id).collection(
			USERS).document(otherUser.id)

		val followersRef = db.collection(USERS).document(otherUser.id).collection(FOLLOWERS).document(otherUser.id).collection(USERS).document(currentUser.id)

		val userDetail = currentUser.userPrivate
		var isUserFollowed = userDetail.followings.contains(otherUser.id)

		val task = if (isUserFollowed) {
			otherUser.weightage -= 0.1
			otherUser.isUserFollowed = false
			isUserFollowed = false
			val existingList = userDetail.followings.toMutableList()
			existingList.remove(otherUser.id)
			userDetail.followings = existingList

			db.runBatch {
				it.update(currentUserPrivateRef, FOLLOWINGS, FieldValue.arrayRemove(otherUser.id))
				it.update(otherUserRef, FOLLOWERS, FieldValue.arrayRemove(currentUser.id))
				it.delete(followingsRef)
				it.delete(followersRef)
			}
		} else {
			otherUser.weightage += 0.1
			otherUser.isUserFollowed = true
			isUserFollowed = true
			val existingList = userDetail.followings.toMutableList()
			existingList.add(otherUser.id)
			userDetail.followings = existingList

			db.runBatch {
				it.update(currentUserPrivateRef, FOLLOWINGS, FieldValue.arrayUnion(otherUser.id))
				it.update(otherUserRef, FOLLOWERS, FieldValue.arrayUnion(currentUser.id))
				it.set(followingsRef, otherUser)
				it.set(followersRef, currentUser)
			}
		}

		task.addOnFailureListener {
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
		}
	}

	override fun onFollowPressed(post: Post): Post {
		val otherUser = post.admin
		if (post.type == PROJECT) {
			val existingList = otherUser.userPrivate.chatChannels.toMutableList()
			existingList.add(post.chatChannelId!!)
			otherUser.userPrivate.chatChannels = existingList
		}
		post.postLocalData.isUserFollowed = !post.postLocalData.isUserFollowed
		val currentUser = currentLocalUser.value!!
		onFollowPressed(currentUser, otherUser)
		return post
	}

	override fun updatePost(post: Post, postMap: Map<String, Any?>) {
		db.collection(POSTS).document(post.id).update(postMap)
			.addOnSuccessListener {
				scope.launch (Dispatchers.IO) {
					postDao.updateItem(post)
					guidelinesUpdateResult.postValue(post)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "update a post." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun <T : Any> getPostsOnStart(
		query: Query,
		ofClass: Class<T>,
		onComplete: (items: List<T>) -> Unit
	) {
		query.get()
			.addOnSuccessListener {
				val objects = it.toObjects(ofClass)
				onComplete(objects)
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get a bunch of post on start." + errorSuffix + it.localizedMessage!!)
			}
	}

	private suspend fun checkChannelsDiff(newUser: User): Boolean {
		val chatChannels = chatChannelDao.checkIfChatChannelsEmpty()
		val newUserDetail = newUser.userPrivate
		if (chatChannels.isNullOrEmpty()) {
			return true
		}
		val local = userDao.getUser(newUser.id)
		val oldUserDetail = local?.userPrivate
		if (newUser == local) {
			return false
		} else if (newUserDetail.chatChannels != oldUserDetail?.chatChannels) {
			return true
		}
		return true
	}

	// TODO("Something is wrong here")
	override suspend fun setUpChannels(user: User) {
		/*if (checkChannelsDiff(user)) {
			getChatChannels(user) { chatChannels ->
				scope.launch(Dispatchers.IO) {
					chatChannelDao.insertItems(chatChannels)
				}
			}
		}*/

		// TODO("Residue code")
		/*   db.collection(CHAT_CHANNELS).document(chatChannelId).collection(CONTRIBUTORS).get()
				  .addOnSuccessListener {
					  if (it != null && !it.isEmpty) {
						  val chatChannelContributors = it.toObjects(ChatChannelContributor::class.java)
						  for (contributor in chatChannelContributors) {
							  contributor.channelId = chatChannelId
						  }
						  scope.launch (Dispatchers.IO) {
							  chatChannelContributorDao.insertContributors(chatChannelContributors)
						  }
					  }
				  }.addOnFailureListener {
					  _networkErrors.postValue(it)
				  }*/
		/*val chatChannelIds = user.chatChannels
			chatChannelIds.forEach { chatChannelId ->
				db.collection(CHAT_CHANNELS).document(chatChannelId).get()
					.addOnSuccessListener {
						if (it != null && it.exists()) {
							val chatChannel = it.toObject(ChatChannel::class.java)!!
							scope.launch (Dispatchers.Default) {
								chatChannelDao.insert(chatChannel)
							}

							db.collection(POSTS).document(chatChannel.postId).get()
								.addOnSuccessListener { postDoc ->
									if (postDoc != null && postDoc.exists()) {
										val post = postDoc.toObject(Post::class.java)!!
										scope.launch (Dispatchers.Default) {
											val posts = filterPosts(listOf(post), user)
											postDao.insertItems(posts)
										}
									}
								}.addOnFailureListener { e ->
									_networkErrors.postValue(e)
								}

						}
					}.addOnFailureListener {
						_networkErrors.postValue(it)
					}


			}*/

	}

	override fun sendMessage(
		message: SimpleMessage,
		chatChannel: ChatChannel
	) {
		val currentUser = currentLocalUser.value!!
		message.senderId = currentUser.id
		message.sender = currentUser

		val messagesRef = if (message.messageId.isEmpty()) {
			val ref =
				db.collection(CHAT_CHANNELS).document(message.chatChannelId).collection(MESSAGES)
					.document()
			message.messageId = ref.id
			ref
		} else {
			db.collection(CHAT_CHANNELS).document(message.chatChannelId).collection(MESSAGES)
				.document(message.messageId)
		}

		db.runBatch {
			it.set(messagesRef, message)
			val map = mapOf(
				LAST_MESSAGE to message,
				UPDATED_AT to System.currentTimeMillis()
			)
			it.update(db.collection(CHAT_CHANNELS).document(message.chatChannelId), map)
		}.addOnSuccessListener {
			scope.launch(Dispatchers.IO) {
				chatChannel.lastMessage = message
				messageDao.insert(message)
				chatChannel.updatedAt = System.currentTimeMillis()
				chatChannelDao.updateItem(chatChannel)
			}
		}.addOnFailureListener {
			Log.e(BUG_TAG, errorPrefix + "send a message." + errorSuffix + it.localizedMessage!!)
		}
	}

	override fun uploadMessageMedia(message: SimpleMessage, chatChannel: ChatChannel) {
		val messageRef = db.collection(CHAT_CHANNELS)
			.document(message.chatChannelId)
			.collection(MESSAGES)
			.document()

		val messageId = messageRef.id
		message.messageId = messageId

		val storageRef = storage.reference

		val randomName: String
		var mediaRef: StorageReference? = null

		if (message.type == IMAGE) {
			randomName = "Image_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(message.createdAt)
			mediaRef = storageRef.child("${message.chatChannelId}/images/messages/$randomName.jpg")
		} else if (message.type == DOCUMENT) {
			randomName = "Document_" + SimpleDateFormat(
				"yyyyMMdd_HHmmss",
				Locale.UK
			).format(message.createdAt)
			mediaRef = storage
				.reference
				.child("${message.chatChannelId}/documents/messages/${randomName}_${message.metaData?.originalFileName}")
		}

		scope.launch(Dispatchers.IO) {
			chatChannel.lastMessage = message
			chatChannel.updatedAt = System.currentTimeMillis()
			messageDao.insert(message)
			chatChannelDao.updateItem(chatChannel)
		}

		mediaRef?.let { ref ->
			ref.putFile(message.content.toUri())
				.addOnSuccessListener {
					ref.downloadUrl
						.addOnSuccessListener { downloadUri ->
							message.content = downloadUri.toString()

							sendMessage(message, chatChannel)
						}
						.addOnFailureListener {
							mediaUploadResult.postValue(Result.Error(it))
						}
				}
				.addOnFailureListener {
					Log.e(BUG_TAG, errorPrefix + "upload a message media." + errorSuffix + it.localizedMessage!!)
				}
		}

	}

	override fun downloadMedia(externalDir: File?, message: SimpleMessage) {

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
			objectRef.getFile(file).addOnSuccessListener {
				scope.launch(Dispatchers.IO) {
					message.metaData?.originalFileName = name
					message.isDownloaded = true
					messageDao.updateItem(message)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get file from server." + errorSuffix + it.localizedMessage!!)
			}
		} else {
			Log.e(BUG_TAG, errorPrefix + "download a file." + errorSuffix + "Probably file already exists.")
		}
	}

	override fun getContributorsForPost(channelId: String) {
		db.collection(CHAT_CHANNELS).document(channelId).collection(CONTRIBUTORS).limit(10).get()
			.addOnSuccessListener {
				if (it != null && !it.isEmpty) {
					val chatChannelContributors = it.toObjects(ChatChannelContributor::class.java)
					for (contributor in chatChannelContributors) {
						/*val channelIdObject = ChannelIds(chatChannelId = channelId, userId = contributor.id)
						val existingList = contributor.channelIds.toMutableList()
						existingList.add(channelIdObject)
						contributor.channelIds = existingList

						scope.launch (Dispatchers.IO) {
							chatChannelDao.insertChannelIds(existingList)
						}*/
						contributor.channelId = channelId
					}
					scope.launch(Dispatchers.IO) {
						chatChannelContributorDao.insertContributors(chatChannelContributors)
					}
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get contributors for a post." + errorSuffix + it.localizedMessage!!)
			}
	}

	override fun updateGuidelines(postId: String, guidelines: String) {
		val map = mapOf("guidelines" to guidelines)

		db.collection(POSTS).document(postId)
			.update(map)
			.addOnSuccessListener {

			}.addOnFailureListener {

			}
	}

	fun getChannelContributors(chatChannelId: String) {
		db.collection(CHAT_CHANNELS).document(chatChannelId).collection(USERS)
			.get()
			.addOnSuccessListener {
				val contributors = it.toObjects(User::class.java)
				for (contributor in contributors) {
					contributor.userPrivate.chatChannels = listOf(chatChannelId)
				}
				scope.launch (Dispatchers.IO) {
					insertUsersWithFilter(contributors, null)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get chat channel contributors." + errorSuffix + it.localizedMessage!!)
			}
	}

	override fun getChannelContributors(
		chatChannelId: String,
		pageSize: Long,
		extra: DocumentSnapshot?,
		ahead: Boolean,
		onComplete: (contributors: QuerySnapshot) -> Unit
	) {
		val query = db.collection(CHAT_CHANNELS).document(chatChannelId).collection(CONTRIBUTORS)
			.orderBy(NAME, Query.Direction.ASCENDING)

		if (ahead) {
			query.endAt(extra)
		} else {
			query.startAfter(extra)
		}

		query.limit(pageSize)
			.get()
			.addOnSuccessListener {
				if (it != null && !it.isEmpty) {
					val chatChannelContributors = it.toObjects(ChatChannelContributor::class.java)
					for (contributor in chatChannelContributors) {
						/*val channelIdObject = ChannelIds(chatChannelId = chatChannelId, userId = contributor.id)
						val existingList = contributor.channelIds.toMutableList()
						existingList.add(channelIdObject)
						contributor.channelIds = existingList

						scope.launch (Dispatchers.IO) {
							chatChannelDao.insertChannelIds(existingList)
						}*/
						contributor.channelId = chatChannelId

					}
					scope.launch(Dispatchers.IO) {
						chatChannelContributorDao.insertContributors(chatChannelContributors)
					}
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get channel contributors." + errorSuffix + it.localizedMessage!!)
			}
	}

	override fun getContributorSnapshot(
		channelId: String,
		id: String,
		onComplete: (doc: DocumentSnapshot) -> Unit
	) {
		db.collection(CHAT_CHANNELS).document(channelId).collection(CONTRIBUTORS).document(id)
			.get()
			.addOnSuccessListener {
				if (it != null && it.exists()) {
					onComplete(it)
				}
			}
	}

	override fun getMedia(
		chatChannelId: String,
		messageId: String,
		onComplete: (simpleMedia: SimpleMedia) -> Unit
	) {
		db.collection(CHAT_CHANNELS).document(chatChannelId).collection(MEDIA).document(messageId)
			.get()
			.addOnSuccessListener {
				if (it != null && it.exists()) {
					val simpleMedia = it.toObject(SimpleMedia::class.java)!!
					onComplete(simpleMedia)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get media. wtf" + errorSuffix + it.localizedMessage!!)
			}
	}

	override suspend fun updateChatChannels(channels: List<ChatChannel>) {
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
					Log.e(TAG, "While updating chat channel from notification, there was no user corresponding to the last message of this chat channel")
				}
			}
		}

		chatChannelDao.updateItems(channels)
	}

	private inline fun <reified T: Any> addDocumentListener(documentReference: DocumentReference) {
		documentReference.addSnapshotListener { value, error ->
			if (error != null) {
				Log.e(TAG, error.localizedMessage!!)
				return@addSnapshotListener
			}

			if (value != null && value.exists()) {
				val obj = value.toObject(T::class.java)!!
				onDocumentUpdate(obj)
			}
		}
	}

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

	private fun <T: Any> onDocumentUpdate(obj: T) {
		when (obj) {
			is User -> {
				val user = _currentLocalUser.value
				if (user != null) {
					val private = user.userPrivate
					obj.userPrivate = private
					_currentLocalUser.postValue(obj)

					scope.launch(Dispatchers.IO) {
						setUpChannels(user)
						_currentLocalUser.postValue(user)
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
						_currentLocalUser.postValue(user)
						user.isCurrentUser = true
						userDao.insertCurrentUser(user)
					}
				}
			}
		}
	}

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

	override fun setMessagesListener(file: File, chatChannel: ChatChannel, contributors: List<User>) {
		db.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId).collection(MESSAGES)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING)
			.limit(30)
			.addSnapshotListener { value, error ->
				if (error != null) {
					Log.e(BUG_TAG, errorPrefix + "set listener for messages." + errorSuffix + error.localizedMessage!!)
					return@addSnapshotListener
				}

				if (value != null && !value.isEmpty) {
					val messages = value.toObjects(SimpleMessage::class.java)
					val extras = mapOf(CONTRIBUTORS to contributors, CHAT_CHANNEL to chatChannel, "FILE" to file)

					insertMessagesWithFilter(messages, extras)

				}
			}
	}

	override fun onNewMessagesFromBackground(
		chatChannelId: String,
		onComplete: (chatChannel: ChatChannel) -> Unit
	) {
		getObject<ChatChannel>(db.collection(CHAT_CHANNELS).document(chatChannelId)) {
			scope.launch(Dispatchers.IO) {
				updateChatChannels(listOf(it))
			}

			onComplete(it)
		}
	}

	override fun getPopularInterests(onComplete: (interests: List<PopularInterest>) -> Unit) {
		/*db.collection(POPULAR_INTERESTS).limit(10)
			.get()
			.addOnSuccessListener {
				val interests = it.toObjects(PopularInterest::class.java)
				onComplete(interests)
			}.addOnFailureListener {
				_networkErrors.postValue(it)
			}*/
	}

	private fun getAllSubStrings(str: String): List<String> {
		// also needs splitting
		val list = mutableListOf<String>()
		for (i in 1 until str.length) {
			list.add(str.substring(0..i))
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

	private suspend fun insertUsersWithFilter(users: List<User>, extras: Map<String, Any?>?) {
		val returnedUsers = filterUsers(users, extras)
		for (user in returnedUsers) {
			val u = userDao.getUser(user.id)
			if (u != null) {
				userDao.updateItem(user)
			} else {
				userDao.insert(user)
			}
		}
	}

	private suspend fun filterUsers(users: List<User>, extras: Map<String, Any?>? = null): List<User> {
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
				}

				if (followingTag != null && followingTag) {
					for (user in users) {
						val existingList = user.userPrivate.followers.toMutableList()
						existingList.add(otherUser.id)
						user.userPrivate.followers = existingList
					}
				}
			}

			val post = extras[POST] as Post?
			if (post != null && post.type == PROJECT) {
				for (user in users) {
					val existingList = user.userPrivate.chatChannels.toMutableList()
					existingList.add(post.chatChannelId!!)
					user.userPrivate.chatChannels = existingList
				}
			}

			val chatChannel = extras[CHAT_CHANNEL] as ChatChannel?
			if (chatChannel != null) {
				for (user in users) {
					val existingList = user.userPrivate.chatChannels.toMutableList()
					existingList.add(chatChannel.chatChannelId)
					user.userPrivate.chatChannels = existingList
				}
				chatChannel.lastMessage?.sender = users.find {
					it.id == chatChannel.lastMessage?.senderId
				} ?: throw Exception("${chatChannel.lastMessage?.senderId} is not valid")

				chatChannelDao.insert(chatChannel)
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
		return users
	}


	@Suppress("UNCHECKED_CAST")
	private fun insertMessagesWithFilter(messages: List<SimpleMessage>, extras: Map<String, Any?>?) {

		if (extras != null) {
			val contributors = extras[CONTRIBUTORS] as List<User>
			val chatChannel = extras[CHAT_CHANNEL] as ChatChannel?
			val file = extras["FILE"] as File?

			for (message in messages) {
				val contributor = contributors.find {
					it.id == message.senderId
				}
				if (contributor != null) {
					message.sender = contributor
				}
			}

			if (chatChannel != null) {
				val lastMessage = messages.first()
				chatChannel.lastMessage = lastMessage

				if (file != null) {
					for (message in messages) {
						if (message.type != TEXT) {
							val f = File(file, message.metaData?.originalFileName!!)
							message.isDownloaded = f.exists()
						}
					}
				}

				scope.launch (Dispatchers.IO) {
					messageDao.insertMessages(messages)
					chatChannelDao.updateItem(chatChannel)
				}

			}
		}
	}


	fun <T> getItems(
		limit: Long,
		query: Query,
		clazz: Class<T>,
		extras: Map<String, Any?>?,
		onComplete: (firstSnapshot: DocumentSnapshot?, lastSnapshot: DocumentSnapshot?, isEnd: Boolean) -> Unit
	) {
		Log.d(BUG_TAG, "Getting items ... of type ${clazz.canonicalName}")
		query.get()
			.addOnSuccessListener {
				Log.d(BUG_TAG, "Success ... ${it.size()}")
				scope.launch(Dispatchers.IO) {
					when (clazz) {
						User::class.java -> {
							val items = it.toObjects(clazz) as List<User>
							insertUsersWithFilter(items, extras)
						}
						Post::class.java -> {
							val items = it.toObjects(clazz) as List<Post>
							insertPosts(items, extras)
						}
						SimpleMessage::class.java -> {
							val items = it.toObjects(clazz) as List<SimpleMessage>
							insertMessagesWithFilter(items, extras)
						}
						UserMinimal::class.java -> {
							val items = it.toObjects(clazz) as List<UserMinimal>
							userMinimalDao.insertItems(items)
						}
						ChatChannel::class.java -> {
							val items = it.toObjects(clazz) as List<ChatChannel>
							chatChannelDao.insertItems(items)
						}
						SimpleRequest::class.java -> {
							val items = it.toObjects(clazz) as List<SimpleRequest>
							requestDao.insertItems(items)
						}
						SimpleNotification::class.java -> {
							val items = it.toObjects(clazz) as List<SimpleNotification>
							notificationDao.insertItems(items)
						}
					}
				}
				onComplete(it.firstOrNull(), it.lastOrNull(), it.size() <= limit)
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get ${clazz::getCanonicalName} items for lists." + errorSuffix + it.localizedMessage!!)
			}
	}

	private suspend fun filterPosts(user: User) {
		val posts = postDao.allPosts()
		if (!posts.isNullOrEmpty()) {
			val aps = mutableListOf<Post>()
			for (post in posts) {
				val ps = filterPosts(listOf(post), user)
				aps.addAll(ps)
			}
			postDao.updateItems(aps)
		}
	}

	private fun filterPosts(posts: List<Post>, user: User?, extras: Map<String, Any?>? = null): List<Post> {
		val blogItems = mutableListOf<BlogItem>()

		val inFeed = extras?.get(HOME_FEED) as Boolean?
		val withTag = extras?.get(WITH_TAG) as String?

		if (user != null) {
			val userDetails = user.userPrivate
			val interests = userDetails.interests.toSet()
			val finalSet = mutableSetOf<String>()
			for (post in posts) {

				post.postLocalData.isCreator = userDetails.projectIds.contains(post.id) || userDetails.blogIds.contains(post.id)
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
				scope.launch (Dispatchers.IO) {
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
				scope.launch (Dispatchers.IO) {
					simpleTagsDao.insertItems(tags)
				}
			}
		}

		return posts
	}

	private suspend fun insertPosts(posts: List<Post>, extras: Map<String, Any?>? = null): List<Post> {
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
	}

	fun <T> getSnapshot(item: T, clazz: Class<T>, onComplete: (doc: DocumentSnapshot) -> Unit) {
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
						Log.e(BUG_TAG, errorPrefix + "get a snapshot." + errorSuffix + it.localizedMessage!!)
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
						Log.e(BUG_TAG, errorPrefix + "get messages for a channel." + errorSuffix + it.localizedMessage!!)
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
						Log.e(BUG_TAG, errorPrefix + "get userMinimal. wtf" + errorSuffix + it.localizedMessage!!)
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
						Log.e(BUG_TAG, errorPrefix + "get user." + errorSuffix + it.localizedMessage!!)
					}
			}
		}
	}

	fun <T> getObject(
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
				Log.e(BUG_TAG, errorPrefix + "get an object of type ${clazz::getCanonicalName}." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun <T : Any> getObject(
		objectId: String,
		objectClass: Class<T>,
		extra: Map<String, String>? = null,
		onComplete: (item: T?) -> Unit
	) {
		val ref = when (objectClass) {
			User::class.java -> {
				Log.d(TAG, "getting user object ... ")
				db.collection(USERS)
			}
			UserMinimal::class.java -> {
				db.collection(USER_MINIMALS)
			}
			Post::class.java -> {
				db.collection(POSTS)
			}
			SimpleMessage::class.java -> {
				if (extra != null) {
					val chatChannelId = extra["chatChannelId"] as String
					db.collection(CHAT_CHANNELS).document(chatChannelId).collection(MESSAGES)
				} else {
					null
				}
			}
			ChatChannel::class.java -> {
				db.collection(CHAT_CHANNELS)
			}
			else -> {
				null
			}
		}


		Log.d(TAG, "FirebaseUtility")

		ref?.let {
			ref.document(objectId).get()
				.addOnSuccessListener {
					Log.d(TAG, "Success")

					val obj = it.toObject(objectClass)
					onComplete(obj)
				}.addOnFailureListener {
					Log.e(BUG_TAG, errorPrefix + "get object of unknown type." + errorSuffix + it.localizedMessage!!)
				}
		}

		if (ref == null) {
			Log.e(BUG_TAG, errorPrefix + "create a reference." + errorSuffix + "Probably ref is null.")
		}

	}

	/*fun <T> getObject(reference: DocumentReference, clazz: Class<T>, onComplete: (obj: T) -> Unit) {

	}*/

	inline fun <reified T: Any> getObject(reference: DocumentReference, crossinline onComplete: (obj: T) -> Unit) {
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
	}

	private inline fun <reified T: Any> getObjects(query: Query, crossinline onComplete: (obz: List<T>) -> Unit, crossinline onError: (err: Exception) -> Unit) {
		query.get().addOnSuccessListener {
			if (!it.isEmpty) {
				onComplete(it.toObjects(T::class.java))
			} else {
				onError(Exception("Collection is empty : $query"))
			}
		}.addOnFailureListener {
			onError(it)
		}
	}

	fun getItemsWithoutCaching(query: Query, onComplete: (querySnapshot: QuerySnapshot) -> Unit) {
		query.get()
			.addOnSuccessListener {
				onComplete(it)
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get items without caching. wtf" + errorSuffix + it.localizedMessage!!)
			}
	}

	fun <T: Any> getSnapshot(docId: String, clazz: Class<T>): Result<DocumentSnapshot> {
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

	fun getSnapshot(documentReference: DocumentReference, onComplete: (doc: DocumentSnapshot) -> Unit) {
		documentReference.get()
			.addOnSuccessListener {
				if (it != null && it.exists()) {
					onComplete(it)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get snapshot. wtf." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun getChatChannelsFromFirebase() {

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
							scope.launch (Dispatchers.IO) {
								insertUsersWithFilter(contributors, mapOf(CHAT_CHANNEL to channel))
							}
						}.addOnFailureListener {
							Log.e(ChatChannelFragment.TAG, it.localizedMessage!!.toString())
						}
				}

			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get chat channels based on contributors." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun getSavedPosts(initialItemPosition: Int, finalItemPosition: Int) {
		val currentUser = currentLocalUser.value!!
		val savedPostIds = currentUser.userPrivate.savedPosts

		if (savedPostIds.isEmpty()) {
			return
		}

		fun downloadSavedPosts(batchCount: Int, initialPos: Int) {
			var initial = initialPos
			var final = minOf(initial + 10, savedPostIds.size)

			Log.d(BUG_TAG, "Downloading saved posts ... ")

			for (j in 0 until batchCount) {

				db.collection(POSTS)
					.whereArrayContainsAny(ID, savedPostIds.subList(initial, final))
					.get()
					.addOnSuccessListener {
						val posts = it.toObjects(Post::class.java)
						scope.launch {
							insertPosts(posts)
						}
					}.addOnFailureListener {
						Log.e(BUG_TAG, errorPrefix + "download saved posts." + errorSuffix + it.localizedMessage!!)
					}

				if (final - initial >= 10) {
					initial = final
					final = minOf(initial + 10, savedPostIds.size)
				}

			}
		}

		// TODO("Notify to the fragment, when it's the end of the list")
		when {
			savedPostIds.size <= (finalItemPosition - initialItemPosition)/2 -> {
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
		}
	}

	fun getNotificationFromFirebase(notificationId: String) {
		val currentUser = currentLocalUser.value!!
		db.collection(USERS).document(currentUser.id)
			.collection(NOTIFICATIONS)
			.document(notificationId)
			.get()
			.addOnSuccessListener {
				if (it != null && it.exists()) {
					val notification = it.toObject(SimpleNotification::class.java)!!
					scope.launch (Dispatchers.IO) {
						notificationDao.insert(notification)
					}
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get notifications." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun getChatChannelFromFirebase(chatChannelId: String) {

		val chatChannelRef = db.collection(CHAT_CHANNELS).document(chatChannelId)

		chatChannelRef.get()
			.addOnSuccessListener {
				if (it != null && it.exists()) {
					val chatChannel = it.toObject(ChatChannel::class.java)!!

					chatChannelRef.collection(USERS)
						.get()
						.addOnSuccessListener { qs ->
							val contributors = qs.toObjects(User::class.java)
							scope.launch (Dispatchers.IO) {
								insertUsersWithFilter(contributors, mapOf(CHAT_CHANNEL to chatChannel))
							}
						}.addOnFailureListener { exc ->
							Log.e(BUG_TAG, errorPrefix + "get single chat channel contributors" + errorSuffix + exc.localizedMessage!!)
						}

				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get single chat channel." + errorSuffix + it.localizedMessage!!)
			}

	}

	fun setNotificationListener() {
		val currentUser = currentLocalUser.value!!
		db.collection(USERS).document(currentUser.id)
			.collection(NOTIFICATIONS)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING)
			.limit(20)
			.addSnapshotListener { value, error ->
				if (error != null) {
					Log.e(BUG_TAG, errorPrefix + "set listener for notifications." + errorSuffix + error.localizedMessage!!)
				}

				if (value != null && !value.isEmpty) {
					val notifications = value.toObjects(SimpleNotification::class.java)
					scope.launch (Dispatchers.IO) {
						notificationDao.insertItems(notifications)
					}
				}

			}

	}

	fun getNotifications() {
		val currentUser = currentLocalUser.value!!
		db.collection(USERS).document(currentUser.id)
			.collection(NOTIFICATIONS)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING)
			.limit(20)
			.get()
			.addOnSuccessListener {
				val notifications = it.toObjects(SimpleNotification::class.java)
				scope.launch (Dispatchers.IO) {
					notificationDao.insertItems(notifications)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get notifications." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun getMyRequests() {
		val currentUser = currentLocalUser.value!!
		db.collection(USERS).document(currentUser.id).collection(REQUESTS)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING)
			.limit(20)
			.get()
			.addOnSuccessListener {
				val requests = it.toObjects(SimpleRequest::class.java)
				scope.launch (Dispatchers.IO) {
					requestDao.insertItems(requests)
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "get post requests." + errorSuffix + it.localizedMessage!!)
			}
	}

	fun updateOtherUser(otherUser: User, map: Map<String, Any?>) {
		db.collection(USERS).document(otherUser.id)
			.update(map)
			.addOnSuccessListener {
				Log.d(BUG_TAG, "Updated other user -> ${otherUser.name}")
				scope.launch (Dispatchers.IO) {
					if (userDao.getUser(otherUser.id) != null) {
						userDao.updateItem(otherUser)
					} else {
						userDao.insert(otherUser)
					}
				}
			}.addOnFailureListener {
				Log.e(BUG_TAG, errorPrefix + "updating other user doc." + errorSuffix + it.localizedMessage!!)
			}
	}

	suspend fun getItemsFromFirebase(
		endBefore: DocumentSnapshot? = null,
		startAfter: DocumentSnapshot? = null,
		query: Query,
		pageSize: Int,
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
							currentUser.userPrivate.followings.size)
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
	}

	fun fetchItems(
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
	}

	suspend fun getProjectContributors(post: Post): Result<QuerySnapshot> {
		return if (post.chatChannelId != null) {
			try {
				val task = db.collection(CHAT_CHANNELS).document(post.chatChannelId!!).collection(
					USERS).limit(10).get()
				Result.Success(task.await())
			} catch (e: Exception) {
				Result.Error(e)
			}
		} else {
			Result.Error(Exception("Post is not a project."))
		}
	}

	suspend fun getRandomTopUsers(): Result<QuerySnapshot>{
		return return try {
			val task = db.collection(USERS).orderBy(SEARCH_RANK, Query.Direction.DESCENDING)
				.limit(15)
				.get()

			val result = task.await()
			Result.Success(result)
		} catch (e: Exception) {
			Result.Error(e)
		}
	}

	companion object {
		const val ZERO: Long = 0
		const val MESSAGE_PAGE_SIZE: Long = 20
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

}