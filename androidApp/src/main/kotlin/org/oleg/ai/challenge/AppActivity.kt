package org.oleg.ai.challenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.data.database.initializeDatabaseContext
import org.oleg.ai.challenge.di.initKoin

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database context for Android
        initializeDatabaseContext(applicationContext)

        // Initialize Koin
        initKoin {
            androidContext(this@AppActivity)
        }

        // Create root component
        val rootComponent = org.koin.core.context.GlobalContext.get().get<RootComponent> {
            parametersOf(defaultComponentContext())
        }

        setContent {
            App(
                rootComponent = rootComponent,
            )
        }
    }
}
