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
import org.piramalswasthya.cho.adapter.AlcoholAdapter
import org.piramalswasthya.cho.adapter.IllnessAdapter
import org.piramalswasthya.cho.databinding.FragmentAlcoholBinding
import org.piramalswasthya.cho.databinding.FragmentTobaccoBinding
import org.piramalswasthya.cho.model.AlcoholDropdown
import org.piramalswasthya.cho.model.IllnessDropdown
import org.piramalswasthya.cho.ui.HistoryFieldsInterface
@AndroidEntryPoint
class AlcoholFragment : Fragment() {

    private var _binding: FragmentAlcoholBinding? = null
    private val binding: FragmentAlcoholBinding
        get() = _binding!!

    private var historyListener: HistoryFieldsInterface? = null
//    private var alcoholOption = ArrayList<AlcoholDropdown>()
    private lateinit var alcoholAdapter: AlcoholAdapter
    val viewModel: AlcoholFieldViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAlcoholBinding.inflate(inflater, container,false)
        return binding.root
    }
    private var fragmentTag :String? = null
    fun setFragmentTag(tag:String){
        fragmentTag = tag
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val alcoholAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item)
                //AlcoholAdapter(requireContext(), R.layout.drop_down,alcoholOption)
        binding.alcoholText.setAdapter(alcoholAdapter)

        viewModel.alcoholDropdown.observe( viewLifecycleOwner) { alc ->
//            alcoholOption.clear()
//            alcoholOption.addAll(alc)
            alcoholAdapter.clear()
            alcoholAdapter.addAll(alc.map { it.habitValue })
            alcoholAdapter.notifyDataSetChanged()
        }

        binding.deleteButton.setOnClickListener {
            fragmentTag?.let {
                historyListener?.onDeleteButtonClickedAlcohol(it)
            }
        }
        binding.plusButton.setOnClickListener {
            fragmentTag?.let {
                historyListener?.onAddButtonClickedAlcohol(it)
            }
        }

        binding.plusButton.isEnabled = false
        binding.resetButton.isEnabled = false
        binding.alcoholText.addTextChangedListener(inputTextWatcher)
        binding.resetButton.setOnClickListener {
            binding.alcoholText.text?.clear()
        }
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
        val durationUnit = binding.alcoholText.text.toString().trim()
        binding.plusButton.isEnabled = durationUnit.isNotEmpty()
        binding.resetButton.isEnabled = durationUnit.isNotEmpty()
    }


}