package com.iseids.farmingUtils.food

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class RecipeIngredient(
    val material: Material,
    val amount: Int,
)

data class CustomRecipe(
    val id: String,
    val result: ItemStack,
    val ingredients: List<RecipeIngredient>,
    val foodPoints: Int,
    val customName: String,
    val displayName: String,
    val description: List<String>,
)
