package com.quietgrid.app.games.takuzu

private fun hexToBits(hex: String, total: Int): IntArray {
    val bits = IntArray(hex.length * 4)
    for (i in hex.indices) {
        val value = Character.digit(hex[i], 16)
        bits[i * 4] = (value shr 3) and 1
        bits[i * 4 + 1] = (value shr 2) and 1
        bits[i * 4 + 2] = (value shr 1) and 1
        bits[i * 4 + 3] = value and 1
    }
    return bits.copyOf(total)
}

fun decodeSolution(solution: String, size: Int): TakuzuGrid {
    val bits = hexToBits(solution, size * size)
    return List(size) { r -> List(size) { c -> bits[r * size + c] } }
}

fun decodeMask(mask: String, size: Int): List<List<Boolean>> {
    val bits = hexToBits(mask, size * size)
    return List(size) { r -> List(size) { c -> bits[r * size + c] == 1 } }
}

fun decodePuzzleBoard(solution: String, mask: String, size: Int): TakuzuGrid {
    val solBits = hexToBits(solution, size * size)
    val maskBits = hexToBits(mask, size * size)
    return List(size) { r ->
        List(size) { c ->
            val i = r * size + c
            if (maskBits[i] == 1) solBits[i] else null
        }
    }
}
