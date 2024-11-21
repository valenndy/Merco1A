package com.example.merco1.repository

import com.example.merco1.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    suspend fun register(user: User, password: String, userType: String): Result<Boolean>
    suspend fun login(email: String, password: String, userType: String): Result<Boolean>
}

class AuthRepositoryImpl : AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Registrar usuario en Firebase Authentication y guardar datos adicionales en Firestore
    override suspend fun register(user: User, password: String, userType: String): Result<Boolean> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("Error creando el usuario"))

            val userData = mapOf(
                "name" to user.name,
                "lastname" to user.lastname,
                "celphone" to user.celphone,
                "email" to user.email,
                "type" to userType // Aseguramos que el tipo se almacena correctamente
            )
            val collectionName = if (userType == "buyer") "buyers" else "sellers"

            db.collection(collectionName).document(userId).set(userData).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Error registrando usuario: ${e.message}"))
        }
    }


    // Iniciar sesión y validar el tipo de usuario en la colección correspondiente
    override suspend fun login(email: String, password: String, userType: String): Result<Boolean> {
        return try {
            // Autenticación en Firebase Authentication
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("Usuario no encontrado"))

            // Verificar en la colección correspondiente
            val collectionName = if (userType == "buyer") "buyers" else "sellers"
            val userSnapshot = db.collection(collectionName).document(userId).get().await()

            if (userSnapshot.exists()) {
                Result.success(true) // Usuario encontrado en la colección correcta
            } else {
                // Si no está en la colección esperada, busca en la otra colección
                val otherCollectionName = if (userType == "buyer") "sellers" else "buyers"
                val otherSnapshot = db.collection(otherCollectionName).document(userId).get().await()

                if (otherSnapshot.exists()) {
                    auth.signOut() // Cerrar sesión si el usuario está en la colección incorrecta
                    Result.failure(Exception("Este usuario está registrado como ${if (userType == "buyer") "Seller" else "Buyer"}."))
                } else {
                    Result.failure(Exception("Usuario no encontrado en ninguna colección."))
                }
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Usuario no encontrado. Verifica tus credenciales."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Contraseña incorrecta o usuario incorrecto."))
        } catch (e: Exception) {
            Result.failure(Exception("Error al iniciar sesión: ${e.message}"))
        }
    }






}
