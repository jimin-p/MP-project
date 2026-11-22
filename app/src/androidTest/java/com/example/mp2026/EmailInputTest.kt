package com.example.mp2026

import android.view.KeyEvent
import android.content.Intent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmailInputTest {
    @Test fun signupAcceptsHardwareKeysAndImeText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(SignUpActivity::class.java.name, null, false)
        instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, SignUpActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val activity = instrumentation.waitForMonitorWithTimeout(monitor, 15000) as SignUpActivity
        try {
            instrumentation.runOnMainSync {
                val field = activity.findViewById<EditText>(R.id.etSignUpEmail)
                assertTrue(field.isEnabled)
                assertTrue(field.requestFocus())
                field.text.clear()
                for (key in listOf(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_C)) {
                    field.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
                    field.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
                }
                assertEquals("abc", field.text.toString())
                val connection = field.onCreateInputConnection(EditorInfo())!!
                assertTrue(connection.commitText(".Test123@example.com", 1))
                assertEquals("abc.Test123@example.com", field.text.toString())
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.removeMonitor(monitor)
        }
    }
}
