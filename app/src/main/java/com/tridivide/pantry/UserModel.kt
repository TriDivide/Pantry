package com.tridivide.pantry

import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object UserModel: Observable() {

    private var mCurrentUser: User? = null

    init {

    }

    private fun instance(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}