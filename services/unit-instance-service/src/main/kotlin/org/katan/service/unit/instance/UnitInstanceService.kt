package org.katan.service.unit.instance

import org.katan.model.unit.UnitInstance
import org.katan.model.unit.UnitInstanceUpdateStatusCode
import kotlin.jvm.Throws

public interface UnitInstanceService {

    @Throws(InstanceException::class)
    public suspend fun getInstance(id: Long): UnitInstance

    public suspend fun deleteInstance(instance: UnitInstance)

    public fun fromSpec(data: Map<String, Any>): UnitInstanceSpec

    public suspend fun createInstanceFor(spec: UnitInstanceSpec): UnitInstance

    public suspend fun updateInstanceStatus(
        instance: UnitInstance,
        code: UnitInstanceUpdateStatusCode
    )
}
