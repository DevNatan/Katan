package org.katan.model.unit.auditlog

/**
 * Changes made to an entity in an audit log entry.
 *
 * @see AuditLogEntry
 */
interface AuditLogChange {

    /**
     * Name of the affected entity by this change.
     */
    val key: String

    /**
     * The old value of the key.
     */
    val oldValue: String?

    /**
     * The new value of the key.
     */
    val newValue: String?
}
