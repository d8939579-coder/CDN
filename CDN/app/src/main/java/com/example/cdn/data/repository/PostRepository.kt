package com.example.cdn.data.repository

import com.example.cdn.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(private val db: FirebaseFirestore) {

    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val subscription = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(posts)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createPost(post: Post) {
        val docRef = db.collection("posts").document()
        val postWithId = post.copy(id = docRef.id)
        docRef.set(postWithId).await()
    }

    suspend fun toggleLike(postId: String, userId: String) {
        val postRef = db.collection("posts").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likes") as? List<String> ?: emptyList()
            val newLikes = if (likes.contains(userId)) {
                likes - userId
            } else {
                likes + userId
            }
            transaction.update(postRef, "likes", newLikes)
        }.await()
    }

    suspend fun votePoll(postId: String, userId: String, optionIndex: Int) {
        val postRef = db.collection("posts").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(Post::class.java) ?: return@runTransaction
            val poll = post.poll ?: return@runTransaction
            
            if (poll.votedUsers.containsKey(userId)) return@runTransaction

            val newOptions = poll.options.mapIndexed { index, option ->
                if (index == optionIndex) option.copy(votes = option.votes + 1) else option
            }
            val newVotedUsers = poll.votedUsers + (userId to optionIndex)
            val newPoll = poll.copy(
                options = newOptions,
                totalVotes = poll.totalVotes + 1,
                votedUsers = newVotedUsers
            )
            
            transaction.update(postRef, "poll", newPoll)
        }.await()
    }
}
