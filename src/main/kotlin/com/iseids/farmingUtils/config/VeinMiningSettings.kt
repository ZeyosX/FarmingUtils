package com.iseids.farmingUtils.config

import com.iseids.farmingUtils.Constants
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration

data class VeinMiningSettings(
    val enabled: Boolean = true,
    val requireSneaking: Boolean = true,
    val maxBlocks: Int = 2048,
    val mineableMaterials: Set<Material> = Constants.SUGAR_CANE_AND_BAMBOO,
) {
    fun supports(material: Material): Boolean = material in mineableMaterials

    companion object {
        fun from(config: FileConfiguration): VeinMiningSettings {
            val configuredMaterials = config.getStringList("vein-mining.materials")
                .mapNotNull(::parseMaterial)
                .toSet()
                .ifEmpty { Constants.SUGAR_CANE_AND_BAMBOO }

            return VeinMiningSettings(
                enabled = config.getBoolean("vein-mining.enabled", true),
                requireSneaking = config.getBoolean("vein-mining.require-sneaking", true),
                maxBlocks = config.getInt("vein-mining.max-blocks", 2048).coerceAtLeast(1),
                mineableMaterials = configuredMaterials,
            )
        }

        private fun parseMaterial(name: String): Material? {
            return runCatching { Material.valueOf(name.trim().uppercase()) }.getOrNull()
        }
    }
}
