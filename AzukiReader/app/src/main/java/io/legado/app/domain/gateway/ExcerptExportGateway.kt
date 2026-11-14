package io.legado.app.domain.gateway

interface ExcerptExportGateway {
    /** Writes all stored quotations to the user-selected document; returns the quotation count. */
    suspend fun exportAll(destination: String): Int
}
