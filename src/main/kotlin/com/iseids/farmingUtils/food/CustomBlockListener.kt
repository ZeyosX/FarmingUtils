package com.iseids.farmingUtils.food

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.Plugin

class CustomBlockListener(private val plugin: Plugin) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val itemMeta = event.itemInHand.itemMeta ?: return
        if (CustomCraftingStation.hasCraftingStationMarker(itemMeta, plugin)) {
            event.player.sendMessage("You have placed the Custom Crafting Station!")
        }
    }
}
