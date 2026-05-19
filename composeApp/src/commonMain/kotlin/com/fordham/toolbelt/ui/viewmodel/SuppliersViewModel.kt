package com.fordham.toolbelt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fordham.toolbelt.domain.model.*
import com.fordham.toolbelt.domain.usecase.*
import com.fordham.toolbelt.ui.tabs.suppliers.SupplierUiModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SuppliersData(
    val pinnedSuppliers: List<SupplierUiModel>,
    val activeSuppliers: List<SupplierUiModel>
)

sealed interface SuppliersOutcome {
    data object Loading : SuppliersOutcome
    data class Success(val data: SuppliersData) : SuppliersOutcome
    data class Failure(val error: FailureMessage) : SuppliersOutcome
}

class SuppliersViewModel(
    private val getSuppliersUseCase: GetSuppliersUseCase,
    private val updateSupplierOrderUseCase: UpdateSupplierOrderUseCase,
    private val toggleSupplierPinUseCase: ToggleSupplierPinUseCase,
    private val hideSupplierUseCase: HideSupplierUseCase,
    private val addSupplierUseCase: AddSupplierUseCase,
    private val getHiddenSuppliersUseCase: GetHiddenSuppliersUseCase,
    private val restoreSupplierUseCase: RestoreSupplierUseCase,
    private val logSupplierPurchaseUseCase: LogSupplierPurchaseUseCase,
    private val seedSuppliersUseCase: SeedSuppliersUseCase,
    private val placesService: com.fordham.toolbelt.util.PlacesService
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            seedSuppliersUseCase()
        }
    }

    private val _isAddSheetVisible = MutableStateFlow(false)
    val isAddSheetVisible = _isAddSheetVisible.asStateFlow()

    private val _capturedPhotoUri = MutableStateFlow<String?>(null)
    val capturedPhotoUri = _capturedPhotoUri.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<com.fordham.toolbelt.util.PlaceSuggestion>>(emptyList())
    val placeSuggestions = _placeSuggestions.asStateFlow()

    private val _isReorderMode = MutableStateFlow(false)
    val isReorderMode = _isReorderMode.asStateFlow()

    private val _reorderList = MutableStateFlow<List<SupplierUiModel>>(emptyList())
    val reorderList = _reorderList.asStateFlow()

    fun onSearchQueryChange(query: String) {
        viewModelScope.launch {
            _placeSuggestions.value = placesService.searchPlaces(query)
        }
    }

    fun clearSuggestions() {
        _placeSuggestions.value = emptyList()
    }

    fun setReorderMode(active: Boolean, currentList: List<SupplierUiModel> = emptyList()) {
        if (active) {
            _reorderList.value = currentList
        }
        _isReorderMode.value = active
    }

    fun swapItems(from: Int, to: Int) {
        val list = _reorderList.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        _reorderList.value = list
    }

    fun saveOrder() {
        viewModelScope.launch {
            _reorderList.value.forEachIndexed { index, model ->
                updateSupplierOrderUseCase(model.domain.id, index)
            }
            _isReorderMode.value = false
        }
    }

    val uiState: StateFlow<SuppliersOutcome> = getSuppliersUseCase()
        .map { result ->
            when (result) {
                is SupplierListOutcome.Success -> {
                    val uiModels = result.suppliers.map { it.toUiModel() }
                    val pinned = uiModels.filter { it.domain.isPinned }
                    val active = uiModels.filter { !it.domain.isPinned }
                    SuppliersOutcome.Success(SuppliersData(pinned, active))
                }
                is SupplierListOutcome.Failure -> SuppliersOutcome.Failure(result.error)
            }
        }
        .onStart { emit(SuppliersOutcome.Loading as SuppliersOutcome) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SuppliersOutcome.Loading)

    val hiddenSuppliers: StateFlow<List<SupplierUiModel>> = getHiddenSuppliersUseCase()
        .map { result -> 
            if (result is SupplierListOutcome.Success) {
                result.suppliers.map { it.toUiModel() }
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreSupplier(id: SupplierId) {
        viewModelScope.launch {
            restoreSupplierUseCase(id)
        }
    }

    fun togglePin(supplier: Supplier) {
        viewModelScope.launch {
            toggleSupplierPinUseCase(supplier.id, !supplier.isPinned)
        }
    }

    fun hideSupplier(id: SupplierId) {
        viewModelScope.launch {
            hideSupplierUseCase(id)
        }
    }

    fun updateOrder(id: SupplierId, newOrder: Int) {
        viewModelScope.launch {
            updateSupplierOrderUseCase(id, newOrder)
        }
    }

    fun addSupplier(name: String, category: SupplierCategory, address: String, phone: String, logoPath: String?) {
        viewModelScope.launch {
            val supplier = Supplier(
                id = SupplierId(com.fordham.toolbelt.util.randomUUID()),
                name = name,
                category = category,
                address = address,
                phone = PhoneNumber(phone),
                customLogoPath = logoPath,
                displayOrder = 0
            )
            addSupplierUseCase(supplier)
            _isAddSheetVisible.value = false
        }
    }

    fun setAddSheetVisible(visible: Boolean) {
        _isAddSheetVisible.value = visible
        if (!visible) _capturedPhotoUri.value = null
    }

    fun onPhotoCaptured(uri: String) {
        _capturedPhotoUri.value = uri
    }

    fun logPurchase(supplierId: SupplierId, amount: Double) {
        viewModelScope.launch {
            logSupplierPurchaseUseCase(supplierId, MoneyAmount(amount))
        }
    }

    private fun Supplier.toUiModel(): SupplierUiModel {
        val logoKey = if (isDefault) {
            when (id.value) {
                "home_depot" -> "logo_home_depot"
                "lowes" -> "logo_lowes"
                "ace" -> "logo_ace"
                "menards" -> "logo_menards"
                "ferguson" -> "logo_ferguson"
                "sherwin" -> "logo_sherwin"
                "grainger" -> "logo_grainger"
                "abc_supply" -> "logo_abc"
                "graybar" -> "logo_graybar"
                "siteone" -> "logo_siteone"
                "amazon_biz" -> "logo_amazon"
                "northern_tool" -> "logo_northern"
                "sunbelt" -> "logo_sunbelt"
                "hilti" -> "logo_hilti"
                "mcmaster" -> "logo_mcmaster"
                else -> null
            }
        } else null
        
        return SupplierUiModel(domain = this, logoKey = logoKey)
    }
}
