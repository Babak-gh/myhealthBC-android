package ca.bc.gov.data

import ca.bc.gov.common.const.SERVER_ERROR
import ca.bc.gov.common.const.SERVER_ERROR_DATA_MISMATCH
import ca.bc.gov.common.const.SERVER_ERROR_INCORRECT_PHN
import ca.bc.gov.common.exceptions.MyHealthNetworkException
import ca.bc.gov.data.remote.ImmunizationApi
import ca.bc.gov.data.remote.model.base.Action
import ca.bc.gov.data.remote.model.request.VaccineStatusRequest
import ca.bc.gov.data.remote.model.request.toMap
import ca.bc.gov.data.remote.model.response.VaccineStatusResponse
import ca.bc.gov.data.utils.safeCall
import javax.inject.Inject

/**
 * @author Pinakin Kansara
 */
class ImmunizationRemoteDataSource @Inject constructor(
    private val immunizationApi: ImmunizationApi
) {

    suspend fun getVaccineStatus(request: VaccineStatusRequest): VaccineStatusResponse {
        val response = safeCall { immunizationApi.getVaccineStatus(request.toMap()) }
            ?: throw MyHealthNetworkException(SERVER_ERROR, "Invalid response")

        if (response.error != null) {
            if (Action.MISMATCH.code == response.error.action?.code) {
                throw MyHealthNetworkException(SERVER_ERROR_DATA_MISMATCH, response.error.message)
            }
            if ("Error parsing phn" == response.error.message) {
                throw MyHealthNetworkException(SERVER_ERROR_INCORRECT_PHN, response.error.message)
            }
            throw MyHealthNetworkException(SERVER_ERROR, response.error.message)
        }
        if (!isResponseValid(response)) {
            throw MyHealthNetworkException(SERVER_ERROR, "Invalid Response")
        }
        return response
    }

    private fun isResponseValid(response: VaccineStatusResponse): Boolean {
        var isValid = false
        with(response.payload) {
            isValid = when {
                firstName.isNullOrBlank()
                    || lastName.isNullOrBlank()
                    || phn.isNullOrBlank()
                    || birthDate.isNullOrBlank()
                    || vaccineDate.isNullOrBlank()
                    || qrCode.data.isNullOrBlank()
                    || qrCode.encoding.isNullOrBlank()
                    || qrCode.mediaType.isNullOrBlank()
                    || federalVaccineProof.data.isNullOrBlank()
                    || federalVaccineProof.mediaType.isNullOrBlank()
                    || federalVaccineProof.encoding.isNullOrBlank() -> {
                    false
                }
                else -> {
                    true
                }
            }
        }
        return isValid
    }
}
