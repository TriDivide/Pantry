package com.tridivide.pantry

import com.google.firebase.firestore.DocumentSnapshot
import java.io.InvalidObjectException
import java.util.*
import kotlin.collections.HashMap

class User {

    var name: String
    var id: String
    var email: String
    var profileImageDownloadUrl: String? = null

    constructor(email: String, name: String) {
        this.name = name
        this.email = email
        this.id = UUID.randomUUID().toString()
    }

    @Throws(InvalidObjectException::class)
    constructor(snapshot: DocumentSnapshot) {
        val data: HashMap<String, Any> = snapshot.data as HashMap<String, Any>
        try {
            id = snapshot.id
            name = data["name"] as String
            email = data["email"] as String
            profileImageDownloadUrl = data["profileImageDownloadUrl"] as String

        }
        catch (e:  java.lang.Exception) {
            throw InvalidObjectException("Snapshot is invalid: " + snapshot.data)
        }
    }

    fun toMap(): HashMap<String, Any?> {
        val map: HashMap<String, Any?> = HashMap()
        map["email"] = email
        map["name"] = name
        map["profileImageDownloadUrl"] = profileImageDownloadUrl

        return map
    }
}