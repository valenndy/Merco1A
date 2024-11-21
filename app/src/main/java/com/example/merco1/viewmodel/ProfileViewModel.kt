package com.example.merco1.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.merco1.domain.model.Message
import com.example.merco1.domain.model.User
import com.example.merco1.repository.UserRepository
import com.example.merco1.repository.UserRepositoryImpl
import com.example.merco1.service.ChatService
import com.example.merco1.service.ChatServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileViewModel(
    val userRepository: UserRepository = UserRepositoryImpl(),
    val chatService: ChatService = ChatServiceImpl()
) : ViewModel() {


    private val _user = MutableLiveData<User?>(User())
    val user: LiveData<User?> get() = _user

    fun getCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getCurrentUser()
            withContext(Dispatchers.Main) {
                _user.value = me
            }
        }
    }

    fun funcion1() {
        viewModelScope.launch(Dispatchers.IO) {
            val chatRoomID = chatService.searchChatId(
                "MJYupfQB3Wa61qoko3jWUJpiEKu1",
                "jje1CoWxMbU99kQ1PUNzl9KGlH02"
            )
        }
    }

    fun funcion2() {
        viewModelScope.launch(Dispatchers.IO) {
            chatService.sendMessage(Message(UUID.randomUUID().toString(), "Prueba de la función 2"), "Tf1Vm5hS6ajLMDfbBHxR")
        }
    }

    fun funcion3() {
        viewModelScope.launch(Dispatchers.IO) {
            chatService.getMessages("Tf1Vm5hS6ajLMDfbBHxR")
        }
    }

}