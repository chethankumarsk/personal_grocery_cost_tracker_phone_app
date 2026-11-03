package dev.groceryledger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.groceryledger.ui.GroceryLedgerApp
import dev.groceryledger.widget.GroceryWidgetProvider

class MainActivity : ComponentActivity() {
    private var quickAddRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as GroceryLedgerApplication).container
        quickAddRequest = consumeQuickAdd(intent)

        setContent {
            GroceryLedgerApp(
                container = container,
                quickAddRequest = quickAddRequest,
                onQuickAddHandled = { quickAddRequest = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        quickAddRequest = consumeQuickAdd(intent)
    }

    override fun onPause() {
        super.onPause()
        // Leaving the app is the natural point to refresh the widget's today total.
        GroceryWidgetProvider.refresh(this)
    }

    /** Reads the widget signal once and clears it so rotation does not reopen the form. */
    private fun consumeQuickAdd(intent: Intent?): Boolean {
        val requested = intent?.getBooleanExtra(EXTRA_QUICK_ADD, false) == true
        intent?.removeExtra(EXTRA_QUICK_ADD)
        return requested
    }

    companion object {
        const val ACTION_QUICK_ADD = "dev.groceryledger.action.QUICK_ADD"
        const val EXTRA_QUICK_ADD = "dev.groceryledger.extra.QUICK_ADD"
    }
}
