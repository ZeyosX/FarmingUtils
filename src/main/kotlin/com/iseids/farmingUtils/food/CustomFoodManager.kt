package com.iseids.farmingUtils.food

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class CustomFoodManager(plugin: Plugin) : Listener {
    private val recipeIdKey: NamespacedKey = NamespacedKey(plugin, "custom_food_recipe_id")
    private val customRecipes: List<CustomRecipe>
    private val recipesByName: Map<String, CustomRecipe>
    private val recipesById: Map<String, CustomRecipe>

    init {
        val recipes = listOf(
            CustomRecipe(
                id = "steak_and_chips",
                result = createCustomFoodItem(
                    id = "steak_and_chips",
                    material = Material.COOKED_BEEF,
                    name = "Steak and Chips",
                    lore = listOf("A delicious combination of steak and chips."),
                ),
                ingredients = mapOf(
                    Material.BEEF to 1,
                    Material.BAKED_POTATO to 1,
                ),
                foodPoints = 8,
                customName = "Steak and Chips",
            ),
            CustomRecipe(
                id = "msa7ab_italy_sandwich",
                result = createCustomFoodItem(
                    id = "msa7ab_italy_sandwich",
                    material = Material.COOKED_CHICKEN,
                    name = "Msa7ab Italy Sandwich",
                    lore = listOf("A delicious sandwich with chicken and bread."),
                ),
                ingredients = mapOf(
                    Material.COOKED_CHICKEN to 1,
                    Material.BREAD to 1,
                ),
                foodPoints = 6,
                customName = "Msa7ab Italy Sandwich",
            ),
        )

        customRecipes = recipes
        recipesByName = recipes.associateBy { it.customName.lowercase() }
        recipesById = recipes.associateBy { it.id }
    }

    fun getRecipeByItem(item: ItemStack): CustomRecipe? {
        val recipeId = item.itemMeta
            ?.persistentDataContainer
            ?.get(recipeIdKey, PersistentDataType.STRING)
            ?: return null
        return recipesById[recipeId]
    }

    fun getRecipeByName(name: String): CustomRecipe? {
        return recipesByName[name.lowercase()]
    }

    fun getRecipes(): List<CustomRecipe> {
        return customRecipes
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerConsume(event: PlayerItemConsumeEvent) {
        val recipe = getRecipeByItem(event.item) ?: return
        restoreFoodPoints(event.player, recipe.foodPoints)
    }

    private fun restoreFoodPoints(player: Player, foodPoints: Int) {
        player.foodLevel = (player.foodLevel + foodPoints).coerceAtMost(20)
    }

    private fun createCustomFoodItem(id: String, material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(name)
        meta.lore = lore
        meta.persistentDataContainer.set(recipeIdKey, PersistentDataType.STRING, id)
        item.itemMeta = meta
        return item
    }
}
