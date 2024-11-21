package com.example.merco1.service


import android.util.Log
import com.example.merco1.domain.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


interface UserServices {
    suspend fun createUser(user: User)
    suspend fun getUserById(id:String):User?
}

class UserServicesImpl: UserServices {
    override suspend fun createUser(user: User) {
        try {
            Log.d("UserServicesImpl", "Creando usuario con ID: ${user.id}")

            // Referencia a la colección "users", creando o actualizando un documento con el ID del usuario
            Firebase.firestore
                .collection("users")           // Colección "users"
                .document(user.id)             // El nombre del documento será el user.id
                .set(user)                     // Guardar el objeto "user" en Firestore
                .await()                       // Esperar a que la operación termine

            Log.d("UserServicesImpl", "Usuario creado con éxito")
        } catch (e: Exception) {
            // Manejo de errores si algo sale peye
            Log.e("UserServicesImpl", "Error al crear el usuario: ${e.message}")
        }
    }


    override suspend fun getUserById(id: String): User? {
        val user = Firebase.firestore
            .collection("users")
            .document(id)
            .get()
            .await()
        val userObject = user.toObject(User::class.java)
        return userObject
    }

}
