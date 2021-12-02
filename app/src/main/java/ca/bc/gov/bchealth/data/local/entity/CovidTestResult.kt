package ca.bc.gov.bchealth.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import kotlinx.parcelize.Parcelize

/*
* Created by amit_metri on 26,November,2021
*/
@Parcelize
@Entity(tableName = "covid_test_results")
class CovidTestResult(

    /*
    * reportId is considered as Primary key to save each test result in a separate row.
    * Each member can have multiple tests done over a period of time.
    * */
    @PrimaryKey
    val reportId: String,
    var patientDisplayName: String,
    val lab: String,
    val collectionDateTime: Date,
    val resultDateTime: Date,
    val testName: String,
    val testType: String,
    val testStatus: String,
    val testOutcome: String,
    val resultTitle: String,
    val resultDescription: String,
    val resultLink: String,

    // For future use. userId can be mapped once user identity is implemented
    val userId: String
) : Parcelable
