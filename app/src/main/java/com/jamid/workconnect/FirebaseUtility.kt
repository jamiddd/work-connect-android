package com.jamid.workconnect

import android.net.Uri
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jamid.workconnect.model.*
import java.io.File

interface FirebaseUtility {

    val auth: FirebaseAuth
    val db: FirebaseFirestore
    val storage: FirebaseStorage


    // user related
    fun signIn(email: String, password: String)
    fun register(email: String, password: String)
    fun checkIfEmailExists(email: String)
    fun signInWithGoogle(credential: AuthCredential)
    fun updateFirebaseUser(firebaseUserMap: MutableMap<String, Any?>)
    fun updateRegistrationToken(currentUser: User, token: String)
    fun uploadProfilePhoto(image: Uri?)
    fun signOut()
    fun createNewUser(tags: List<String>? = null): User?
    suspend fun checkIfUsernameExists(currentUser: User, username: String? = null)
    suspend fun getToken(): String?
    suspend fun getCurrentUser(): User?
    suspend fun uploadCurrentUser(user: User): User?
    suspend fun updateCurrentUser(currentUser: User, userMap: Map<String, Any?>): User?


    // post related
    fun uploadPostImage(image: Uri, type: String)
    suspend fun getPost(postId: String): Post?
    suspend fun uploadPost(currentUser: User, post: Post): Pair<Post, ChatChannel>?
    suspend fun undoProjectRequest(currentUser: User, request: SimpleRequest): Pair<User, SimpleRequest>?
    suspend fun acceptProjectRequest(currentUser: User, notification: SimpleNotification)
    suspend fun denyProjectRequest(currentUser: User, notification: SimpleNotification)
    suspend fun joinProject(currentUser: User, post: Post): Pair<User, SimpleRequest>?
    suspend fun onLikePressedWithoutCaching(currentUser: User, post: Post): Pair<User, Post>?
    suspend fun onDislikePressedWithoutCaching(currentUser: User, post: Post): Pair<User, Post>?
    suspend fun onPostSavedWithoutCaching(currentUser: User, post: Post): Pair<User, Post>?
    suspend fun onFollowPressed(currentUser: User, post: Post): Pair<User, Post>?
    suspend fun updatePost(post: Post, postMap: Map<String, Any?>): Post?

    // message related
    suspend fun sendMessage(currentUser: User, message: SimpleMessage, chatChannel: ChatChannel): Pair<ChatChannel, SimpleMessage>?
    suspend fun uploadMessageMedia(currentUser: User, message: SimpleMessage, chatChannel: ChatChannel): Pair<ChatChannel, SimpleMessage>?
    suspend fun onNewMessagesFromBackground(chatChannelId: String): ChatChannel?

    suspend fun downloadMedia(destinationFile: File, message: SimpleMessage): SimpleMessage?
    // extras
    fun clearSignInChanges()
    suspend fun removeInterest(currentUser: User, interest: String): User?
    suspend fun addInterest(currentUser: User, interest: String): User?

}
