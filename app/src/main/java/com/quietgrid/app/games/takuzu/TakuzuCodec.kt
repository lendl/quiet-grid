package com.quietgrid.app.games.takuzu

import com.quietgrid.engine.takuzu.TakuzuGrid
import com.quietgrid.engine.takuzu.decodeMask as engineDecodeMask
import com.quietgrid.engine.takuzu.decodePuzzleBoard as engineDecodePuzzleBoard
import com.quietgrid.engine.takuzu.decodeSolution as engineDecodeSolution

fun decodeSolution(solution: String, size: Int): TakuzuGrid = engineDecodeSolution(solution, size)
fun decodeMask(mask: String, size: Int): List<List<Boolean>> = engineDecodeMask(mask, size)
fun decodePuzzleBoard(solution: String, mask: String, size: Int): TakuzuGrid = engineDecodePuzzleBoard(solution, mask, size)
