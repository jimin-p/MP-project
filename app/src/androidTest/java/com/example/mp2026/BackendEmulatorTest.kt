package com.example.mp2026

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mp2026.data.AuthRepository
import com.example.mp2026.data.AcademyRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicit opt-in, fake project, local services only. Never creates production users/data. */
@RunWith(AndroidJUnit4::class)
class BackendEmulatorTest {
    companion object {
        @JvmStatic @BeforeClass fun connectEmulators() {
            assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
            val host = InstrumentationRegistry.getArguments().getString("emulatorHost") ?: "10.0.2.2"
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            FirebaseApp.getInstance().delete()
            FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setProjectId("demo-classpot").setApplicationId("1:123456789:android:classpot-test")
                .setApiKey("fake-emulator-key").build())
            FirebaseAuth.getInstance().useEmulator(host, 9099)
            FirebaseFirestore.getInstance().apply {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
                useEmulator(host, 8080)
            }
        }
    }

    @Test fun signupProfileCatalogHomeAndLoginRoundTrip() {
        val auth = AuthRepository()
        auth.signOut()
        val email = "android-${System.nanoTime()}@example.com"
        val password = "Test-only-password-123!"
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val signupMonitor = instrumentation.addMonitor(SignUpActivity::class.java.name, null, false)
        val homeMonitor = instrumentation.addMonitor(HomeActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch<MainActivity>(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    // Empty login fields must not prevent opening the signup screen.
                    activity.findViewById<android.widget.TextView>(R.id.tvSignUp).performClick()
                }
                val signup = instrumentation.waitForMonitorWithTimeout(signupMonitor, 10000) as? SignUpActivity
                assertNotNull("Signup screen must open", signup)
                instrumentation.runOnMainSync {
                    signup!!.findViewById<android.widget.EditText>(R.id.etSignUpEmail).setText(email)
                    signup.findViewById<android.widget.EditText>(R.id.etSignUpPassword).setText(password)
                    val confirmation = signup.findViewById<android.widget.EditText>(R.id.etSignUpConfirm)
                    confirmation.setText("different-password")
                    signup.findViewById<android.widget.Button>(R.id.btnCreateAccount).performClick()
                    assertNotNull(confirmation.error)
                    assertNull(auth.currentUser)
                    confirmation.setText(password)
                    signup.findViewById<android.widget.Button>(R.id.btnCreateAccount).performClick()
                }
                assertNotNull("Successful signup must open Home", instrumentation.waitForMonitorWithTimeout(homeMonitor, 20000))
            }
        } finally {
            instrumentation.removeMonitor(signupMonitor)
            instrumentation.removeMonitor(homeMonitor)
        }
        val user = auth.currentUser!!
        try {
            Tasks.await(auth.syncProfile(user), 20, TimeUnit.SECONDS)
            Tasks.await(auth.syncProfile(user), 20, TimeUnit.SECONDS)
            val profile = Tasks.await(FirebaseFirestore.getInstance().collection("users").document(user.uid).get(Source.SERVER), 20, TimeUnit.SECONDS)
            assertEquals(email, profile.getString("email"))
            val latch = CountDownLatch(1)
            var count = 0
            var failure: Throwable? = null
            val listener = AcademyRepository().observe { result ->
                result.onSuccess { if (!it.fromCache) { count = it.academies.size; latch.countDown() } }
                    .onFailure { failure = it; latch.countDown() }
            }
            try {
                assertTrue(latch.await(20, TimeUnit.SECONDS))
                assertNull(failure)
                assertEquals(130, count)
            } finally { listener.remove() }
            val intent = Intent(ApplicationProvider.getApplicationContext(), HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<HomeActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    assertTrue(activity.findViewById<android.widget.Button>(R.id.btnShowList).isShown)
                    assertTrue(activity.findViewById<android.widget.Button>(R.id.btnHomeFilter).isEnabled)
                }
            }
            auth.signOut()
            Tasks.await(auth.signIn(email, password), 20, TimeUnit.SECONDS)
            assertEquals(user.uid, auth.currentUser!!.uid)
        } finally {
            auth.currentUser?.let { Tasks.await(it.delete(), 20, TimeUnit.SECONDS) }
            auth.signOut()
        }
    }
}
