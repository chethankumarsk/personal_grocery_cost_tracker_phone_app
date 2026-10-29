package dev.groceryledger

import android.os.Looper
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class MainActivityTest {
    @Test
    fun activityLaunchesWithoutCrashing() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull(activity)
    }
}
