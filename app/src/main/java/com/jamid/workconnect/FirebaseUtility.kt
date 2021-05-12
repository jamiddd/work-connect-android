package com.jamid.workconnect

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.workconnect.model.*
import java.io.File

interface FirebaseUtility {
    // user related
    fun getToken()
    fun signIn(email: String, password: String)
    fun register(email: String, password: String)
    fun checkIfEmailExists(email: String)
    fun signInWithGoogle()
    fun getCurrentUser(userId: String)
    fun uploadCurrentUser(user: User)
    fun updateFirebaseUser(firebaseUserMap: MutableMap<String, Any?>)
    fun uploadCurrentUser(userMap: MutableMap<String, Any?>)
    fun updateCurrentUser(userMap: Map<String, Any?>)
    fun updateRegistrationToken(token: String)
    fun uploadProfilePhoto(image: Uri?)
    fun signOut()

    // post related
    fun uploadPost(post: Post)
    fun joinProject(post: Post)
    fun uploadPostImage(image: Uri, type: String)

    fun onLikePressed(post: Post): Post
    fun onDislikePressed(post: Post): Post
    fun onPostSaved(post: Post): Post
    fun onFollowPressed(post: Post): Post




    fun updatePost(post: Post, postMap: Map<String, Any?>)
    fun getContributorsForPost(channelId: String)
    fun updateGuidelines(postId: String, guidelines: String)



    // message related
    suspend fun setUpChannels(user: User)
    fun sendMessage(message: SimpleMessage, chatChannel: ChatChannel)
    fun uploadMessageMedia(message: SimpleMessage, chatChannel: ChatChannel)
    suspend fun updateChatChannels(channels: List<ChatChannel>)
    fun setMessagesListener(file: File, chatChannel: ChatChannel, contributors: List<User>)
    fun onNewMessagesFromBackground(chatChannelId: String, onComplete: (chatChannel: ChatChannel) -> Unit)

    fun downloadMedia(externalDir: File?, message: SimpleMessage)
    fun getMedia(chatChannelId: String, messageId: String, onComplete: (simpleMedia: SimpleMedia) -> Unit)


    // extras
    fun getPopularInterests(onComplete: (interests: List<PopularInterest>) -> Unit)

    fun getChannelContributors(chatChannelId: String, pageSize: Long, extra: DocumentSnapshot? = null, ahead: Boolean = false, onComplete: (contributors: QuerySnapshot) -> Unit)

    fun getContributorSnapshot(channelId: String, id: String, onComplete: (doc: DocumentSnapshot) -> Unit)
    fun clearSignInChanges()

    fun getPost(postId: String)

    fun checkIfAlreadySentRequest(post: Post, onComplete: (requests: List<SimpleRequest>) -> Unit)

    // paging

}
