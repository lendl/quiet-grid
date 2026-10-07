package com.quietgrid.engine.whodidit

import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator

const val WHODIDIT_POOL_SIZE = 7

@Serializable
enum class WhoDidItCategory {
    @SerialName("suspect") SUSPECT,
    @SerialName("place") PLACE,
    @SerialName("item") ITEM,
    @SerialName("motive") MOTIVE,
}

object WhoDidItRefSerializer : KSerializer<WhoDidItRef> {
    private val delegate = ListSerializer(Int.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: WhoDidItRef) {
        encoder.encodeSerializableValue(delegate, listOf(value.category, value.slot))
    }

    override fun deserialize(decoder: Decoder): WhoDidItRef {
        val values = decoder.decodeSerializableValue(delegate)
        require(values.size == 2) { "ref must have 2 elements" }
        return WhoDidItRef(values[0], values[1])
    }
}

@Serializable(with = WhoDidItRefSerializer::class)
data class WhoDidItRef(val category: Int, val slot: Int)

@Serializable
enum class WhoDidItTrait(val category: WhoDidItCategory, val dimension: String) {
    @SerialName("suspect.height.tall") SUSPECT_TALL(WhoDidItCategory.SUSPECT, "height"),
    @SerialName("suspect.height.short") SUSPECT_SHORT(WhoDidItCategory.SUSPECT, "height"),
    @SerialName("suspect.accessory.glasses") SUSPECT_GLASSES(WhoDidItCategory.SUSPECT, "accessory"),
    @SerialName("suspect.accessory.hat") SUSPECT_HAT(WhoDidItCategory.SUSPECT, "accessory"),
    @SerialName("suspect.accessory.scarf") SUSPECT_SCARF(WhoDidItCategory.SUSPECT, "accessory"),
    @SerialName("place.setting.indoor") PLACE_INDOOR(WhoDidItCategory.PLACE, "setting"),
    @SerialName("place.setting.outdoor") PLACE_OUTDOOR(WhoDidItCategory.PLACE, "setting"),
    @SerialName("item.material.wood") ITEM_WOOD(WhoDidItCategory.ITEM, "material"),
    @SerialName("item.material.metal") ITEM_METAL(WhoDidItCategory.ITEM, "material"),
    @SerialName("item.material.cloth") ITEM_CLOTH(WhoDidItCategory.ITEM, "material"),
    @SerialName("item.weight.heavy") ITEM_HEAVY(WhoDidItCategory.ITEM, "weight"),
    @SerialName("item.weight.light") ITEM_LIGHT(WhoDidItCategory.ITEM, "weight"),
}

enum class WhoDidItClueType { SAME, NOT_SAME, EITHER, TRAIT, NOT_TRAIT, CRIME, CRIME_TRAIT }

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("t")
sealed interface WhoDidItClue {
    val type: WhoDidItClueType

    @Serializable
    @SerialName("SAME")
    data class Same(val a: WhoDidItRef, val b: WhoDidItRef) : WhoDidItClue {
        override val type get() = WhoDidItClueType.SAME
    }

    @Serializable
    @SerialName("NOT_SAME")
    data class NotSame(val a: WhoDidItRef, val b: WhoDidItRef) : WhoDidItClue {
        override val type get() = WhoDidItClueType.NOT_SAME
    }

    @Serializable
    @SerialName("EITHER")
    data class Either(val a: WhoDidItRef, val b: WhoDidItRef, val c: WhoDidItRef) : WhoDidItClue {
        override val type get() = WhoDidItClueType.EITHER
    }

    @Serializable
    @SerialName("TRAIT")
    data class HasTrait(val a: WhoDidItRef, val trait: WhoDidItTrait) : WhoDidItClue {
        override val type get() = WhoDidItClueType.TRAIT
    }

    @Serializable
    @SerialName("NOT_TRAIT")
    data class LacksTrait(val a: WhoDidItRef, val trait: WhoDidItTrait) : WhoDidItClue {
        override val type get() = WhoDidItClueType.NOT_TRAIT
    }

    @Serializable
    @SerialName("CRIME")
    data class Crime(val e: WhoDidItRef) : WhoDidItClue {
        override val type get() = WhoDidItClueType.CRIME
    }

    @Serializable
    @SerialName("CRIME_TRAIT")
    data class CrimeTrait(val trait: WhoDidItTrait) : WhoDidItClue {
        override val type get() = WhoDidItClueType.CRIME_TRAIT
    }
}

@Serializable
data class WhoDidItStatement(val s: Int, val clue: WhoDidItClue)

@Serializable
data class WhoDidItPuzzleEntry(
    val id: String,
    val difficulty: String,
    val theme: String,
    val case: Int,
    val size: Int,
    val categories: List<WhoDidItCategory>,
    val entries: List<List<Int>>,
    val solution: List<List<Int>>,
    val culprit: Int,
    val clues: List<WhoDidItClue>,
    val statements: List<WhoDidItStatement> = emptyList(),
)

data class WhoDidItShape(val size: Int, val categoryCount: Int)

val WHODIDIT_SHAPES: Map<Difficulty, WhoDidItShape> = mapOf(
    Difficulty.EASY to WhoDidItShape(3, 3),
    Difficulty.MEDIUM to WhoDidItShape(4, 3),
    Difficulty.HARD to WhoDidItShape(4, 4),
    Difficulty.EXPERT to WhoDidItShape(5, 4),
)

fun whoDidItCategoryPairs(categoryCount: Int): List<Pair<Int, Int>> {
    val last = categoryCount - 1
    val rowCategories = listOf(0) + (last downTo 2)
    return rowCategories.flatMap { row ->
        (1..last).filter { col -> row == 0 || col < row }.map { col -> row to col }
    }
}

fun WhoDidItPuzzleEntry.ownerOf(ref: WhoDidItRef): Int =
    if (ref.category == 0) ref.slot else solution[ref.category].indexOf(ref.slot)

fun WhoDidItPuzzleEntry.paired(a: WhoDidItRef, b: WhoDidItRef): Boolean = ownerOf(a) == ownerOf(b)
