package ca.bc.gov.bchealth.ui.healthrecord

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.databinding.FragmentHealthRecordsBinding
import ca.bc.gov.bchealth.utils.viewBindings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * @author Pinakin Kansara
 */
@AndroidEntryPoint
class HealthRecordsFragment : Fragment(R.layout.fragment_health_records) {

    private val binding by viewBindings(FragmentHealthRecordsBinding::bind)
    private val viewModel: HealthRecordsViewModel by viewModels()
    private lateinit var adapter: HealthRecordsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                collectHealthRecordsFlow()
            }
        }

        adapter = HealthRecordsAdapter {
            val action =
                HealthRecordsFragmentDirections.actionHealthRecordsFragmentToIndividualHealthRecordFragment(
                    it.patientId.toLong()
                )
            findNavController().navigate(action)
        }

        binding.rvMembers.adapter = adapter
        binding.rvMembers.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private suspend fun collectHealthRecordsFlow() {
        viewModel.patientHealthRecords.collect { records ->
            if (records.isEmpty()) {
                findNavController().navigate(R.id.action_healthRecordsFragment_to_addHealthRecordsFragment)
            }
            if (::adapter.isInitialized) {
                adapter.submitList(records)
            }
        }
    }
}