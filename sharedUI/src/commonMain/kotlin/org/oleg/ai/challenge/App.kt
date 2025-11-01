package org.oleg.ai.challenge

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.root.PreviewRootComponent
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.theme.AppTheme
import org.oleg.ai.challenge.ui.root.RootContent

@Composable
fun App(
    rootComponent: RootComponent,
) = AppTheme({
    RootContent(
        component = rootComponent,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
})

@Preview
@Composable
private fun AppPreview() {
    App(rootComponent = PreviewRootComponent())
}
