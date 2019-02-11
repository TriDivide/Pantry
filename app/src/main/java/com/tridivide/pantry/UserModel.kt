package com.tridivide.pantry

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.lang.Exception
import java.util.*

object UserModel: Observable() {

    private var mCurrentUser: User? = null

    private fun instance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    private fun getDatabaseRef(): CollectionReference {
        return instance().collection("users")
    }

    fun signUpAndLogin(name: String, email: String, password: String, profileImageUrl: Uri?, callback: (User?, Exception?) -> Unit) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener { signUp ->
            Log.i("signupandLogin", "registered user " + signUp.isSuccessful)
            if (signUp.isSuccessful) {
                val userId = FirebaseAuth.getInstance().uid
                Log.i("SignUpAndLogin", "registered User")
                if (userId != null) {
                    val user = User(email, name)
                    uploadUser(user) { _, error ->
                        error?.printStackTrace() ?: run {
                            profileImageUrl?.let {imageUrl ->
                                setUserProfileImage(userId, imageUrl) { _, error ->
                                    error?.printStackTrace() ?: run {
                                        callback(user, null)
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    private fun uploadUser(user: User, callback: (Boolean?, Exception?) -> Unit) {
        val uid = FirebaseAuth.getInstance().uid
        if (uid != null) {
            getDatabaseRef().document(uid).set(user.toMap()).addOnCompleteListener { updatedUser ->
                if (updatedUser.isSuccessful) {
                    callback(true, null)
                }
                else {
                    callback(false, updatedUser.exception)
                }
            }
        }
        else {
            callback(null, Exception("something went wrong"))
        }
    }

    private fun setUserProfileImage(userId: String, profileImageUrl: Uri, callback: (Boolean?, Exception?) -> Unit) {
        val user = mCurrentUser
        if (user != null) {
            FirebaseStorage.getInstance().reference.child("users").child(userId).putFile(profileImageUrl).addOnCompleteListener { uploadedImage ->
                if (uploadedImage.isSuccessful) {
                    val metadata = uploadedImage.result?.metadata
                    if (metadata != null) {
                        val storage = uploadedImage.result?.storage
                        storage?.downloadUrl?.addOnSuccessListener {
                            user.profileImageDownloadUrl = it.toString()
                            updateUser(user, callback)
                        }?.addOnFailureListener {
                            updateUser(user, callback)
                        }
                    }
                }
            }
        }
    }

    fun updateUser(user: User, callback: (Boolean?, Exception?) -> Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val oldEmailAddress = firebaseUser.email
            firebaseUser.updateEmail(user.email).addOnCompleteListener {
                if (it.isSuccessful) {
                    getDatabaseRef().document(firebaseUser.uid).set(user.toMap(), SetOptions.merge()).addOnCompleteListener{ user ->
                        if (user.isSuccessful) {
                            getDatabaseRef().document(firebaseUser.uid).get().addOnCompleteListener { data ->
                                if (data.isSuccessful) {
                                    data.result?.let { res ->
                                        try {
                                            val updatedUser = User(res)
                                            this.mCurrentUser = updatedUser
                                            callback(true, null)
                                        }
                                        catch(e: Exception) {
                                            callback(null, e)
                                        }
                                    }
                                }
                                else {
                                    if (oldEmailAddress != null) {
                                        firebaseUser.updateEmail(oldEmailAddress)
                                    }
                                    callback(null, user.exception)
                                }
                            }
                        }
                        else {
                            val exception = Exception("something went wrong.")
                            callback(null, exception)
                        }
                    }
                }
            }
        }
    }

    fun getCurrentUser(callback: (User?, Exception?) -> Unit) {
        if (mCurrentUser != null) {
            callback(mCurrentUser, null)
        }
        else {
            val userId = FirebaseAuth.getInstance().uid
            if (userId != null) {
                getDatabaseRef().document(userId).get().addOnSuccessListener {
                    mCurrentUser = User(it)
                    setChanged()
                    notifyObservers()
                    callback(mCurrentUser, null)
                }.addOnFailureListener{
                    callback(null, it)
                }
            }
            else {
                callback(null, Exception("Something went wrong"))
            }
        }
    }

    fun login(email: String, password: String, callback: (User?, Exception?) -> Unit) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener{
            if(it.isSuccessful) {
                val userId = FirebaseAuth.getInstance().uid
                if (userId != null) {
                    getDatabaseRef().document(userId).get().addOnCompleteListener { result ->
                        if (result.isSuccessful && result.result != null) {
                            var user: User? = null
                            try {
                                user = User(result.result!!)
                            }
                            catch (e: Exception) {
                                callback(null, e)
                            }
                            callback(user, null)
                        }
                        else {
                            callback(null, result.exception)
                        }
                    }
                }
            }
        }
    }

    fun resetPassword(email: String, callback: (Boolean?, Exception?) -> Unit) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener {
            callback(it.isSuccessful, it.exception)
        }
    }
}