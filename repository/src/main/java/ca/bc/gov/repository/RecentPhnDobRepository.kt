package ca.bc.gov.repository

import ca.bc.gov.preference.EncryptedPreferenceStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * @author Pinakin Kansara
 */
class RecentPhnDobRepository @Inject constructor(
    private val encryptedPreferenceStorage: EncryptedPreferenceStorage
) {

    val recentPhnDob: Flow<Pair<String, String>> = encryptedPreferenceStorage.recentPhnDobData.map {
        val data = it.split(",")
        Pair(data[0], data[1])
    }

    suspend fun setRecentPhnDob(phn: String, dob: String) {
        encryptedPreferenceStorage.setRecentPhnDob(phn.plus(",").plus(dob))
    }
}
