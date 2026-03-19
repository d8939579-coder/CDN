package com.example.cdn.data.repository

import com.example.cdn.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    suspend fun getUserData(uid: String): User? {
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    suspend fun updateProfile(uid: String, name: String, bio: String, profileImage: String) {
        val updates = mapOf(
            "name" to name,
            "bio" to bio,
            "profileImage" to profileImage
        )
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun followUser(currentUserId: String, targetUserId: String) {
        val batch = db.batch()
        
        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)
        
        batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
        batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
        
        batch.commit().await()
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        val batch = db.batch()
        
        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)
        
        batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
        batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
        
        batch.commit().await()
    }
}
