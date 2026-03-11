package com.iseids.farmingUtils.food

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.PlayerInventory
import kotlin.math.min

class CustomCraftingListener(
    private val customFoodManager: CustomFoodManager,
    private val craftingStation: CustomCraftingStation,
    private val sounds: CookpotSoundSettings,
) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!CustomCraftingStation.isCraftingMenu(event.view.title)) {
            return
        }

        event.isCancelled = true
        val clickedItem = event.currentItem ?: return
        val displayName = clickedItem.itemMeta?.displayName
        if (displayName?.startsWith("§6Page ") == true) {
            return
        }
        when (displayName) {
            "§ePrevious Page" -> {
                sounds.menuOpen.play(player)
                craftingStation.openCraftingMenu(player, CustomCraftingStation.currentPageFromTitle(event.view.title) - 1)
                return
            }
            "§aNext Page" -> {
                sounds.menuOpen.play(player)
                craftingStation.openCraftingMenu(player, CustomCraftingStation.currentPageFromTitle(event.view.title) + 1)
                return
            }
        }
        val recipe = customFoodManager.getRecipeByItem(clickedItem) ?: return
        if (!hasIngredients(player.inventory, recipe.ingredients)) {
            sounds.craftFailure.play(player)
            player.sendMessage("§cYou do not have the required ingredients for ${recipe.customName}.")
            return
        }

        removeIngredients(player.inventory, recipe.ingredients)
        player.inventory.addItem(recipe.result.clone())
        sounds.craftSuccess.play(player)
        player.sendMessage("§aYou crafted ${recipe.displayName}§a!")
    }

    private fun hasIngredients(inventory: PlayerInventory, requirements: List<RecipeIngredient>): Boolean {
        if (requirements.isEmpty()) {
            return true
        }

        val remaining = aggregateRequirements(requirements)
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

    private fun removeIngredients(inventory: PlayerInventory, requirements: List<RecipeIngredient>) {
        val remaining = aggregateRequirements(requirements)
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

    private fun aggregateRequirements(requirements: List<RecipeIngredient>): MutableMap<Material, Int> {
        val remaining = linkedMapOf<Material, Int>()
        requirements.forEach { ingredient ->
            remaining[ingredient.material] = (remaining[ingredient.material] ?: 0) + ingredient.amount
        }
        return remaining
    }
}
