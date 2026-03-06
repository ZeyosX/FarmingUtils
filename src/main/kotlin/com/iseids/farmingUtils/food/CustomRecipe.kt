package com.iseids.farmingUtils.food

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class CustomRecipe(
    val id: String,
    val result: ItemStack,
    val ingredients: Map<Material, Int>,
    val foodPoints: Int,
    val customName: String,
)
