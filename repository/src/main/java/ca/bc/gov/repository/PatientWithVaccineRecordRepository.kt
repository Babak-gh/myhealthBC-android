package ca.bc.gov.repository

import ca.bc.gov.common.const.DATABASE_ERROR
import ca.bc.gov.common.exceptions.MyHealthException
import ca.bc.gov.common.model.relation.PatientAndVaccineRecord
import ca.bc.gov.data.datasource.PatientWithVaccineRecordLocalDataSource
import ca.bc.gov.data.local.entity.PatientOrderUpdate
import ca.bc.gov.repository.model.PatientVaccineRecord
import ca.bc.gov.repository.model.mapper.toPatient
import ca.bc.gov.repository.model.mapper.toVaccineRecord
import ca.bc.gov.repository.patient.PatientRepository
import ca.bc.gov.repository.vaccine.VaccineDoseRepository
import ca.bc.gov.repository.vaccine.VaccineRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * @author Pinakin Kansara
 */
class PatientWithVaccineRecordRepository @Inject constructor(
    private val localDataSource: PatientWithVaccineRecordLocalDataSource,
    private val patientRepository: PatientRepository,
    private val vaccineRecordRepository: VaccineRecordRepository,
    private val vaccineDoseRepository: VaccineDoseRepository,
    private val qrCodeGeneratorRepository: QrCodeGeneratorRepository
) {

    val patientsVaccineRecord: Flow<List<PatientVaccineRecord>> =
        localDataSource.patientsVaccineRecord.map { patientsVaccineRecords ->
            patientsVaccineRecords.filter { record ->
                record.vaccineRecord != null
            }.map { record ->

                val vaccineRecord = record.vaccineRecord!!.toVaccineRecord()
                vaccineRecord.qrCodeImage =
                    qrCodeGeneratorRepository.generateQRCode(vaccineRecord.shcUri!!)
                PatientVaccineRecord(
                    record.patient.toPatient(),
                    vaccineRecord
                )
            }
        }

    /**
     * Insert Vaccine Record [PatientVaccineRecord]
     * @param patientVaccineRecord
     * @return inserted recordId or -1L
     */
    suspend fun insertPatientsVaccineRecord(patientVaccineRecord: PatientVaccineRecord): Long {
        val patientId =
            patientRepository.insertPatient(patientVaccineRecord.patientDto)
        patientVaccineRecord.vaccineRecordDto.patientId = patientId
        val vaccineRecordId = vaccineRecordRepository.insertVaccineRecord(
            patientVaccineRecord.vaccineRecordDto
        )
        patientVaccineRecord.vaccineRecordDto.doseDtos.forEach { vaccineDose ->
            vaccineDose.vaccineRecordId = vaccineRecordId
        }
        val dosesId =
            vaccineDoseRepository.insertAllVaccineDose(patientVaccineRecord.vaccineRecordDto.doseDtos)
        return patientId
    }

    suspend fun updatePatientVaccineRecord(patientVaccineRecord: PatientVaccineRecord): Long {
        val patientId =
            patientRepository.updatePatient(patientVaccineRecord.patientDto)
        val vaccineRecordId = vaccineRecordRepository.getVaccineRecordId(patientId) ?: return -1L
        vaccineDoseRepository.deleteVaccineDose(vaccineRecordId)
        val vaccineRecord = patientVaccineRecord.vaccineRecordDto
        vaccineRecord.patientId = patientId
        vaccineRecord.id = vaccineRecordId
        val id = vaccineRecordRepository.updateVaccineRecord(
            patientVaccineRecord.vaccineRecordDto
        )
        println("id = $id")
        patientVaccineRecord.vaccineRecordDto.doseDtos.forEach { vaccineDose ->
            vaccineDose.vaccineRecordId = vaccineRecordId
        }
        val vaccineDoseIds =
            vaccineDoseRepository.insertAllVaccineDose(patientVaccineRecord.vaccineRecordDto.doseDtos)
        return patientId
    }

    suspend fun getPatientWithVaccine(patientId: Long): PatientAndVaccineRecord =
        localDataSource.getPatientWithVaccineRecord(patientId) ?: throw MyHealthException(
            DATABASE_ERROR, "No record found for patient id=  $patientId"
        )

    suspend fun updatePatientOrder(patientOrderMapping: List<Pair<Long, Long>>) {
        localDataSource.updatePatientOrder(
            patientOrderMapping.map {
                PatientOrderUpdate(it.first, it.second)
            }
        )
    }
}
