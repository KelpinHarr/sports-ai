package com.example.sportsanalysis.data.repository

import com.example.sportsanalysis.domain.model.FreeThrowAnalysis
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalysisRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun uid(): String = auth.currentUser?.uid
        ?: error("Not signed in — call ensureSignedIn() first")

    suspend fun save(analysis: FreeThrowAnalysis): String {
        ensureSignedIn()

        val userId = uid()
        val ref = firestore
            .collection("users")
            .document(userId)
            .collection("analyses")
            .document

        val id = ref.id
        ref.set(analysis.copy(id = id, userId = userId))
        return id
    }

    fun observeHistory(): Flow<List<FreeThrowAnalysis>> {
        val userId = uid()

        return firestore
            .collection("users")
            .document(userId)
            .collection("analyses")
            .snapshots
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { document ->
                        runCatching { document.data<FreeThrowAnalysis>() }.getOrNull()
                    }
                    .sortedByDescending { it.timestampMs }
            }
    }

    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}
