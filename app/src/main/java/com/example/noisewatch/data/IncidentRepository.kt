package com.example.noisewatch.data

import com.example.noisewatch.data.local.IncidentDao
import com.example.noisewatch.model.Incident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidentRepository(
    private val incidentDao: IncidentDao
) {
    suspend fun saveIncident(incident: Incident): Long {
        return incidentDao.insertIncident(incident.toEntity())
    }

    fun getIncidentById(id: Long): Flow<Incident?> {
        return incidentDao.getIncidentById(id).map { entity ->
            entity?.let { Incident.fromEntity(it) }
        }
    }

    suspend fun getIncidentByIdSync(id: Long): Incident? {
        return incidentDao.getIncidentByIdSync(id)?.let { Incident.fromEntity(it) }
    }

    fun getAllIncidents(): Flow<List<Incident>> {
        return incidentDao.getAllIncidents().map { entities ->
            entities.map { Incident.fromEntity(it) }
        }
    }

    suspend fun deleteAllIncidents() {
        incidentDao.deleteAllIncidents()
    }

    suspend fun deleteIncidentById(id: Long) {
        incidentDao.deleteIncidentById(id)
    }
}
