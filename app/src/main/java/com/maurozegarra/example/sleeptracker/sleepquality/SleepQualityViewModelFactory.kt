package com.maurozegarra.example.sleeptracker.sleepquality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maurozegarra.example.sleeptracker.database.SleepDatabaseDao

//@formatter:off
class SleepQualityViewModelFactory(private val sleepNightKey: Long,
                                   private val dataSource: SleepDatabaseDao) : ViewModelProvider.Factory {
    //@formatter:on
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepQualityViewModel::class.java)) {
            return SleepQualityViewModel(sleepNightKey, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
