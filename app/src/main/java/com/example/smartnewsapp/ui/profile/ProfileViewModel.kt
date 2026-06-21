package com.example.smartnewsapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnewsapp.data.local.InterestProfile
import com.example.smartnewsapp.domain.InterestGraphManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    interestGraphManager: InterestGraphManager
) : ViewModel() {

    val interestProfile = interestGraphManager.getInterestProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

}
