package ca.bc.gov.bchealth.ui.healthrecord.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bc.gov.bchealth.R
import ca.bc.gov.common.const.SERVER_ERROR_DATA_MISMATCH
import ca.bc.gov.common.const.SERVER_ERROR_INCORRECT_PHN
import ca.bc.gov.common.exceptions.MyHealthException
import ca.bc.gov.common.exceptions.NetworkConnectionException
import ca.bc.gov.common.model.ErrorData
import ca.bc.gov.repository.FetchTestResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author Pinakin Kansara
 */
@HiltViewModel
class FetchTestRecordsViewModel @Inject constructor(
    private val repository: FetchTestResultRepository
) : ViewModel() {

    private val _uiState =
        MutableSharedFlow<FetchTestRecordUiState>(replay = 0, extraBufferCapacity = 1)
    val uiState: SharedFlow<FetchTestRecordUiState> = _uiState.asSharedFlow()

    fun fetchTestRecord(phn: String, dateOfBirth: String, collectionDate: String) =
        viewModelScope.launch {

            _uiState.tryEmit(
                FetchTestRecordUiState(
                    onLoading = true
                )
            )

            try {
                val (patientId, tesTestResultId) = repository.fetchCovidTestRecord(
                    phn,
                    dateOfBirth,
                    collectionDate
                )
                _uiState.tryEmit(
                    FetchTestRecordUiState(
                        onTestResultFetched = tesTestResultId,
                        patientId = patientId
                    )
                )
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionException -> {
                        _uiState.tryEmit(
                            FetchTestRecordUiState(
                                onLoading = false,
                                isConnected = false
                            )
                        )
                    }
                    is MyHealthException -> {
                        when (e.errCode) {
                            SERVER_ERROR_DATA_MISMATCH -> {
                                _uiState.tryEmit(
                                    FetchTestRecordUiState(
                                        errorData = ErrorData(
                                            R.string.error_data_mismatch_title,
                                            R.string.error_test_result_data_mismatch_message
                                        )
                                    )
                                )
                            }
                            SERVER_ERROR_INCORRECT_PHN -> {
                                _uiState.tryEmit(
                                    FetchTestRecordUiState(
                                        errorData = ErrorData(
                                            R.string.error_data_mismatch_title,
                                            R.string.error_vaccine_data_mismatch_message
                                        )
                                    )
                                )
                            }
                            else -> {
                                _uiState.tryEmit(
                                    FetchTestRecordUiState(
                                        errorData = ErrorData(
                                            R.string.error,
                                            R.string.error_message
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
}

data class FetchTestRecordUiState(
    val onLoading: Boolean = false,
    val onTestResultFetched: Long = -1L,
    val isError: Boolean = false,
    val errorData: ErrorData? = null,
    val patientId: Long = -1L,
    val isConnected: Boolean = true
)
