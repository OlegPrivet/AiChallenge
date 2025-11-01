import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.koin.core.parameter.parametersOf
import org.oleg.ai.challenge.App
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.di.initKoin
import java.awt.Dimension

fun main() = application {
    // Initialize Koin
    initKoin()

    // Create lifecycle and component context for desktop
    val lifecycle = LifecycleRegistry()
    val rootComponent = org.koin.core.context.GlobalContext.get().get<RootComponent> {
        parametersOf(DefaultComponentContext(lifecycle = lifecycle))
    }

    Window(
        title = "AiChallenge",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App(rootComponent = rootComponent)
    }
}

