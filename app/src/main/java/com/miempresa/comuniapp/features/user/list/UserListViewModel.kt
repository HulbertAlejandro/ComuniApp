package com.miempresa.comuniapp.features.user.list

import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel // Anotamos el ViewModel con @HiltViewModel para que Hilt pueda inyectarlo
class UserListViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    // Exponemos la lista de usuarios desde el repositorio como un StateFlow para que la UI pueda observar los cambios
    val users: StateFlow<List<User>> = repository.users

}