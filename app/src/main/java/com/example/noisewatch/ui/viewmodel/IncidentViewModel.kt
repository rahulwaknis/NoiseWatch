package com.example.noisewatch.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.noisewatch.data.IncidentRepository
import com.example.noisewatch.data.local.NoiseWatchDatabase
import com.example.noisewatch.model.Incident
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class IncidentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IncidentRepository

    val allIncidents: StateFlow<List<Incident>>

    init {
        val db = NoiseWatchDatabase.getInstance(application)
        repository = IncidentRepository(db.incidentDao())
        allIncidents = repository.getAllIncidents().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    suspend fun saveIncident(incident: Incident): Long {
        return repository.saveIncident(incident)
    }

    fun getIncidentById(id: Long) = repository.getIncidentById(id)

    suspend fun deleteAllIncidents() {
        repository.deleteAllIncidents()
    }
}
