package com.example.cdn

data class Post(
    val id: String,
    val author: String,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class Story(
    val id: String,
    val author: String,
    val imageUrl: String? = null
)

data class Poll(
    val id: String,
    val victimName: String,
    val crimeDescription: String,
    val topic: String,
    val question: String,
    val options: List<String>,
    var hasVoted: Boolean = false,
    val votes: MutableMap<Int, Int> = mutableMapOf()
)
