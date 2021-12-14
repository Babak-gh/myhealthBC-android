package ca.bc.gov.data

import ca.bc.gov.common.const.SERVER_ERROR
import ca.bc.gov.common.exceptions.MyHealthException
import ca.bc.gov.data.remote.ImmunizationApi
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

    //TODO: add object validation logic here
    suspend fun getVaccineStatus(request: VaccineStatusRequest): VaccineStatusResponse {
        val response = safeCall { immunizationApi.getVaccineStatus(request.toMap()) }
            ?: throw MyHealthException(SERVER_ERROR, "Invalid response")

        if (response.error != null) {
            throw MyHealthException(SERVER_ERROR, response.error.message)
        }
        return response
    }
}