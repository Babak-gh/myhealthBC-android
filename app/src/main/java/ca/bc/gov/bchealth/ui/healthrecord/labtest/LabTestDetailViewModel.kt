package ca.bc.gov.bchealth.ui.healthrecord.labtest

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bc.gov.bchealth.R
import ca.bc.gov.common.exceptions.NetworkConnectionException
import ca.bc.gov.common.model.labtest.LabOrderWithLabTestDto
import ca.bc.gov.common.utils.toDate
import ca.bc.gov.common.utils.toDateTimeString
import ca.bc.gov.repository.labtest.LabOrderRepository
import ca.bc.gov.repository.worker.MobileConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabTestDetailViewModel @Inject constructor(
    private val labOrderRepository: LabOrderRepository,
    private val mobileConfigRepository: MobileConfigRepository
) :
    ViewModel() {

    private val _uiState = MutableStateFlow(LabTestDetailUiState())
    val uiState: StateFlow<LabTestDetailUiState> = _uiState.asStateFlow()

    fun getLabTestDetails(labOrderId: Long) = viewModelScope.launch {
        try {
            _uiState.update {
                it.copy(onLoading = true)
            }
            val labOrderWithLabTestsAndPatientDto = labOrderRepository.findByLabOrderId(labOrderId)

            _uiState.update {
                it.copy(
                    onLoading = false,
                    labPdfId = labOrderWithLabTestsAndPatientDto.labOrderWithLabTest.labOrder.labPdfId,
                    labTestDetails = prepareLabTestDetailsData(labOrderWithLabTestsAndPatientDto.labOrderWithLabTest),
                    toolbarTitle = labOrderWithLabTestsAndPatientDto.labOrderWithLabTest.labOrder.commonName,
                    showDownloadOption = labOrderWithLabTestsAndPatientDto.labOrderWithLabTest.labOrder.reportingAvailable
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    onError = true
                )
            }
        }
    }

    private fun prepareLabTestDetailsData(labOrderWithLabTestDto: LabOrderWithLabTestDto): List<LabTestDetail> {
        val labTestDetails = mutableListOf<LabTestDetail>()
        labTestDetails.add(
            LabTestDetail(
                title1 = R.string.collection_date,
                collectionDateTime = labOrderWithLabTestDto.labOrder.collectionDateTime?.toDate(),
                timelineDateTime = labOrderWithLabTestDto.labOrder.timelineDateTime.toDateTimeString(),
                title2 = R.string.ordering_provider,
                orderingProvider = labOrderWithLabTestDto.labOrder.orderingProvider,
                title3 = R.string.reporting_lab,
                reportingSource = labOrderWithLabTestDto.labOrder.reportingSource
            )
        )

        var testStatus = 0
        labOrderWithLabTestDto.labTests.forEachIndexed { index, labTest ->
            var result: Boolean? = null
            when {
                labTest.testStatus.equals("Active", true) -> {
                    testStatus = R.string.pending
                }
                labTest.testStatus.equals("cancelled", true) -> {
                    testStatus = R.string.cancelled
                }
                labTest.testStatus.equals("corrected", true) -> {
                    testStatus = R.string.corrected
                    result = labTest.outOfRange
                }
                else -> {
                    testStatus = R.string.completed
                    result = labTest.outOfRange
                }
            }

            if (index == 0) {
                labTestDetails.add(
                    LabTestDetail(
                        id = labTest.id,
                        header = R.string.test_summary,
                        summary = R.string.summary_desc,
                        title1 = R.string.test_name,
                        testName = labTest.batteryType,
                        title2 = R.string.result,
                        isOutOfRange = result,
                        title3 = R.string.lab_test_status,
                        testStatus = testStatus,
                        viewType = ITEM_VIEW_TYPE_LAB_TEST
                    )
                )
            } else {
                labTestDetails.add(
                    LabTestDetail(
                        id = labTest.id,
                        title1 = R.string.test_name,
                        testName = labTest.batteryType,
                        title2 = R.string.result,
                        isOutOfRange = result,
                        title3 = R.string.lab_test_status,
                        testStatus = testStatus,
                        viewType = ITEM_VIEW_TYPE_LAB_TEST
                    )
                )
            }
        }

        if (labOrderWithLabTestDto.labTests.isEmpty() && testStatus == 0) {
            labTestDetails.add(
                0,
                LabTestDetail(
                    bannerTitle = R.string.results_are_pending,
                    bannerText = R.string.lab_test_detail_banner_1,
                    viewType = ITEM_VIEW_TYPE_LAB_TEST_BANNER
                )
            )
        }
        when (testStatus) {
            R.string.pending -> {
                labTestDetails.add(
                    0,
                    LabTestDetail(
                        bannerTitle = R.string.results_are_pending,
                        bannerText = R.string.lab_test_detail_banner_2,
                        viewType = ITEM_VIEW_TYPE_LAB_TEST_BANNER
                    )
                )
            }
            R.string.cancelled -> {
                labTestDetails.add(
                    0,
                    LabTestDetail(
                        bannerTitle = R.string.lab_test_has_been_cancelled,
                        bannerText = R.string.find_resources_to_learn_about_lab_test,
                        viewType = ITEM_VIEW_TYPE_LAB_TEST_BANNER
                    )
                )
            }
        }

        return labTestDetails
    }

    fun getLabTestPdf(labOrderId: String) = viewModelScope.launch {
        _uiState.update {
            it.copy(onLoading = true)
        }
        try {
            val isHgServicesUp = mobileConfigRepository.getBaseUrl()
            if (isHgServicesUp) {
                val pdfData = labOrderRepository.fetchLabTestPdf(labOrderId, false)
                _uiState.update {
                    it.copy(pdfData = pdfData)
                }
            } else {
                _uiState.update {
                    it.copy(
                        onLoading = false,
                        isHgServicesUp = false
                    )
                }
            }
        } catch (e: java.lang.Exception) {
            when (e) {
                is NetworkConnectionException -> {
                    _uiState.update {
                        it.copy(
                            onLoading = false,
                            isConnected = false
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            onLoading = false,
                            onError = true
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val ITEM_VIEW_TYPE_LAB_ORDER = 0
        const val ITEM_VIEW_TYPE_LAB_TEST = 1
        const val ITEM_VIEW_TYPE_LAB_TEST_BANNER = 2
    }

    fun resetUiState() {
        _uiState.update {
            it.copy(
                onLoading = false,
                onError = false,
                labTestDetails = null,
                toolbarTitle = null,
                showDownloadOption = false,
                pdfData = null,
                isConnected = true,
                isHgServicesUp = true
            )
        }
    }
}

data class LabTestDetailUiState(
    val onLoading: Boolean = false,
    val onError: Boolean = false,
    val labPdfId: String? = null,
    val labTestDetails: List<LabTestDetail>? = null,
    val toolbarTitle: String? = null,
    val showDownloadOption: Boolean = false,
    val pdfData: String? = null,
    val isHgServicesUp: Boolean = true,
    val isConnected: Boolean = true
)

data class LabTestDetail(
    var id: Long = -1L,
    @StringRes var header: Int? = null,
    @StringRes var summary: Int? = null,
    @StringRes val title1: Int? = null,
    @StringRes val title2: Int? = null,
    @StringRes val title3: Int? = null,
    val collectionDateTime: String? = "N/A",
    val timelineDateTime: String? = null,
    val orderingProvider: String? = "N/A",
    val reportingSource: String? = "N/A",
    val testName: String? = "N/A",
    val isOutOfRange: Boolean? = null,
    val testStatus: Int? = null,
    val bannerTitle: Int? = null,
    val bannerText: Int? = null,
    val viewType: Int = LabTestDetailViewModel.ITEM_VIEW_TYPE_LAB_ORDER
)
