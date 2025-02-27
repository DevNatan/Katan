package org.katan.service.dockerNetwork.di

import org.katan.service.dockerNetwork.DockerNetworkServiceImpl
import org.katan.service.network.NetworkService
import org.koin.core.module.Module
import org.koin.dsl.module

public val dockerNetworkServiceDI: Module = module {
    single<NetworkService> { DockerNetworkServiceImpl(get(), get()) }
}
