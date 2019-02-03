package com.tridivide.pantry

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.lang.Exception
import java.net.URI
import java.util.*

object UserModel: Observable() {

    private var mCurrentUser: User? = null

    init {

    }

    private fun instance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    private fun getDatabaseRef(): CollectionReference {
        return instance().collection("users")
    }

    fun signUpAndLogin(name: String, email: String, password: String, profileImageUrl: Uri?, onComplete: IDataModelResult<User>) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener { signUp ->
            Log.i("signupandLogin", "registered user " + signUp.isSuccessful)
            if (signUp.isSuccessful) {
                val userId = FirebaseAuth.getInstance().uid
                Log.i("SignUpAndLogin", "registered User")
                if (userId != null) {
                    val user = User(email, name)

                    uploadUser(user, object :IDataModelResult<Boolean> {
                        override fun onComplete(data: Boolean?, exception: Exception?) {
                            if (data != null && data) {
                                setUserProfileImage(userId, profileImageUrl!!, object : IDataModelResult<Boolean>{
                                    override fun onComplete(data: Boolean?, exception: Exception?) {
                                        if (data != null && data) {
                                            onComplete.onComplete(user, null)
                                        }
                                    }
                                })
                            }
                        }
                    })
                }
            }
        }
    }

    private fun uploadUser(user: User, onComplete: IDataModelResult<Boolean>) {
        val uid = FirebaseAuth.getInstance().uid
        if (uid != null) {
            getDatabaseRef().document(uid).set(user.toMap()).addOnCompleteListener { user ->
                if (user.isSuccessful) {
                    onComplete.onComplete(true, null)
                }
                else {
                    onComplete.onComplete(false, user.exception)
                }
            }
        }
        else {
            onComplete.onComplete(null, null)
        }
    }

    private fun setUserProfileImage(userId: String, profileImageUrl: Uri, onComplete: IDataModelResult<Boolean>) {
        val user = mCurrentUser
        if (user != null) {
            FirebaseStorage.getInstance().reference.child("users").child(userId).putFile(profileImageUrl).addOnCompleteListener { uploadedImage ->
                if (uploadedImage.isSuccessful) {
                    val metadata = uploadedImage.result?.metadata
                    if (metadata != null) {
                        val storage = uploadedImage.result?.storage
                        storage?.downloadUrl?.addOnSuccessListener {
                            user.profileImageDownloadUrl = it.toString()
                            updateUser(user, onComplete)
                        }?.addOnFailureListener {
                            updateUser(user, onComplete)
                        }
                    }
                }
            }
        }
    }

    fun updateUser(user: User, onComplete: IDataModelResult<Boolean>) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val oldEmailAddress = firebaseUser.email
            firebaseUser.updateEmail(user.email).addOnCompleteListener {
                if (it.isSuccessful) {
                    getDatabaseRef().document(firebaseUser.uid).set(user.toMap(), SetOptions.merge()).addOnCompleteListener{ user ->
                        if (user.isSuccessful) {
                            getDatabaseRef().document(firebaseUser.uid).get().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    it.result.let { res ->
                                        try {
                                            if (res is DocumentSnapshot) {
                                                val user = User(res)
                                                this.mCurrentUser = user
                                                onComplete.onComplete(true, null)
                                            }
                                        }
                                        catch(e: Exception) {
                                            onComplete.onComplete(null, e)
                                        }
                                    }
                                }
                                else {
                                    if (oldEmailAddress != null) {
                                        firebaseUser.updateEmail(oldEmailAddress)
                                    }
                                    onComplete.onComplete(null, user.exception)
                                }
                            }
                        }
                        else {
                            onComplete.onComplete(null, null)
                        }
                    }
                }
            }
        }
    }

    fun getCurrentUser(onComplete: IDataModelResult<User?>) {
        if (mCurrentUser != null) {
            onComplete.onComplete(mCurrentUser, null)
        }
        else {
            val userId = FirebaseAuth.getInstance().uid
            if (userId != null) {
                getDatabaseRef().document(userId).get().addOnSuccessListener {
                    mCurrentUser = User(it)
                    setChanged()
                    notifyObservers()
                    onComplete.onComplete(mCurrentUser, null)
                }.addOnFailureListener{
                    onComplete.onComplete(null, it)
                }
            }
            else {
                return onComplete.onComplete(null, null)
            }
        }
    }

    fun login(email: String, password: String, onComplete: IDataModelResult<User>) {
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
                                onComplete.onComplete(null, e)
                            }
                            onComplete.onComplete(user, null)
                        }
                        else {
                            onComplete.onComplete(null, result.exception)
                        }
                    }
                }
            }
        }
    }

    fun resetPassword(email: String, onComplete: IDataModelResult<Boolean>) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener {
            onComplete.onComplete(it.isSuccessful, it.exception)
        }
    }
}