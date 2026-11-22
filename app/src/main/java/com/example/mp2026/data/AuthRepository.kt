package com.example.mp2026.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun signIn(email: String, password: String): Task<AuthResult> =
        auth.signInWithEmailAndPassword(email.trim(), password)

    fun signUp(email: String, password: String): Task<AuthResult> =
        auth.createUserWithEmailAndPassword(email.trim(), password)

    fun resetPassword(email: String): Task<Void> = auth.sendPasswordResetEmail(email.trim())

    fun signOut() = auth.signOut()

    /** Profile failure never pretends account creation failed: Auth and Firestore are separate services. */
    fun syncProfile(user: FirebaseUser): Task<Void> {
        val reference = db.collection("users").document(user.uid)
        return db.runTransaction { transaction ->
            val existing = transaction.get(reference)
            val fields = mutableMapOf<String, Any>(
                "email" to user.email.orEmpty(), "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existing.exists()) {
                fields["displayName"] = user.displayName.orEmpty()
                fields["createdAt"] = FieldValue.serverTimestamp()
                transaction.set(reference, fields)
            } else {
                transaction.update(reference, fields)
            }
            null
        }
    }
}
