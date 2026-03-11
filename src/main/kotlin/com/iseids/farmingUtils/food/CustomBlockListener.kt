package com.iseids.farmingUtils.food

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.Plugin

class CustomBlockListener(
    private val plugin: Plugin,
    private val settings: CookpotSettings,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val itemMeta = event.itemInHand.itemMeta ?: return
        if (CustomCraftingStation.hasCraftingStationMarker(itemMeta, plugin)) {
            settings.sounds.stationPlace.play(event.player)
            event.player.sendMessage("§6You placed the ${settings.station.displayName}§6.")
        }
    }
}
