package com.lukakordzaia.streamflow.ui.phone.singletitle.choosetitledetails

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lukakordzaia.streamflow.database.DbDetails
import com.lukakordzaia.streamflow.database.ImoviesDatabase
import com.lukakordzaia.streamflow.datamodels.TitleEpisodes
import com.lukakordzaia.streamflow.datamodels.VideoPlayerData
import com.lukakordzaia.streamflow.network.LoadingState
import com.lukakordzaia.streamflow.network.Result
import com.lukakordzaia.streamflow.repository.SingleTitleRepository
import com.lukakordzaia.streamflow.ui.baseclasses.BaseViewModel
import com.lukakordzaia.streamflow.utils.AppConstants
import kotlinx.coroutines.launch

class ChooseTitleDetailsViewModel(private val repository: SingleTitleRepository) : BaseViewModel() {
    val chooseDetailsLoader = MutableLiveData<LoadingState>()

    private val _movieNotYetAdded = MutableLiveData<Boolean>()
    val movieNotYetAdded: LiveData<Boolean> = _movieNotYetAdded

    private val _availableLanguages = MutableLiveData<MutableList<String>>()
    val availableLanguages: LiveData<MutableList<String>> = _availableLanguages

    private val _chosenLanguage = MutableLiveData<String>()
    val chosenLanguage: LiveData<String> = _chosenLanguage

    private val _chosenSeason = MutableLiveData<Int>()
    val chosenSeason: LiveData<Int> = _chosenSeason

    private val _episodeNames = MutableLiveData<List<TitleEpisodes>>()
    val episodeNames: LiveData<List<TitleEpisodes>> = _episodeNames


    fun onPlayButtonPressed(titleId: Int, isTvShow: Boolean) {
        navigateToNewFragment(
                ChooseTitleDetailsFragmentDirections.actionChooseTitleDetailsFragmentToVideoPlayerFragmentNav(
                        VideoPlayerData(
                                titleId,
                                isTvShow,
                                if (isTvShow) 1 else 0,
                                chosenLanguage.value!!,
                                if (isTvShow) 1 else 0,
                                0L,
                                null
                        )
                ),
        )
    }

    fun onEpisodePressed(titleId: Int, isTvShow: Boolean, chosenEpisode: Int) {
        navigateToNewFragment(
                ChooseTitleDetailsFragmentDirections.actionChooseTitleDetailsFragmentToVideoPlayerFragmentNav(
                        VideoPlayerData(
                                titleId,
                                isTvShow,
                                chosenSeason.value!!,
                                chosenLanguage.value!!,
                                chosenEpisode,
                                0L,
                                null
                        )
                )
        )
    }

    fun onContinueWatchingPressed(dbDetails: DbDetails) {
        navigateToNewFragment(
                ChooseTitleDetailsFragmentDirections.actionChooseTitleDetailsFragmentToVideoPlayerFragmentNav(
                        VideoPlayerData(
                                dbDetails.titleId,
                                dbDetails.isTvShow,
                                dbDetails.season,
                                dbDetails.language,
                                dbDetails.episode,
                                dbDetails.watchedDuration,
                                null
                        )
                ))
    }

    fun checkTitleInDb(context: Context, titleId: Int): LiveData<Boolean> {
        val database = ImoviesDatabase.getDatabase(context)?.getDao()
        return repository.checkTitleInDb(database!!, titleId)
    }

    fun getTitleDbDetails(context: Context, titleId: Int): LiveData<DbDetails> {
        val database = ImoviesDatabase.getDatabase(context)?.getDao()
        return repository.getSingleWatchedTitles(database!!, titleId)
    }

    fun setFileLanguage(language: String) {
        _chosenLanguage.value = language
    }

    fun getSeasonFiles(titleId: Int, season: Int) {
        _chosenSeason.value = season
        viewModelScope.launch {
            chooseDetailsLoader.value = LoadingState.LOADING
            when (val files = repository.getSingleTitleFiles(titleId, season)) {
                is Result.Success -> {
                    val data = files.data.data
                    if (data.isNotEmpty()) {
                        val fetchLanguages: MutableList<String> = ArrayList()

                        data[0].files.forEach {
                            fetchLanguages.add(it.lang)
                        }
                        _availableLanguages.value = fetchLanguages

                        val getEpisodeNames: MutableList<TitleEpisodes> = ArrayList()
                        data.forEach {
                            getEpisodeNames.add(TitleEpisodes(it.episode, it.title, it.covers.x1050!!))
                        }
                        _episodeNames.value = getEpisodeNames

                        _movieNotYetAdded.value = false
                    } else {
                        _movieNotYetAdded.value = true
                    }
                    chooseDetailsLoader.value = LoadingState.LOADED
                }
                is Result.Error -> {
                    when (files.exception) {
                        AppConstants.UNKNOWN_ERROR -> {
                            _movieNotYetAdded.value = true
                        }
                        else -> {
                            newToastMessage(files.exception)
                        }
                    }
                }
                is Result.Internet -> {
                    setNoInternet()
                }
            }
        }
    }
}