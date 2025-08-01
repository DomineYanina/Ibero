package com.example.ibero

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.example.ibero.data.AppDatabase
import com.example.ibero.data.Inspection
import com.example.ibero.repository.InspectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // ViewModel para manejar la lógica de datos y el ciclo de vida
    private lateinit var viewModel: InspectionViewModel

    // Vistas de la interfaz de usuario
    private lateinit var editInspectionDate: EditText
    private lateinit var editInspectionTime: EditText
    private lateinit var editInspectorName: EditText
    private lateinit var editOrderNumber: EditText
    private lateinit var editArticleReference: EditText
    private lateinit var editSupplier: EditText
    private lateinit var editColor: EditText
    private lateinit var editTotalLotQuantity: EditText
    private lateinit var editSampleQuantity: EditText
    private lateinit var spinnerDefectType: AutoCompleteTextView // Usamos AutoCompleteTextView para spinner de Material Design
    private lateinit var layoutOtherDefect: TextInputLayout
    private lateinit var editOtherDefectDescription: EditText
    private lateinit var editDefectiveItemsQuantity: EditText
    private lateinit var editDefectDescription: EditText
    private lateinit var spinnerActionTaken: AutoCompleteTextView // Usamos AutoCompleteTextView
    private lateinit var btnCaptureImage: MaterialButton
    private lateinit var imagePreviewContainer: LinearLayout
    private lateinit var btnSaveInspection: MaterialButton
    private lateinit var btnClearForm: MaterialButton
    private lateinit var textSyncStatus: TextView
    private lateinit var btnForceSync: MaterialButton
    private lateinit var recyclerViewHistory: RecyclerView

    // Adaptador para el historial de inspecciones
    private lateinit var historyAdapter: InspectionHistoryAdapter

    // Lista para almacenar las rutas de las imágenes capturadas temporalmente
    private val capturedImagePaths = mutableListOf<String>()
    private var currentPhotoPath: String? = null // Ruta de la foto actual que se está capturando

    // Launcher para la cámara
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                capturedImagePaths.add(path)
                displayImagePreview(path)
            }
        } else {
            // Si la captura de imagen fue cancelada o falló, borra el archivo temporal
            currentPhotoPath?.let { path ->
                File(path).delete()
            }
        }
        currentPhotoPath = null // Resetea la ruta de la foto actual
    }

    // Launcher para permisos de cámara
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado. No se puede tomar la foto.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        initViews()

        // Configurar ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = InspectionRepository(database.inspectionDao())
        val factory = InspectionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(InspectionViewModel::class.java)

        // Configurar adaptadores para Spinners (AutoCompleteTextView)
        setupSpinners()

        // Configurar listeners de eventos
        setupListeners()

        // Observar datos del ViewModel
        observeViewModel()

        // Inicializar el historial de inspecciones
        setupHistoryRecyclerView()

        // Autocompletar fecha y hora actuales
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
        // Tipos de Defecto
        val defectTypes = arrayOf("Mancha", "Hebra Suelta", "Descosido", "Error de Talla", "Error de Color", "Otro")
        val defectAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defectTypes)
        spinnerDefectType.setAdapter(defectAdapter)

        spinnerDefectType.setOnItemClickListener { parent, view, position, id ->
            val selectedDefect = parent.getItemAtPosition(position).toString()
            if (selectedDefect == "Otro") {
                layoutOtherDefect.visibility = View.VISIBLE
            } else {
                layoutOtherDefect.visibility = View.GONE
                editOtherDefectDescription.setText("") // Limpiar el campo si no es "Otro"
            }
        }

        // Acciones Tomadas
        val actionTakenOptions = arrayOf("Aprobado", "Rechazado", "Aprobado con Observaciones", "Reproceso")
        val actionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, actionTakenOptions)
        spinnerActionTaken.setAdapter(actionAdapter)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun setupListeners() {
        // Listener para la fecha de inspección
        editInspectionDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Listener para la hora de inspección
        editInspectionTime.setOnClickListener {
            showTimePickerDialog()
        }

        // Listener para el botón de capturar imagen
        btnCaptureImage.setOnClickListener {
            checkCameraPermissionAndTakePicture()
        }

        // Listener para el botón de guardar inspección
        btnSaveInspection.setOnClickListener {
            saveInspection()
        }

        // Listener para el botón de limpiar formulario
        btnClearForm.setOnClickListener {
            clearForm()
        }

        // Listener para el botón de forzar sincronización
        btnForceSync.setOnClickListener {
            // Iniciar la sincronización manualmente
            if (isNetworkAvailable()) {
                Toast.makeText(this, "Iniciando sincronización manual...", Toast.LENGTH_SHORT).show()
                viewModel.syncUnsyncedInspections()
            } else {
                Toast.makeText(this, "No hay conexión a internet para sincronizar.", Toast.LENGTH_SHORT).show()
            }
        }

        // Validaciones en tiempo real para campos obligatorios
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

    @RequiresApi(Build.VERSION_CODES.M)
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun observeViewModel() {
        // Observar el número de inspecciones no sincronizadas
        viewModel.unsyncedCount.observe(this) { count ->
            val networkStatus = if (isNetworkAvailable()) "Online" else "Offline"
            textSyncStatus.text = "Estado: $networkStatus | Pendientes: $count"
            // Si hay conexión y pendientes, iniciar sincronización automática
            if (isNetworkAvailable() && count > 0) {
                viewModel.syncUnsyncedInspections()
            }
        }

        // Observar el historial de inspecciones para actualizar el RecyclerView
        viewModel.allInspections.observe(this) { inspections ->
            historyAdapter.submitList(inspections)
        }

        // Observar mensajes de éxito/error de sincronización
        viewModel.syncMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearSyncMessage() // Limpiar el mensaje después de mostrarlo
            }
        }
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = InspectionHistoryAdapter { inspection ->
            // Aquí puedes implementar la lógica para ver detalles de la inspección
            // Por ahora, solo un Toast
            Toast.makeText(this, "Detalles de: ${inspection.articleReference}", Toast.LENGTH_SHORT).show()
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
        }, hour, minute, true) // true para formato de 24 horas
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
        // Crea un nombre de archivo de imagen único
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefijo */
            ".jpg", /* sufijo */
            storageDir /* directorio */
        ).apply {
            // Guarda un archivo: ruta para usar con ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Asegúrate de que haya una actividad de cámara para manejar el intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Crea el archivo donde debería ir la foto
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error al crear el archivo
                    Toast.makeText(this, "Error al crear archivo de imagen: ${ex.message}", Toast.LENGTH_LONG).show()
                    null
                }
                // Continúa solo si el archivo fue creado exitosamente
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider", // Autoridad del FileProvider
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
            setBackgroundResource(R.drawable.rounded_image_background) // Fondo redondeado para la imagen
        }

        // Cargar imagen usando Glide
        Glide.with(this)
            .load(imagePath)
            .centerCrop()
            .into(imageView)

        imagePreviewContainer.addView(imageView)
        imagePreviewContainer.visibility = View.VISIBLE
    }

    private fun saveInspection() {
        // Validaciones de campos obligatorios
        if (!validateForm()) {
            Toast.makeText(this, "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_LONG).show()
            return
        }

        val inspectionDateStr = editInspectionDate.text.toString()
        val inspectionTimeStr = editInspectionTime.text.toString()
        val inspectorName = editInspectorName.text.toString().trim()
        val orderNumber = editOrderNumber.text.toString().trim()
        val articleReference = editArticleReference.text.toString().trim()
        val supplier = editSupplier.text.toString().trim()
        val color = editColor.text.toString().trim()
        val totalLotQuantity = editTotalLotQuantity.text.toString().toIntOrNull() ?: 0
        val sampleQuantity = editSampleQuantity.text.toString().toIntOrNull() ?: 0
        val defectType = spinnerDefectType.text.toString().trim()
        val otherDefectDescription = if (defectType == "Otro") editOtherDefectDescription.text.toString().trim() else null
        val defectiveItemsQuantity = editDefectiveItemsQuantity.text.toString().toIntOrNull() ?: 0
        val defectDescription = editDefectDescription.text.toString().trim()
        val actionTaken = spinnerActionTaken.text.toString().trim()

        // Convertir fecha a Date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val inspectionDate = dateFormat.parse(inspectionDateStr) ?: Date()

        // Generar un ID único para el registro (para evitar duplicados en Google Sheets)
        val uniqueId = UUID.randomUUID().toString()

        val newInspection = Inspection(
            inspectionDate = inspectionDate,
            inspectionTime = inspectionTimeStr,
            inspectorName = inspectorName,
            orderNumber = orderNumber,
            articleReference = articleReference,
            supplier = supplier,
            color = color,
            totalLotQuantity = totalLotQuantity,
            sampleQuantity = sampleQuantity,
            defectType = defectType,
            otherDefectDescription = otherDefectDescription,
            defectiveItemsQuantity = defectiveItemsQuantity,
            defectDescription = defectDescription,
            actionTaken = actionTaken,
            imagePaths = capturedImagePaths.toList(), // Copia la lista de rutas locales
            imageUrls = emptyList(), // Inicialmente vacío, se llenará después de la sincronización
            isSynced = false, // No sincronizado al guardar por primera vez
            uniqueId = uniqueId
        )

        lifecycleScope.launch {
            viewModel.insertInspection(newInspection)
            Toast.makeText(this@MainActivity, "Inspección guardada localmente.", Toast.LENGTH_SHORT).show()
            clearForm() // Limpiar formulario después de guardar
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validar campos de texto
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

        // Validar Spinners (AutoCompleteTextView)
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

        // Validar "Otro Defecto" si aplica
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
        spinnerDefectType.setText("", false) // Limpiar texto y no seleccionar nada
        layoutOtherDefect.visibility = View.GONE
        editOtherDefectDescription.setText("")
        editDefectiveItemsQuantity.setText("")
        editDefectDescription.setText("")
        spinnerActionTaken.setText("", false) // Limpiar texto y no seleccionar nada

        // Limpiar previsualizaciones de imágenes y la lista de rutas
        imagePreviewContainer.removeAllViews()
        imagePreviewContainer.visibility = View.GONE
        capturedImagePaths.clear()

        // Restablecer fecha y hora actuales
        setCurrentDateTime()

        // Limpiar errores de validación
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

    // Comprueba si hay conexión a internet
    @RequiresApi(Build.VERSION_CODES.M)
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // TODO: Implementar un BroadcastReceiver para detectar cambios en la conectividad
    // y llamar a viewModel.syncUnsyncedInspections() cuando la conexión se restablezca.
    // Esto es más avanzado y se puede añadir después de la funcionalidad básica.

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        // Actualizar el estado de la red cuando la actividad se reanuda
        viewModel.updateNetworkStatus(isNetworkAvailable())
    }
}