package com.example.merco1.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merco1.domain.model.User
import com.example.merco1.repository.AuthRepository
import com.example.merco1.repository.AuthRepositoryImpl
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    val authState = MutableLiveData(0)
    val errorMessage = MutableLiveData<String?>()

    fun registerUser(user: User, password: String, userType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { authState.value = 1 }
            val result = authRepository.register(user, password, userType)
            withContext(Dispatchers.Main) {
                authState.value = if (result.isSuccess) 3 else 2
                errorMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun loginUser(email: String, password: String, userType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { authState.value = 1 } // Estado: cargando
            val result = authRepository.login(email, password, userType)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    authState.value = 3 // Estado: éxito
                } else {
                    authState.value = 2 // Estado: error
                    errorMessage.value = result.exceptionOrNull()?.localizedMessage
                }
            }
        }
    }
}