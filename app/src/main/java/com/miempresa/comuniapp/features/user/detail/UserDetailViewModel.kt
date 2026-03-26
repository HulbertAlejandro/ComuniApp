package com.miempresa.comuniapp.features.user.detail

import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    fun findById(userId: String): User? {
        return repository.findById(userId)
    }
}
