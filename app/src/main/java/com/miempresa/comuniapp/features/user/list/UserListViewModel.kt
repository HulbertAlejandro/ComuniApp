package com.miempresa.comuniapp.features.user.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val users: StateFlow<List<User>> = repository.users

    val filteredUsers: StateFlow<List<User>> =
        combine(users, _searchQuery) { users, query ->
            if (query.isEmpty()) users
            else users.filter {
                it.name.contains(query, true) ||
                        it.email.contains(query, true) ||
                        it.city.contains(query, true)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }
}