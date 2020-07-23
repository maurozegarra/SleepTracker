package com.maurozegarra.example.sleeptracker.sleeptracker

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.maurozegarra.example.sleeptracker.database.SleepDatabaseDao
import com.maurozegarra.example.sleeptracker.database.SleepNight
import com.maurozegarra.example.sleeptracker.formatNights
import kotlinx.coroutines.*

//@formatter:off
class SleepTrackerViewModel(val database: SleepDatabaseDao,
                            application: Application) : AndroidViewModel(application) {
    //@formatter:off
    // viewModelJob allows us to cancel all coroutines started by this ViewModel.
    private var viewModelJob = Job()

    // A [CoroutineScope] keeps track of all coroutines started by this ViewModel.
    // Because we pass it [viewModelJob], any coroutine started in this uiScope can be cancelled
    // by calling `viewModelJob.cancel()`

    // By default, all coroutines started in uiScope will launch in [Dispatchers.Main] which is
    // the main thread on Android. This is a sensible default because most coroutines started by
    // a [ViewModel] update the UI after performing some processing.

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()

    private val nights = database.getAllNights()

    // Converted nights to Spanned for displaying.
    val nightsString:LiveData<Spanned> = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    // Call this immediately after navigating to [SleepQualityFragment]
    // It will clear the navigation request, so if the user rotates their phone it won't navigate twice.
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    // Handling the case of the stopped app or forgotten recording, the start and end times will be
    // the same
    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            // If the start time and end time are not the same, then we do not have an unfinished
            // recording.
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    private suspend fun update(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }

    private suspend fun insert(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }

    // Executes when the START button is clicked
    fun onStartTracking() {
        uiScope.launch {
            // Create a new night, which captures the current time,
            // and insert it into the database.
            val newNight = SleepNight()

            insert(newNight)

            tonight.value = getTonightFromDatabase()
        }
    }


    // Executes when the STOP button is clicked
    fun onStopTracking() {
        uiScope.launch {
            // In Kotlin, the return@label syntax is used for specifying which function among
            // several nested ones this statement returns from.
            // In this case, we are specifying to return from launch(),
            // not the lambda.
            val oldNight = tonight.value ?: return@launch

            // Update the night in the database to add the end time.
            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)

            // Set state to navigate to the SleepQualityFragment.
            _navigateToSleepQuality.value = oldNight
        }
    }


    // Executes when the CLEAR button is clicked
    fun onClear() {
        uiScope.launch {
            // Clear the database table.
            clear()

            // And clear tonight since it's no longer in the database
            tonight.value = null
        }
    }

    // Called when the ViewModel is dismantled. At this point, we want to cancel all coroutines,
    // otherwise we end up with processes that have nowhere to return to using memory and resources
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
