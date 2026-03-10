package com.iseids.farmingUtils

import com.iseids.farmingUtils.config.VeinMiningSettings
import com.iseids.farmingUtils.crops.CropInteraction
import com.iseids.farmingUtils.food.CustomBlockListener
import com.iseids.farmingUtils.food.CustomCraftingListener
import com.iseids.farmingUtils.food.CustomCraftingStation
import com.iseids.farmingUtils.food.CustomFoodManager
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

class FarmingUtils : JavaPlugin() {
    private lateinit var cropInteraction: CropInteraction
    private lateinit var customFoodManager: CustomFoodManager

    override fun onEnable() {
        saveDefaultConfig()

        val veinMiningSettings = VeinMiningSettings.from(config)

        cropInteraction = CropInteraction(veinMiningSettings)
        customFoodManager = CustomFoodManager(this)
        val customCraftingStation = CustomCraftingStation(this, customFoodManager)

        server.pluginManager.registerEvents(cropInteraction, this)
        server.pluginManager.registerEvents(customFoodManager, this)
        server.pluginManager.registerEvents(customCraftingStation, this)
        server.pluginManager.registerEvents(CustomCraftingListener(customFoodManager), this)
        server.pluginManager.registerEvents(CustomBlockListener(this), this)
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
        sender.sendMessage("${ChatColor.DARK_GRAY}- Vein mining can be configured in config.yml")
        sender.sendMessage("${ChatColor.AQUA}Cookpot recipes:")
        sender.sendMessage("${ChatColor.GRAY}- Steak and Chips: Beef + Baked Potato")
        sender.sendMessage("${ChatColor.GRAY}- Msa7ab Italy Sandwich: Cooked Chicken + Bread")
        sender.sendMessage("${ChatColor.GRAY}- Farmer Salad: 2 Beetroot + Carrot + Baked Potato")
        sender.sendMessage("${ChatColor.GRAY}- Honey Glazed Chicken: Cooked Chicken + Honey Bottle")
        sender.sendMessage("${ChatColor.GRAY}- Hearty Stew: Cooked Beef + Carrot + Baked Potato + Bowl")
        sender.sendMessage("${ChatColor.GRAY}- Berry Tart: 3 Sweet Berries + 2 Wheat + Sugar + Egg")
        sender.sendMessage("${ChatColor.AQUA}How to use the Cookpot:")
        sender.sendMessage("${ChatColor.GRAY}1. Craft the Cookpot with Apple, Wheat, and Sugar.")
        sender.sendMessage("${ChatColor.GRAY}2. Place it down and right-click it to open the recipe menu.")
        sender.sendMessage("${ChatColor.GRAY}3. Hover the food you want to see the required ingredients.")
        sender.sendMessage("${ChatColor.GRAY}4. Keep the ingredients in your inventory, then click the recipe to craft it.")
    }
}
