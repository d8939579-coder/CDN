package com.example.cdn.data.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Timestamp? = null,
    val likes: List<String> = emptyList(),
    val poll: Poll? = null
)

data class Poll(
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    val totalVotes: Int = 0,
    val votedUsers: Map<String, Int> = emptyMap() // userId -> optionIndex
)

data class PollOption(
    val text: String = "",
    val votes: Int = 0
)
