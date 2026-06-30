package com.stravamusic.app.data.repository

import com.stravamusic.app.data.local.ActivityDao
import com.stravamusic.app.data.local.ActivityEntity
import kotlinx.coroutines.flow.Flow

/** Thin wrapper around the DAO so the rest of the app depends on an interface-ish layer. */
class ActivityRepository(private val dao: ActivityDao) {

    fun observeActivities(): Flow<List<ActivityEntity>> = dao.observeAll()

    suspend fun getActivity(id: Long): ActivityEntity? = dao.getById(id)

    suspend fun saveActivity(activity: ActivityEntity): Long = dao.insert(activity)

    suspend fun deleteActivity(activity: ActivityEntity) = dao.delete(activity)
}
