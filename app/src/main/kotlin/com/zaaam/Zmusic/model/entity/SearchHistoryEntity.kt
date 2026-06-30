package com.zaaam.Zmusic.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// FIX #15: Unique index pada query untuk mendukung atomic upsert via ON CONFLICT
@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
