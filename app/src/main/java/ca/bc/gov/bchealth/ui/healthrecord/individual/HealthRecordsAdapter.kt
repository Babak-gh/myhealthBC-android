package ca.bc.gov.bchealth.ui.healthrecord.individual

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.bc.gov.bchealth.databinding.ItemHealthRecordsAbstractBinding
import ca.bc.gov.bchealth.ui.filter.TimelineTypeFilter
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.COVID_TEST_RECORD
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.HEALTH_VISIT_RECORD
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.HOSPITAL_VISITS_RECORD
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.IMMUNIZATION_RECORD
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.LAB_TEST
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.MEDICATION_RECORD
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.SPECIAL_AUTHORITY_RECORD
import ca.bc.gov.bchealth.ui.healthrecord.individual.HealthRecordType.VACCINE_RECORD
import ca.bc.gov.common.utils.toDate
import ca.bc.gov.common.utils.toStartOfDayInstant
/**
 * @author Pinakin Kansara
 */
class HealthRecordsAdapter(
    private val itemClickListener: ItemClickListener
) :
    ListAdapter<HealthRecordItem, HealthRecordsAdapter.ViewHolder>(HealthRecordDiffCallBacks()),
    Filterable {

    private lateinit var defaultList: List<HealthRecordItem>

    fun interface ItemClickListener {
        fun onItemClick(record: HealthRecordItem)
    }

    class ViewHolder(val binding: ItemHealthRecordsAbstractBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemHealthRecordsAbstractBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        holder.binding.tvTitle.text = record.title
        var description = ""
        holder.binding.imgIcon.setImageResource(record.icon)
        when (record.healthRecordType) {
            VACCINE_RECORD -> {
                description = record.date.toDate()
            }
            COVID_TEST_RECORD -> {
                description = "${record.testOutcome} • ${record.date.toDate()}"
            }
            MEDICATION_RECORD -> {
                description = "${record.description} • ${record.date.toDate()}"
            }
            LAB_TEST -> {
                description = record.description
            }
            IMMUNIZATION_RECORD -> {
                description = record.date.toDate()
            }
            HEALTH_VISIT_RECORD -> {
                description = "${record.description} • ${record.date.toDate()}"
            }
            SPECIAL_AUTHORITY_RECORD -> {
                description = "${record.description} • ${record.date.toDate()}"
            }
            HOSPITAL_VISITS_RECORD -> {
                description = record.description
            }
        }

        holder.binding.tvDesc.text = description
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(record)
        }
    }

    fun setData(list: List<HealthRecordItem>) {
        defaultList = list
        submitList(list)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val filteredList = mutableListOf<HealthRecordItem>()
                val tempList = mutableListOf<HealthRecordItem>()
                if (charSequence.isNullOrBlank()) {
                    return FilterResults()
                } else {
                    val list = charSequence.split(",")

                    val fromDate = list.find { it.contains("FROM:") }?.substringAfter(":")
                    val toDate = list.find { it.contains("TO:") }?.substringAfter(":")

                    tempList.addAll(getFilterByDate(fromDate, toDate))

                    list.forEach { type ->
                        when (type) {
                            TimelineTypeFilter.ALL.name -> {
                                filteredList.addAll(tempList)
                            }
                            TimelineTypeFilter.MEDICATION.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == MEDICATION_RECORD })
                            }
                            TimelineTypeFilter.LAB_TEST.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == LAB_TEST })
                            }
                            TimelineTypeFilter.COVID_19_TEST.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == COVID_TEST_RECORD })
                            }
                            TimelineTypeFilter.IMMUNIZATION.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == IMMUNIZATION_RECORD })
                            }
                            TimelineTypeFilter.HEALTH_VISIT.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == HEALTH_VISIT_RECORD })
                            }
                            TimelineTypeFilter.SPECIAL_AUTHORITY.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == SPECIAL_AUTHORITY_RECORD })
                            }
                            TimelineTypeFilter.HOSPITAL_VISITS.name -> {
                                filteredList.addAll(tempList.filter { it.healthRecordType == HOSPITAL_VISITS_RECORD })
                            }
                        }
                    }
                    return FilterResults().apply {
                        values = filteredList.sortedByDescending { it.date }
                    }
                }
            }

            override fun publishResults(constraint: CharSequence?, result: FilterResults?) {
                if (result?.values == null) {
                    submitList(emptyList())
                } else {
                    submitList(result.values as List<HealthRecordItem>)
                }
            }
        }
    }

    private fun getFilterByDate(fromDate: String?, toDate: String?): MutableList<HealthRecordItem> {
        return if (!fromDate.isNullOrBlank() && !toDate.isNullOrBlank()) {
            defaultList.filter { it.date.toStartOfDayInstant() >= fromDate.toDate() && it.date <= toDate.toDate() }
                .toMutableList()
        } else if (!fromDate.isNullOrBlank()) {
            defaultList.filter { it.date.toStartOfDayInstant() >= fromDate.toDate() }
                .toMutableList()
        } else if (!toDate.isNullOrBlank()) {
            defaultList.filter { it.date.toStartOfDayInstant() <= toDate.toDate() }.toMutableList()
        } else {
            defaultList.toMutableList()
        }
    }
}

class HealthRecordDiffCallBacks : DiffUtil.ItemCallback<HealthRecordItem>() {
    override fun areItemsTheSame(oldItem: HealthRecordItem, newItem: HealthRecordItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: HealthRecordItem, newItem: HealthRecordItem): Boolean {
        return oldItem.title == newItem.title
    }
}
