package com.example.mp2026.data

import com.example.mp2026.Academy
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges

data class AcademySnapshot(val academies: List<Academy>, val fromCache: Boolean)

class AcademyRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    // Regional demo catalog. For nationwide data, replace with paged/geohash queries.
    fun observe(onResult: (Result<AcademySnapshot>) -> Unit): ListenerRegistration =
        db.collection("academies").addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
            } else if (snapshot != null) {
                onResult(runCatching {
                    AcademySnapshot(snapshot.documents.map { AcademyMapper.fromFields(it.id, it.data.orEmpty()) },
                        snapshot.metadata.isFromCache)
                })
            }
        }
}
