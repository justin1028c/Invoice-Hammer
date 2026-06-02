package com.fordham.toolbelt.data.implementation

import com.fordham.toolbelt.data.JobNoteDao
import com.fordham.toolbelt.domain.repository.ForemanJobMemoryPort

class RoomForemanJobMemoryAdapter(
    private val jobNoteDao: JobNoteDao
) : ForemanJobMemoryPort {
    override suspend fun appendRelevantNotes(context: String, userInput: String): String = buildString {
        append(context)
        try {
            val stopWords = setOf(
                "the", "and", "with", "this", "your", "that", "from", "have", "will",
                "please", "just", "about", "some", "been", "what", "were", "invoice", "client"
            )
            
            // Extract clean, normalized search keywords (stemming/plurals/possessives)
            val cleanTerms = userInput.lowercase()
                .split(Regex("[^a-zA-Z0-9']"))
                .map { it.trim().trim('\'') }
                .filter { it.length > 3 && it !in stopWords }
                .map { term ->
                    when {
                        term.endsWith("'s") -> term.dropLast(2)
                        term.endsWith("s") && !term.endsWith("ss") -> term.dropLast(1)
                        else -> term
                    }
                }
                .filter { it.length > 2 && it !in stopWords }
                .distinct()

            val addedNotes = mutableSetOf<String>()
            for (term in cleanTerms) {
                val matchingNotes = jobNoteDao.getRelevantContext(term)
                val newNotes = matchingNotes.filter { it.text !in addedNotes }.take(3)
                if (newNotes.isNotEmpty()) {
                    append("\n\n[RELEVANT JOB MEMORY FOR \"$term\"]")
                    newNotes.forEach {
                        append("\n- ${it.text}")
                        addedNotes.add(it.text)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
