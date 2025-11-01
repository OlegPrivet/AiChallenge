package org.oleg.ai.challenge.component.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.component.main.MainComponent

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class MainChild(val component: MainComponent) : Child()
        data class ChatChild(val component: ChatComponent) : Child()
    }
}
