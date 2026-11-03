package dev.groceryledger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.groceryledger.widget.GroceryWidgetProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GroceryWidgetProviderTest {
    @Test
    fun buildsTheWidgetViewsWithTheAddAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Throws if the layout, text resources or PendingIntent are misconfigured.
        val views = GroceryWidgetProvider.buildViews(context, todayTotalPaise = 12_345)

        assertNotNull(views)
    }
}
