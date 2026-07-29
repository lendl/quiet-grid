package com.quietgrid.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DifficultyAuditEntry(
    val dedupeKey: String,
    val difficulty: String,
    val hardestTechnique: String?,
    val stepCount: Int,
    val score: Int,
    val generatedAt: String,
)

@Serializable
private data class GenerationStateFile(
    val triedKeys: MutableMap<String, String> = mutableMapOf(), // dedupeKey -> status ("valid" | "invalid")
    val difficultyAudit: MutableList<DifficultyAuditEntry> = mutableListOf(),
)

class GenerationState(private val statePath: String) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file = File(statePath)
    private val state: GenerationStateFile = if (file.exists()) {
        json.decodeFromString(GenerationStateFile.serializer(), file.readText())
    } else {
        GenerationStateFile()
    }

    fun hasTried(dedupeKey: String): Boolean = state.triedKeys.containsKey(dedupeKey)

    fun recordTried(dedupeKey: String, status: String) {
        state.triedKeys[dedupeKey] = status
    }

    fun recordDifficultyAudit(entry: DifficultyAuditEntry) {
        state.difficultyAudit.add(entry)
    }

    fun save() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(GenerationStateFile.serializer(), state))
    }
}
