package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.oleg.ai.challenge.component.agentcreation.AgentCreationComponent
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.component.planner.PlannerComponent
import org.oleg.ai.challenge.component.rag.DocumentManagementComponent
import org.oleg.ai.challenge.component.rag.RagSettingsComponent
import org.oleg.ai.challenge.component.rag.StatisticsDashboardComponent
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.Conversation
import org.oleg.ai.challenge.data.repository.ChatRepository

/**
 * Configuration for the right pane slot navigation.
 */
@Serializable
private sealed class RightPaneConfig {
    @Serializable
    data class ChatConfig(val chatId: Long) : RightPaneConfig()

    @Serializable
    data class AgentCreationConfig(val chatId: Long) : RightPaneConfig()

    @Serializable
    data object PlannerConfig : RightPaneConfig()

    @Serializable
    data object DocumentsConfig : RightPaneConfig()

    @Serializable
    data object RagSettingsConfig : RightPaneConfig()

    @Serializable
    data object StatisticsConfig : RightPaneConfig()

    @Serializable
    data object UserProfileConfig : RightPaneConfig()
}

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val chatRepository: ChatRepository,
    private val onNavigateToMcp: () -> Unit,
    private val documentManagementComponentFactory: (
        componentContext: ComponentContext,
        onNavigateBack: () -> Unit
    ) -> DocumentManagementComponent,
    private val ragSettingsComponentFactory: (
        componentContext: ComponentContext,
        onNavigateBack: () -> Unit
    ) -> RagSettingsComponent,
    private val statisticsDashboardComponentFactory: (
        componentContext: ComponentContext,
        onNavigateBack: () -> Unit
    ) -> StatisticsDashboardComponent,
    private val userProfileComponentFactory: (
        componentContext: ComponentContext,
        onNavigateBack: () -> Unit
    ) -> org.oleg.ai.challenge.component.userprofile.UserProfileComponent,
    private val agentCreationComponentFactory: (
        componentContext: ComponentContext,
        onAgentsCreated: (mainAgent: Agent, subAgents: List<Agent>) -> Unit,
        onNavigateBack: () -> Unit,
    ) -> AgentCreationComponent,
    private val chatComponentFactory: (
        componentContext: ComponentContext,
        chatId: Long?,
    ) -> ChatComponent,
    private val plannerComponentFactory: (
        componentContext: ComponentContext,
        onNavigateBack: () -> Unit,
    ) -> PlannerComponent,
) : MainComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // State
    private val _chatList = MutableValue<List<Conversation>>(emptyList())
    override val chatList: Value<List<Conversation>> = _chatList

    private val _selectedChatId = MutableValue(MainComponent.NO_SELECTION)
    override val selectedChatId: Value<Long> = _selectedChatId

    // Navigation for right pane slot
    private val slotNavigation = SlotNavigation<RightPaneConfig>()

    // Right pane child slot
    override val rightPaneChild: Value<ChildSlot<*, MainComponent.RightPaneChild>> = childSlot(
        source = slotNavigation,
        serializer = RightPaneConfig.serializer(),
        handleBackButton = false,
    ) { config, componentContext ->
        when (config) {
            is RightPaneConfig.ChatConfig -> {
                MainComponent.RightPaneChild.Chat(
                    chatComponentFactory(componentContext, config.chatId)
                )
            }
            is RightPaneConfig.AgentCreationConfig -> {
                MainComponent.RightPaneChild.AgentCreation(
                    agentCreationComponentFactory(
                        componentContext,
                        { mainAgent, subAgents ->
                            handleAgentsCreated(config.chatId, mainAgent, subAgents)
                        },
                        {
                            handleBackFromAgentCreation(config.chatId)
                        }
                    )
                )
            }
            is RightPaneConfig.PlannerConfig -> {
                MainComponent.RightPaneChild.Planner(
                    plannerComponentFactory(componentContext, ::handleBackFromPlanner)
                )
            }
            is RightPaneConfig.DocumentsConfig -> {
                MainComponent.RightPaneChild.Documents(
                    documentManagementComponentFactory(componentContext, ::handleBackFromDocuments)
                )
            }
            is RightPaneConfig.RagSettingsConfig -> {
                MainComponent.RightPaneChild.RagSettings(
                    ragSettingsComponentFactory(componentContext, ::handleBackFromRagSettings)
                )
            }
            is RightPaneConfig.StatisticsConfig -> {
                MainComponent.RightPaneChild.Statistics(
                    statisticsDashboardComponentFactory(componentContext, ::handleBackFromStatistics)
                )
            }
            is RightPaneConfig.UserProfileConfig -> {
                MainComponent.RightPaneChild.UserProfile(
                    userProfileComponentFactory(componentContext, ::handleBackFromUserProfile)
                )
            }
        }
    }

    // Temporary storage for the chat ID being created
    private var pendingChatId: Long? = null

    init {
        // Load chat list from repository
        chatRepository.getAllConversations()
            .onEach { conversations ->
                _chatList.value = conversations
            }
            .launchIn(scope)
    }

    override fun onCreateNewChat() {
        // Create an empty chat in the database
        scope.launch {
            val chatId = chatRepository.createChat()
            pendingChatId = chatId
            _selectedChatId.value = MainComponent.NO_SELECTION

            // Navigate to agent creation screen using slot navigation
            slotNavigation.activate(RightPaneConfig.AgentCreationConfig(chatId))
        }
    }

    override fun onSelectChat(chatId: Long) {
        _selectedChatId.value = chatId
        pendingChatId = null

        // Navigate to the chat using slot navigation
        slotNavigation.activate(RightPaneConfig.ChatConfig(chatId))
    }

    override fun onDeleteChat(chatId: Long) {
        scope.launch {
            chatRepository.deleteChat(chatId)

            // If the deleted chat was selected, clear selection and dismiss slot
            if (_selectedChatId.value == chatId) {
                _selectedChatId.value = MainComponent.NO_SELECTION
                slotNavigation.dismiss()
            }
        }
    }

    override fun onChatCreated() {
        // Called when agent creation is completed
        // This is handled by handleAgentsCreated
    }

    override fun onBackFromAgentCreation() {
        // Get the pending chat ID and handle back navigation
        pendingChatId?.let { chatId ->
            handleBackFromAgentCreation(chatId)
        }
    }

    override fun onNavigateToMcp() {
        onNavigateToMcp.invoke()
    }

    override fun onNavigateToPlanner() {
        _selectedChatId.value = MainComponent.NO_SELECTION
        slotNavigation.activate(RightPaneConfig.PlannerConfig)
    }

    override fun onNavigateToDocuments() {
        _selectedChatId.value = MainComponent.NO_SELECTION
        slotNavigation.activate(RightPaneConfig.DocumentsConfig)
    }

    override fun onNavigateToRagSettings() {
        _selectedChatId.value = MainComponent.NO_SELECTION
        slotNavigation.activate(RightPaneConfig.RagSettingsConfig)
    }

    override fun onNavigateToStatistics() {
        _selectedChatId.value = MainComponent.NO_SELECTION
        slotNavigation.activate(RightPaneConfig.StatisticsConfig)
    }

    override fun onNavigateToUserProfile() {
        _selectedChatId.value = MainComponent.NO_SELECTION
        slotNavigation.activate(RightPaneConfig.UserProfileConfig)
    }

    /**
     * Handle agents being created for the pending chat.
     */
    private fun handleAgentsCreated(chatId: Long, mainAgent: Agent, subAgents: List<Agent>) {
        scope.launch {
            // Save agents to database
            chatRepository.saveAgents(
                chatId = chatId,
                mainAgent = mainAgent,
                subAgents = subAgents
            )

            // Open the newly created chat
            onSelectChat(chatId)
        }
    }

    /**
     * Handle navigation back from agent creation.
     */
    private fun handleBackFromAgentCreation(chatId: Long) {
        // Delete the chat that was being created
        scope.launch {
            chatRepository.deleteChat(chatId)
            pendingChatId = null
        }

        // Dismiss the slot to return to empty state
        slotNavigation.dismiss()
    }

    /**
     * Handle navigation back from planner.
     */
    private fun handleBackFromPlanner() {
        slotNavigation.dismiss()
    }

    private fun handleBackFromDocuments() {
        slotNavigation.dismiss()
    }

    private fun handleBackFromRagSettings() {
        slotNavigation.dismiss()
    }

    private fun handleBackFromStatistics() {
        slotNavigation.dismiss()
    }

    private fun handleBackFromUserProfile() {
        slotNavigation.dismiss()
    }
}
