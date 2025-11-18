package org.oleg.ai.challenge.component.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.component.main.PreviewMainComponent

class PreviewRootComponent(
    override val childStack: Value<ChildStack<*, RootComponent.Child>> = MutableValue(
        ChildStack(
            configuration = Unit,
            instance = RootComponent.Child.MainChild(PreviewMainComponent())
        )
    )
) : RootComponent {
    override fun onBackClicked() = Unit
}
