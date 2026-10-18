package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.antigravity.substance.data.db.SubstanceDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify seed defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Substance", appName)

        val defaultSubstances = SubstanceDatabase.defaultSubstances()
        assertEquals(6, defaultSubstances.size)
        assertTrue(defaultSubstances.any { it.name == "Caffeine" })
        assertTrue(defaultSubstances.any { it.name == "Kratom" })

        val demoLogs = SubstanceDatabase.buildDemoLogs()
        assertTrue(demoLogs.size > 40)
    }
}
