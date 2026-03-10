package com.iseids.farmingUtils.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VeinMiningSettingsTest {
    @Test
    fun `loads configured values and ignores invalid materials`() {
        val config = YamlConfiguration()
        config.set("vein-mining.enabled", false)
        config.set("vein-mining.require-sneaking", false)
        config.set("vein-mining.max-blocks", 64)
        config.set("vein-mining.materials", listOf("sugar_cane", "not_a_material", "bamboo"))

        val settings = VeinMiningSettings.from(config)

        assertFalse(settings.enabled)
        assertFalse(settings.requireSneaking)
        assertEquals(64, settings.maxBlocks)
        assertEquals(setOf(Material.SUGAR_CANE, Material.BAMBOO), settings.mineableMaterials)
    }

    @Test
    fun `falls back to defaults for invalid block caps and empty material lists`() {
        val config = YamlConfiguration()
        config.set("vein-mining.max-blocks", 0)
        config.set("vein-mining.materials", listOf("still_invalid"))

        val settings = VeinMiningSettings.from(config)

        assertTrue(settings.enabled)
        assertTrue(settings.requireSneaking)
        assertEquals(1, settings.maxBlocks)
        assertTrue(settings.supports(Material.SUGAR_CANE))
        assertTrue(settings.supports(Material.BAMBOO))
    }
}
