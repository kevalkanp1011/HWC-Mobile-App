package org.piramalswasthya.cho.ui.commons.history_custom.FieldsFragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.piramalswasthya.cho.R
import org.piramalswasthya.cho.adapter.FamilyMemberAdapter
import org.piramalswasthya.cho.adapter.IllnessAdapter
import org.piramalswasthya.cho.databinding.FragmentAAFragmentsBinding
import org.piramalswasthya.cho.databinding.FragmentIllnessFieldsBinding
import org.piramalswasthya.cho.model.FamilyMemberDropdown
import org.piramalswasthya.cho.model.IllnessDropdown
import org.piramalswasthya.cho.ui.HistoryFieldsInterface

@AndroidEntryPoint
class AAFragments : Fragment() {

    private val AA = arrayOf(
                "Asthama",
                "Diabetes Mellitus",
                "Epilepsy(Convulsions)",
                "Hemiplegia/Stroke",
                "HIV/AIDS",
                "Hypertension",
                "Ischemic Heart Disease",
                "Syphilis",
                "Thyroid Problem",
                "Other"
    )

    private val TimePeriodAgo = arrayOf(
        "Day(s)",
        "Week(s)",
        "Month(s)",
        "Year(s)"
    )
    private var _binding: FragmentAAFragmentsBinding? = null
    private val binding: FragmentAAFragmentsBinding
        get() = _binding!!

    private lateinit var dropdownAA: AutoCompleteTextView
    private lateinit var dropdownTimePeriodAgo: AutoCompleteTextView
    val viewModel: AssociatedAilmentsViewModel by viewModels()
    private var historyListener: HistoryFieldsInterface? = null
    private var familyOption = ArrayList<FamilyMemberDropdown>()
    private lateinit var familyAdapter: FamilyMemberAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAAFragmentsBinding.inflate(inflater, container,false)
        return binding.root
    }
    private var fragmentTag :String? = null
    fun setFragmentTag(tag:String){
        fragmentTag = tag
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dropdownAA = binding.aaText
        dropdownTimePeriodAgo = binding.dropdownDurUnit
        val aaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, AA)
        dropdownAA.setAdapter(aaAdapter)
        val timePeriodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,TimePeriodAgo)
        dropdownTimePeriodAgo.setAdapter(timePeriodAdapter)

//        familyAdapter = FamilyMemberAdapter(requireContext(), R.layout.drop_down,familyOption)
//        binding.familyText.setAdapter(familyAdapter)
//
//        viewModel.familyDropdown.observe( viewLifecycleOwner) { aa ->
//            familyOption.clear()
//            familyOption.addAll(aa)
//            familyAdapter.notifyDataSetChanged()
//        }
        val familyAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item)
        //AlcoholAdapter(requireContext(), R.layout.drop_down,alcoholOption)
        binding.familyText.setAdapter(familyAdapter)

        viewModel.familyDropdown.observe( viewLifecycleOwner) { alc ->
//            alcoholOption.clear()
//            alcoholOption.addAll(alc)
            familyAdapter.clear()
            familyAdapter.addAll(alc.map { it.benRelationshipType })
            familyAdapter.notifyDataSetChanged()
        }

        binding.deleteButton.setOnClickListener {
            fragmentTag?.let {
                historyListener?.onDeleteButtonClickedAA(it)
            }
        }
        binding.plusButton.setOnClickListener {
            fragmentTag?.let {
                historyListener?.onAddButtonClickedAA(it)
            }
        }

        binding.plusButton.isEnabled = false
        binding.resetButton.isEnabled = false
        binding.dropdownDurUnit.addTextChangedListener(inputTextWatcher)
        binding.inputDuration.addTextChangedListener(inputTextWatcher)
        binding.aaText.addTextChangedListener(inputTextWatcher)
        binding.resetButton.setOnClickListener {
            binding.dropdownDurUnit.text?.clear()
            binding.inputDuration.text?.clear()
            binding.aaText.text?.clear()
        }

        dropdownAA.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Check if the selected item is "Other"
                val selectedOption = s.toString()
                val isOtherSelected = selectedOption.equals("Other", ignoreCase = true)

                // Show or hide the otherTextField based on the selection
                if (isOtherSelected) {
                    // Show the otherTextField
                    binding.otherTextFieldLayout.visibility = View.VISIBLE
                } else {
                    // Hide the otherTextField
                    binding.otherTextFieldLayout.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun setListener(listener: HistoryFieldsInterface) {
        this.historyListener = listener
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // No action needed
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateAddAndResetButtonState()
        }

        override fun afterTextChanged(s: Editable?) {
            // No action needed
        }
    }

    private fun updateAddAndResetButtonState() {
        val durationUnit = binding.dropdownDurUnit.text.toString().trim()
        val duration = binding.inputDuration.text.toString().trim()
        val chiefComplaint = binding.aaText.text.toString().trim()
        val familym = binding.inputDuration.text.toString().trim()
        val inC = binding.aaText.text.toString().trim()
        binding.plusButton.isEnabled = durationUnit.isNotEmpty()&&duration.isNotEmpty()&&chiefComplaint.isNotEmpty()&&familym.isNotEmpty()&&inC.isNotEmpty()
        binding.resetButton.isEnabled = durationUnit.isNotEmpty()&&duration.isNotEmpty()&&chiefComplaint.isNotEmpty()&&familym.isNotEmpty()&&inC.isNotEmpty()

    }

}