package com.example.ibero

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.example.ibero.ui.InspectionHistoryAdapter
import com.example.ibero.data.HistoricalInspection
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: InspectionViewModel

    private lateinit var editInspectionDate: EditText
    private lateinit var editInspectionTime: EditText
    private lateinit var editInspectorName: EditText
    private lateinit var editOrderNumber: EditText
    private lateinit var editArticleReference: EditText
    private lateinit var editSupplier: EditText
    private lateinit var editColor: EditText
    private lateinit var editTotalLotQuantity: EditText
    private lateinit var editSampleQuantity: EditText
    private lateinit var spinnerDefectType: AutoCompleteTextView
    private lateinit var layoutOtherDefect: TextInputLayout
    private lateinit var editOtherDefectDescription: EditText
    private lateinit var editDefectiveItemsQuantity: EditText
    private lateinit var editDefectDescription: EditText
    private lateinit var spinnerActionTaken: AutoCompleteTextView
    private lateinit var btnCaptureImage: MaterialButton
    private lateinit var imagePreviewContainer: LinearLayout
    private lateinit var btnSaveInspection: MaterialButton
    private lateinit var btnClearForm: MaterialButton
    private lateinit var textSyncStatus: TextView
    private lateinit var btnForceSync: MaterialButton
    private lateinit var recyclerViewHistory: RecyclerView

    private lateinit var historyAdapter: InspectionHistoryAdapter

    private val capturedImagePaths = mutableListOf<String>()
    private var currentPhotoPath: String? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                capturedImagePaths.add(path)
                displayImagePreview(path)
            }
        } else {
            currentPhotoPath?.let { path ->
                File(path).delete()
            }
        }
        currentPhotoPath = null
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado. No se puede tomar la foto.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        val factory = InspectionViewModelFactory(application, enableAutoSync = true)
        viewModel = ViewModelProvider(this, factory).get(InspectionViewModel::class.java)

        setupSpinners()
        setupListeners()
        observeViewModel()
        setupHistoryRecyclerView()
        setCurrentDateTime()
    }

    private fun initViews() {
        editInspectionDate = findViewById(R.id.edit_inspection_date)
        editInspectionTime = findViewById(R.id.edit_inspection_time)
        editInspectorName = findViewById(R.id.edit_inspector_name)
        editOrderNumber = findViewById(R.id.edit_order_number)
        editArticleReference = findViewById(R.id.edit_article_reference)
        editSupplier = findViewById(R.id.edit_supplier)
        editColor = findViewById(R.id.edit_color)
        editTotalLotQuantity = findViewById(R.id.edit_total_lot_quantity)
        editSampleQuantity = findViewById(R.id.edit_sample_quantity)
        spinnerDefectType = findViewById(R.id.spinner_defect_type)
        layoutOtherDefect = findViewById(R.id.layout_other_defect)
        editOtherDefectDescription = findViewById(R.id.edit_other_defect_description)
        editDefectiveItemsQuantity = findViewById(R.id.edit_defective_items_quantity)
        editDefectDescription = findViewById(R.id.edit_defect_description)
        spinnerActionTaken = findViewById(R.id.spinner_action_taken)
        btnCaptureImage = findViewById(R.id.btn_capture_image)
        imagePreviewContainer = findViewById(R.id.image_preview_container)
        btnSaveInspection = findViewById(R.id.btn_save_inspection)
        btnClearForm = findViewById(R.id.btn_clear_form)
        textSyncStatus = findViewById(R.id.text_sync_status)
        btnForceSync = findViewById(R.id.btn_force_sync)
        recyclerViewHistory = findViewById(R.id.recycler_view_history)
    }

    private fun setupSpinners() {
        val defectTypes = arrayOf("Mancha", "Hebra Suelta", "Descosido", "Error de Talla", "Error de Color", "Otro")
        val defectAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defectTypes)
        spinnerDefectType.setAdapter(defectAdapter)

        spinnerDefectType.setOnItemClickListener { parent, view, position, id ->
            val selectedDefect = parent.getItemAtPosition(position).toString()
            if (selectedDefect == "Otro") {
                layoutOtherDefect.visibility = View.VISIBLE
            } else {
                layoutOtherDefect.visibility = View.GONE
                editOtherDefectDescription.setText("")
            }
        }

        val actionTakenOptions = arrayOf("Aprobado", "Rechazado", "Aprobado con Observaciones", "Reproceso")
        val actionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, actionTakenOptions)
        spinnerActionTaken.setAdapter(actionAdapter)
    }

    private fun setupListeners() {
        editInspectionDate.setOnClickListener {
            showDatePickerDialog()
        }

        editInspectionTime.setOnClickListener {
            showTimePickerDialog()
        }

        btnCaptureImage.setOnClickListener {
            checkCameraPermissionAndTakePicture()
        }

        btnSaveInspection.setOnClickListener {
            Toast.makeText(this, "Esta funcionalidad ha sido movida a la pantalla de registro.", Toast.LENGTH_SHORT).show()
        }

        btnClearForm.setOnClickListener {
            clearForm()
        }

        btnForceSync.setOnClickListener {
            viewModel.performSync()
        }

        addRequiredFieldValidation(editArticleReference, "Referencia del Artículo es obligatoria")
        addRequiredFieldValidation(editSampleQuantity, "Cantidad de Muestra es obligatoria")
        addRequiredFieldValidation(spinnerDefectType, "Tipo de Defecto es obligatorio")
        addRequiredFieldValidation(editDefectiveItemsQuantity, "Cantidad de Defectos es obligatoria")
        addRequiredFieldValidation(spinnerActionTaken, "Acción Tomada es obligatoria")
    }

    private fun addRequiredFieldValidation(editText: EditText, errorMessage: String) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    editText.error = errorMessage
                } else {
                    editText.error = null
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.allInspections.observe(this) { inspections ->
            // Mapeamos de Inspection a HistoricalInspection antes de pasarlo al adaptador
            val historicalInspections = inspections.map { inspection ->
                HistoricalInspection(
                    hojaDeRuta = "N/A", // No existe en esta actividad, se puede poner N/A o un valor por defecto
                    articulo = inspection.articulo,
                    tipoCalidad = "N/A", // No existe, se puede poner N/A
                    tipoDeFalla = inspection.tipoDeFalla, // Mapeamos el tipo de defecto
                    metrosDeTela = inspection.metrosDeTela, // Mapeamos la cantidad de muestra como metros de tela
                    fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(inspection.fecha)
                )
            }
            // Se usa el método updateList, que es el que existe en tu adaptador.
            historyAdapter.updateList(historicalInspections)
        }

        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage()
            }
        }
    }

    private fun setupHistoryRecyclerView() {
        // Se corrigió la inicialización del adaptador para usar un constructor vacío
        historyAdapter = InspectionHistoryAdapter(mutableListOf()) { inspection ->
            Toast.makeText(this, "Detalles de: ${inspection.articulo}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        recyclerViewHistory.adapter = historyAdapter
    }

    private fun setCurrentDateTime() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        editInspectionDate.setText(dateFormat.format(calendar.time))
        editInspectionTime.setText(timeFormat.format(calendar.time))
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            editInspectionDate.setText(dateFormat.format(selectedCalendar.time))
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            editInspectionTime.setText(selectedTime)
        }, hour, minute, true)
        timePickerDialog.show()
    }

    private fun checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "Error al crear archivo de imagen: ${ex.message}", Toast.LENGTH_LONG).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            } ?: run {
                Toast.makeText(this, "No se encontró aplicación de cámara.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayImagePreview(imagePath: String) {
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.image_preview_size),
                resources.getDimensionPixelSize(R.dimen.image_preview_size)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.image_preview_margin)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.rounded_image_background)
        }

        Glide.with(this)
            .load(imagePath)
            .centerCrop()
            .into(imageView)

        imagePreviewContainer.addView(imageView)
        imagePreviewContainer.visibility = View.VISIBLE
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val requiredEditTexts = listOf(
            editArticleReference,
            editSampleQuantity,
            editDefectiveItemsQuantity
        )
        for (editText in requiredEditTexts) {
            if (editText.text.isNullOrBlank()) {
                editText.error = "${editText.hint} es obligatorio"
                isValid = false
            } else {
                editText.error = null
            }
        }

        val requiredSpinners = listOf(
            spinnerDefectType,
            spinnerActionTaken
        )
        for (spinner in requiredSpinners) {
            if (spinner.text.isNullOrBlank()) {
                spinner.error = "${spinner.hint} es obligatorio"
                isValid = false
            } else {
                spinner.error = null
            }
        }

        if (spinnerDefectType.text.toString() == "Otro" && editOtherDefectDescription.text.isNullOrBlank()) {
            editOtherDefectDescription.error = "Especificar otro defecto es obligatorio"
            isValid = false
        } else if (spinnerDefectType.text.toString() != "Otro") {
            editOtherDefectDescription.error = null
        }

        return isValid
    }

    private fun clearForm() {
        editInspectionDate.setText("")
        editInspectionTime.setText("")
        editInspectorName.setText("")
        editOrderNumber.setText("")
        editArticleReference.setText("")
        editSupplier.setText("")
        editColor.setText("")
        editTotalLotQuantity.setText("")
        editSampleQuantity.setText("")
        spinnerDefectType.setText("", false)
        layoutOtherDefect.visibility = View.GONE
        editOtherDefectDescription.setText("")
        editDefectiveItemsQuantity.setText("")
        editDefectDescription.setText("")
        spinnerActionTaken.setText("", false)

        imagePreviewContainer.removeAllViews()
        imagePreviewContainer.visibility = View.GONE
        capturedImagePaths.clear()

        setCurrentDateTime()

        val allEditTexts = listOf(
            editInspectionDate, editInspectionTime, editInspectorName, editOrderNumber,
            editArticleReference, editSupplier, editColor, editTotalLotQuantity,
            editSampleQuantity, editOtherDefectDescription, editDefectiveItemsQuantity,
            editDefectDescription, spinnerDefectType, spinnerActionTaken
        )
        for (editText in allEditTexts) {
            editText.error = null
        }
        Toast.makeText(this, "Formulario limpiado.", Toast.LENGTH_SHORT).show()
    }
}
