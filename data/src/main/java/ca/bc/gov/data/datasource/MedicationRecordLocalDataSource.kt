package ca.bc.gov.data.datasource

import ca.bc.gov.common.model.DispensingPharmacyDto
import ca.bc.gov.common.model.MedicationRecordDto
import ca.bc.gov.common.model.MedicationSummaryDto
import ca.bc.gov.common.model.relation.MedicationWithSummaryAndPharmacyDto
import ca.bc.gov.data.local.dao.DispensingPharmacyDao
import ca.bc.gov.data.local.dao.MedicationRecordDao
import ca.bc.gov.data.local.dao.MedicationSummaryDao
import ca.bc.gov.data.model.mapper.toDto
import ca.bc.gov.data.model.mapper.toEntity
import javax.inject.Inject

/**
 * @author Pinakin Kansara
 */
class MedicationRecordLocalDataSource @Inject constructor(
    private val medicationRecordDao: MedicationRecordDao,
    private val medicationSummaryDao: MedicationSummaryDao,
    private val dispensingPharmacyDao: DispensingPharmacyDao
) {

    suspend fun insert(medicationRecord: MedicationRecordDto): Long {
        var medicationRecordId =
            medicationRecordDao.insert(medicationRecord.toEntity())
        if (medicationRecordId == -1L) {
            medicationRecordId =
                medicationRecordDao.getMedicationRecordId(medicationRecord.dispenseDate) ?: -1L
        }
        return medicationRecordId
    }

    suspend fun insert(medicationSummary: MedicationSummaryDto): Long {
        return medicationSummaryDao.insert(medicationSummary.toEntity())
    }

    suspend fun insert(dispensingPharmacy: DispensingPharmacyDto): Long {
        return dispensingPharmacyDao.insert(dispensingPharmacy.toEntity())
    }

    suspend fun getMedicationWithSummaryAndPharmacy(medicationRecordId: Long): MedicationWithSummaryAndPharmacyDto?
    = medicationRecordDao.getMedicationWithSummaryAndPharmacy(medicationRecordId)?.toDto()
}
