package ca.bc.gov.bchealth.usecases.records

import ca.bc.gov.common.BuildConfig
import ca.bc.gov.common.model.AuthParametersDto
import ca.bc.gov.common.model.services.DiagnosticImagingDataDto
import ca.bc.gov.common.model.services.OrganDonorDto
import ca.bc.gov.common.model.services.PatientDataTypeDto
import ca.bc.gov.repository.di.IoDispatcher
import ca.bc.gov.repository.services.DiagnosticImagingRepository
import ca.bc.gov.repository.services.OrganDonorRepository
import ca.bc.gov.repository.services.PatientServicesRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class FetchPatientDataUseCase @Inject constructor(
    private val patientServicesRepository: PatientServicesRepository,
    private val organDonorRepository: OrganDonorRepository,
    private val diagnosticImagingRepository: DiagnosticImagingRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseRecordUseCase(dispatcher) {

    suspend fun execute(
        patientId: Long,
        authParameters: AuthParametersDto
    ) {

        val patientDataList = patientServicesRepository.fetchPatientData(authParameters.hdid)

        if (BuildConfig.FLAG_SERVICE_TAB) {
            val data = patientDataList.firstOrNull { it.type == PatientDataTypeDto.ORGAN_DONOR_REGISTRATION }
            data?.let {
                val organDonorDto = it as OrganDonorDto
                organDonorDto.patientId = patientId
                organDonorRepository.insert(organDonorDto)
            }
        }

        if (BuildConfig.FLAG_DIAGNOSTIC_IMAGING) {
            val data = patientDataList.filter { it.type == PatientDataTypeDto.DIAGNOSTIC_IMAGING_EXAM }
                .map {
                    val diagnosticImagingDataDto = it as DiagnosticImagingDataDto
                    diagnosticImagingDataDto.patientId = patientId
                    return@map diagnosticImagingDataDto
                }
            diagnosticImagingRepository.insert(data)
        }
    }
}
