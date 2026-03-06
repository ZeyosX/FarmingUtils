package com.iseids.farmingUtils

import org.bukkit.Material
import java.util.EnumSet

object Constants {
    val HOES: Set<Material> = enumSetOf(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.NETHERITE_HOE,
    ).withOptionalMaterials(
        "COPPER_HOE",
    )

    val AXES: Set<Material> = enumSetOf(
        Material.WOODEN_AXE,
        Material.STONE_AXE,
        Material.IRON_AXE,
        Material.GOLDEN_AXE,
        Material.DIAMOND_AXE,
        Material.NETHERITE_AXE,
    ).withOptionalMaterials(
        "COPPER_AXE",
    )

    val CROPS: Set<Material> = enumSetOf(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART,
    )

    val SUGAR_CANE_AND_BAMBOO: Set<Material> = enumSetOf(
        Material.SUGAR_CANE,
        Material.BAMBOO,
    )

    private fun enumSetOf(vararg values: Material): Set<Material> {
        return EnumSet.noneOf(Material::class.java).apply { values.forEach(::add) }
    }

    private fun Set<Material>.withOptionalMaterials(vararg names: String): Set<Material> {
        val result = EnumSet.copyOf(this)
        names.forEach { name ->
            materialOrNull(name)?.let(result::add)
        }
        return result
    }

    private fun materialOrNull(name: String): Material? {
        return runCatching { java.lang.Enum.valueOf(Material::class.java, name) }.getOrNull()
    }
}
