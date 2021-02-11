package com.lukakordzaia.medootv.ui.phone.searchtitles

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lukakordzaia.medootv.network.Result
import com.lukakordzaia.medootv.datamodels.TitleList
import com.lukakordzaia.medootv.repository.SearchTitleRepository
import com.lukakordzaia.medootv.ui.baseclasses.BaseViewModel
import kotlinx.coroutines.launch

class SearchTitlesViewModel(private val repository: SearchTitleRepository) : BaseViewModel() {
    private val _searchList = MutableLiveData<List<TitleList.Data>>()
    val searchList: LiveData<List<TitleList.Data>> = _searchList

    private val fetchSearchTitleList: MutableList<TitleList.Data> = ArrayList()

    fun onSingleTitlePressed(titleId: Int) {
        navigateToNewFragment(SearchTitlesFragmentDirections.actionSearchTitlesFragmentToSingleTitleFragmentNav(titleId))
    }

    fun clearSearchResults() {
        fetchSearchTitleList.clear()
    }

    fun getSearchTitles(keywords: String, page: Int) {
        viewModelScope.launch {
            when (val movies = repository.getSearchTitles(keywords, page)) {
                is Result.Success -> {
                    val data = movies.data.data
                    data.forEach {
                        fetchSearchTitleList.add(it)
                    }
                    _searchList.value = fetchSearchTitleList
                }
                is Result.Error -> {
                    Log.d("errornewmovies", movies.exception)
                }
            }
        }
    }

    fun getSearchTitlesTv(keywords: String, page: Int) {
        viewModelScope.launch {
            when (val movies = repository.getSearchTitles(keywords, page)) {
                is Result.Success -> {
                    val data = movies.data.data
                    _searchList.value = data
                }
                is Result.Error -> {
                    Log.d("errornewmovies", movies.exception)
                }
            }
        }
    }
}