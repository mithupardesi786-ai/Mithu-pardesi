package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "PASSPORT", "DRIVING_LICENSE", "RESIDENCE_PERMIT"
    val documentNumber: String,
    val fullName: String,
    val nationality: String,
    val birthDate: String,
    val expiryDate: String,
    val issueDate: String,
    val issuer: String,
    val photoSeed: Int = 1, // Deterministic seed for drawing stylized avatar faces
    val additionalData: String, // "Classes: A, B", "Permit Type: Permanent", "Passport Type: P"
    val qrCodePayload: String, // String encoded data for local authorities to verify
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
