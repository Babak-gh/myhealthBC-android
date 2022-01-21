package ca.bc.gov.bchealth.ui.healthpass.add

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentFetchVaccineRecordBinding
import ca.bc.gov.bchealth.ui.custom.setUpDatePickerUi
import ca.bc.gov.bchealth.ui.custom.validateDatePickerData
import ca.bc.gov.bchealth.ui.custom.validatePhnNumber
import ca.bc.gov.bchealth.utils.redirect
import ca.bc.gov.bchealth.utils.showError
import ca.bc.gov.bchealth.utils.viewBindings
import ca.bc.gov.bchealth.viewmodel.AnalyticsFeatureViewModel
import ca.bc.gov.bchealth.viewmodel.RecentPhnDobViewModel
import ca.bc.gov.common.model.analytics.AnalyticsAction
import ca.bc.gov.common.model.analytics.AnalyticsActionData
import com.queue_it.androidsdk.Error
import com.queue_it.androidsdk.QueueITEngine
import com.queue_it.androidsdk.QueueListener
import com.queue_it.androidsdk.QueuePassedInfo
import com.queue_it.androidsdk.QueueService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * @author Pinakin Kansara
 */
@AndroidEntryPoint
class FetchVaccineRecordFragment : Fragment(R.layout.fragment_fetch_vaccine_record) {
    private val binding by viewBindings(FragmentFetchVaccineRecordBinding::bind)
    private val viewModel: FetchVaccineRecordViewModel by viewModels()
    private lateinit var savedStateHandle: SavedStateHandle
    private val analyticsFeatureViewModel: AnalyticsFeatureViewModel by viewModels()
    private val recentPhnDobViewModel: RecentPhnDobViewModel by viewModels()

    companion object {
        private const val TAG = "FetchVaccineRecordFragment"
        const val VACCINE_RECORD_ADDED_SUCCESS = "VACCINE_RECORD_ADDED_SUCCESS"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        savedStateHandle.set(VACCINE_RECORD_ADDED_SUCCESS, null)

        setupToolBar()

        setUpPhnUI()

        setUpDobUI()

        setUpDovUI()

        initClickListeners()

        observeCovidTestResult()
    }

    private fun showLoader(value: Boolean) {
        binding.btnSubmit.isEnabled = !value
        binding.progressBar.isVisible = value
    }

    private fun observeCovidTestResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->

                    showLoader(uiState.onLoading)

                    if (uiState.errorData != null) {
                        requireContext().showError(
                            getString(uiState.errorData.title),
                            getString(uiState.errorData.message)
                        )
                    }

                    if (uiState.queItTokenUpdated) {
                        fetchVaccineRecord()
                    }

                    if (uiState.onMustBeQueued && uiState.queItUrl != null) {
                        Log.d(TAG, "Mut be queue, url = ${uiState.queItUrl}")
                        queUser(uiState.queItUrl)
                    }

                    if (uiState.vaccineRecord != null) {
                        savedStateHandle.set(VACCINE_RECORD_ADDED_SUCCESS, uiState.vaccineRecord)
                        analyticsFeatureViewModel.track(
                            AnalyticsAction.ADD_QR,
                            AnalyticsActionData.GET
                        )
                        if (binding.checkboxRemember.isChecked) {
                            val phn = binding.edPhn.text.toString()
                            val dob = binding.edDob.text.toString()
                            recentPhnDobViewModel.setRecentPhnDobData(phn, dob)
                        }
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.btnSubmit.setOnClickListener {
            fetchVaccineRecord()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun fetchVaccineRecord() {
        val phn = binding.edPhn.text.toString()
        val dob = binding.edDob.text.toString()
        val dov = binding.edDov.text.toString()

        if (this.validatePhnNumber(
                binding.edPhnNumber
            ) &&
            this.validateDatePickerData(
                    binding.tipDob,
                    getString(R.string.dob_required)
                ) &&
            this.validateDatePickerData(
                    binding.tipDov,
                    getString(R.string.dov_required)
                )
        ) {

            viewModel.fetchVaccineRecord(phn, dob, dov)
        }
    }

    private fun setUpDovUI() {
        this.setUpDatePickerUi(binding.tipDov, "DATE_OF_VACCINATION")
    }

    private fun setUpDobUI() {
        this.setUpDatePickerUi(binding.tipDob, "DATE_OF_BIRTH")
    }

    private fun setUpPhnUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recentPhnDobViewModel.recentPhnDob.collect { recentPhnDob ->
                    val (phn, dob) = recentPhnDob
                    val phnArray = arrayOf(phn)

                    val adapter: ArrayAdapter<String> = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        phnArray
                    )

                    val textView = binding.edPhnNumber.editText as AutoCompleteTextView
                    textView.setAdapter(adapter)
                    textView.onItemClickListener =
                        AdapterView.OnItemClickListener { _, _, _, _ ->
                            binding.tipDob.editText?.setText(dob)
                            binding.tipDov.editText?.requestFocus()
                        }

                    binding.edPhnNumber.setEndIconDrawable(R.drawable.ic_arrow_down)
                    binding.edPhnNumber.setEndIconOnClickListener {
                        textView.showDropDown()
                    }
                }
            }
        }
    }

    private fun setupToolBar() {
        binding.toolbar.apply {
            ivLeftOption.visibility = View.VISIBLE
            ivLeftOption.setImageResource(R.drawable.ic_action_back)
            ivLeftOption.setOnClickListener {
                findNavController().popBackStack()
            }

            tvTitle.visibility = View.VISIBLE
            tvTitle.text = getString(R.string.add_a_health_pass)

            ivRightOption.visibility = View.VISIBLE
            ivRightOption.setImageResource(R.drawable.ic_help)
            ivRightOption.setOnClickListener {
                requireActivity().redirect(getString(R.string.url_help))
            }
            ivRightOption.contentDescription = getString(R.string.help)

            line1.visibility = View.VISIBLE
        }
    }

    private fun queUser(value: String) {
        try {
            val uri = Uri.parse(URLDecoder.decode(value, StandardCharsets.UTF_8.name()))
            val customerId = uri.getQueryParameter("c")
            val waitingRoomId = uri.getQueryParameter("e")
            QueueService.IsTest = false
            val queueITEngine = QueueITEngine(
                requireActivity(),
                customerId,
                waitingRoomId,
                "",
                "",
                object : QueueListener() {
                    override fun onQueuePassed(queuePassedInfo: QueuePassedInfo?) {
                        Log.d(TAG, "onQueuePassed: updatedToken ${queuePassedInfo?.queueItToken}")
                        viewModel.setQueItToken(queuePassedInfo?.queueItToken)
                    }

                    override fun onQueueViewWillOpen() {
                    }

                    override fun onQueueDisabled() {
                    }

                    override fun onQueueItUnavailable() {
                    }

                    override fun onError(error: Error?, errorMessage: String?) {
                    }
                }
            )
            queueITEngine.run(requireActivity())
        } catch (e: Exception) {
        }
    }
}
