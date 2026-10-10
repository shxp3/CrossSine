package net.ccbluex.liquidbounce.features.module.modules.client.impl

import net.ccbluex.liquidbounce.features.module.modules.client.Interface
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Forge event handler that cancels the vanilla HUD elements replaced by
 * [HotbarStatBars] — health hearts, armor icons, food bar, air bubbles
 * and the mounted-entity HP bar.
 *
 * Why a Forge event handler instead of a Mixin inject?
 *
 *   In this Forge build the `@Inject(method = "renderPlayerStats", ...)`
 *   transformer either doesn't apply or `ci.cancel()` is swallowed before
 *   reaching our handler (probably due to Forge's own `RenderGameOverlayEvent`
 *   wrappers around each draw step). The official-supported escape hatch in
 *   Forge 1.8.9 for hiding individual HUD elements is `RenderGameOverlayEvent
 *   .Pre` — Forge itself fires it around every element and respects
 *   `setCanceled(true)`. This is also lighter than the off-screen translate
 *   trick: vanilla never even starts the GL draw.
 *
 * Registered in `CrossSine.startClient`.
 */
object HotbarStatBarsHandler {

    @SubscribeEvent
    fun onRenderOverlayPre(event: RenderGameOverlayEvent.Pre) {
        if (!Interface.state) return
        if (!Interface.statBars.get()) return

        when (event.type) {
            RenderGameOverlayEvent.ElementType.HEALTH,
            RenderGameOverlayEvent.ElementType.ARMOR,
            RenderGameOverlayEvent.ElementType.FOOD,
            RenderGameOverlayEvent.ElementType.AIR,
            RenderGameOverlayEvent.ElementType.HEALTHMOUNT,
            RenderGameOverlayEvent.ElementType.EXPERIENCE -> event.isCanceled = true
            else -> {} // leave xp / hotbar / chat / boss / etc untouched
        }
    }
}
