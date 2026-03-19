package com.example.cdn.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val profileImage: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val role: String = "member"
)
