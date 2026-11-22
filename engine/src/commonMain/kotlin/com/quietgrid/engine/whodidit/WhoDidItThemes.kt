package com.quietgrid.engine.whodidit

import com.quietgrid.engine.whodidit.WhoDidItCategory.ITEM
import com.quietgrid.engine.whodidit.WhoDidItCategory.MOTIVE
import com.quietgrid.engine.whodidit.WhoDidItCategory.PLACE
import com.quietgrid.engine.whodidit.WhoDidItCategory.SUSPECT
import com.quietgrid.engine.whodidit.WhoDidItTrait.*

enum class WhoDidItTone { MISCHIEF, THEFT, MURDER }

data class WhoDidItThemeDef(
    val id: String,
    val tone: WhoDidItTone,
    val caseCount: Int,
    val pools: Map<WhoDidItCategory, List<Set<WhoDidItTrait>>>,
)

private fun suspect(height: WhoDidItTrait, accessory: WhoDidItTrait) = setOf(height, accessory)
private fun place(setting: WhoDidItTrait) = setOf(setting)
private fun item(material: WhoDidItTrait, weight: WhoDidItTrait) = setOf(material, weight)

private val NO_TRAITS = emptySet<WhoDidItTrait>()
private val MOTIVES = List(WHODIDIT_POOL_SIZE) { NO_TRAITS }

private fun theme(
    id: String,
    tone: WhoDidItTone,
    suspects: List<Set<WhoDidItTrait>>,
    places: List<Set<WhoDidItTrait>>,
    items: List<Set<WhoDidItTrait>>,
) = WhoDidItThemeDef(
    id = id,
    tone = tone,
    caseCount = 1,
    pools = mapOf(SUSPECT to suspects, PLACE to places, ITEM to items, MOTIVE to MOTIVES),
)

val WHODIDIT_THEMES: List<WhoDidItThemeDef> = listOf(
    theme(
        "bakery", WhoDidItTone.MISCHIEF,
        listOf(
            suspect(SUSPECT_TALL, SUSPECT_GLASSES), suspect(SUSPECT_SHORT, SUSPECT_HAT),
            suspect(SUSPECT_TALL, SUSPECT_SCARF), suspect(SUSPECT_SHORT, SUSPECT_GLASSES),
            suspect(SUSPECT_TALL, SUSPECT_HAT), suspect(SUSPECT_SHORT, SUSPECT_SCARF),
            suspect(SUSPECT_TALL, SUSPECT_GLASSES),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR),
        ),
        listOf(
            item(ITEM_WOOD, ITEM_HEAVY), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_CLOTH, ITEM_LIGHT),
            item(ITEM_METAL, ITEM_HEAVY), item(ITEM_CLOTH, ITEM_HEAVY), item(ITEM_WOOD, ITEM_LIGHT),
            item(ITEM_METAL, ITEM_HEAVY),
        ),
    ),
    theme(
        "farm", WhoDidItTone.MISCHIEF,
        listOf(
            suspect(SUSPECT_SHORT, SUSPECT_HAT), suspect(SUSPECT_TALL, SUSPECT_GLASSES),
            suspect(SUSPECT_SHORT, SUSPECT_SCARF), suspect(SUSPECT_TALL, SUSPECT_HAT),
            suspect(SUSPECT_SHORT, SUSPECT_GLASSES), suspect(SUSPECT_TALL, SUSPECT_SCARF),
            suspect(SUSPECT_SHORT, SUSPECT_HAT),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR),
        ),
        listOf(
            item(ITEM_METAL, ITEM_HEAVY), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_WOOD, ITEM_LIGHT),
            item(ITEM_CLOTH, ITEM_HEAVY), item(ITEM_CLOTH, ITEM_LIGHT), item(ITEM_METAL, ITEM_HEAVY),
            item(ITEM_WOOD, ITEM_HEAVY),
        ),
    ),
    theme(
        "castle", WhoDidItTone.THEFT,
        listOf(
            suspect(SUSPECT_TALL, SUSPECT_HAT), suspect(SUSPECT_SHORT, SUSPECT_SCARF),
            suspect(SUSPECT_SHORT, SUSPECT_GLASSES), suspect(SUSPECT_TALL, SUSPECT_SCARF),
            suspect(SUSPECT_SHORT, SUSPECT_HAT), suspect(SUSPECT_TALL, SUSPECT_GLASSES),
            suspect(SUSPECT_SHORT, SUSPECT_SCARF),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR),
        ),
        listOf(
            item(ITEM_METAL, ITEM_LIGHT), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_CLOTH, ITEM_HEAVY),
            item(ITEM_CLOTH, ITEM_LIGHT), item(ITEM_WOOD, ITEM_HEAVY), item(ITEM_WOOD, ITEM_LIGHT),
            item(ITEM_METAL, ITEM_HEAVY),
        ),
    ),
    theme(
        "museum", WhoDidItTone.THEFT,
        listOf(
            suspect(SUSPECT_SHORT, SUSPECT_GLASSES), suspect(SUSPECT_TALL, SUSPECT_SCARF),
            suspect(SUSPECT_TALL, SUSPECT_HAT), suspect(SUSPECT_SHORT, SUSPECT_SCARF),
            suspect(SUSPECT_TALL, SUSPECT_GLASSES), suspect(SUSPECT_SHORT, SUSPECT_HAT),
            suspect(SUSPECT_TALL, SUSPECT_SCARF),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_INDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
            place(PLACE_OUTDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
        ),
        listOf(
            item(ITEM_METAL, ITEM_HEAVY), item(ITEM_WOOD, ITEM_LIGHT), item(ITEM_CLOTH, ITEM_LIGHT),
            item(ITEM_WOOD, ITEM_HEAVY), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_CLOTH, ITEM_HEAVY),
            item(ITEM_WOOD, ITEM_HEAVY),
        ),
    ),
    theme(
        "hotel", WhoDidItTone.THEFT,
        listOf(
            suspect(SUSPECT_TALL, SUSPECT_HAT), suspect(SUSPECT_SHORT, SUSPECT_GLASSES),
            suspect(SUSPECT_SHORT, SUSPECT_SCARF), suspect(SUSPECT_TALL, SUSPECT_GLASSES),
            suspect(SUSPECT_SHORT, SUSPECT_HAT), suspect(SUSPECT_TALL, SUSPECT_SCARF),
            suspect(SUSPECT_SHORT, SUSPECT_GLASSES),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
            place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR),
        ),
        listOf(
            item(ITEM_CLOTH, ITEM_HEAVY), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_METAL, ITEM_HEAVY),
            item(ITEM_CLOTH, ITEM_LIGHT), item(ITEM_WOOD, ITEM_LIGHT), item(ITEM_WOOD, ITEM_HEAVY),
            item(ITEM_METAL, ITEM_LIGHT),
        ),
    ),
    theme(
        "manor", WhoDidItTone.MURDER,
        listOf(
            suspect(SUSPECT_SHORT, SUSPECT_SCARF), suspect(SUSPECT_TALL, SUSPECT_GLASSES),
            suspect(SUSPECT_TALL, SUSPECT_HAT), suspect(SUSPECT_SHORT, SUSPECT_GLASSES),
            suspect(SUSPECT_TALL, SUSPECT_SCARF), suspect(SUSPECT_SHORT, SUSPECT_HAT),
            suspect(SUSPECT_TALL, SUSPECT_GLASSES),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR), place(PLACE_OUTDOOR),
            place(PLACE_INDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
        ),
        listOf(
            item(ITEM_METAL, ITEM_HEAVY), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_CLOTH, ITEM_LIGHT),
            item(ITEM_CLOTH, ITEM_HEAVY), item(ITEM_WOOD, ITEM_LIGHT), item(ITEM_WOOD, ITEM_HEAVY),
            item(ITEM_METAL, ITEM_HEAVY),
        ),
    ),
    theme(
        "train", WhoDidItTone.MURDER,
        listOf(
            suspect(SUSPECT_SHORT, SUSPECT_HAT), suspect(SUSPECT_TALL, SUSPECT_SCARF),
            suspect(SUSPECT_TALL, SUSPECT_GLASSES), suspect(SUSPECT_SHORT, SUSPECT_SCARF),
            suspect(SUSPECT_TALL, SUSPECT_HAT), suspect(SUSPECT_SHORT, SUSPECT_GLASSES),
            suspect(SUSPECT_TALL, SUSPECT_HAT),
        ),
        listOf(
            place(PLACE_INDOOR), place(PLACE_INDOOR), place(PLACE_INDOOR), place(PLACE_OUTDOOR),
            place(PLACE_OUTDOOR), place(PLACE_OUTDOOR), place(PLACE_INDOOR),
        ),
        listOf(
            item(ITEM_METAL, ITEM_HEAVY), item(ITEM_METAL, ITEM_LIGHT), item(ITEM_CLOTH, ITEM_HEAVY),
            item(ITEM_CLOTH, ITEM_LIGHT), item(ITEM_WOOD, ITEM_HEAVY), item(ITEM_WOOD, ITEM_LIGHT),
            item(ITEM_METAL, ITEM_LIGHT),
        ),
    ),
)

fun whoDidItTheme(id: String): WhoDidItThemeDef = WHODIDIT_THEMES.first { it.id == id }
