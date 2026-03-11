package com.iseids.farmingUtils

import com.iseids.farmingUtils.config.VeinMiningSettings
import com.iseids.farmingUtils.crops.CropInteraction
import com.iseids.farmingUtils.food.CustomBlockListener
import com.iseids.farmingUtils.food.CustomCraftingListener
import com.iseids.farmingUtils.food.CustomCraftingStation
import com.iseids.farmingUtils.food.CustomFoodManager
import com.iseids.farmingUtils.food.CookpotSettings
import com.iseids.farmingUtils.food.CookpotSettingsLoader
import com.iseids.farmingUtils.food.CustomRecipe
import com.iseids.farmingUtils.food.formatMaterialName
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

open class FarmingUtils : JavaPlugin() {
    private lateinit var cropInteraction: CropInteraction
    private lateinit var customFoodManager: CustomFoodManager
    private lateinit var cookpotSettings: CookpotSettings

    override fun onEnable() {
        saveDefaultConfig()
        config.options().copyDefaults(true)
        saveConfig()

        val veinMiningSettings = VeinMiningSettings.from(config)
        cookpotSettings = CookpotSettingsLoader.load(this)

        cropInteraction = CropInteraction(veinMiningSettings)
        customFoodManager = CustomFoodManager(this, cookpotSettings.recipes, cookpotSettings.sounds)
        val customCraftingStation = CustomCraftingStation(this, customFoodManager, cookpotSettings)

        server.pluginManager.registerEvents(cropInteraction, this)
        server.pluginManager.registerEvents(customFoodManager, this)
        server.pluginManager.registerEvents(customCraftingStation, this)
        server.pluginManager.registerEvents(CustomCraftingListener(customFoodManager, customCraftingStation, cookpotSettings.sounds), this)
        server.pluginManager.registerEvents(CustomBlockListener(this, cookpotSettings), this)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (!command.name.equals("farmingutils", ignoreCase = true)) {
            return false
        }

        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            sendHelp(sender)
            return true
        }

        sender.sendMessage("${ChatColor.RED}Unknown subcommand. Use /$label help")
        return true
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== FarmingUtils Help ===")
        sender.sendMessage("${ChatColor.YELLOW}/farmingutils help ${ChatColor.GRAY}- Show this help menu")
        sender.sendMessage("${ChatColor.AQUA}What this plugin does:")
        sender.sendMessage("${ChatColor.GRAY}- Right-click mature crops with a hoe to harvest and replant")
        sender.sendMessage("${ChatColor.GRAY}- Right-click farmland with a hoe to plant connected farmland automatically")
        sender.sendMessage("${ChatColor.GRAY}- Mine sugar cane/bamboo with an axe while crouching to vein mine connected stalks")
        sender.sendMessage("${ChatColor.GRAY}- Supports vanilla and copper hoes/axes when available on your server version")
        sender.sendMessage("${ChatColor.GRAY}- Adds a custom Crafting Station (Cookpot) with ingredient-based crafting")
        sender.sendMessage("${ChatColor.GRAY}- Custom foods restore extra hunger when consumed")
        sender.sendMessage("${ChatColor.GRAY}- Cookpot recipes, sounds, colors, and head dishes are configurable")
        sender.sendMessage("${ChatColor.DARK_GRAY}- Vein mining can be configured in config.yml")
        sender.sendMessage("${ChatColor.AQUA}Cookpot crafting recipe:")
        cookpotSettings.station.recipeDisplayLines("${ChatColor.GRAY}").forEach(sender::sendMessage)
        sender.sendMessage("${ChatColor.AQUA}Cookpot recipes (${customFoodManager.getRecipes().size} total):")
        customFoodManager.getRecipes().take(cookpotSettings.helpPreviewLimit).forEach { recipe ->
            sender.sendMessage("${ChatColor.GRAY}- ${recipe.customName}: ${formatIngredients(recipe)}")
        }
        val hiddenRecipeCount = customFoodManager.getRecipes().size - cookpotSettings.helpPreviewLimit
        if (hiddenRecipeCount > 0) {
            sender.sendMessage("${ChatColor.DARK_GRAY}- ...and $hiddenRecipeCount more in the Cookpot menu")
        }
        sender.sendMessage("${ChatColor.AQUA}How to use the Cookpot:")
        sender.sendMessage("${ChatColor.GRAY}1. Craft the Cookpot using the recipe shown above.")
        sender.sendMessage("${ChatColor.GRAY}2. Place it down and right-click it to open the recipe menu.")
        sender.sendMessage("${ChatColor.GRAY}3. Hover the food you want to see the required ingredients.")
        sender.sendMessage("${ChatColor.GRAY}4. Keep the ingredients in your inventory, then click the recipe to craft it.")
    }

    private fun formatIngredients(recipe: CustomRecipe): String {
        return recipe.ingredients.joinToString(" + ") { ingredient ->
            val materialName = formatMaterialName(ingredient.material)
            if (ingredient.amount == 1) materialName else "${ingredient.amount} $materialName"
        }
    }
}
