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

class CustomCraftingStation(private val plugin: JavaPlugin) : Listener {
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

    companion object {
        const val CRAFTING_MENU_TITLE: String = "Custom Crafting"
        const val RESULT_SLOT: Int = 8

        private const val STATION_MARKER_KEY = "custom_crafting_station_key"
        private const val STATION_MARKER_VALUE = "unique_id_1234"
        private const val INFO_PREFIX = "\u00A77"
        private val KEY_CACHE: MutableMap<Plugin, NamespacedKey> = WeakHashMap()

        private val BEEF_ICON: ItemStack by lazy {
            ItemStack(Material.BEEF).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("${INFO_PREFIX}Ingredient: Beef")
                }
            }
        }

        private val POTATO_ICON: ItemStack by lazy {
            ItemStack(Material.BAKED_POTATO).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("${INFO_PREFIX}Ingredient: Baked Potato")
                }
            }
        }

        private val CRAFT_BUTTON: ItemStack by lazy {
            ItemStack(Material.COOKED_BEEF).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("Craft Steak and Chips")
                    lore = listOf(
                        "${INFO_PREFIX}Consumes ingredients from your inventory.",
                    )
                }
            }
        }

        fun createCraftingInventory(): Inventory {
            val inventory = Bukkit.createInventory(null, 9, CRAFTING_MENU_TITLE)
            inventory.setItem(0, BEEF_ICON.clone())
            inventory.setItem(1, POTATO_ICON.clone())
            inventory.setItem(RESULT_SLOT, CRAFT_BUTTON.clone())
            return inventory
        }

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
