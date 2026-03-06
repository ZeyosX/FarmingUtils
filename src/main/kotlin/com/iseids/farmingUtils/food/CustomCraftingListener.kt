package com.iseids.farmingUtils.food

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.PlayerInventory
import kotlin.math.min

class CustomCraftingListener(private val customFoodManager: CustomFoodManager) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.title != CustomCraftingStation.CRAFTING_MENU_TITLE) {
            return
        }

        event.isCancelled = true
        if (event.rawSlot != CustomCraftingStation.RESULT_SLOT) {
            return
        }

        val recipe = customFoodManager.getRecipeByName("Steak and Chips") ?: return
        if (!hasIngredients(player.inventory, recipe.ingredients)) {
            player.sendMessage("You do not have the required ingredients.")
            return
        }

        removeIngredients(player.inventory, recipe.ingredients)
        player.inventory.addItem(recipe.result.clone())
        player.sendMessage("You crafted ${recipe.customName}!")
    }

    private fun hasIngredients(inventory: PlayerInventory, requirements: Map<Material, Int>): Boolean {
        if (requirements.isEmpty()) {
            return true
        }

        val remaining: MutableMap<Material, Int> = requirements.toMutableMap()
        for (item in inventory.storageContents) {
            val stack = item ?: continue
            val needed = remaining[stack.type] ?: continue
            val left = needed - stack.amount
            if (left <= 0) {
                remaining.remove(stack.type)
            } else {
                remaining[stack.type] = left
            }

            if (remaining.isEmpty()) {
                return true
            }
        }

        return remaining.isEmpty()
    }

    private fun removeIngredients(inventory: PlayerInventory, requirements: Map<Material, Int>) {
        val remaining: MutableMap<Material, Int> = requirements.toMutableMap()
        val contents = inventory.storageContents

        for (slot in contents.indices) {
            val stack = contents[slot] ?: continue
            val needed = remaining[stack.type] ?: continue
            if (needed <= 0) {
                continue
            }

            val taken = min(stack.amount, needed)
            val after = stack.amount - taken

            if (after <= 0) {
                contents[slot] = null
            } else {
                stack.amount = after
                contents[slot] = stack
            }

            val stillNeeded = needed - taken
            if (stillNeeded <= 0) {
                remaining.remove(stack.type)
            } else {
                remaining[stack.type] = stillNeeded
            }

            if (remaining.isEmpty()) {
                break
            }
        }

        inventory.storageContents = contents
    }
}
