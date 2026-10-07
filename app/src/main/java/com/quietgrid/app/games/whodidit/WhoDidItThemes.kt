package com.quietgrid.app.games.whodidit

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.quietgrid.app.R
import com.quietgrid.engine.whodidit.WhoDidItTrait

data class WhoDidItCaseRes(
    @StringRes val title: Int,
    @StringRes val question: Int,
    @StringRes val culpritNoun: Int,
    @StringRes val crimePlace: Int,
    @StringRes val crimeItem: Int,
    @StringRes val crimeMotive: Int,
    @StringRes val reveal: Int,
    @StringRes val revealMotive: Int,
)

data class WhoDidItThemeRes(
    @StringRes val title: Int,
    @ArrayRes val suspects: Int,
    @ArrayRes val places: Int,
    @ArrayRes val placePreps: Int,
    @ArrayRes val items: Int,
    @ArrayRes val motives: Int,
    val cases: List<WhoDidItCaseRes>,
)

data class WhoDidItTraitRes(
    val icon: String,
    @StringRes val label: Int,
    @StringRes val predicate: Int,
    @StringRes val negPredicate: Int,
    @StringRes val firstPerson: Int,
    @StringRes val firstPersonNeg: Int,
)

private fun themeRes(title: Int, suspects: Int, places: Int, placePreps: Int, items: Int, motives: Int, vararg cases: WhoDidItCaseRes) =
    WhoDidItThemeRes(title, suspects, places, placePreps, items, motives, cases.toList())

val WHODIDIT_THEME_RES: Map<String, WhoDidItThemeRes> = mapOf(
    "bakery" to themeRes(
        R.string.whodidit_theme_bakery_title,
        R.array.whodidit_theme_bakery_suspects,
        R.array.whodidit_theme_bakery_places,
        R.array.whodidit_theme_bakery_place_preps,
        R.array.whodidit_theme_bakery_items,
        R.array.whodidit_theme_bakery_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_bakery_case1_title, R.string.whodidit_theme_bakery_case1_question, R.string.whodidit_theme_bakery_case1_culprit, R.string.whodidit_theme_bakery_case1_crime_place, R.string.whodidit_theme_bakery_case1_crime_item, R.string.whodidit_theme_bakery_case1_crime_motive, R.string.whodidit_theme_bakery_case1_reveal, R.string.whodidit_theme_bakery_case1_reveal_motive,
        ),
    ),
    "farm" to themeRes(
        R.string.whodidit_theme_farm_title,
        R.array.whodidit_theme_farm_suspects,
        R.array.whodidit_theme_farm_places,
        R.array.whodidit_theme_farm_place_preps,
        R.array.whodidit_theme_farm_items,
        R.array.whodidit_theme_farm_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_farm_case1_title, R.string.whodidit_theme_farm_case1_question, R.string.whodidit_theme_farm_case1_culprit, R.string.whodidit_theme_farm_case1_crime_place, R.string.whodidit_theme_farm_case1_crime_item, R.string.whodidit_theme_farm_case1_crime_motive, R.string.whodidit_theme_farm_case1_reveal, R.string.whodidit_theme_farm_case1_reveal_motive,
        ),
    ),
    "castle" to themeRes(
        R.string.whodidit_theme_castle_title,
        R.array.whodidit_theme_castle_suspects,
        R.array.whodidit_theme_castle_places,
        R.array.whodidit_theme_castle_place_preps,
        R.array.whodidit_theme_castle_items,
        R.array.whodidit_theme_castle_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_castle_case1_title, R.string.whodidit_theme_castle_case1_question, R.string.whodidit_theme_castle_case1_culprit, R.string.whodidit_theme_castle_case1_crime_place, R.string.whodidit_theme_castle_case1_crime_item, R.string.whodidit_theme_castle_case1_crime_motive, R.string.whodidit_theme_castle_case1_reveal, R.string.whodidit_theme_castle_case1_reveal_motive,
        ),
    ),
    "museum" to themeRes(
        R.string.whodidit_theme_museum_title,
        R.array.whodidit_theme_museum_suspects,
        R.array.whodidit_theme_museum_places,
        R.array.whodidit_theme_museum_place_preps,
        R.array.whodidit_theme_museum_items,
        R.array.whodidit_theme_museum_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_museum_case1_title, R.string.whodidit_theme_museum_case1_question, R.string.whodidit_theme_museum_case1_culprit, R.string.whodidit_theme_museum_case1_crime_place, R.string.whodidit_theme_museum_case1_crime_item, R.string.whodidit_theme_museum_case1_crime_motive, R.string.whodidit_theme_museum_case1_reveal, R.string.whodidit_theme_museum_case1_reveal_motive,
        ),
    ),
    "hotel" to themeRes(
        R.string.whodidit_theme_hotel_title,
        R.array.whodidit_theme_hotel_suspects,
        R.array.whodidit_theme_hotel_places,
        R.array.whodidit_theme_hotel_place_preps,
        R.array.whodidit_theme_hotel_items,
        R.array.whodidit_theme_hotel_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_hotel_case1_title, R.string.whodidit_theme_hotel_case1_question, R.string.whodidit_theme_hotel_case1_culprit, R.string.whodidit_theme_hotel_case1_crime_place, R.string.whodidit_theme_hotel_case1_crime_item, R.string.whodidit_theme_hotel_case1_crime_motive, R.string.whodidit_theme_hotel_case1_reveal, R.string.whodidit_theme_hotel_case1_reveal_motive,
        ),
    ),
    "manor" to themeRes(
        R.string.whodidit_theme_manor_title,
        R.array.whodidit_theme_manor_suspects,
        R.array.whodidit_theme_manor_places,
        R.array.whodidit_theme_manor_place_preps,
        R.array.whodidit_theme_manor_items,
        R.array.whodidit_theme_manor_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_manor_case1_title, R.string.whodidit_theme_manor_case1_question, R.string.whodidit_theme_manor_case1_culprit, R.string.whodidit_theme_manor_case1_crime_place, R.string.whodidit_theme_manor_case1_crime_item, R.string.whodidit_theme_manor_case1_crime_motive, R.string.whodidit_theme_manor_case1_reveal, R.string.whodidit_theme_manor_case1_reveal_motive,
        ),
    ),
    "train" to themeRes(
        R.string.whodidit_theme_train_title,
        R.array.whodidit_theme_train_suspects,
        R.array.whodidit_theme_train_places,
        R.array.whodidit_theme_train_place_preps,
        R.array.whodidit_theme_train_items,
        R.array.whodidit_theme_train_motives,
        WhoDidItCaseRes(
            R.string.whodidit_theme_train_case1_title, R.string.whodidit_theme_train_case1_question, R.string.whodidit_theme_train_case1_culprit, R.string.whodidit_theme_train_case1_crime_place, R.string.whodidit_theme_train_case1_crime_item, R.string.whodidit_theme_train_case1_crime_motive, R.string.whodidit_theme_train_case1_reveal, R.string.whodidit_theme_train_case1_reveal_motive,
        ),
    ),
)

val WHODIDIT_TRAIT_RES: Map<WhoDidItTrait, WhoDidItTraitRes> = mapOf(
    WhoDidItTrait.SUSPECT_TALL to WhoDidItTraitRes("↑", R.string.whodidit_trait_suspect_tall_label, R.string.whodidit_trait_suspect_tall_predicate, R.string.whodidit_trait_suspect_tall_neg, R.string.whodidit_trait_suspect_tall_first, R.string.whodidit_trait_suspect_tall_first_neg),
    WhoDidItTrait.SUSPECT_SHORT to WhoDidItTraitRes("↓", R.string.whodidit_trait_suspect_short_label, R.string.whodidit_trait_suspect_short_predicate, R.string.whodidit_trait_suspect_short_neg, R.string.whodidit_trait_suspect_short_first, R.string.whodidit_trait_suspect_short_first_neg),
    WhoDidItTrait.SUSPECT_GLASSES to WhoDidItTraitRes("👓", R.string.whodidit_trait_suspect_glasses_label, R.string.whodidit_trait_suspect_glasses_predicate, R.string.whodidit_trait_suspect_glasses_neg, R.string.whodidit_trait_suspect_glasses_first, R.string.whodidit_trait_suspect_glasses_first_neg),
    WhoDidItTrait.SUSPECT_HAT to WhoDidItTraitRes("🎩", R.string.whodidit_trait_suspect_hat_label, R.string.whodidit_trait_suspect_hat_predicate, R.string.whodidit_trait_suspect_hat_neg, R.string.whodidit_trait_suspect_hat_first, R.string.whodidit_trait_suspect_hat_first_neg),
    WhoDidItTrait.SUSPECT_SCARF to WhoDidItTraitRes("🧣", R.string.whodidit_trait_suspect_scarf_label, R.string.whodidit_trait_suspect_scarf_predicate, R.string.whodidit_trait_suspect_scarf_neg, R.string.whodidit_trait_suspect_scarf_first, R.string.whodidit_trait_suspect_scarf_first_neg),
    WhoDidItTrait.PLACE_INDOOR to WhoDidItTraitRes("🏠", R.string.whodidit_trait_place_indoor_label, R.string.whodidit_trait_place_indoor_predicate, R.string.whodidit_trait_place_indoor_neg, R.string.whodidit_trait_place_indoor_first, R.string.whodidit_trait_place_indoor_first_neg),
    WhoDidItTrait.PLACE_OUTDOOR to WhoDidItTraitRes("🌳", R.string.whodidit_trait_place_outdoor_label, R.string.whodidit_trait_place_outdoor_predicate, R.string.whodidit_trait_place_outdoor_neg, R.string.whodidit_trait_place_outdoor_first, R.string.whodidit_trait_place_outdoor_first_neg),
    WhoDidItTrait.ITEM_WOOD to WhoDidItTraitRes("🌲", R.string.whodidit_trait_item_wood_label, R.string.whodidit_trait_item_wood_predicate, R.string.whodidit_trait_item_wood_neg, R.string.whodidit_trait_item_wood_first, R.string.whodidit_trait_item_wood_first_neg),
    WhoDidItTrait.ITEM_METAL to WhoDidItTraitRes("🔩", R.string.whodidit_trait_item_metal_label, R.string.whodidit_trait_item_metal_predicate, R.string.whodidit_trait_item_metal_neg, R.string.whodidit_trait_item_metal_first, R.string.whodidit_trait_item_metal_first_neg),
    WhoDidItTrait.ITEM_CLOTH to WhoDidItTraitRes("🧵", R.string.whodidit_trait_item_cloth_label, R.string.whodidit_trait_item_cloth_predicate, R.string.whodidit_trait_item_cloth_neg, R.string.whodidit_trait_item_cloth_first, R.string.whodidit_trait_item_cloth_first_neg),
    WhoDidItTrait.ITEM_HEAVY to WhoDidItTraitRes("⚖️", R.string.whodidit_trait_item_heavy_label, R.string.whodidit_trait_item_heavy_predicate, R.string.whodidit_trait_item_heavy_neg, R.string.whodidit_trait_item_heavy_first, R.string.whodidit_trait_item_heavy_first_neg),
    WhoDidItTrait.ITEM_LIGHT to WhoDidItTraitRes("🍃", R.string.whodidit_trait_item_light_label, R.string.whodidit_trait_item_light_predicate, R.string.whodidit_trait_item_light_neg, R.string.whodidit_trait_item_light_first, R.string.whodidit_trait_item_light_first_neg),
)
