package com.iseids.farmingUtils.food

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

data class SoundEffect(
    val sound: Sound,
    val volume: Float,
    val pitch: Float,
) {
    fun play(player: Player) {
        player.playSound(player.location, sound, volume, pitch)
    }
}

data class CookpotSoundSettings(
    val stationPlace: SoundEffect,
    val menuOpen: SoundEffect,
    val craftSuccess: SoundEffect,
    val craftFailure: SoundEffect,
    val consumeBonus: SoundEffect,
)

data class CookpotStationSettings(
    val displayName: String,
    val lore: List<String>,
    val headId: String?,
    val textureUrl: String,
    val recipeShape: List<String>,
    val recipeIngredients: Map<Char, Material>,
) {
    fun recipeDisplayLines(prefix: String): List<String> {
        val legend = recipeIngredients.entries
            .joinToString(", ") { "${it.key} = ${formatMaterialName(it.value)}" }

        return recipeShape.map { row ->
            "$prefix${row.map { slot -> if (slot == ' ') "[ ]" else "[$slot]" }.joinToString(" ")}"
        } + "$prefix$legend"
    }
}

data class RecipeDefinition(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val resultMaterial: Material,
    val headId: String?,
    val textureUrl: String?,
    val ingredients: List<RecipeIngredient>,
    val foodPoints: Int,
)

data class CookpotSettings(
    val helpPreviewLimit: Int,
    val station: CookpotStationSettings,
    val sounds: CookpotSoundSettings,
    val recipes: List<RecipeDefinition>,
)

object CookpotSettingsLoader {
    fun load(plugin: JavaPlugin): CookpotSettings {
        val cookpot = requireSection(plugin.config, "cookpot")
        val station = loadStationSettings(requireSection(cookpot, "station"))
        val sounds = loadSoundSettings(requireSection(cookpot, "sounds"))
        val recipes = loadRecipes(requireSection(cookpot, "recipes"))

        return CookpotSettings(
            helpPreviewLimit = cookpot.getInt("help-preview-limit", 12).coerceAtLeast(1),
            station = station,
            sounds = sounds,
            recipes = recipes,
        )
    }

    private fun loadStationSettings(section: ConfigurationSection): CookpotStationSettings {
        val recipeSection = requireSection(section, "crafting-recipe")
        val ingredientSection = requireSection(recipeSection, "ingredients")
        val recipeIngredients = linkedMapOf<Char, Material>()

        ingredientSection.getKeys(false).forEach { key ->
            require(key.length == 1) { "Cookpot station recipe key '$key' must be a single character." }
            recipeIngredients[key.single()] = parseMaterial(
                ingredientSection.getString(key),
                "cookpot.station.crafting-recipe.ingredients.$key",
            )
        }

        return CookpotStationSettings(
            displayName = colorize(section.getString("name") ?: "&6&lCookpot"),
            lore = section.getStringList("lore").map(::colorize),
            headId = section.getString("head-id"),
            textureUrl = requireString(section, "texture-url"),
            recipeShape = recipeSection.getStringList("shape").ifEmpty { listOf(" A ", "WSW", " W ") },
            recipeIngredients = recipeIngredients,
        )
    }

    private fun loadSoundSettings(section: ConfigurationSection): CookpotSoundSettings {
        return CookpotSoundSettings(
            stationPlace = loadSound(section, "station-place"),
            menuOpen = loadSound(section, "menu-open"),
            craftSuccess = loadSound(section, "craft-success"),
            craftFailure = loadSound(section, "craft-failure"),
            consumeBonus = loadSound(section, "consume-bonus"),
        )
    }

    private fun loadRecipes(section: ConfigurationSection): List<RecipeDefinition> {
        return section.getKeys(false).map { id ->
            val recipe = requireSection(section, id)
            val ingredientSection = requireSection(recipe, "ingredients")
            val ingredients = ingredientSection.getKeys(false).map { materialKey ->
                RecipeIngredient(
                    material = parseMaterial(materialKey, "cookpot.recipes.$id.ingredients.$materialKey"),
                    amount = ingredientSection.getInt(materialKey).takeIf { it > 0 }
                        ?: error("cookpot.recipes.$id.ingredients.$materialKey must be greater than 0."),
                )
            }

            RecipeDefinition(
                id = id,
                displayName = colorize(requireString(recipe, "name")),
                description = recipe.getStringList("description").map(::colorize),
                resultMaterial = parseMaterial(recipe.getString("material"), "cookpot.recipes.$id.material"),
                headId = recipe.getString("head-id"),
                textureUrl = recipe.getString("texture-url")?.takeIf { it.isNotBlank() },
                ingredients = ingredients,
                foodPoints = recipe.getInt("food-points").takeIf { it > 0 }
                    ?: error("cookpot.recipes.$id.food-points must be greater than 0."),
            )
        }
    }

    private fun loadSound(section: ConfigurationSection, path: String): SoundEffect {
        val soundSection = requireSection(section, path)
        return SoundEffect(
            sound = parseSound(requireString(soundSection, "sound"), "${soundSection.currentPath}.sound"),
            volume = soundSection.getDouble("volume", 1.0).toFloat(),
            pitch = soundSection.getDouble("pitch", 1.0).toFloat(),
        )
    }

    private fun requireSection(section: ConfigurationSection, path: String): ConfigurationSection {
        return requireNotNull(section.getConfigurationSection(path)) {
            "Missing config section: ${section.currentPath}.$path"
        }
    }

    private fun requireString(section: ConfigurationSection, path: String): String {
        return requireNotNull(section.getString(path)) {
            "Missing config value: ${section.currentPath}.$path"
        }
    }

    private fun parseMaterial(value: String?, path: String): Material {
        return requireNotNull(value)
            .let {
                runCatching { Material.valueOf(it) }.getOrElse { _ ->
                    error("Invalid material '$it' at $path")
                }
            }
    }

    private fun parseSound(value: String, path: String): Sound {
        Registry.SOUNDS.match(value)?.let { return it }

        val normalizedKey = value.lowercase().replace('_', '.')
        Registry.SOUNDS.get(NamespacedKey.minecraft(normalizedKey))?.let { return it }

        error("Invalid sound '$value' at $path")
    }

    private fun colorize(input: String): String {
        return ChatColor.translateAlternateColorCodes('&', input)
    }
}

fun formatMaterialName(material: Material): String {
    return material.name
        .lowercase()
        .split('_')
        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
}
