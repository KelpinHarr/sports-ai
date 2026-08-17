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
        ?: error("Not signed in — call signInAnonymously() first")

    suspend fun save(analysis: FreeThrowAnalysis): String {
        val ref = firestore
            .collection("users").document(uid())
            .collection("analyses")
            .document
        val id = ref.id
        ref.set(analysis.copy(id = id, userId = uid()))
        return id
    }

    fun observeHistory(): Flow<List<FreeThrowAnalysis>> {
        return firestore
            .collection("users").document(uid())
            .collection("analyses")
            .snapshots
            .map { snap ->
                snap.documents
                    .map { it.data<FreeThrowAnalysis>() }
                    .sortedByDescending { it.timestampMs }
            }
    }

    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}