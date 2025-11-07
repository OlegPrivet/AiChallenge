package org.oleg.ai.challenge.ui.agentcreation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.oleg.ai.challenge.BuildConfig

// Common AI models available on OpenRouter
val AVAILABLE_MODELS = listOf(
    BuildConfig.DEFAULT_MODEL,
    "openai/gpt-3.5-turbo",
    "openai/gpt-4-turbo",
    "openai/gpt-4o",
    "openai/gpt-4o-mini",
    "openai/gpt-5",
    "openai/gpt-5-pro",
)

@Composable
fun ModelSelector(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "AI Model"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = { onModelSelected(it) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = false,  // Allow custom model input
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select model",
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            AVAILABLE_MODELS.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}
