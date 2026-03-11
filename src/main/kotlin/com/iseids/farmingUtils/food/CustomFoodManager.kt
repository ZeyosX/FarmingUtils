package com.iseids.farmingUtils.food

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.util.UUID

class CustomFoodManager(
    plugin: JavaPlugin,
    definitions: List<RecipeDefinition>,
    private val sounds: CookpotSoundSettings,
) : Listener {
    private val recipeIdKey: NamespacedKey = NamespacedKey(plugin, "custom_food_recipe_id")
    private val customRecipes: List<CustomRecipe>
    private val recipesByName: Map<String, CustomRecipe>
    private val recipesById: Map<String, CustomRecipe>

    init {
        val recipes = definitions.map(::createRecipe)
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
        if (event.item.type != Material.PLAYER_HEAD) {
            restoreFoodPoints(event.player, recipe.foodPoints)
        }
        sounds.consumeBonus.play(event.player)
    }

    private fun restoreFoodPoints(player: Player, foodPoints: Int) {
        player.foodLevel = (player.foodLevel + foodPoints).coerceAtMost(20)
    }

    private fun createRecipe(definition: RecipeDefinition): CustomRecipe {
        val item = createCustomFoodItem(definition)
        val customName = org.bukkit.ChatColor.stripColor(definition.displayName) ?: definition.id

        return CustomRecipe(
            id = definition.id,
            result = item,
            ingredients = definition.ingredients,
            foodPoints = definition.foodPoints,
            customName = customName,
            displayName = definition.displayName,
            description = definition.description,
        )
    }

    private fun createCustomFoodItem(definition: RecipeDefinition): ItemStack {
        val material = if (definition.textureUrl != null) Material.PLAYER_HEAD else definition.resultMaterial
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        meta.setDisplayName(definition.displayName)
        meta.lore = buildList {
            addAll(definition.description)
            add("")
            add("§6Restores §e${definition.foodPoints} hunger")
        }
        meta.persistentDataContainer.set(recipeIdKey, PersistentDataType.STRING, definition.id)
        applyHeadTexture(meta, definition)
        makeConsumable(meta, definition.foodPoints, definition.textureUrl != null)
        item.itemMeta = meta
        return item
    }

    private fun applyHeadTexture(meta: ItemMeta, definition: RecipeDefinition) {
        val textureUrl = definition.textureUrl ?: return
        val skullMeta = meta as? SkullMeta ?: return
        skullMeta.ownerProfile = Bukkit.createPlayerProfile(
            UUID.nameUUIDFromBytes(textureUrl.toByteArray()),
            definition.customProfileName(),
        ).apply {
            val textures = textures
            textures.skin = URI.create(textureUrl).toURL()
            setTextures(textures)
        }
    }

    private fun makeConsumable(meta: ItemMeta, foodPoints: Int, forceCustomFood: Boolean) {
        if (!forceCustomFood) {
            return
        }

        runCatching {
            val food = meta.invokeNoArg("getFood") ?: return@runCatching
            food.invokeSingleArg("setNutrition", foodPoints)
            food.invokeSingleArg("setSaturation", foodPoints / 2f)
            meta.invokeMatchingSingleArg("setFood", food)

            val consumable = meta.invokeNoArg("getConsumable") ?: return@runCatching
            consumable.invokeSingleArg("setConsumeSeconds", 1.2f)
            consumable.invokeSingleArg("setSound", sounds.consumeBonus.sound)
            meta.invokeMatchingSingleArg("setConsumable", consumable)
        }
    }

    private fun RecipeDefinition.customProfileName(): String {
        return customHeadProfileName(displayName)
    }

    private fun customHeadProfileName(displayName: String): String {
        return (org.bukkit.ChatColor.stripColor(displayName) ?: "CookpotItem")
            .replace(" ", "")
            .take(16)
            .ifBlank { "CookpotItem" }
    }

    private fun Any.invokeNoArg(methodName: String): Any? {
        return javaClass.methods
            .firstOrNull { it.name == methodName && it.parameterCount == 0 }
            ?.invoke(this)
    }

    private fun Any.invokeSingleArg(methodName: String, value: Any) {
        javaClass.methods
            .firstOrNull { method ->
                method.name == methodName &&
                    method.parameterCount == 1 &&
                    wrapPrimitive(method.parameterTypes[0]).isAssignableFrom(wrapPrimitive(value.javaClass))
            }
            ?.invoke(this, value)
    }

    private fun Any.invokeMatchingSingleArg(methodName: String, value: Any) {
        javaClass.methods
            .firstOrNull { method ->
                method.name == methodName &&
                    method.parameterCount == 1 &&
                    method.parameterTypes[0].isInstance(value)
            }
            ?.invoke(this, value)
    }

    private fun wrapPrimitive(type: Class<*>): Class<*> {
        return when (type) {
            Integer.TYPE -> Integer::class.java
            java.lang.Float.TYPE -> java.lang.Float::class.java
            java.lang.Double.TYPE -> java.lang.Double::class.java
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            java.lang.Short.TYPE -> java.lang.Short::class.java
            java.lang.Byte.TYPE -> java.lang.Byte::class.java
            Character.TYPE -> Character::class.java
            else -> type
        }
    }
}
