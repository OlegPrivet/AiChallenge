package org.oleg.ai.challenge.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.ui.main.MainScreen
import org.oleg.ai.challenge.ui.mcp.McpConnectionScreen

@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier
) {
    Children(
        stack = component.childStack,
        modifier = modifier,
        animation = stackAnimation(slide())
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.MainChild -> MainScreen(instance.component)
            is RootComponent.Child.McpConnectionChild -> McpConnectionScreen(
                component = instance.component,
                onBack = component::onBackClicked
            )
        }
    }
}
