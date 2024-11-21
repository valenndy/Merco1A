package com.example.merco1.repository

import com.example.merco1.domain.model.User
import com.example.merco1.service.UserServices
import com.example.merco1.service.UserServicesImpl
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

interface UserRepository {
    suspend fun createUser(user: User)
    suspend fun getCurrentUser(): User?
}

class UserRepositoryImpl(
    private val userServices: UserServices = UserServicesImpl()
) : UserRepository {

    override suspend fun createUser(user: User) {
        try {
            userServices.createUser(user)
        } catch (e: Exception) {
            // Maneja el error de creación de usuario, por ejemplo, logueando o lanzando una excepción
            throw Exception("Error creating user: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = Firebase.auth.currentUser
        return if (firebaseUser != null) {
            try {
                userServices.getUserById(firebaseUser.uid)
            } catch (e: Exception) {
                // Maneja el error de obtención de usuario, por ejemplo, logueando o lanzando una excepción
                throw Exception("Error fetching user: ${e.message}")
            }
        } else {
            null // No hay un usuario autenticado
        }
    }
}
