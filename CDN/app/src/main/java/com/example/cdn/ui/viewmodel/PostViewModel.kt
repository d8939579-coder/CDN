package com.example.cdn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cdn.data.model.Post
import com.example.cdn.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPosts().collect {
                _posts.value = it
            }
        }
    }

    fun createPost(post: Post) {
        viewModelScope.launch {
            repository.createPost(post)
        }
    }

    fun toggleLike(postId: String, userId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }

    fun votePoll(postId: String, userId: String, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(postId, userId, optionIndex)
        }
    }
}
