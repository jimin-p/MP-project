package com.example.mp2026

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

/** Exists only in the local build. adb reverse connects device loopback to the PC. */
class LocalFirebaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        check(FirebaseApp.getInstance().options.projectId == "demo-classpot") {
            "Local builds must use the demo Firebase project."
        }
        FirebaseAuth.getInstance().useEmulator("127.0.0.1", 9099)
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
            useEmulator("127.0.0.1", 8080)
        }
        // Emulator users disappear on server restart; always show login on a fresh local launch.
        FirebaseAuth.getInstance().signOut()
    }
}
