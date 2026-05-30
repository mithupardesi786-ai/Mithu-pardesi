package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Document
import com.example.data.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DocumentRepository
    val allDocuments: StateFlow<List<Document>>

    // PIN and Security status
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _savedPin = MutableStateFlow("2026")
    val savedPin: StateFlow<String> = _savedPin.asStateFlow()

    private val _currentPinInput = MutableStateFlow("")
    val currentPinInput: StateFlow<String> = _currentPinInput.asStateFlow()

    // Screen navigation state
    private val _currentTab = MutableStateFlow("cards") // "cards", "scanner", "settings"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Searching and filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("ALL") // "ALL", "PASSPORT", "DRIVING_LICENSE", "RESIDENCE_PERMIT"
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    // Active expanded document
    private val _selectedDocumentId = MutableStateFlow<Int?>(null)
    val selectedDocumentId: StateFlow<Int?> = _selectedDocumentId.asStateFlow()

    // Modal adding states
    private val _isAddSheetOpen = MutableStateFlow(false)
    val isAddSheetOpen: StateFlow<Boolean> = _isAddSheetOpen.asStateFlow()

    // Simulated scanner authority status (active scan result overlay)
    private val _scannedVerificationData = MutableStateFlow<String?>(null)
    val scannedVerificationData: StateFlow<String?> = _scannedVerificationData.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database.documentDao())
        allDocuments = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed sample documents on first opening if database is empty
        viewModelScope.launch {
            try {
                val list = repository.allDocuments.first()
                if (list.isEmpty()) {
                    seedSampleData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Combine documents with search & filter parameters
    val filteredDocuments: StateFlow<List<Document>> = combine(
        allDocuments,
        searchQuery,
        typeFilter
    ) { docs, query, filter ->
        docs.filter { doc ->
            val matchesFilter = filter == "ALL" || doc.type == filter
            val matchesSearch = doc.fullName.contains(query, ignoreCase = true) ||
                    doc.documentNumber.contains(query, ignoreCase = true) ||
                    doc.issuer.contains(query, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun lockApp() {
        _isLocked.value = true
        _currentPinInput.value = ""
    }

    fun unlockWithBiometrics() {
        // Instant simulated premium biometric (fingerprint / face ID) signature scan
        _isLocked.value = false
        _currentPinInput.value = ""
    }

    fun appendPinChar(char: Char) {
        if (_currentPinInput.value.length < 4) {
            _currentPinInput.value += char
            if (_currentPinInput.value == _savedPin.value) {
                _isLocked.value = false
            } else if (_currentPinInput.value.length == 4) {
                // Wrong pin auto flush with a small delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(800)
                    _currentPinInput.value = ""
                }
            }
        }
    }

    fun deletePinChar() {
        if (_currentPinInput.value.isNotEmpty()) {
            _currentPinInput.value = _currentPinInput.value.dropLast(1)
        }
    }

    fun changePin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _savedPin.value = newPin
        }
    }

    fun selectDocument(id: Int?) {
        _selectedDocumentId.value = id
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: String) {
        _typeFilter.value = filter
    }

    fun toggleAddSheet(open: Boolean) {
        _isAddSheetOpen.value = open
    }

    fun setScannedVerificationData(data: String?) {
        _scannedVerificationData.value = data
    }

    // CRUD database updates
    fun addDocument(
        type: String,
        docNum: String,
        name: String,
        nationality: String,
        birthDate: String,
        expiryDate: String,
        issueDate: String,
        issuer: String,
        additional: String,
        seed: Int = (1..100).random()
    ) {
        viewModelScope.launch {
            val payload = "VERIFIED_SECURE_ID\nTYPE: $type\nNO: $docNum\nOWNER: $name\nNAT: $nationality\nEXP: $expiryDate\nISSUER: $issuer"
            val newDoc = Document(
                type = type,
                documentNumber = docNum,
                fullName = name,
                nationality = nationality,
                birthDate = birthDate,
                expiryDate = expiryDate,
                issueDate = issueDate,
                issuer = issuer,
                photoSeed = seed,
                additionalData = additional,
                qrCodePayload = payload
            )
            repository.insert(newDoc)
            _isAddSheetOpen.value = false
        }
    }

    fun deleteDocument(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_selectedDocumentId.value == id) {
                _selectedDocumentId.value = null
            }
        }
    }

    private suspend fun seedSampleData() {
        val sampleDocs = listOf(
            Document(
                type = "PASSPORT",
                documentNumber = "EP8843901",
                fullName = "JOHNATHAN DOE",
                nationality = "UNITED KINGDOM",
                birthDate = "1994-03-12",
                expiryDate = "2032-10-30",
                issueDate = "2022-10-31",
                issuer = "UK HER MAJESTY PASSPORT OFFICE",
                photoSeed = 24,
                additionalData = "Passport Type: P | Code: GBR",
                qrCodePayload = "VERIFIED_SECURE_ID\nTYPE: PASSPORT\nNO: EP8843901\nOWNER: JOHNATHAN DOE\nNAT: UNITED KINGDOM\nEXP: 2032-10-30\nISSUER: GBR HMPO"
            ),
            Document(
                type = "DRIVING_LICENSE",
                documentNumber = "DL-908842-CA",
                fullName = "JOHNATHAN DOE",
                nationality = "UNITED STATES",
                birthDate = "1994-03-12",
                expiryDate = "2029-05-15",
                issueDate = "2024-05-16",
                issuer = "CALIFORNIA DMV",
                photoSeed = 24,
                additionalData = "Classes: AM / B / C1",
                qrCodePayload = "VERIFIED_SECURE_ID\nTYPE: DRIVING_LICENSE\nNO: DL-908842-CA\nOWNER: JOHNATHAN DOE\nNAT: USA\nEXP: 2029-05-15\nISSUER: CAL DMV"
            ),
            Document(
                type = "RESIDENCE_PERMIT",
                documentNumber = "RP-77421-EU",
                fullName = "JOHNATHAN DOE",
                nationality = "UNITED STATES",
                birthDate = "1994-03-12",
                expiryDate = "2034-01-20",
                issueDate = "2024-01-21",
                issuer = "REPUBLIC OF CORE GERMANY",
                photoSeed = 24,
                additionalData = "Permit Type: Permanent Residence | Category: Employment",
                qrCodePayload = "VERIFIED_SECURE_ID\nTYPE: RESIDENCE_PERMIT\nNO: RP-77421-EU\nOWNER: JOHNATHAN DOE\nNAT: USA\nEXP: 2034-01-20\nISSUER: GER AUSLANDERB"
            )
        )
        for (doc in sampleDocs) {
            repository.insert(doc)
        }
    }
}
