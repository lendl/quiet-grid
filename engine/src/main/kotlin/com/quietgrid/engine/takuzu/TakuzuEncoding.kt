package com.quietgrid.engine.takuzu

private fun bitsToHex(bits: List<Int>): String {
    val padded = bits + List((4 - bits.size % 4) % 4) { 0 }
    return padded.chunked(4).joinToString("") { nibble ->
        val value = (nibble[0] shl 3) or (nibble[1] shl 2) or (nibble[2] shl 1) or nibble[3]
        value.toString(16)
    }
}

fun gridToHex(grid: TakuzuGrid): String = bitsToHex(grid.flatMap { row -> row.map { it!! } })

fun maskToHex(mask: List<List<Boolean>>): String = bitsToHex(mask.flatMap { row -> row.map { if (it) 1 else 0 } })

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
    return List(size) { r -> List(size) { c -> val i = r * size + c; if (maskBits[i] == 1) solBits[i] else null } }
}
