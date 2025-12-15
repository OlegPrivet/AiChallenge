package org.oleg.ai.challenge.ui.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.oleg.ai.challenge.component.userprofile.UserProfileComponent
import org.oleg.ai.challenge.data.settings.UserProfile

/**
 * Screen for managing user profile information.
 * Allows users to input their name and preferences for AI personalization.
 */
@Composable
fun UserProfileScreen(
    component: UserProfileComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.subscribeAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = component::onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "User Profile",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text(
            text = "This information will be shared with the AI assistant in new chats",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Profile information card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profile Information",
                    style = MaterialTheme.typography.titleMedium
                )

                // Name field
                OutlinedTextField(
                    value = state.name,
                    onValueChange = {
                        if (it.length <= UserProfile.MAX_NAME_LENGTH) {
                            component.updateName(it)
                        }
                    },
                    label = { Text("Name") },
                    placeholder = { Text("Your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("${state.name.length}/${UserProfile.MAX_NAME_LENGTH}")
                    }
                )

                // Preferences field (multiline)
                OutlinedTextField(
                    value = state.preferences,
                    onValueChange = {
                        if (it.length <= UserProfile.MAX_PREFERENCES_LENGTH) {
                            component.updatePreferences(it)
                        }
                    },
                    label = { Text("Preferences & Description") },
                    placeholder = { Text("Tell the AI about your preferences, background, or any context you'd like it to know...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    minLines = 8,
                    maxLines = 8,
                    supportingText = {
                        Text("${state.preferences.length}/${UserProfile.MAX_PREFERENCES_LENGTH}")
                    }
                )
            }
        }

        // Status indicator
        if (state.isComplete) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Profile is complete and will be included in new chats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Fill in both fields to enable profile injection in new chats",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Clear profile button
        Button(
            onClick = component::clearProfile,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.name.isNotBlank() || state.preferences.isNotBlank()
        ) {
            Text("Clear Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
