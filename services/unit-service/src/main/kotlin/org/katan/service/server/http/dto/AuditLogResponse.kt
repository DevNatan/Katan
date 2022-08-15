package org.katan.service.server.http.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.katan.model.unit.auditlog.AuditLog
import org.katan.model.unit.auditlog.AuditLogChange
import org.katan.model.unit.auditlog.AuditLogEntry
import org.katan.model.unit.auditlog.AuditLogEvent

@Serializable
internal data class AuditLogResponse(
    val entries: List<AuditLogEntryResponse>
) {

    constructor(auditLog: AuditLog) : this(auditLog.entries.map(::AuditLogEntryResponse))
}

@Serializable
internal data class AuditLogEntryResponse(
    val id: String,
    @SerialName("target-id") val targetId: Long,
    @SerialName("actor-id") val actorId: Long?,
    val event: AuditLogEvent,
    val reason: String?,
    val additionalData: String?,
    @SerialName("created-at") val createdAt: Instant,
    val changes: List<AuditLogEntryChangesResponse>
) {

    constructor(entry: AuditLogEntry) : this(
        id = entry.id.toString(),
        targetId = entry.targetId,
        actorId = entry.actorId,
        event = entry.event,
        reason = entry.reason,
        additionalData = entry.additionalData,
        createdAt = entry.createdAt,
        changes = entry.changes.map(::AuditLogEntryChangesResponse)
    )
}

@Serializable
internal data class AuditLogEntryChangesResponse(
    val key: String,
    @SerialName("old-value") val oldValue: String?,
    @SerialName("new-value") val newValue: String?
) {

    constructor(change: AuditLogChange) : this(
        key = change.key,
        oldValue = change.oldValue,
        newValue = change.newValue
    )
}
