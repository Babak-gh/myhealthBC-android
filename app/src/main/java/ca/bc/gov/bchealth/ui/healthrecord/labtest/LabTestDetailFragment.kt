package ca.bc.gov.bchealth.ui.healthrecord.labtest

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.DividerItemDecoration
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentLabTestDetailBinding
import ca.bc.gov.bchealth.ui.BaseFragment
import ca.bc.gov.bchealth.utils.AlertDialogHelper
import ca.bc.gov.bchealth.utils.PdfHelper
import ca.bc.gov.bchealth.utils.showNoInternetConnectionMessage
import ca.bc.gov.bchealth.utils.showServiceDownMessage
import ca.bc.gov.bchealth.utils.viewBindings
import ca.bc.gov.bchealth.viewmodel.PdfDecoderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class LabTestDetailFragment : BaseFragment(R.layout.fragment_lab_test_detail) {

    private val binding by viewBindings(FragmentLabTestDetailBinding::bind)
    private val viewModel: LabTestDetailViewModel by viewModels()
    private lateinit var labTestDetailAdapter: LabTestDetailAdapter
    private val args: LabTestDetailFragmentArgs by navArgs()
    private val pdfDecoderViewModel: PdfDecoderViewModel by viewModels()
    private var fileInMemory: File? = null
    private var resultListener = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        fileInMemory?.delete()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        viewModel.getLabTestDetails(args.labOrderId)
        observeUiState()
        observePdfData()
    }

    private fun initUI() {
        setUpRecyclerView()
    }

    private lateinit var menuInflated: Menu
    override fun setToolBar(appBarConfiguration: AppBarConfiguration) {
        with(binding.layoutToolbar.topAppBar) {
            setNavigationIcon(R.drawable.ic_toolbar_back)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            title = getString(R.string.filter)
            inflateMenu(R.menu.menu_lab_test_details)
            menuInflated = menu
            setOnMenuItemClickListener { menu ->
                when (menu.itemId) {
                    R.id.menu_download -> viewModel.getLabTestPdf()
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun setUpRecyclerView() {
        labTestDetailAdapter = LabTestDetailAdapter()
        binding.rvLabTestDetailList.apply {
            adapter = labTestDetailAdapter
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.isVisible = state.onLoading

                    handledServiceDown(state)

                    if (state.labTestDetails?.isNotEmpty() == true) {
                        labTestDetailAdapter.submitList(state.labTestDetails)
                        binding.layoutToolbar.topAppBar.title = state.toolbarTitle
                    }

                    if (state.onError) {
                        showError()
                        viewModel.resetUiState()
                    }

                    handlePdfDownload(state)

                    handleNoInternetConnection(state)
                }
            }
        }
    }

    private fun handledServiceDown(state: LabTestDetailUiState) {
        if (!state.isHgServicesUp) {
            binding.root.showServiceDownMessage(requireContext())
            viewModel.resetUiState()
        }
    }

    private fun handleNoInternetConnection(uiState: LabTestDetailUiState) {
        if (!uiState.isConnected) {
            binding.root.showNoInternetConnectionMessage(requireContext())
            viewModel.resetUiState()
        }
    }

    private fun handlePdfDownload(state: LabTestDetailUiState) {

        menuInflated.getItem(0).isVisible = state.showDownloadOption

        if (state.pdfData?.isNotEmpty() == true) {
            pdfDecoderViewModel.base64ToPDFFile(state.pdfData)
            viewModel.resetUiState()
        }
    }

    private fun showError() {
        AlertDialogHelper.showAlertDialog(
            context = requireContext(),
            title = getString(R.string.error),
            msg = getString(R.string.error_message),
            positiveBtnMsg = getString(R.string.dialog_button_ok)
        )
    }

    private fun observePdfData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pdfDecoderViewModel.uiState.collect { uiState ->
                    uiState.pdf?.let {
                        val (base64Pdf, file) = it
                        if (file != null) {
                            try {
                                fileInMemory = file
                                PdfHelper().showPDF(file, requireActivity(), resultListener)
                            } catch (e: Exception) {
                                fallBackToPdfRenderer(base64Pdf)
                            }
                        } else {
                            fallBackToPdfRenderer(base64Pdf)
                        }
                        pdfDecoderViewModel.resetUiState()
                    }
                }
            }
        }
    }

    private fun fallBackToPdfRenderer(federalTravelPass: String) {
        findNavController().navigate(
            R.id.pdfRendererFragment,
            bundleOf(
                "base64pdf" to federalTravelPass,
                "title" to getString(R.string.lab_test)
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (fileInMemory != null) {
            fileInMemory?.delete()
            fileInMemory = null
        }
    }
}
