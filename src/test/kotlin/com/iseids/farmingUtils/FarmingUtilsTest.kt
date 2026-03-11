package com.iseids.farmingUtils

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import com.iseids.farmingUtils.food.CustomCraftingStation
import com.iseids.farmingUtils.food.CustomFoodManager
import com.iseids.farmingUtils.food.CookpotSettingsLoader
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FarmingUtilsTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: FarmingUtils
    private lateinit var world: World

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(FarmingUtils::class.java)
        world = server.addSimpleWorld("vein-test")
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `plugin loads vein mining defaults from config`() {
        assertTrue(plugin.config.getBoolean("vein-mining.enabled"))
        assertTrue(plugin.config.getBoolean("vein-mining.require-sneaking"))
        assertTrue(plugin.config.getInt("vein-mining.max-blocks") > 0)
        assertEquals(listOf("SUGAR_CANE", "BAMBOO"), plugin.config.getStringList("vein-mining.materials"))
    }

    @Test
    fun `help command documents crouch vein mining`() {
        val player = server.addPlayer()

        assertTrue(server.dispatchCommand(player, "farmingutils help"))

        player.assertSaid("§6=== FarmingUtils Help ===")
        player.assertSaid("§e/farmingutils help §7- Show this help menu")
        player.assertSaid("§bWhat this plugin does:")
        player.assertSaid("§7- Right-click mature crops with a hoe to harvest and replant")
        player.assertSaid("§7- Right-click farmland with a hoe to plant connected farmland automatically")
        player.assertSaid("§7- Mine sugar cane/bamboo with an axe while crouching to vein mine connected stalks")
        player.assertSaid("§7- Supports vanilla and copper hoes/axes when available on your server version")
        player.assertSaid("§7- Adds a custom Crafting Station (Cookpot) with ingredient-based crafting")
        player.assertSaid("§7- Custom foods restore extra hunger when consumed")
        player.assertSaid("§8- Vein mining can be configured in config.yml")
        player.assertSaid("§bCookpot crafting recipe:")
        player.assertSaid("§7[ ] [A] [ ]")
        player.assertSaid("§7[W] [S] [W]")
        player.assertSaid("§7[ ] [W] [ ]")
        player.assertSaid("§7A = Apple, W = Wheat, S = Sugar")
        player.assertSaid("§bCookpot recipes (56 total):")
        player.assertSaid("§7- Steak and Chips: Beef + Baked Potato")
        player.assertSaid("§7- Msa7ab Italy Sandwich: Cooked Chicken + Bread")
        player.assertSaid("§7- Farmer Salad: 2 Beetroot + Carrot + Baked Potato")
        player.assertSaid("§7- Honey Glazed Chicken: Cooked Chicken + Honey Bottle")
        player.assertSaid("§7- Hearty Stew: Cooked Beef + Carrot + Baked Potato + Bowl")
        player.assertSaid("§7- Berry Tart: 3 Sweet Berries + 2 Wheat + Sugar + Egg")
        player.assertSaid("§8- ...and 44 more in the Cookpot menu")
        player.assertSaid("§bHow to use the Cookpot:")
        player.assertSaid("§71. Craft the Cookpot using the recipe shown above.")
        player.assertSaid("§72. Place it down and right-click it to open the recipe menu.")
        player.assertSaid("§73. Hover the food you want to see the required ingredients.")
        player.assertSaid("§74. Keep the ingredients in your inventory, then click the recipe to craft it.")
    }

    @Test
    fun `custom food manager exposes multiple cookpot recipes`() {
        val settings = CookpotSettingsLoader.load(plugin)
        val foodManager = CustomFoodManager(plugin, settings.recipes, settings.sounds)
        val recipes = foodManager.getRecipes()

        assertEquals(56, recipes.size)
        assertTrue(recipes.any { it.id == "steak_and_chips" })
        assertTrue(recipes.any { it.id == "msa7ab_italy_sandwich" })
        assertTrue(recipes.any { it.id == "farmer_salad" })
        assertTrue(recipes.any { it.id == "honey_glazed_chicken" })
        assertTrue(recipes.any { it.id == "hearty_stew" })
        assertTrue(recipes.any { it.id == "berry_tart" })
        assertTrue(recipes.any { it.id == "vanilla_cake" })
        assertTrue(recipes.any { it.id == "cheese_burger" })
        assertTrue(recipes.any { it.id == "margherita_pizza" })
        assertEquals("Farmer Salad", foodManager.getRecipeById("farmer_salad")?.customName)
        assertEquals("Honey Glazed Chicken", foodManager.getRecipeByItem(
            foodManager.getRecipeById("honey_glazed_chicken")!!.result,
        )?.customName)
    }

    @Test
    fun `cookpot block uses the requested custom player head`() {
        val settings = CookpotSettingsLoader.load(plugin)
        val foodManager = CustomFoodManager(plugin, settings.recipes, settings.sounds)
        val station = CustomCraftingStation(plugin, foodManager, settings)
        val item = station.createCraftingStationBlock()
        val meta = item.itemMeta as SkullMeta

        assertEquals(Material.PLAYER_HEAD, item.type)
        assertEquals(
            listOf(
                "§7A festive crafting station for hearty meals and sweets.",
                "§eRight-click to browse the full cookpot menu.",
                "§8Head ID: 35970",
            ),
            meta.lore,
        )
        assertEquals("https://textures.minecraft.net/texture/1591b61529d25a7ecd6bec00948e6fe155e3007f2d7fe559f3a83c6f808e434d", meta.ownerProfile?.textures?.skin?.toString())
        assertTrue(CustomCraftingStation.hasCraftingStationMarker(meta, plugin))
    }

    @Test
    fun `head based recipes use custom player head textures`() {
        val settings = CookpotSettingsLoader.load(plugin)
        val foodManager = CustomFoodManager(plugin, settings.recipes, settings.sounds)
        val vanillaCake = foodManager.getRecipeById("vanilla_cake")!!.result
        val burger = foodManager.getRecipeById("cheese_burger")!!.result

        assertEquals(Material.PLAYER_HEAD, vanillaCake.type)
        assertEquals(Material.PLAYER_HEAD, burger.type)
        assertEquals(
            "https://textures.minecraft.net/texture/8588dd1f74385c9c84a1872b1eb209fe3808efd4cb78ea6330b505321cdb920e",
            (vanillaCake.itemMeta as SkullMeta).ownerProfile?.textures?.skin?.toString(),
        )
        assertEquals(
            "https://textures.minecraft.net/texture/77d4341144c2c5eeb1a22a237d1a03946b4049750449d08cc74f5847edb50eb4",
            (burger.itemMeta as SkullMeta).ownerProfile?.textures?.skin?.toString(),
        )
    }

    @Test
    fun `vein mining only triggers while sneaking`() {
        val player = prepareMiner()
        val targetBlock = world.getBlockAt(0, 64, 0)
        world.getBlockAt(0, 64, 0).type = Material.SUGAR_CANE
        world.getBlockAt(0, 65, 0).type = Material.SUGAR_CANE
        world.getBlockAt(1, 64, 0).type = Material.SUGAR_CANE

        player.isSneaking = false
        val normalBreakEvent = player.simulateBlockBreak(targetBlock)
        assertNotNull(normalBreakEvent)
        assertFalse(normalBreakEvent.isCancelled)
        assertEquals(Material.AIR, world.getBlockAt(0, 64, 0).type)
        assertEquals(Material.SUGAR_CANE, world.getBlockAt(0, 65, 0).type)
        assertEquals(Material.SUGAR_CANE, world.getBlockAt(1, 64, 0).type)

        world.getBlockAt(0, 64, 0).type = Material.SUGAR_CANE
        world.getBlockAt(0, 65, 0).type = Material.SUGAR_CANE
        world.getBlockAt(1, 64, 0).type = Material.SUGAR_CANE

        player.isSneaking = true
        val veinMineEvent = player.simulateBlockBreak(targetBlock)
        assertNotNull(veinMineEvent)
        assertTrue(veinMineEvent.isCancelled)
        assertEquals(Material.AIR, world.getBlockAt(0, 64, 0).type)
        assertEquals(Material.AIR, world.getBlockAt(0, 65, 0).type)
        assertEquals(Material.AIR, world.getBlockAt(1, 64, 0).type)
    }

    private fun prepareMiner(): PlayerMock {
        return server.addPlayer().also { player ->
            player.inventory.setItemInMainHand(ItemStack(Material.IRON_AXE))
        }
    }
}
