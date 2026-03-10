package com.iseids.farmingUtils.food

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.WeakHashMap

class CustomCraftingStation(
    private val plugin: JavaPlugin,
    private val customFoodManager: CustomFoodManager,
) : Listener {
    init {
        addCustomBlockRecipe()
    }

    private fun addCustomBlockRecipe() {
        val recipeKey = NamespacedKey(plugin, "custom_crafting_station")
        if (Bukkit.getRecipe(recipeKey) != null) {
            return
        }

        val recipe = ShapedRecipe(recipeKey, createCraftingStationBlock())
            .shape(" A ", "WSW", " W ")
            .setIngredient('A', Material.APPLE)
            .setIngredient('W', Material.WHEAT)
            .setIngredient('S', Material.SUGAR)

        Bukkit.addRecipe(recipe)
    }

    fun createCraftingStationBlock(): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as? SkullMeta ?: return item

        meta.setDisplayName("Cookpot")
        meta.lore = listOf("Head ID: 1668", "Custom Crafting Station")
        markAsCraftingStation(meta.persistentDataContainer, plugin)

        item.itemMeta = meta
        return item
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val itemMeta = event.itemInHand.itemMeta ?: return
        if (!hasCraftingStationMarker(itemMeta, plugin)) {
            return
        }

        val tileState = event.blockPlaced.state as? TileState ?: return
        markAsCraftingStation(tileState.persistentDataContainer, plugin)
        tileState.update(true)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        if (!isCustomCraftingStation(clickedBlock, plugin)) {
            return
        }

        event.isCancelled = true
        openCraftingMenu(event.player)
    }

    private fun openCraftingMenu(player: Player) {
        player.openInventory(createCraftingInventory())
    }

    private fun createCraftingInventory(): Inventory {
        val recipes = customFoodManager.getRecipes()
        val inventorySize = ((recipes.size + 8) / 9).coerceAtLeast(1) * 9
        val inventory = Bukkit.createInventory(null, inventorySize, CRAFTING_MENU_TITLE)

        recipes.forEachIndexed { index, recipe ->
            inventory.setItem(index, createMenuItem(recipe))
        }

        return inventory
    }

    private fun createMenuItem(recipe: CustomRecipe): ItemStack {
        val item = recipe.result.clone()
        val meta = item.itemMeta ?: return item
        meta.lore = buildList {
            add("${INFO_PREFIX}Ingredients:")
            recipe.ingredients.entries
                .sortedBy { it.key.name }
                .forEach { (material, amount) ->
                    add("${INFO_PREFIX}- ${formatMaterialName(material)} x$amount")
                }
            add("${INFO_PREFIX}Restores ${recipe.foodPoints} hunger")
            add("${INFO_PREFIX}Click to craft")
        }
        item.itemMeta = meta
        return item
    }

    private fun formatMaterialName(material: Material): String {
        return material.name
            .lowercase()
            .split('_')
            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    companion object {
        const val CRAFTING_MENU_TITLE: String = "Custom Crafting"

        private const val STATION_MARKER_KEY = "custom_crafting_station_key"
        private const val STATION_MARKER_VALUE = "unique_id_1234"
        private const val INFO_PREFIX = "\u00A77"
        private val KEY_CACHE: MutableMap<Plugin, NamespacedKey> = WeakHashMap()

        fun isCustomCraftingStation(block: Block?, plugin: Plugin): Boolean {
            if (block == null || block.type != Material.PLAYER_HEAD) {
                return false
            }

            val tileState = block.state as? TileState ?: return false
            val key = stationKey(plugin)
            return tileState.persistentDataContainer.get(key, PersistentDataType.STRING) == STATION_MARKER_VALUE
        }

        fun hasCraftingStationMarker(itemMeta: ItemMeta, plugin: Plugin): Boolean {
            val key = stationKey(plugin)
            return itemMeta.persistentDataContainer.get(key, PersistentDataType.STRING) == STATION_MARKER_VALUE
        }

        fun markAsCraftingStation(container: PersistentDataContainer, plugin: Plugin) {
            val key = stationKey(plugin)
            container.set(key, PersistentDataType.STRING, STATION_MARKER_VALUE)
        }

        private fun stationKey(plugin: Plugin): NamespacedKey {
            return KEY_CACHE.getOrPut(plugin) { NamespacedKey(plugin, STATION_MARKER_KEY) }
        }
    }
}
