package com.gigakin.stockbuddy.ui.fieldconfig

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.repo.FieldConfigRepository
import kotlinx.coroutines.launch

/** FR-77i-o: the headline dynamic field configuration feature. */
class FieldConfigViewModel(private val repository: FieldConfigRepository) : ViewModel() {

    val fields: LiveData<List<FieldDefinitionEntity>> = repository.observeFields()

    fun applyTemplate(template: List<FieldDefinitionEntity>) = viewModelScope.launch {
        repository.saveFields(template)
    }

    fun addField(field: FieldDefinitionEntity) = viewModelScope.launch {
        val current = repository.getFields()
        repository.saveFields(current + field)
    }

    fun removeField(field: FieldDefinitionEntity) = viewModelScope.launch {
        val current = repository.getFields()
        repository.saveFields(current.filterNot { it.key == field.key })
    }
}
