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
import java.net.URL
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.ceil
import kotlin.math.min

class CustomCraftingStation(
    private val plugin: JavaPlugin,
    private val customFoodManager: CustomFoodManager,
    private val settings: CookpotSettings,
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
            .shape(*settings.station.recipeShape.toTypedArray())

        settings.station.recipeIngredients.forEach { (symbol, material) ->
            recipe.setIngredient(symbol, material)
        }

        Bukkit.addRecipe(recipe)
    }

    fun createCraftingStationBlock(): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as? SkullMeta ?: return item

        meta.setDisplayName(settings.station.displayName)
        meta.lore = settings.station.lore
        meta.setOwnerProfile(createProfile(settings.station.textureUrl))
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
        settings.sounds.menuOpen.play(event.player)
        openCraftingMenu(event.player, 0)
    }

    fun openCraftingMenu(player: Player, page: Int) {
        player.openInventory(createCraftingInventory(page))
    }

    private fun createCraftingInventory(page: Int): Inventory {
        val recipes = customFoodManager.getRecipes()
        val totalPages = totalPages(recipes.size)
        val currentPage = page.coerceIn(0, totalPages - 1)
        val inventory = Bukkit.createInventory(null, MENU_SIZE, menuTitle(currentPage, totalPages))
        val startIndex = currentPage * RECIPES_PER_PAGE
        val endIndex = min(startIndex + RECIPES_PER_PAGE, recipes.size)

        recipes.subList(startIndex, endIndex).forEachIndexed { index, recipe ->
            inventory.setItem(index, createMenuItem(recipe))
        }

        if (totalPages > 1) {
            if (currentPage > 0) {
                inventory.setItem(PREVIOUS_PAGE_SLOT, navigationItem("§ePrevious Page"))
            }
            inventory.setItem(PAGE_INFO_SLOT, navigationItem("§6Page ${currentPage + 1}/$totalPages"))
            if (currentPage < totalPages - 1) {
                inventory.setItem(NEXT_PAGE_SLOT, navigationItem("§aNext Page"))
            }
        }

        return inventory
    }

    private fun createMenuItem(recipe: CustomRecipe): ItemStack {
        val item = recipe.result.clone()
        val meta = item.itemMeta ?: return item
        meta.lore = buildList {
            addAll(recipe.description)
            add("")
            add("${INFO_PREFIX}Ingredients:")
            recipe.ingredients.forEach { ingredient ->
                add("${INFO_PREFIX}- ${formatMaterialName(ingredient.material)} x${ingredient.amount}")
            }
            add("")
            add("§6Restores §e${recipe.foodPoints} hunger")
            add("§aClick to craft")
        }
        item.itemMeta = meta
        return item
    }

    companion object {
        const val CRAFTING_MENU_TITLE: String = "Custom Crafting"
        const val MENU_SIZE: Int = 54
        const val RECIPES_PER_PAGE: Int = 45
        const val PREVIOUS_PAGE_SLOT: Int = 45
        const val PAGE_INFO_SLOT: Int = 49
        const val NEXT_PAGE_SLOT: Int = 53

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

        fun isCraftingMenu(title: String): Boolean {
            return title.startsWith(CRAFTING_MENU_TITLE)
        }

        fun currentPageFromTitle(title: String): Int {
            val pageToken = Regex("""\((\d+)/(\d+)\)$""").find(title)?.groupValues?.getOrNull(1)
            return (pageToken?.toIntOrNull() ?: 1) - 1
        }

        private fun totalPages(recipeCount: Int): Int {
            return ceil(recipeCount / RECIPES_PER_PAGE.toDouble()).toInt().coerceAtLeast(1)
        }

        private fun menuTitle(page: Int, totalPages: Int): String {
            return "$CRAFTING_MENU_TITLE (${page + 1}/$totalPages)"
        }

        private fun navigationItem(name: String): ItemStack {
            val item = ItemStack(Material.ARROW)
            val meta = item.itemMeta ?: return item
            meta.setDisplayName(name)
            item.itemMeta = meta
            return item
        }

        private fun createProfile(textureUrl: String) = Bukkit.createPlayerProfile(
            UUID.nameUUIDFromBytes(textureUrl.toByteArray()),
            "Cookpot",
        ).apply {
            val textures = textures
            textures.skin = URL(textureUrl)
            setTextures(textures)
        }
    }
}
