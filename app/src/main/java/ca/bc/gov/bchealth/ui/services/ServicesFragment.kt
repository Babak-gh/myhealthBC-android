package ca.bc.gov.bchealth.ui.services

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.compose.MyHealthTheme
import ca.bc.gov.bchealth.ui.BaseFragment
import ca.bc.gov.bchealth.ui.custom.MyHealthToolBar
import ca.bc.gov.bchealth.ui.healthrecord.HealthRecordPlaceholderFragment
import ca.bc.gov.bchealth.ui.healthrecord.NavigationAction
import ca.bc.gov.bchealth.ui.login.BcscAuthViewModel
import ca.bc.gov.bchealth.ui.login.LoginStatus
import ca.bc.gov.bchealth.utils.PdfHelper
import ca.bc.gov.bchealth.utils.launchAndRepeatWithLifecycle
import ca.bc.gov.bchealth.utils.observeWork
import ca.bc.gov.bchealth.utils.redirect
import ca.bc.gov.bchealth.viewmodel.PdfDecoderViewModel
import ca.bc.gov.repository.bcsc.BACKGROUND_AUTH_RECORD_FETCH_WORK_NAME
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * @author Pinakin Kansara
 */
@AndroidEntryPoint
class ServicesFragment : BaseFragment(null) {

    private val pdfDecoderViewModel: PdfDecoderViewModel by viewModels()
    private val servicesViewModel: ServicesViewModel by viewModels()
    private val bcscAuthViewModel: BcscAuthViewModel by viewModels()
    private var isHealthRecordsFlowActive = false
    private var fileInMemory: File? = null
    private var resultListener = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        fileInMemory?.delete()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchAndRepeatWithLifecycle {
            handlePDFState()
        }

        observeNavigationFlow()

        observeHealthRecordsSyncCompletion()

        if (!isHealthRecordsFlowActive) {
            isHealthRecordsFlowActive = true
            launchAndRepeatWithLifecycle(Lifecycle.State.RESUMED) {
                bcscAuthViewModel.authStatus.collect {

                    if (it.showLoading) {
                        servicesViewModel.showProgressBar()
                    } else {
                        when (it.loginStatus) {
                            LoginStatus.ACTIVE -> {
                                observeHealthRecordsSyncCompletion()
                            }
                            LoginStatus.EXPIRED -> {
                                findNavController().navigate(R.id.bcServiceCardSessionFragment)
                            }
                            LoginStatus.NOT_AUTHENTICATED -> {
                                findNavController().navigate(R.id.bcServicesCardLoginFragment)
                            }
                        }
                    }
                }
            }
            bcscAuthViewModel.checkSession()
        }
    }

    @Composable
    override fun GetComposableLayout() {
        MyHealthTheme {
            Scaffold(
                topBar = {
                    MyHealthToolBar(
                        title = "",
                        actions = {
                            IconButton(onClick = { findNavController().navigate(R.id.settingsFragment) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = stringResource(
                                        id = R.string.settings
                                    ),
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }
                    )
                },
                content = {
                    ServicesScreen(
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(it),
                        viewModel = servicesViewModel,
                        onRegisterOnUpdateDecisionClicked = { url ->
                            requireActivity().redirect(url)
                        },
                        onDownloadButtonClicked = { file ->
                            pdfDecoderViewModel.base64ToPDFFile(file)
                        }
                    )
                },
                contentColor = contentColorFor(backgroundColor = MaterialTheme.colors.background)
            )
        }
    }

    private fun observeHealthRecordsSyncCompletion() {
        observeWork(BACKGROUND_AUTH_RECORD_FETCH_WORK_NAME) { state ->
            if (state == WorkInfo.State.RUNNING) {
                servicesViewModel.showProgressBar()
            } else {
                servicesViewModel.getOrganDonationStatus()
            }
        }
    }

    private suspend fun handlePDFState() {
        pdfDecoderViewModel.uiState.collect { uiState ->
            if (uiState.pdf != null) {
                val (federalTravelPass, file) = uiState.pdf
                if (file != null) {
                    try {
                        fileInMemory = file
                        PdfHelper().showPDF(file, requireActivity(), resultListener)
                    } catch (e: Exception) {
                        navigateToViewOrganDonorDecision(federalTravelPass)
                    }
                } else {
                    navigateToViewOrganDonorDecision(federalTravelPass)
                }
                pdfDecoderViewModel.resetUiState()
            }
        }
    }

    private fun navigateToViewOrganDonorDecision(organDonorDecisionFile: String) {
        findNavController().navigate(
            R.id.pdfRendererFragment,
            bundleOf(
                "base64pdf" to organDonorDecisionFile,
                "title" to getString(R.string.organ_donor_registration_title)
            )
        )
    }

    private fun observeNavigationFlow() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<NavigationAction>(
            HealthRecordPlaceholderFragment.PLACE_HOLDER_NAVIGATION
        )?.observe(viewLifecycleOwner) {
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<NavigationAction>(
                HealthRecordPlaceholderFragment.PLACE_HOLDER_NAVIGATION
            )
            it?.let {
                when (it) {
                    NavigationAction.ACTION_BACK -> {
                        findNavController().popBackStack()
                    }
                    NavigationAction.ACTION_RE_CHECK -> {
                        bcscAuthViewModel.checkSession()
                    }
                }
            }
        }
    }
}
