package org.piramalswasthya.cho.ui.commons.personal_details

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.piramalswasthya.cho.R
import org.piramalswasthya.cho.adapter.PatientItemAdapter
import org.piramalswasthya.cho.database.shared_preferences.PreferenceDao
import org.piramalswasthya.cho.databinding.FragmentPersonalDetailsBinding
import org.piramalswasthya.cho.model.NetworkBody
import org.piramalswasthya.cho.model.PatientDisplayWithVisitInfo
import org.piramalswasthya.cho.network.ESanjeevaniApiService
import org.piramalswasthya.cho.network.interceptors.TokenESanjeevaniInterceptor
import org.piramalswasthya.cho.repositories.CaseRecordeRepo
import org.piramalswasthya.cho.repositories.VisitReasonsAndCategoriesRepo
import org.piramalswasthya.cho.repositories.VitalsRepo
import org.piramalswasthya.cho.ui.abha_id_activity.AbhaIdActivity
import org.piramalswasthya.cho.ui.commons.SpeechToTextContract
import org.piramalswasthya.cho.ui.edit_patient_details_activity.EditPatientDetailsActivity
import org.piramalswasthya.cho.ui.home.HomeViewModel
import org.piramalswasthya.cho.ui.web_view_activity.WebViewActivity
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import javax.inject.Inject


@AndroidEntryPoint
class PersonalDetailsFragment : Fragment() {
    @Inject
    lateinit var apiService : ESanjeevaniApiService
    private lateinit var viewModel: PersonalDetailsViewModel
    private lateinit var homeviewModel: HomeViewModel
    private var itemAdapter : PatientItemAdapter? = null
    private var usernameEs : String = ""
    private var passwordEs : String = ""
    private var errorEs : String = ""
    private var network : Boolean = false

    @Inject
    lateinit var preferenceDao: PreferenceDao
    @Inject
    lateinit var caseRecordeRepo: CaseRecordeRepo
    @Inject
    lateinit var visitReasonsAndCategoriesRepo: VisitReasonsAndCategoriesRepo
    @Inject
    lateinit var vitalsRepo: VitalsRepo
    private var _binding: FragmentPersonalDetailsBinding? = null
    private var patientCount : Int = 0

    private val binding
        get() = _binding!!

    private val abhaDisclaimer by lazy {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.beneficiary_abha_number))
            .setMessage("it")
            .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
            .create()
    }
//    private val parentViewModel: HomeViewModel by lazy {
//        ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
//    }
//private val parentViewModel: HomeViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        HomeViewModel.resetSearchBool()
        _binding = FragmentPersonalDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        homeviewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
//        parentViewModel.searchBool.observe((viewLifecycleOwner)){
        HomeViewModel.searchBool.observe(viewLifecycleOwner){
            bool ->
            when(bool!!) {
                true ->{
//                    binding.search.post {
//                        lifecycleScope.launch {
//                            withContext(Dispatchers.IO){
//                                delay(5000)
//                            }
                            binding.search.requestFocus()
                            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                        }

//                    }
//                }
//                    Handler(Looper.getMainLooper()).postDelayed(
//                    {
//                    binding.search.requestFocus()
//            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
//            imm?.showSoftInput(binding.search, InputMethodManager.SHOW_FORCED);
//                }
//            , 100)
                else -> {}
            }

        }
        binding.searchTil.setEndIconOnClickListener {
            speechToTextLauncherForSearchByName.launch(Unit)
        }
        viewModel = ViewModelProvider(this).get(PersonalDetailsViewModel::class.java)
        viewModel.patientObserver.observe(viewLifecycleOwner) { state ->
            when (state!!) {
                PersonalDetailsViewModel.NetworkState.SUCCESS -> {
                    var result = ""
                    if(itemAdapter?.itemCount==0||itemAdapter?.itemCount==1) {
                        result = getString(R.string.patient_cnt_display)
                    }
                    else {
                        result = getString(R.string.patients_cnt_display)
                    }
                     itemAdapter = context?.let { it ->
                         PatientItemAdapter(
                            apiService,
                            it,
//                            onItemClicked = {
//                                val intent = Intent(context, EditPatientDetailsActivity::class.java)
//                                intent.putExtra("patientId", it.patient.patientID);
//                                startActivity(intent)
//                            },
                            clickListener = PatientItemAdapter.BenClickListener(
                            {
                                benVisitInfo ->
                                    if(preferenceDao.isRegistrarSelected()){

                                    }
                                    else if( benVisitInfo.nurseFlag == 9 && benVisitInfo.doctorFlag == 2 && preferenceDao.isDoctorSelected() ){
                                         Toast.makeText(
                                            requireContext(),
                                            resources.getString(R.string.pendingForLabtech),
                                            Toast.LENGTH_SHORT
                                         ).show()
                                    }
                                    else if( benVisitInfo.nurseFlag == 9 && benVisitInfo.doctorFlag == 9 && preferenceDao.isDoctorSelected() ){
                                        Toast.makeText(
                                            requireContext(),
                                            resources.getString(R.string.flowCompleted),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else{
                                        var modifiedInfo = benVisitInfo
                                        if(preferenceDao.isNurseSelected()){
                                            modifiedInfo = PatientDisplayWithVisitInfo(benVisitInfo)
                                        }
                                        val intent = Intent(context, EditPatientDetailsActivity::class.java)
                                        intent.putExtra("benVisitInfo", modifiedInfo);
                                        startActivity(intent)
                                        requireActivity().finish()
                                    }
                            },
                            {
                                benVisitInfo ->
                                    Log.d("ben click listener", "ben click listener")
                                    checkAndGenerateABHA(benVisitInfo)
                            },
                            {
                                benVisitInfo -> callLoginDialog(benVisitInfo)
                            },
                            {
                                benVisitInfo ->
                                    lifecycleScope.launch {
                                        generatePDF(benVisitInfo)
                                    }

                            },
                            {
                                benVisitInfo ->  openDialog(benVisitInfo)
                            }
                         ),
                            showAbha = true
                        )
                    }

                    binding.patientListContainer.patientList.adapter = itemAdapter

                    if(preferenceDao.isRegistrarSelected() || preferenceDao.isNurseSelected()){
                        lifecycleScope.launch {
                            viewModel.patientListForNurse?.collect { it ->
                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
                                binding.patientListContainer.patientCount.text =
                                    it.size.toString() + getResultStr(it.size)
                                patientCount = it.size
                            }
                        }
                    }
                    else if(preferenceDao.isDoctorSelected()){
                        lifecycleScope.launch {
                            viewModel.patientListForDoctor?.collect { it ->
                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
                                binding.patientListContainer.patientCount.text =
                                    it.size.toString() + getResultStr(it.size)
                                patientCount = it.size
                            }
                        }
                    }
                    else if(preferenceDao.isLabSelected()){
                        lifecycleScope.launch {
                            viewModel.patientListForLab?.collect { it ->
                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
                                binding.patientListContainer.patientCount.text =
                                    it.size.toString() + getResultStr(it.size)
                                patientCount = it.size
                            }
                        }
                    }
                    else if(preferenceDao.isPharmaSelected()){
                        lifecycleScope.launch {
                            viewModel.patientListForPharmacist?.collect { it ->
                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
                                binding.patientListContainer.patientCount.text =
                                    itemAdapter?.itemCount.toString() + getResultStr(itemAdapter?.itemCount)
                                patientCount = it.size
                            }
                        }
                    }

//                    if(preferenceDao.isUserOnlyDoctorOrMo() || (preferenceDao.isUserSwitchRole() && preferenceDao.getSwitchRole() == "Doctor") || (preferenceDao.isCHO() && preferenceDao.getCHOSecondRole() == "Doctor")) {
//
//                    }
//                    else if (preferenceDao.isStartingLabTechnician() || (preferenceDao.isUserSwitchRole() && preferenceDao.getSwitchRole() == "Lab Technician") ||
//                        (preferenceDao.isCHO() && preferenceDao.getCHOSecondRole() == "Lab Technician")) {
//                        lifecycleScope.launch {
//                            viewModel.patientListForLab?.collect { it ->
//                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
//                                binding.patientListContainer.patientCount.text =
//                                    it.size.toString() + getResultStr(it.size)
//                                patientCount = it.size
//                            }
//                        }
//                    }
//                    else if (preferenceDao.isPharmacist() || (preferenceDao.isUserSwitchRole() && preferenceDao.getSwitchRole() == "Pharmacist") ||
//                        (preferenceDao.isCHO() && preferenceDao.getCHOSecondRole() == "Pharmacist")) {
//                        lifecycleScope.launch {
//                            viewModel.patientListForPharmacist?.collect { it ->
//                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
//                                binding.patientListContainer.patientCount.text =
//                                    itemAdapter?.itemCount.toString() + getResultStr(itemAdapter?.itemCount)
//                                patientCount = it.size
//                            }
//                        }
//                    }
//                    else if (preferenceDao.isUserOnlyNurseOrCHO() || (preferenceDao.isUserSwitchRole() && preferenceDao.getSwitchRole() == "Nurse") ||
//                        (preferenceDao.isCHO() && preferenceDao.getCHOSecondRole() == "Nurse")){
//                        lifecycleScope.launch {
//                            viewModel.patientListForNurse?.collect { it ->
//                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
//                                binding.patientListContainer.patientCount.text =
//                                    it.size.toString() + getResultStr(it.size)
//                                patientCount = it.size
//                            }
//                        }
//                    }
//                    else {
//                        lifecycleScope.launch {
//                            viewModel.patientListForNurse?.collect { it ->
//                                itemAdapter?.submitList(it.sortedByDescending { it.patient.registrationDate})
//                                binding.patientListContainer.patientCount.text =
//                                    it.size.toString() + getResultStr(it.size)
//                                patientCount = it.size
//                            }
//                        }
//                    }

                }

                else -> {

                }
            }

            viewModel.abha.observe(viewLifecycleOwner) {
                it.let {
                    if (it != null) {
                        abhaDisclaimer.setMessage(it)
                        abhaDisclaimer.show()
                    }
                }
            }

            viewModel.benRegId.observe(viewLifecycleOwner) {
                if (it != null) {
                    val intent = Intent(requireActivity(), AbhaIdActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.putExtra("benId", viewModel.benId.value)
                    intent.putExtra("benRegId", it)
                    requireActivity().startActivity(intent)
                    viewModel.resetBenRegId()
                }
            }

            binding.search.setOnFocusChangeListener { searchView, b ->
                if (b)
                    (searchView as EditText).addTextChangedListener(searchTextWatcher)
                else
                    (searchView as EditText).removeTextChangedListener(searchTextWatcher)

            }
        }
    }
    private lateinit var syncBottomSheet : SyncBottomSheetFragment
    private fun openDialog(benVisitInfo: PatientDisplayWithVisitInfo) {
        syncBottomSheet = SyncBottomSheetFragment(benVisitInfo)
        if(!syncBottomSheet.isVisible)
            syncBottomSheet.show(childFragmentManager, resources.getString(R.string.sync))
        Timber.tag("sync").i("${benVisitInfo}")
    }

    var pageHeight = 1120
    var pageWidth = 792


    private suspend fun generatePDF(benVisitInfo: PatientDisplayWithVisitInfo) {
        val patientName = (benVisitInfo.patient.firstName?:"") + " " + (benVisitInfo.patient.lastName?:"")
        val prescriptions = caseRecordeRepo.getPrescriptionCaseRecordeByPatientIDAndBenVisitNo(patientID =
        benVisitInfo.patient.patientID,benVisitNo = benVisitInfo.benVisitNo!!)
        val chiefComplaints = visitReasonsAndCategoriesRepo.getChiefComplaintDBByPatientId(patientID =
        benVisitInfo.patient.patientID,benVisitNo = benVisitInfo.benVisitNo!!)
        val vitals = vitalsRepo.getPatientVitalsByPatientIDAndBenVisitNo(patientID =
        benVisitInfo.patient.patientID,benVisitNo = benVisitInfo.benVisitNo!!)
//        Log.d("prescriptionMsg", prescriptions.toString())

        val pdfDocument: PdfDocument = PdfDocument()

        val heading: Paint = Paint()
        val content: Paint = Paint()
        val subheading: Paint = Paint()

        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)
        val canvas: Canvas = myPage.canvas

        // Set up initial positions for the table
        val xPosition = 75F
        var y = 270F // Declare y as a var
        val rowHeight = 50F
        val spaceBetweenNameAndPrescription = 30F
        val leftSideX = 50F
        val rightSideX = 400F
        val middleX = 220F
        val bottomRightX = 400F
        val yPosition = 270F

        // Set up Paint for text
        val textPaint: Paint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 15F
            color = ContextCompat.getColor(requireContext(), android.R.color.black)
            textAlign = Paint.Align.LEFT
        }

        content.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        content.textSize = 15F
        content.color = ContextCompat.getColor(requireContext(), android.R.color.black)
        content.textAlign = Paint.Align.CENTER

        subheading.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        subheading.textSize = 16F
        subheading.color = ContextCompat.getColor(requireContext(), android.R.color.black)
        subheading.textAlign = Paint.Align.CENTER

        heading.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))
        heading.textSize = 40F
        heading.color = ContextCompat.getColor(requireContext(), android.R.color.black)
        heading.textAlign = Paint.Align.CENTER

        val spaceAfterHeading = 20F
        canvas.drawText("Prescription", 396F, 100F + spaceAfterHeading, heading)


        val leftMargin = 100F
        canvas.drawText("Name: $patientName", leftMargin+65F, 180F, subheading)
        canvas.drawText("Age: ${benVisitInfo.patient.age} ${benVisitInfo.ageUnit}", leftMargin, 200F, subheading)
        canvas.drawText("Gender: ${benVisitInfo.genderName}", leftMargin, 220F, subheading)

// Add mobile number and line break
        val mobileNumber = "Mobile: ${benVisitInfo.patient.phoneNo ?: "N/A"}"
        canvas.drawText(mobileNumber, 95F, 240F, subheading)

        val spaceAfterLine = 20F
        canvas.drawLine(50F, 260F, pageWidth - 50F, 260F, subheading)
        canvas.drawText(" ", 50F, 260F + spaceAfterLine, subheading)

// Draw items on the right side
        val rightMargin = 200F
        canvas.drawText("Date:${benVisitInfo.visitDate}", rightSideX + rightMargin, 180F, subheading)
        canvas.drawText("Beneficiary Reg ID: ${benVisitInfo.patient.beneficiaryRegID}", rightSideX + rightMargin, 200F, subheading)
        canvas.drawText("Consultation ID: ${benVisitInfo.benVisitNo}", rightSideX + rightMargin, 220F, subheading)


        // Define fixed column widths
        val columnWidth = 150F
        y+=30

        val chiefComplaintHeader = "Chief Complaints"
        val chiefComplaintHeaderSize = 25F // Adjust the size as needed
        val chiefComplaintHeaderX = (pageWidth / 2).toFloat() // Center the heading
        canvas.drawText(chiefComplaintHeader, chiefComplaintHeaderX, y, subheading.apply {
            textSize = chiefComplaintHeaderSize
            textAlign = Paint.Align.CENTER
        })

// Move down to the first row of Chief Complaints
        y += rowHeight

// Define fixed column widths for Chief Complaints
        val chiefComplaintColumnWidth = 150F

// Draw table header for Chief Complaints
        canvas.drawText("S.No.", xPosition, y, subheading)
        canvas.drawText("Chief Complaint", xPosition + chiefComplaintColumnWidth, y, subheading)
        canvas.drawText("Duration", xPosition + 2 * chiefComplaintColumnWidth, y, subheading)
        canvas.drawText("Duration Unit", xPosition + 3 * chiefComplaintColumnWidth, y, subheading)
        canvas.drawText("Description", xPosition + 4 * chiefComplaintColumnWidth, y, subheading)

// Move down to the first row
        y += rowHeight // Reassign y

// Iterate through the list of Chief Complaints and draw each as a row
        if (!chiefComplaints.isNullOrEmpty()) {
            var chiefComplaintCount: Int = 0
            for (chiefComplaint in chiefComplaints) {
                // Draw each field with a fixed width
                if (chiefComplaint != null) {
                    chiefComplaintCount++
                    drawTextWithWrapping(
                        canvas,
                        chiefComplaintCount.toString(),
                        xPosition,
                        y,
                        chiefComplaintColumnWidth,
                        content
                    )
                    drawTextWithWrapping(
                        canvas,
                        chiefComplaint.chiefComplaint ?: "",
                        xPosition + chiefComplaintColumnWidth,
                        y,
                        chiefComplaintColumnWidth,
                        content
                    )
                    drawTextWithWrapping(
                        canvas,
                        chiefComplaint.duration ?: "",
                        xPosition + 2 * chiefComplaintColumnWidth,
                        y,
                        chiefComplaintColumnWidth,
                        content
                    )
                    drawTextWithWrapping(
                        canvas,
                        chiefComplaint.durationUnit ?: "",
                        xPosition + 3 * chiefComplaintColumnWidth,
                        y,
                        chiefComplaintColumnWidth,
                        content
                    )
                    drawTextWithWrapping(
                        canvas,
                        chiefComplaint.description ?: "",
                        xPosition + 4 * chiefComplaintColumnWidth,
                        y,
                        chiefComplaintColumnWidth,
                        content
                    )

                    // Move down to the next row
                    y += rowHeight // Reassign y
                }
            }
        }

        canvas.drawLine(50F, y, pageWidth - 50F, y, subheading)
        y += spaceAfterLine
        y+=30

        // Add a heading for the Vitals section
        val vitalsSectionHeader = "Vitals"
        val vitalsSectionHeaderSize = 25F
        val vitalsSectionHeaderX = (pageWidth / 2).toFloat()
        canvas.drawText(vitalsSectionHeader, vitalsSectionHeaderX, y, subheading.apply {
            textSize = vitalsSectionHeaderSize
            textAlign = Paint.Align.CENTER
        })

        // Move down to the first row of Vitals
        y += rowHeight

        // Define fixed column widths for Vitals
        val vitalsColumnWidth = 200F

        // Draw table header for Vitals
        canvas.drawText("Vitals Name", xPosition, y, subheading)
        canvas.drawText("Vitals Value", xPosition + vitalsColumnWidth, y, subheading)

        // Move down to the first row
        y += rowHeight

        // Function to draw Vitals Name and Value
        fun drawVitals(vitalsName: String, vitalsValue: String) {
            drawTextWithWrapping(canvas, vitalsName, xPosition, y, vitalsColumnWidth, content)
            drawTextWithWrapping(canvas, vitalsValue, xPosition + vitalsColumnWidth, y, vitalsColumnWidth, content)
            y += rowHeight
        }

        // Draw Vitals based on the available data
        with(vitals) {
            this?.height?.let { drawVitals("Height", it) }
            this?.weight?.let { drawVitals("Weight", it) }
            this?.bmi?.let { drawVitals("BMI", it) }
            this?.waistCircumference?.let { drawVitals("Waist Circumference", it) }
            this?.temperature?.let { drawVitals("Temperature", it) }
            this?.pulseRate?.let { drawVitals("Pulse Rate", it) }
            this?.spo2?.let { drawVitals("SpO2", it) }
            this?.bpSystolic?.let { drawVitals("BP Systolic", it) }
            this?.bpDiastolic?.let { drawVitals("BP Diastolic", it) }
            this?.respiratoryRate?.let { drawVitals("Respiratory Rate", it) }
            this?.rbs?.let { drawVitals("RBS", it) }
        }

// Draw heading for the next section
        val nextSectionHeader = "Prescription" // Replace with your desired heading
        val nextSectionHeaderSize = 25F // Adjust the size as needed
        val nextSectionHeaderX = (pageWidth / 2).toFloat() // Center the heading
        canvas.drawText(nextSectionHeader, nextSectionHeaderX, y, subheading.apply {
            textSize = nextSectionHeaderSize
            textAlign = Paint.Align.CENTER
        })
        y += rowHeight


        // Draw table header
        canvas.drawText("S.No.", xPosition, y, subheading)
        canvas.drawText("Medication", xPosition + columnWidth, y, subheading)
        canvas.drawText("Frequency", xPosition + 2 * columnWidth, y, subheading)
        canvas.drawText("Duration", xPosition + 3 * columnWidth, y, subheading)
//        canvas.drawText("Quantity", xPosition + 4 * columnWidth, y, subheading)
//        canvas.drawText("Instructions", xPosition + 5 * columnWidth, y, subheading)
        canvas.drawText("Instructions", xPosition + 4 * columnWidth, y, subheading)

        // Move down to the first row
        y += rowHeight // Reassign y

        // Iterate through the list of prescriptions and draw each as a row
        if (!prescriptions.isNullOrEmpty()) {
            var count:Int = 0
            for (prescription in prescriptions) {
                // Draw each field with a fixed width
                if(prescription!=null) {
                    count++
                    drawTextWithWrapping(
                        canvas,
                        count.toString(),
                        xPosition,
                        y,
                        columnWidth,
                        content
                    )
                    drawTextWithWrapping(
                        canvas,
                        prescription.itemName,
                        xPosition + columnWidth,
                        y,
                        columnWidth,
                        content
                    )
                    drawTextWithWrapping(
                        canvas,
                        prescription.frequency ?: "",
                        xPosition + 2 * columnWidth,
                        y,
                        columnWidth,
                        content
                    )
                    if (prescription.unit.isNullOrEmpty()) {
                        drawTextWithWrapping(
                            canvas,
                            (prescription.duration) ?: "",
                            xPosition + 3 * columnWidth,
                            y,
                            columnWidth,
                            content
                        )
                    } else {
                        drawTextWithWrapping(
                            canvas,
                            (prescription.duration + " " + prescription.unit),
                            xPosition + 3 * columnWidth,
                            y,
                            columnWidth,
                            content
                        )
                    }
//                    drawTextWithWrapping(
//                        canvas,
//                        prescription.quantityInHand.toString(),
//                        xPosition + 4 * columnWidth,
//                        y,
//                        columnWidth,
//                        content
//                    )
                    drawTextWithWrapping(
                        canvas,
                        prescription.instruciton,
                        xPosition + 4 * columnWidth,
                        y,
                        columnWidth,
                        content
                    )

                    // Move down to the next row
                    y += rowHeight // Reassign y
                }
            }
        }

        pdfDocument.finishPage(myPage)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName : String =  "Prescription_$patientName"+"_${timeStamp}_.pdf"

        val outputStream: OutputStream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputStream = createPdfForApi33(fileName)
        } else {
            val downloadsDirectory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDirectory, fileName)

            outputStream = FileOutputStream(file)

        }

        try {
            pdfDocument.writeTo(outputStream)

            Toast.makeText(requireContext(), "PDF file generated for Prescription.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()

            Toast.makeText(requireContext(), "Failed to generate PDF file", Toast.LENGTH_SHORT)
                .show()
        }
        pdfDocument.close()
    }

    private fun drawTextWithWrapping(canvas: Canvas, text: String?, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        var yPos = y
        val textLines = wrapText(text?:"", paint, maxWidth)
        for (line in textLines) {
            canvas.drawText(line, x, yPos, paint)
            yPos += paint.textSize
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val lineWidth = paint.measureText(testLine)
            if (lineWidth <= maxWidth) {
                currentLine = testLine
            } else {
                result.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine)
        }
        return result
    }

    private fun createPdfForApi33(fileName:String): OutputStream {
        val outst: OutputStream
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val pdfUri: Uri? = requireContext().contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        )
        outst = pdfUri?.let { requireContext().contentResolver.openOutputStream(it) }!!
        Objects.requireNonNull(outst)
        return outst
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun checkPermissions() {

        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        if (!hasPermission(permissions[0])) {
            permissionLauncher.launch(permissions)
        }
    }

    private var permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        var isGranted = true
        for (item in it){
            if (!item.value) {
                isGranted = false
            }
        }
        if (isGranted) {
            Toast.makeText(requireContext(), "Permissions Granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Permissions Denied", Toast.LENGTH_SHORT).show()

        }
    }

    private val searchTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {
            viewModel.filterText(p0?.toString() ?: "")
            binding.patientListContainer.patientCount.text =
                patientCount.toString() + getResultStr(patientCount)
            Log.d("arr","${patientCount}")
        }

    }
    fun getResultStr(count:Int?):String{
        if(count==1||count==0){
            return getString(R.string.patient_cnt_display)
        }
        return getString(R.string.patients_cnt_display)
    }
    private val speechToTextLauncherForSearchByName = registerForActivityResult(SpeechToTextContract()) { result ->
        if (result.isNotBlank() && result.isNotEmpty() && !result.any { it.isDigit() }) {
            binding.search.setText(result)
            binding.search.addTextChangedListener(searchTextWatcher)
        }
    }
    private fun encryptSHA512(input: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    private fun callLoginDialog(benVisitInfo: PatientDisplayWithVisitInfo) {
        if (benVisitInfo.patient.phoneNo.isNullOrEmpty()) {
            context?.let {
                MaterialAlertDialogBuilder(it).setTitle(getString(R.string.alert_popup))
                    .setMessage(getString(R.string.phone_no_not_found))
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }.create()
                    .show()
            }
        } else{
        network = isInternetAvailable(requireContext())
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_esanjeevani_login, null)
        val dialog = context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle("eSanjeevani Login")
                .setView(dialogView)
                .setNegativeButton("Cancel") { dialog, _ ->
                    // Handle cancel button click
                    dialog.dismiss()
                }
                .create()
        }
        dialog?.show()
            val loginBtn = dialogView.findViewById<MaterialButton>(R.id.loginButton)
            val rememberMeEsanjeevani = dialogView.findViewById<CheckBox>(R.id.cb_remember_es)
        if (network) {
            // Internet is available
            dialogView.findViewById<ConstraintLayout>(R.id.cl_error_es).visibility = View.GONE
            dialogView.findViewById<LinearLayout>(R.id.ll_login_es).visibility = View.VISIBLE
            val rememberedUsername : String? = viewModel.fetchRememberedUsername()
            val rememberedPassword : String? = viewModel.fetchRememberedPassword()
            if(!rememberedUsername.isNullOrBlank() && !rememberedPassword.isNullOrBlank()){
                dialogView.findViewById<TextInputEditText>(R.id.et_username_es).text = Editable.Factory.getInstance().newEditable(rememberedUsername)
                dialogView.findViewById<TextInputEditText>(R.id.et_password_es).text = Editable.Factory.getInstance().newEditable(rememberedPassword)
                rememberMeEsanjeevani.isChecked = true
            }
        } else {
            dialogView.findViewById<LinearLayout>(R.id.ll_login_es).visibility = View.GONE
            dialogView.findViewById<ConstraintLayout>(R.id.cl_error_es).visibility = View.VISIBLE
        }


        loginBtn.setOnClickListener {

            usernameEs =
                dialogView.findViewById<TextInputEditText>(R.id.et_username_es).text.toString()
                    .trim()
            passwordEs =
                dialogView.findViewById<TextInputEditText>(R.id.et_password_es).text.toString()
                    .trim()
            if(rememberMeEsanjeevani.isChecked){
                viewModel.rememberUserEsanjeevani(usernameEs,passwordEs)
            }else{
                viewModel.forgetUserEsanjeevani()
            }
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    var passWord = encryptSHA512(encryptSHA512(passwordEs) + encryptSHA512("token"))

                    var networkBody = NetworkBody(
                        usernameEs,
                        passWord,
                        "token",
                        "11001"
                    )
                    val errorTv = dialogView.findViewById<MaterialTextView>(R.id.tv_error_es)
                    network = isInternetAvailable(requireContext())
                    if (!network) {
                        errorTv.text = requireContext().getString(R.string.network_error)
                        errorTv.visibility = View.VISIBLE
                    } else {
                        errorTv.text = ""
                        errorTv.visibility = View.GONE
                        val responseToken = apiService.getJwtToken(networkBody)
                        if (responseToken.message == "Success") {
                            val token = responseToken.model?.access_token;
                            if (token != null) {
                                TokenESanjeevaniInterceptor.setToken(token)
                            }
                            val intent = Intent(context, WebViewActivity::class.java)
                            intent.putExtra("patientId", benVisitInfo.patient.patientID);
                            intent.putExtra("usernameEs", usernameEs);
                            intent.putExtra("passwordEs", passwordEs);
                            context?.startActivity(intent)
                            dialog?.dismiss()
                        } else {
                            errorEs = responseToken.message
                            errorTv.text = errorEs
                            errorTv.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    Timber.d("GHere is error $e")
                }
            }
        }
    }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
    private fun checkAndGenerateABHA(benVisitInfo: PatientDisplayWithVisitInfo) {
        Log.d("checkAndGenerateABHA click listener","checkAndGenerateABHA click listener")
        viewModel.fetchAbha(benVisitInfo.patient.beneficiaryID!!)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            android.R.id.home -> {
//                // hide the soft keyboard when the navigation drawer is shown on the screen.
//                binding.search.clearFocus()
//                true
//            }
//
//            else -> false
//        }
//    }

}
