package com.maurozegarra.example.sleeptracker.sleepquality

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.maurozegarra.example.sleeptracker.database.SleepDatabaseDao
import kotlinx.coroutines.*

//@formatter:off
class SleepQualityViewModel(private val sleepNightKey: Long = 0L,
                            val database: SleepDatabaseDao) : ViewModel() {
    //@formatter:on
    private val viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val navigateToSleepTracker: LiveData<Boolean?>
        get() = _navigateToSleepTracker

    private val _navigateToSleepTracker = MutableLiveData<Boolean?>()

    fun doneNavigating() {
        _navigateToSleepTracker.value = null
    }

    fun onSetSleepQuality(quality: Int) {
        uiScope.launch {
            // IO is a thread pool for running operations that access the disk, such as
            // our Room database.
            withContext(Dispatchers.IO) {
                val tonight = database.get(sleepNightKey) ?: return@withContext
                tonight.sleepQuality = quality
                database.update(tonight)
            }

            // Setting this state variable to true will alert the observer and trigger navigation.
            _navigateToSleepTracker.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
