package com.gigakin.stockbuddy.ui.fieldconfig

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigakin.stockbuddy.data.db.entity.FieldDefinitionEntity
import com.gigakin.stockbuddy.data.repo.FieldConfigRepository
import com.gigakin.stockbuddy.util.ArticleIdMode
import kotlinx.coroutines.launch

/** FR-77i-o / NFR-51-56: the headline dynamic field configuration + Article ID toggle. */
class FieldConfigViewModel(private val repository: FieldConfigRepository) : ViewModel() {

    val fields: LiveData<List<FieldDefinitionEntity>> = repository.observeFields()
    var articleIdMode: ArticleIdMode
        get() = repository.articleIdMode
        set(value) { repository.articleIdMode = value }

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
