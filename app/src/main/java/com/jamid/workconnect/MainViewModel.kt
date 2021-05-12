package com.jamid.workconnect

import android.app.Application
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.*
import androidx.paging.*
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.auth.SignInFormResult
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.data.WorkConnectDatabase
import com.jamid.workconnect.home.BlogItem
import com.jamid.workconnect.message.ChatChannelFragment
import com.jamid.workconnect.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min

class MainViewModel(application: Application) : AndroidViewModel(application) {

	/* private val auth = Firebase.auth
	 private val db = Firebase.firestore
	 private val storage = Firebase.storage*/

	val repo: MainRepository
	private val database: WorkConnectDatabase =
		WorkConnectDatabase.getInstance(application.applicationContext, viewModelScope)
	init {
		repo = MainRepository(viewModelScope, database)
	}

	val declineProjectResult: LiveData<Result<SimpleNotification>> = repo.declineProjectResult
	val acceptProjectResult: LiveData<Result<SimpleNotification>> = repo.acceptProjectResult
	val undoProjectResult: LiveData<Result<SimpleRequest>> = repo.undoProjectResult

	private val _currentImageUri = MutableLiveData<Uri>()
	val currentImageUri: LiveData<Uri> = _currentImageUri

	private val _currentDocUri = MutableLiveData<Uri>()
	val currentDocUri: LiveData<Uri> = _currentDocUri

	private val _currentCroppedImageUri = MutableLiveData<Uri>()
	val currentCroppedImageUri: LiveData<Uri> = _currentCroppedImageUri

	private val _primaryBottomSheetState = MutableLiveData<Int>().apply { value = null }
	val primaryBottomSheetState: LiveData<Int> = _primaryBottomSheetState

	private val _fragmentTransactionListener = MutableLiveData<Int>().apply { value = null }
	val fragmentTransactionListener: LiveData<Int> = _fragmentTransactionListener

	fun onFragmentTransactionComplete(backStackEntryCount: Int) {
		_fragmentTransactionListener.postValue(backStackEntryCount)
	}

	fun setPrimaryBottomSheetState(sheetState: Int? = null) {
		_primaryBottomSheetState.postValue(sheetState)
	}

	val fragmentTagStack = Stack<String>()

	private val _currentFragmentTag = MutableLiveData<String>().apply { value = null }
	val currentFragmentTag: LiveData<String> = _currentFragmentTag

	fun setCurrentFragmentTag(tag: String) {
		_currentFragmentTag.postValue(tag)
	}

	private val _currentPrimaryFragmentId = MutableLiveData<Int>()

	private val _firebaseUser = MutableLiveData<FirebaseUser>()

	val contentInset = MutableLiveData<Pair<Int, Int>>().apply { value = null }

	private val _networkErrors = MutableLiveData<Exception>()
	val networkErrors: LiveData<Exception> = _networkErrors

	private val _miniUser = MutableLiveData<UserMinimal>()
	val miniUser: LiveData<UserMinimal> = _miniUser

	fun setUserMinimal(userMinimal: UserMinimal?) {
		_miniUser.postValue(userMinimal)
	}

	val windowInsets = MutableLiveData<Pair<Int, Int>>().apply {
		value = Pair(0, 0)
	}

	var extras = mutableMapOf<String, Any?>()

	val chatChannelsLiveData: LiveData<List<ChatChannel>> = repo.chatChannelsLiveData

	private val _user = MutableLiveData<User>().apply {
		value = null
	}
	val user: LiveData<User> = repo.currentLocalUser
	var uid = repo.uid

	val firebaseErrors = repo.networkErrors

	val firebaseUser = repo.currentFirebaseUser
	val signInResult: LiveData<Result<FirebaseUser>> = repo.signInResult
	val registerResult: LiveData<Result<FirebaseUser>> = repo.registerResult
	val emailExists: LiveData<Boolean> = repo.emailExists


	fun sendRegistrationTokenToServer(token: String) {
		repo.updateRegistrationToken(token)
	}

	val config = PagedList.Config.Builder()
		.setEnablePlaceholders(false)
		.setPageSize(50)
		.setPrefetchDistance(10)
		.build()

	private val currentChatChannelId = MutableLiveData<String>().apply {
		value = ""
	}


	val contributorConfig = PagedList.Config.Builder()
		.setEnablePlaceholders(false)
		.setPageSize(10)
		.setPrefetchDistance(3)
		.build()


	fun pagedContributors(chatChannelId: String) = database.chatChannelContributorDao()
		.getPagedContributors(chatChannelId)
		.toLiveData(contributorConfig, null, ContributorsBoundaryCallback(chatChannelId, repo))


	fun uploadUser(tags: List<String>? = null) {
		repo.uploadUser(tags)
	}

	/*fun createNewUser(interests: List<String>) {
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
					User(uid, name!!, newUsername, email, null, photo, interests, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis(), emptyList())
				} else {
					User(uid, name!!, username, email, null, photo, interests, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis(), emptyList())
				}
			} else {
				User(uid, name!!, username, email, null, photo, interests, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis(), emptyList())
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
	}*/


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
				val asc = ch.toInt()
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
				val asc = ch.toInt()
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
				val asc = ch.toInt()
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
		/*if (email.isValidEmail()) {
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

		}*/
	}

	val userNameExists: LiveData<Result<Boolean>> = repo.usernameExists

	fun checkIfUsernameExists(username: String) {
		repo.checkIfUsernameExists(username)
		/*val user = user.value ?: return
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
			}*/
	}

	fun signIn(email: String, password: String) {
		repo.signIn(email, password)
	}

	fun register(email: String, password: String) {
		repo.register(email, password)
	}

	private val _firebaseUserUpdateResult = MutableLiveData<Result<FirebaseUser>>()
	val firebaseUserUpdateResult: LiveData<Result<FirebaseUser>> = repo.firebaseUserUpdateResult

	fun updateFirebaseUser(downloadUrl: Uri? = null, fullName: String? = null) {
		val map = mutableMapOf<String, Any?>(
			"photoUri" to downloadUrl,
			"fullName" to fullName
		)
		repo.updateFirebaseUser(map)
		/*val currentUser = firebaseUser.value
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
		}*/
	}

	val profilePhotoUploadResult: LiveData<Uri?> = repo.profilePhotoUpload

	fun setProfilePhotoUploadResult(s: String?) {
		repo.setProfilePhotoUploadResult(s)
	}

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

	private val _blogFragmentData = MutableLiveData<ArrayList<BlogItem>>()
	val blogFragmentData: LiveData<ArrayList<BlogItem>> = _blogFragmentData

	fun setBlogFragmentData(blogItemsList: ArrayList<BlogItem>?) {
		_blogFragmentData.postValue(blogItemsList)
	}

	val postPhotoUploadResult: LiveData<Result<Uri>> = repo.postPhotoUpload

	fun uploadPostImage(image: Uri?, type: String) {
		if (image != null) {
			/*val stream = FileInputStream(File(path))
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
			}*/
			repo.uploadPostImage(image, type)
		}
	}

	// the state before changes
	fun onLikePressed(post: Post): Post {
		return repo.onLikePressed(post)
	}

	fun onDislikePressed(post: Post): Post {
		return repo.onDislikePressed(post)
	}

	fun onSavePressed(post: Post): Post {
		return repo.onSaved(post)
	}

	fun onFollowPressed(post: Post): Post {
		return repo.onFollowPressed(post)
	}

	fun onFollowPressed(currentUser: User, otherUser: User) {
		repo.onFollowPressed(currentUser, otherUser)
	}

	fun signOut() {
		repo.signOut()
	}

	val postUploadResult: LiveData<Result<Post>> = repo.postUpload

	fun uploadPost(post: Post) {
		post.location = currentLocation.value
		repo.uploadPost(post)
	}


	fun setSignInResult(result: Result<FirebaseUser>?) {
//        _signInResult.postValue(result)
	}

	fun setRegisterResult(result: Result<FirebaseUser>?) {
//        _registerResult.postValue(result)
	}

	fun setCurrentError(e: Exception?) {
		_networkErrors.postValue(e)
	}

	//    private val _requestSentResult = MutableLiveData<Result<String>>()
	val requestSentResult: LiveData<Result<String>> = repo.requestSent


	/*
	* PROCEDURE FOR REQUESTING FOR PROJECT
	*
	* 1. Add new request to the post request collection
	* 2. Add new notification request in post-user's notification collection
	* 3. Update activeRequests in current user document
	* */
	fun joinProject(post: Post) {
		/*val requestRef = db.collection(POSTS).document(post.id).collection(REQUESTS).document()
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
		}*/
		repo.joinProject(post)
	}

	fun clearProjectFragmentResults() = viewModelScope.launch (Dispatchers.Default) {
		repo.clearRequestResult()
	}

	val updateUserResult: LiveData<Result<Map<String, Any?>>> = repo.updateUser

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
	fun updateUser(
		fullName: String,
		username: String,
		about: String,
		interests: MutableList<String>,
		imageUri: String?
	) {
		/*val userMap = mapOf(
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
		}*/

	}

	fun updateUser(userMap: Map<String, Any?>) {
		repo.updateUser(userMap)
	}

	fun clearEditChanges() {
		_currentImageUri.postValue(null)
		_currentCroppedImageUri.postValue(null)
		repo.clearEditChanges()
	}

	private val _deletePostResult = MutableLiveData<Result<Void>>()
	val deletePostResult: LiveData<Result<Void>> = _deletePostResult

	fun deletePost(post: Post) {
		/*TODO("Don't delete, It's not complete")
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
		}*/

	}

	fun sendMessage(
		message: SimpleMessage,
		chatChannel: ChatChannel,
		simpleMedia: SimpleMedia? = null
	) = viewModelScope.launch(Dispatchers.IO) {
		repo.sendMessage(message, chatChannel)
		/*val currentUser = user.value!!

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
		}*/
	}

	// first: messageId, second: downloadUrl
	val mediaUploadResult: LiveData<Result<SimpleMessage>> = repo.mediaUploadResult

	fun uploadMessageMedia(message: SimpleMessage, chatChannel: ChatChannel) = viewModelScope.launch(Dispatchers.IO) {
		/*val currentUser = auth.currentUser
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
		}*/
		repo.uploadMessageMedia(message, chatChannel)
	}

	fun setImageMessageUploadResult(result: Result<String>?) {
//        _imgMessageUploadResult.postValue(result)
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
		repo.updateMessage(messages)
	}

	fun clearMessages() = viewModelScope.launch(Dispatchers.IO) {
		database.messageDao().clearMessages()
	}


	fun insertChatChannelContributors(user: User) = viewModelScope.launch(Dispatchers.IO) {
//        repo.clearChatChannelContributorsAndChannelIds()
//        val chatChannelIds = user.chatChannels
//        for (channel in chatChannelIds) {
//            Log.d(TAG, channel)
//            db.collection(CHAT_CHANNELS).document(channel).collection(CONTRIBUTORS)
//                .get()
//                .addOnSuccessListener {
//                    if (it != null && !it.isEmpty) {
//                        val contributors = it.toObjects(ChatChannelContributor::class.java)
//                        for (contributor in contributors) {
//                            val channelIdObject = ChannelIds(chatChannelId = channel, userId = contributor.id)
//                            val existingList = contributor.channelIds.toMutableList()
//                            existingList.add(channelIdObject)
//                            contributor.channelIds = existingList
//                        }
//                        insertContributors(contributors)
//                    }
//                }
//        }
	}

	fun channelContributors(chatChannelId: String) = repo.channelContributors(chatChannelId)

	fun channelContributorsLive(channelId: String) = repo.channelContributorsLive("%$channelId%")

	val guidelinesUpdateResult: LiveData<Post> = repo.guidelinesUpdateResult

	fun updateGuidelines(postId: String, guidelines: String) {
		repo.updateGuidelines(postId, guidelines)
	}


	fun clearGuidelinesUpdateResult() {
		repo.clearGuideUpdateResult()
	}

	fun setCurrentDoc(doc: Uri?) {
		_currentDocUri.postValue(doc)
	}

	private val _docUploadResult = MutableLiveData<Result<SimpleMessage>>().apply { value = null }
	val docUploadResult: LiveData<Result<SimpleMessage>> = _docUploadResult

	fun setDocUploadResult(res: Result<SimpleMessage>?) {
		_docUploadResult.postValue(res)
	}

	suspend fun checkIfFileDownloaded(messageId: String): SimpleMedia? {
		return repo.checkIfDownloaded(messageId)
	}

	fun insertSimpleMedia(simpleMedia: SimpleMedia?) = viewModelScope.launch(Dispatchers.IO) {
		repo.insertSimpleMedia(simpleMedia)
	}


	fun setChannelListener(file: File, chatChannel: ChatChannel, contributors: List<User>) {
		repo.setMessagesListener(file, chatChannel, contributors)
	}

	fun onNewMessageNotification(
		chatChannelId: String,
		onComplete: (chatChannel: ChatChannel) -> Unit
	) {
		repo.onNewMessageNotification(chatChannelId) {
			onComplete(it)
		}
	}

	/*fun getPopularInterests(onComplete: (interests: List<PopularInterest>) -> Unit) {
		repo.getPopularInterests {
			onComplete(it)
		}
	}*/

	suspend fun getCurrentUserAsContributor(chatChannelId: String) =
		repo.getCurrentUserAsContributor(uid!!, chatChannelId)

	fun getCachedPost(postId: String) = repo.getCachedPost(postId)

	fun updatePost(post: Post, guidelines: String) {
		repo.updatePost(post, guidelines)
	}

	/*fun fetchUserData(fUser: FirebaseUser) {
		repo.fetchUserData(fUser)
	}*/

	suspend fun getLocalMedia(messageId: String): SimpleMedia? {
		return repo.getLocalMedia(messageId)
	}

	fun downloadMedia(externalDir: File?, message: SimpleMessage) =
		viewModelScope.launch(Dispatchers.IO) {
			repo.downloadMedia(externalDir, message)
		}

	fun mediaLiveData(messageId: String) = repo.listenToDownloadProcess(messageId)

	fun getMedia(
		chatChannelId: String,
		messageId: String,
		onComplete: (simpleMedia: SimpleMedia) -> Unit
	) {
		repo.getMedia(chatChannelId, messageId) {
			onComplete(it)
		}
	}

	fun getContributorsForPost(channelId: String) {
		repo.getContributorsForPost(channelId)
	}

	fun getLimitedContributorsForPost(chatChannelId: String) {
		repo.getContributorsForPost(chatChannelId)
	}

	fun clearSignInChanges() {
		_signInFormResult.postValue(null)
		repo.clearSignInChanges()
	}

	fun getPost(postId: String) {
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


	private val postPagingConfig = PagedList.Config.Builder()
		.setEnablePlaceholders(false)
		.setPageSize(15)
		.setInitialLoadSizeHint(20)
		.setMaxSize(50)
		.setPrefetchDistance(5)
		.build()

	private val userPagingConfig = PagedList.Config.Builder()
		.setEnablePlaceholders(false)
		.setPageSize(10)
		.setInitialLoadSizeHint(15)
		.setMaxSize(20)
		.setPrefetchDistance(3)
		.build()

	private val notificationConfig = PagedList.Config.Builder()
		.setEnablePlaceholders(false)
		.setPageSize(10)
		.setMaxSize(20)
		.setInitialLoadSizeHint(15)
		.setPrefetchDistance(5)
		.build()

	private val chatChannelsConfig = PagedList.Config.Builder()
		.setEnablePlaceholders(false)
		.setPageSize(10)
		.setInitialLoadSizeHint(15)
		.setMaxSize(20)
		.setPrefetchDistance(5)
		.build()

	fun chatMessages(chatChannelId: String, contributors: List<User>) = database.messageDao()
		.getLiveChatMessages(chatChannelId)
		.toLiveData(
			config, null, GenericBoundaryCallback(
				50,
				Firebase.firestore
					.collection(CHAT_CHANNELS)
					.document(chatChannelId)
					.collection(MESSAGES)
					.orderBy(CREATED_AT, Query.Direction.DESCENDING),
				repo,
				SimpleMessage::class.java,
				mapOf(CONTRIBUTORS to contributors)
			)
		)


	fun chatChannels(uid: String) = database.chatChannelDao()
		.chatChannels()
		.toLiveData(
			chatChannelsConfig, null, GenericBoundaryCallback(
				10, Firebase.firestore.collection(
					CHAT_CHANNELS
				).whereArrayContains(CONTRIBUTORS_LIST, uid)
					.orderBy(UPDATED_AT, Query.Direction.DESCENDING), repo, ChatChannel::class.java
			)
		)

	suspend fun randomTopUsers(): Result<QuerySnapshot> {
		return repo.getRandomTopUsers()
	}

	fun randomUsers() = database.userDao()
		.randomUsers()
		.toLiveData(
			userPagingConfig, null, GenericBoundaryCallback(
				10, Firebase.firestore.collection(
					USERS
				).orderBy(SEARCH_RANK), repo, User::class.java
			)
		)

	val localTags: LiveData<List<String>> = repo.localTagsList

	fun getHomeFeedDataSourceFactory(tag: String? = null) : DataSource.Factory<Int, Post> {
		return if (tag != null) {
			database.postDao()
				.getPagedPostsWithTag("%$tag%")
		} else {
			database.postDao()
				.getPagedPosts()
		}
	}

	fun getQueryForPosts(tag: String? = null, interests: List<String>? = null, userIds: List<String>? = null): Query {

		// This method should be used in such a way that if one argument is present,
		// the other shouldn't be present
		// Both may not be present

		var query: Query = Firebase.firestore.collection(POSTS)

		query = if (tag != null) {
			query.whereArrayContains(TAGS, tag)
		} else if (!interests.isNullOrEmpty()) {
			query.whereArrayContainsAny(TAGS, interests.take(min(10, interests.size)))
		} else if (!userIds.isNullOrEmpty()) {
			Log.d(BUG_TAG, "Using user ids to fetch posts")
			query.whereArrayContainsAny(UID, userIds.take(min(10, userIds.size)))
		} else {
			query
		}

		return query.orderBy(CREATED_AT, Query.Direction.DESCENDING)

	}

	fun posts(extras: Map<String, Any?>? = null): LiveData<PagedList<Post>> {

		// dependencies
		val tag = currentHomeTag.value
		val user = user.value
		val postsRef = Firebase.firestore.collection(POSTS)

		val (query, dataSourceFactory) = when {
			tag != null -> {
				Log.d(BUG_TAG, "Getting posts with tag ... ")
				postsRef.whereArrayContains(TAGS, tag) to database.postDao().getPagedPostsWithTag("%$tag%")
			}
			user != null -> {
				val list = user.userPrivate.followings.subList(0,
					minOf(10, user.userPrivate.followings.size)
				)
				Log.d(BUG_TAG, "Getting posts from all the followed users ... $list")
				postsRef.whereIn(
					UID,
					list
				) to database.postDao().getPagedPostsFromFollowings()
			}
			else -> {
				Log.d(BUG_TAG, "Getting random posts... ")
				postsRef to database.postDao()
					.getPagedPosts()
			}
		}

		return dataSourceFactory.toLiveData(
			postPagingConfig,
			null,
			GenericBoundaryCallback(
				15,
				query.orderBy(CREATED_AT, Query.Direction.DESCENDING),
				repo,
				Post::class.java,
				extras)
		)
	}

	private val postPagingConfigAlt = PagedList.Config.Builder()
		.setEnablePlaceholders(true)
		.setPageSize(15)
		.setInitialLoadSizeHint(20)
		.setMaxSize(50)
		.setPrefetchDistance(5)
		.build()

	fun projects() = database.postDao()
		.getPagedProjects()
		.toLiveData(
			postPagingConfigAlt, null, GenericBoundaryCallback(
				15,
				Firebase.firestore.collection(
					POSTS
				).orderBy(CREATED_AT, Query.Direction.DESCENDING).whereEqualTo(TYPE, PROJECT),
				repo,
				Post::class.java
			)
		)

	fun blogs() = database.postDao()
		.getPagedBlogs()
		.toLiveData(
			postPagingConfigAlt, null, GenericBoundaryCallback(
				15,
				Firebase.firestore.collection(
					POSTS
				).orderBy(CREATED_AT, Query.Direction.DESCENDING).whereEqualTo(TYPE, BLOG),
				repo,
				Post::class.java
			)
		)

	fun userProjects(uid: String) = database.postDao()
		.getUserPagedProjects(uid)
		.toLiveData(
			postPagingConfigAlt, null, GenericBoundaryCallback(
				15, Firebase.firestore.collection(
					POSTS
				).orderBy(CREATED_AT, Query.Direction.DESCENDING).whereEqualTo(UID, uid)
					.whereEqualTo(TYPE, PROJECT), repo, Post::class.java
			)
		)

	fun userBlogs(uid: String) = database.postDao()
		.getUserPagedBlogs(uid)
		.toLiveData(
			postPagingConfig, null, GenericBoundaryCallback(
				15, Firebase.firestore.collection(
					POSTS
				).orderBy(CREATED_AT, Query.Direction.DESCENDING).whereEqualTo(UID, uid)
					.whereEqualTo(TYPE, BLOG), repo, Post::class.java
			)
		)

	fun userCollaborations(uid: String) = database.postDao()
		.getUserCollaborations("%$uid%")
		.toLiveData(
			postPagingConfig, null, GenericBoundaryCallback(
				15, Firebase.firestore
					.collection(POSTS)
					.orderBy(CREATED_AT, Query.Direction.DESCENDING)
					.whereArrayContains(CONTRIBUTORS, uid)
					.whereEqualTo(TYPE, PROJECT), repo, Post::class.java
			)
		)

	private fun followersQuery(uid: String) =
		Firebase.firestore.collection(
			USERS).document(uid).collection(FOLLOWERS).document(uid).collection(USERS).orderBy(
			NAME, Query.Direction.ASCENDING)

	private fun getBoundaryCallback(uid: String) = GenericBoundaryCallback(
		15,
		followersQuery(uid),
		repo,
		User::class.java
	)

	fun <T : Any> getObject(
		objectId: String,
		objectClass: Class<T>,
		onComplete: (obj: T?) -> Unit
	) {
		repo.getObject(objectId, objectClass) {
			onComplete(it)
		}
	}

	inline fun <reified T: Any> getObject(documentReference: DocumentReference, crossinline onComplete: (obj: T) -> Unit) {
		repo.getObject<T>(documentReference) {
			onComplete(it)
		}
	}

	fun <T> getObject(documentReference: DocumentReference, clazz: Class<T>, onComplete: (obj: T) ->  Unit) {
		repo.getObject(documentReference, clazz) {
			onComplete(it)
		}
	}

	fun getOtherUser(userId: String) {
		repo.getOtherUser(userId)
	}

	fun searchPosts(item: String, type: String): LiveData<PagedList<Post>> = database.postDao()
		.getSearchedPosts("%$item%", type)
		.toLiveData(postPagingConfig, null, GenericBoundaryCallback(15, Firebase.firestore.collection(POSTS)
			.whereEqualTo(TYPE, type)
			.whereArrayContainsAny(
				INDICES,
				listOf(
					item,
					item.capitalize(Locale.ROOT), item.decapitalize(Locale.ROOT),
					item.toUpperCase(Locale.ROOT), item.toLowerCase(Locale.ROOT)
				)
			)
			.orderBy(RANK, Query.Direction.DESCENDING), repo, Post::class.java)
		)

	fun searchUsers(item: String): LiveData<PagedList<User>> = database.userDao()
		.getSearchedUsers("%$item%")
		.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(USERS)
			.whereArrayContainsAny(INDICES, listOf(
				item,
				item.capitalize(Locale.ROOT), item.decapitalize(Locale.ROOT),
				item.toUpperCase(Locale.ROOT), item.toLowerCase(Locale.ROOT)
			))
			.orderBy(SEARCH_RANK, Query.Direction.DESCENDING), repo, User::class.java))


	/*fun userFollowers(uid: String) = getDataSourceFactory(uid)
		.toLiveData(userPagingConfig, null, *//*GenericBoundaryCallback(10, Firebase.firestore.collection(
			USERS).document(uid).collection(FOLLOWERS).document(uid).collection(USERS).orderBy(
			NAME, Query.Direction.ASCENDING), repo, User::class.java) *//* null, Executors.newSingleThreadExecutor())*/

	fun userFollowings(otherUser: User, query: String? = null): LiveData<PagedList<User>> {
		return if (query == null) {
			database.userDao()
				.getFollowings("%${otherUser.id}%")
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(otherUser.id).collection(FOLLOWINGS).document(otherUser.id).collection(USERS).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java, mapOf(OTHER_USER to otherUser, FOLLOWINGS to true)))
		} else {
			database.userDao()
				.getFollowings("%${otherUser.id}%", "%$query%")
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(otherUser.id).collection(FOLLOWINGS).document(otherUser.id).collection(USERS).whereArrayContainsAny(INDICES, listOf(
					query,
					query.capitalize(Locale.ROOT), query.decapitalize(Locale.ROOT),
					query.toUpperCase(Locale.ROOT), query.toLowerCase(Locale.ROOT)
				)).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java, mapOf(OTHER_USER to otherUser, FOLLOWINGS to true)))
		}
	}

	fun userFollowers(otherUser: User, query: String? = null): LiveData<PagedList<User>> {
		if (query == null) {
			return database.userDao()
				.getFollowers("%${otherUser.id}%")
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(otherUser.id).collection(FOLLOWERS).document(otherUser.id).collection(USERS).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java, mapOf(OTHER_USER to otherUser, FOLLOWERS to true)))
		} else {
			return database.userDao()
				.getFollowers("%${otherUser.id}%", "%$query%")
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(otherUser.id).collection(FOLLOWERS).document(otherUser.id).collection(USERS).whereArrayContainsAny(INDICES, listOf(
					query,
					query.capitalize(Locale.ROOT), query.decapitalize(Locale.ROOT),
					query.toUpperCase(Locale.ROOT), query.toLowerCase(Locale.ROOT)
				)).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java, mapOf(OTHER_USER to otherUser, FOLLOWERS to true)))
		}
	}

	fun userFollowers(uid: String, query: String? = null) : LiveData<PagedList<User>> {
		if (query == null) {
			return database.userDao()
				.getFollowersForCurrentUser()
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(uid).collection(FOLLOWERS).document(uid).collection(USERS).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java))
		} else {
			return database.userDao()
				.getFollowersForCurrentUserWithQuery("%$query%")
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(uid).collection(FOLLOWERS).document(uid).collection(USERS).whereArrayContainsAny(INDICES, listOf(
					query,
					query.capitalize(Locale.ROOT), query.decapitalize(Locale.ROOT),
					query.toUpperCase(Locale.ROOT), query.toLowerCase(Locale.ROOT)
				)).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java))
		}
	}


	fun userFollowings(uid: String, query: String? = null) : LiveData<PagedList<User>> {
		return if (query == null) {
			database.userDao()
				.getFollowingsForCurrentUser()
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(uid).collection(FOLLOWINGS).document(uid).collection(USERS).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java))
		} else {
			database.userDao()
				.getFollowingsForCurrentUserWithQuery("%$query%")
				.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
					USERS).document(uid).collection(FOLLOWINGS).document(uid).collection(USERS).whereArrayContainsAny(INDICES, listOf(
					query,
					query.capitalize(Locale.ROOT), query.decapitalize(Locale.ROOT),
					query.toUpperCase(Locale.ROOT), query.toLowerCase(Locale.ROOT)
				)).orderBy(
					NAME, Query.Direction.ASCENDING), repo, User::class.java))
		}
	}

	suspend fun getLocalUser(uid: String): User? {
		return repo.getLocalUser(uid)
	}

	fun notifications(uid: String) = database.notificationDao()
		.getPagedNotifications()
		.toLiveData(notificationConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
			USERS).document(uid).collection(NOTIFICATIONS)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING),
			repo, SimpleNotification::class.java))

	fun activeRequests(id: String) = database.activeRequestDao()
		.getPagedActiveRequests()
		.toLiveData(notificationConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
			USERS).document(id).collection(REQUESTS)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING),
			repo, SimpleRequest::class.java))

	fun acceptProjectRequest(notification: SimpleNotification) {
		repo.acceptProjectRequest(notification)
	}

	fun declineProjectRequest(notification: SimpleNotification) {
		repo.declineProjectRequest(notification)
	}

	fun undoProjectRequest(request: SimpleRequest) {
		repo.undoProjectRequest(request)
	}

	fun projectContributors(post: Post) = database.userDao()
		.getPagedContributors("%${post.chatChannelId!!}%")
		.toLiveData(userPagingConfig, null, GenericBoundaryCallback(10, Firebase.firestore.collection(
			CHAT_CHANNELS).document(post.chatChannelId!!).collection(USERS).orderBy(NAME, Query.Direction.DESCENDING), repo, User::class.java, mapOf(POST to post)))

	fun projectContributors(channelId: String) = database.userDao()
		.getPagedContributors("%$channelId%")
		.toLiveData(userPagingConfig)


	fun getChannelContributors(channelId: String) {
		repo.getChannelContributorsFromFirebase(channelId)
	}

	fun getChatChannels() {
		Log.d(ChatChannelFragment.TAG, "Get chat channels MV")
		repo.getChatChannels()
	}

	fun updateMessage(message: SimpleMessage) = viewModelScope.launch (Dispatchers.IO) {
		repo.updateMessage(message)
	}

	fun messagesByType(chatChannelId: String, type: String) = database.messageDao()
		.getImageMessages(chatChannelId, type)
		.toLiveData(20)

	private val _inset = MutableLiveData<Pair<Int, Int>>().apply { value = Pair(0, 0)}
	val inset: LiveData<Pair<Int, Int>> = _inset

	fun setAppBarHeight(appBarHeight: Int) = viewModelScope.launch {
		val existingInset = inset.value!!
		if (existingInset.second == 0) {
			delay(1000)
		}
		_inset.postValue(Pair(appBarHeight, existingInset.second))
	}

	fun setBottomBarHeight(bottomBarHeight: Int) = viewModelScope.launch {
		val existingInset = inset.value!!
		if (existingInset.first == 0) {
			delay(1000)
		}
		_inset.postValue(Pair(existingInset.first, bottomBarHeight))
	}

	fun getSavedPosts(initialItemPosition: Int, finalItemPosition: Int) = viewModelScope.launch(Dispatchers.IO) {
		Log.d(BUG_TAG, "Getting saved posts ... VM")
		repo.getSavedPosts(initialItemPosition, finalItemPosition)
	}

	fun savedPosts() = database.postDao()
		.getPagedSavedPosts()
		.toLiveData(20)

	fun deleteLocalRequest(notificationId: String, postId: String, requestId: String, chatChannelId: String) = viewModelScope.launch (Dispatchers.IO) {
		repo.deleteLocalRequest(notificationId, postId, requestId, chatChannelId)
	}

	fun setNotificationListener() {
		repo.setNotificationListener()
	}

	fun clearPosts() = viewModelScope.launch (Dispatchers.IO) {
		repo.clearPosts()
	}

	fun getNotifications() = viewModelScope.launch(Dispatchers.IO) {
		repo.getNotifications()
	}

	/*fun getMyRequests() = viewModelScope.launch(Dispatchers.IO) {
		repo.getMyRequests()
	}*/

	fun clearAndFetchNewRequests() = viewModelScope.launch (Dispatchers.IO) {
		repo.clearRequests()
		repo.getMyRequests()
	}

	private val _profileViewPagerNestedScrollingEnabled = MutableLiveData<Boolean>().apply { value = false }
	val profileViewPagerNestedScrollingEnabled: LiveData<Boolean> = _profileViewPagerNestedScrollingEnabled

	fun setProfileViewPagerNestedScrollingEnabled(b: Boolean) {
		val existing = _profileViewPagerNestedScrollingEnabled.value
		if (existing != b) {
			_profileViewPagerNestedScrollingEnabled.postValue(b)
		}
	}

	fun increaseProjectWeight(cachedPost: Post?) = viewModelScope.launch (Dispatchers.Default) {
		if (cachedPost != null && Firebase.auth.currentUser != null) {
			repo.increaseProjectWeightage(cachedPost)
		}
	}

	val topTags = database.simpleTagsDao().topTags()

	@ExperimentalPagingApi
	fun postsFlow(scope: LifecycleCoroutineScope, tag: String? = null): Flow<PagingData<Post>> {
		return if (tag != null) {
			Pager(
				PagingConfig(20)
			) {
				PostPagingSource(repo)
			}.flow.map { pagingData ->
				pagingData.filter {
					it.tags.contains(tag)
				}
			}
		} else {
			Pager(
				PagingConfig(20), remoteMediator = PostRemoteMediator(
					repo, mapOf(
						HOME_FEED to true, WITH_TAG to tag
					)
				)
			) {
				PostPagingSource(repo)
			}.flow.map { pagingData ->
				pagingData.filter {
					!it.tags.contains("")
				}
			}
		}
	}

	suspend fun getProjectContributors(post: Post): Result<QuerySnapshot> {
		return repo.getProjectContributors(post)
	}

	companion object {
		const val TAG = "MainViewModel"
	}

}