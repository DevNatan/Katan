package org.katan.service.server.http.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ModifyUnitRequest(
    val name: String? = null,
    @SerialName("external-id") val externalId: String? = null
) {

    fun isEmpty(): Boolean {
        return name == null
    }
}
