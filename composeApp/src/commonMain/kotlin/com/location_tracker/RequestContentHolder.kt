package com.location_tracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mantém em memória o conteúdo da última request e um resumo da resposta.
 *
 * Finalidade:
 * - Exibir na UI (via `StateFlow`) informações de debug sobre o que foi enviado/recebido.
 * - Facilitar testes manuais do fluxo de envio/cache/sincronização sem depender de logs.
 */
class RequestContentHolder {
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    fun update(
        requestBody: String,
        responseSummary: String,
    ) {
        _content.value = "Request:\n$requestBody\n\nResponse: $responseSummary"
    }
}
