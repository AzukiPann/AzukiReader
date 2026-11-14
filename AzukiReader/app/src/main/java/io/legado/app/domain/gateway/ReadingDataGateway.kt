package io.legado.app.domain.gateway

interface ReadingDataGateway {
    suspend fun exportData(destination: String)
    suspend fun importData(source: String)
}
