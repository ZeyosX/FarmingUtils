package com.iseids.farmingUtils.crops

import com.iseids.farmingUtils.extensions.isAxe
import com.iseids.farmingUtils.extensions.isCrop
import com.iseids.farmingUtils.extensions.isHoe
import com.iseids.farmingUtils.extensions.isSugarCaneOrBamboo
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class CropInteraction : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        handleRightClick(event.player, clickedBlock)
    }

    private fun handleRightClick(player: Player, block: Block) {
        val heldItem = player.inventory.itemInMainHand
        val heldType = heldItem.type

        if (heldType == Material.AIR && !block.type.isCrop()) {
            return
        }

        when {
            heldItem.isAxe() && block.isSugarCaneOrBamboo() -> {
                CropBreaker.breakConnectedCrops(block, heldItem, player)
            }

            heldItem.isHoe() -> {
                if (block.type == Material.FARMLAND) {
                    CropPlanter.plantConnectedFarmland(player, block, heldItem)
                    return
                }
                CropHarvester.harvestAndReplant(player, block, heldItem)
            }

            heldType == Material.AIR -> {
                CropHarvester.harvestAndReplant(player, block, heldItem, radius = 0)
            }
        }
    }
}
