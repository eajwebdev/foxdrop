package ph.foxdrop.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ph.foxdrop.core.designsystem.theme.FoxMotion
import ph.foxdrop.core.designsystem.theme.LocalFoxMotion

/**
 * Scales the element to 0.96 while pressed (§11A.4). The scale is applied in the layer, so pressing never
 * recomposes or re-lays out the content. Uses the theme's [FoxMotion], so "Remove animations" is respected.
 */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = this then PressScaleElement(interactionSource)

private data class PressScaleElement(val interactionSource: InteractionSource) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource)

    override fun update(node: PressScaleNode) {
        if (node.interactionSource != interactionSource) {
            node.interactionSource = interactionSource
            node.observe()
        }
    }
}

private class PressScaleNode(var interactionSource: InteractionSource) :
    Modifier.Node(),
    LayoutModifierNode,
    CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)
    private var job: Job? = null

    override fun onAttach() = observe()

    fun observe() {
        if (!isAttached) return
        job?.cancel()
        job =
            coroutineScope.launch {
                val pressed = mutableSetOf<PressInteraction.Press>()
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressed += interaction
                        is PressInteraction.Release -> pressed -= interaction.press
                        is PressInteraction.Cancel -> pressed -= interaction.press
                    }
                    val target = if (pressed.isEmpty()) 1f else FoxMotion.PRESSED_SCALE
                    launch { scale.animateTo(target, currentValueOf(LocalFoxMotion).press()) }
                }
            }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }
}
