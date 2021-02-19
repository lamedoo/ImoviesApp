package com.lukakordzaia.streamflow.ui.phone.profile

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lukakordzaia.streamflow.database.ImoviesDatabase
import com.lukakordzaia.streamflow.datamodels.*
import com.lukakordzaia.streamflow.network.Result
import com.lukakordzaia.streamflow.repository.TraktRepository
import com.lukakordzaia.streamflow.ui.baseclasses.BaseViewModel
import com.lukakordzaia.streamflow.ui.phone.MainActivity
import com.lukakordzaia.streamflow.ui.tv.TvActivity
import com.lukakordzaia.streamflow.utils.AppConstants
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: TraktRepository) : BaseViewModel() {
    private val _traktDeviceCode = MutableLiveData<TraktvDeviceCode>()
    val traktDeviceCode: LiveData<TraktvDeviceCode> = _traktDeviceCode

    private var counter: Long = 0
    private val validationCounter = MutableLiveData<Long>(0)

    private val _traktUserToken = MutableLiveData<TraktGetToken>()
    val traktUserToken: LiveData<TraktGetToken> = _traktUserToken

    private val _traktSfListExists = MutableLiveData<Boolean>()
    val traktSfListExists: LiveData<Boolean> = _traktSfListExists

    private val _traktUserProfile = MutableLiveData<TraktUserProfile>()
    val traktUserProfile: LiveData<TraktUserProfile> = _traktUserProfile

    fun getDeviceCode() {
        viewModelScope.launch {
            when (val deviceCode = repository.getDeviceCode()) {
                is Result.Success -> {
                    val data = deviceCode.data

                    _traktDeviceCode.value = data
                }
            }
        }
    }

    fun getUserToken(tokenRequest: TraktRequestToken) {
        viewModelScope.launch {
            when (val token = repository.getUserToken(tokenRequest)) {
                is Result.Success -> {
                    val data = token.data

                    _traktUserToken.value = data

                    getSfList("Bearer ${data.accessToken}")
                    getUserProfile("Bearer ${data.accessToken}")
                }
                is Result.Error -> {
                    when (token.exception) {
                        AppConstants.TRAKT_PENDING_AUTH -> {
                            if (validationCounter.value!! < "${traktDeviceCode.value!!.expiresIn}00".toLong()) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    getUserToken(tokenRequest)
                                }, 5000)

                                counter += 5000
                                validationCounter.value = counter
                            }
                        }
                    }
                }
            }
        }
    }

    fun createNewList(newList: TraktNewList, accessToken: String) {
        viewModelScope.launch {
            when (val list = repository.createNewList(newList, accessToken)) {
                is Result.Success -> {
                    Log.d("traktvlist", "წარმატებულია")
                }
            }
        }
    }

    private fun getSfList(accessToken: String) {
        viewModelScope.launch {
            when (val sfList = repository.getSfList(accessToken)) {
                is Result.Success -> {
                    _traktSfListExists.value = true
                    Log.d("traktvlist", sfList.data.toString())
                }
                is Result.Error -> {
                    when (sfList.exception) {
                        AppConstants.TRAKT_NOT_FOUND -> {
                            _traktSfListExists.value = false
                        }
                    }
                }
            }
        }
    }

    fun getUserProfile(accessToken: String) {
        viewModelScope.launch {
            when (val user = repository.getUserProfile(accessToken)) {
                is Result.Success -> {
                    _traktUserProfile.value = user.data
                }
            }
        }
    }

    fun deleteWatchedHistory(context: Context) {
        viewModelScope.launch {
            ImoviesDatabase.getDatabase(context)?.getDao()?.deleteDBContent()
        }
    }

    fun onDeletePressedPhone(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onDeletePressedTv(context: Context) {
        val intent = Intent(context, TvActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}