package com.iseids.farmingUtils

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
        cropInteraction = CropInteraction()
        customFoodManager = CustomFoodManager(this)
        val customCraftingStation = CustomCraftingStation(this)

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
        sender.sendMessage("${ChatColor.GRAY}- Right-click sugar cane/bamboo with an axe to break connected stalks")
        sender.sendMessage("${ChatColor.GRAY}- Supports vanilla and copper hoes/axes when available on your server version")
        sender.sendMessage("${ChatColor.GRAY}- Adds a custom Crafting Station (Cookpot) with ingredient-based crafting")
        sender.sendMessage("${ChatColor.GRAY}- Custom foods restore extra hunger when consumed")
    }
}
