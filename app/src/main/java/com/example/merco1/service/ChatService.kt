package com.example.merco1.service


import com.example.merco1.domain.model.Message
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

interface ChatService {
    suspend fun searchChatId(userID1:String, userID2: String):String
    suspend fun sendMessage(message: Message, chatroomID:String)
    suspend fun getMessages(chatroomID: String):List<Message?>
    suspend fun getLiveMessages(chatroomID: String)
}

class ChatServiceImpl: ChatService {
    override suspend fun searchChatId(userID1: String, userID2: String): String {
        val result = Firebase.firestore.collection("users")
            .document(userID2)
            .collection("chats")
            .whereEqualTo("userid",userID1)
            .get()
            .await()
        if(result.documents.size == 0){
            return ""
        }else{
            val chatid = result.documents[0].get("chatid").toString()
            return chatid
        }
    }

    override suspend fun sendMessage(message: Message, chatroomID: String) {
        Firebase.firestore.collection("chats")
            .document(chatroomID)
            .collection("messages")
            .document(message.id)
            .set(message)
            .await()
    }

    override suspend fun getMessages(chatroomID: String): List<Message?> {
        val result = Firebase.firestore
            .collection("chats")
            .document(chatroomID)
            .collection("messages")
            .get()
            .await()
        val messages = result.documents.map { doc ->
            doc.toObject(Message::class.java)
        }
        return messages
    }

    override suspend fun getLiveMessages(chatroomID: String) {
        TODO("Not yet implemented")
    }

}