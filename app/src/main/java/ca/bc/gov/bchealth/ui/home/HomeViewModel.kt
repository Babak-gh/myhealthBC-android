package ca.bc.gov.bchealth.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.utils.COMMUNICATION_BANNER_MAX_LENGTH
import ca.bc.gov.bchealth.utils.INDEX_NOT_FOUND
import ca.bc.gov.bchealth.utils.fromHtml
import ca.bc.gov.bchealth.workers.WorkerInvoker
import ca.bc.gov.common.exceptions.ServiceDownException
import ca.bc.gov.common.model.AuthenticationStatus
import ca.bc.gov.common.model.banner.BannerDto
import ca.bc.gov.common.utils.toDate
import ca.bc.gov.common.utils.yyyy_MM_dd
import ca.bc.gov.repository.BannerRepository
import ca.bc.gov.repository.OnBoardingRepository
import ca.bc.gov.repository.bcsc.BcscAuthRepo
import ca.bc.gov.repository.bcsc.PostLoginCheck
import ca.bc.gov.repository.immunization.ImmunizationRecommendationRepository
import ca.bc.gov.repository.patient.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val onBoardingRepository: OnBoardingRepository,
    private val patientRepository: PatientRepository,
    private val bcscAuthRepo: BcscAuthRepo,
    private val workerInvoker: WorkerInvoker,
    recommendationRepository: ImmunizationRecommendationRepository,
    private val bannerRepository: BannerRepository,
) : ViewModel() {

    private var bannerRequested = false

    private val _bannerState = MutableStateFlow<BannerItem?>(null)
    val bannerState: StateFlow<BannerItem?> = _bannerState.asStateFlow()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    var isAuthenticationRequired: Boolean = true
    var isForceLogout: Boolean = false

    private val _homeList = MutableStateFlow<List<HomeRecordItem>?>(null)
    val homeList: StateFlow<List<HomeRecordItem>?> = _homeList.asStateFlow()

    private val recommendationItem = HomeRecordItem(
        R.drawable.ic_recommendation,
        R.string.home_recommendations_title,
        R.string.home_recommendations_body,
        R.drawable.ic_right_arrow,
        R.string.learn_more,
        HomeNavigationType.RECOMMENDATIONS
    )

    private val displayRecommendations =
        recommendationRepository.getAllRecommendations().map { list ->
            val isLoggedIn: Boolean = try {
                bcscAuthRepo.checkSession()
            } catch (e: Exception) {
                false
            }
            list.isNotEmpty() && isLoggedIn
        }

    fun launchCheck() = viewModelScope.launch {
        if (bcscAuthRepo.checkSession()) {
            onBoardingRepository.onBCSCLoginRequiredPostBiometric = false
        }
        when {
            onBoardingRepository.onBoardingRequired -> {
                _uiState.update { state ->
                    state.copy(isLoading = false, isOnBoardingRequired = true)
                }
            }

            onBoardingRepository.isReOnBoardingRequired -> {
                _uiState.update { state ->
                    state.copy(isLoading = false, isReOnBoardingRequired = true)
                }
            }

            isAuthenticationRequired -> {
                _uiState.update { state -> state.copy(isAuthenticationRequired = true) }
            }

            onBoardingRepository.onBCSCLoginRequiredPostBiometric -> {
                _uiState.update { state -> state.copy(isBcscLoginRequiredPostBiometrics = true) }
            }

            bcscAuthRepo.getPostLoginCheck() == PostLoginCheck.IN_PROGRESS.name -> {
                _uiState.update { state -> state.copy(isForceLogout = true) }
            }
        }
    }

    fun onBoardingShown() {
        _uiState.update {
            it.copy(isOnBoardingRequired = false, isReOnBoardingRequired = false)
        }
    }

    fun onAuthenticationRequired(isRequired: Boolean) {
        isAuthenticationRequired = isRequired
        _uiState.update { state -> state.copy(isAuthenticationRequired = isRequired) }
    }

    fun onBcscLoginRequired(isRequired: Boolean) {
        onBoardingRepository.onBCSCLoginRequiredPostBiometric = isRequired
        _uiState.update { state -> state.copy(isBcscLoginRequiredPostBiometrics = isRequired) }
    }

    fun onForceLogout(isRequired: Boolean) {
        isForceLogout = isRequired
        _uiState.update { state -> state.copy(isForceLogout = isRequired) }
    }

    fun getAuthenticatedPatientName() = viewModelScope.launch {
        try {
            val patient =
                patientRepository.findPatientByAuthStatus(AuthenticationStatus.AUTHENTICATED)
            val names = patient.fullName.split(" ")
            val firstName = if (names.isNotEmpty()) names.first() else ""
            _uiState.update {
                it.copy(patientFirstName = firstName)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(patientFirstName = "")
            }
        }
    }

    suspend fun getHomeRecordsList() {
        val isLoggedIn: Boolean = try {
            bcscAuthRepo.checkSession()
        } catch (e: Exception) {
            false
        }

        val list = mutableListOf(
            HomeRecordItem(
                R.drawable.ic_login_info,
                R.string.health_records,
                R.string.health_records_desc,
                0,
                if (isLoggedIn) R.string.view_records else R.string.get_started,
                HomeNavigationType.HEALTH_RECORD
            ),
            HomeRecordItem(
                R.drawable.ic_resources,
                R.string.health_resources,
                R.string.resources_desc,
                R.drawable.ic_right_arrow,
                R.string.learn_more,
                HomeNavigationType.RESOURCES
            ),
            HomeRecordItem(
                R.drawable.ic_green_tick,
                R.string.health_passes,
                R.string.proof_of_vaccination_desc,
                R.drawable.ic_right_arrow,
                R.string.add_proofs,
                HomeNavigationType.VACCINE_PROOF
            ),
        )

        _homeList.update { list }
        displayRecommendations.collect {
            manageRecommendationCard(it)
        }
    }

    private fun manageRecommendationCard(displayCard: Boolean) {
        _homeList.value?.let { list ->
            val cardIndex = list.indexOfFirst {
                it.recordType == HomeNavigationType.RECOMMENDATIONS
            }

            if (displayCard) {
                if (cardIndex == INDEX_NOT_FOUND) {
                    _homeList.update {
                        list.toMutableList().apply { add(1, recommendationItem) }
                    }
                }
            } else {
                if (cardIndex > INDEX_NOT_FOUND) {
                    _homeList.update {
                        list.toMutableList().apply { removeAt(cardIndex) }
                    }
                }
            }
        }
    }

    fun executeOneTimeDataFetch() {
        fetchBanner()
        workerInvoker.executeOneTimeDataFetch()
    }

    private fun fetchBanner() {
        if (bannerRequested.not()) {
            viewModelScope.launch {

                try {
                    callBannerRepository()
                } catch (e: Exception) {
                    when (e) {
                        is ServiceDownException -> displayServiceDownMessage()
                        else -> e.printStackTrace()
                    }
                }
            }

            bannerRequested = true
        }
    }

    private fun displayServiceDownMessage() {
        _uiState.update { state ->
            state.copy(displayServiceDownMessage = true)
        }
    }

    private suspend fun callBannerRepository() {
        bannerRepository.getBanner()?.apply {
            if (validateBannerDates(this)) {
                _bannerState.update {
                    BannerItem(
                        title = title,
                        date = startDate.toDate(yyyy_MM_dd),
                        body = body,
                        displayReadMore = shouldDisplayReadMore(body),
                    )
                }
            }
        }
    }

    private fun validateBannerDates(bannerDto: BannerDto): Boolean {
        val currentTime = System.currentTimeMillis()
        return bannerDto.startDate.toEpochMilli() <= currentTime &&
            currentTime < bannerDto.endDate.toEpochMilli()
    }

    fun toggleBanner() {
        _bannerState.update { it?.copy(expanded = it.expanded.not()) }
    }

    fun dismissBanner() {
        _bannerState.update { it?.copy(isHidden = true) }
    }

    private fun shouldDisplayReadMore(body: String): Boolean =
        body.fromHtml().length > COMMUNICATION_BANNER_MAX_LENGTH

    fun resetUiState() {
        _uiState.tryEmit(HomeUiState())
    }
}

data class BannerItem(
    val title: String,
    val date: String,
    val body: String,
    val displayReadMore: Boolean,
    var expanded: Boolean = true,
    var isHidden: Boolean = false
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isOnBoardingRequired: Boolean = false,
    val isReOnBoardingRequired: Boolean = false,
    val isAuthenticationRequired: Boolean = false,
    val isBcscLoginRequiredPostBiometrics: Boolean = false,
    val patientFirstName: String? = null,
    val isForceLogout: Boolean = false,
    val displayServiceDownMessage: Boolean = false
)

data class HomeRecordItem(
    @DrawableRes val iconTitle: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    @StringRes val btnTitle: Int,
    val recordType: HomeNavigationType
)

enum class HomeNavigationType {
    HEALTH_RECORD,
    RECOMMENDATIONS,
    RESOURCES,
    VACCINE_PROOF,
}
