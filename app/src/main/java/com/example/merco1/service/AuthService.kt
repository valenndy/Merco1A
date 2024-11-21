package com.example.merco1.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

interface AuthService {
    suspend fun createUser(email: String, password: String)
    suspend fun loginWithEmailAndPassword(email: String, password: String)
}

class AuthServiceImpl : AuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun createUser(email: String, password: String) {
        // Crear el usuario con correo y contraseña en Firebase Authentication
        Log.e("AuthServiceImpl", "Creando usuario con email: $email")
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun loginWithEmailAndPassword(email: String, password: String) {
        // Iniciar sesión con correo y contraseña en Firebase Authentication
        auth.signInWithEmailAndPassword(email, password).await()
    }
}