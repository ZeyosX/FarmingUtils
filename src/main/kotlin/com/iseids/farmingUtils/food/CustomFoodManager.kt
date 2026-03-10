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
            CustomRecipe(
                id = "farmer_salad",
                result = createCustomFoodItem(
                    id = "farmer_salad",
                    material = Material.BEETROOT_SOUP,
                    name = "Farmer Salad",
                    lore = listOf("A fresh bowl packed with garden vegetables."),
                ),
                ingredients = mapOf(
                    Material.BEETROOT to 2,
                    Material.CARROT to 1,
                    Material.BAKED_POTATO to 1,
                ),
                foodPoints = 7,
                customName = "Farmer Salad",
            ),
            CustomRecipe(
                id = "honey_glazed_chicken",
                result = createCustomFoodItem(
                    id = "honey_glazed_chicken",
                    material = Material.COOKED_CHICKEN,
                    name = "Honey Glazed Chicken",
                    lore = listOf("Sweet and savory roasted chicken."),
                ),
                ingredients = mapOf(
                    Material.COOKED_CHICKEN to 1,
                    Material.HONEY_BOTTLE to 1,
                ),
                foodPoints = 8,
                customName = "Honey Glazed Chicken",
            ),
            CustomRecipe(
                id = "hearty_stew",
                result = createCustomFoodItem(
                    id = "hearty_stew",
                    material = Material.RABBIT_STEW,
                    name = "Hearty Stew",
                    lore = listOf("A dense stew for long workdays in the fields."),
                ),
                ingredients = mapOf(
                    Material.COOKED_BEEF to 1,
                    Material.CARROT to 1,
                    Material.BAKED_POTATO to 1,
                    Material.BOWL to 1,
                ),
                foodPoints = 10,
                customName = "Hearty Stew",
            ),
            CustomRecipe(
                id = "berry_tart",
                result = createCustomFoodItem(
                    id = "berry_tart",
                    material = Material.SWEET_BERRIES,
                    name = "Berry Tart",
                    lore = listOf("A bright dessert with a crisp crust."),
                ),
                ingredients = mapOf(
                    Material.SWEET_BERRIES to 3,
                    Material.WHEAT to 2,
                    Material.SUGAR to 1,
                    Material.EGG to 1,
                ),
                foodPoints = 5,
                customName = "Berry Tart",
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

    fun getRecipeById(id: String): CustomRecipe? {
        return recipesById[id]
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
