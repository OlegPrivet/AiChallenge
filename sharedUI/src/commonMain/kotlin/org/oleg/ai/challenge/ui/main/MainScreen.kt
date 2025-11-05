package org.oleg.ai.challenge.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.main.MainComponent
import org.oleg.ai.challenge.component.main.PreviewMainComponent
import org.oleg.ai.challenge.theme.AppTheme

@Composable
fun MainScreen(
    component: MainComponent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AI Challenge",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { component.onNavigateToChat() },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            Text("Start Chat")
        }
    }

    // Prompt Setup Dialog
    PromptSetupDialog(
        visible = component.isPromptDialogVisible.subscribeAsState().value,
        onDismiss = component::onDismissPromptDialog,
        onStartChat = component::onSavePromptsAndNavigate
    )
}

@Preview
@Composable
private fun MainScreenPreview() {
    AppTheme {
        MainScreen(PreviewMainComponent())
    }
}
