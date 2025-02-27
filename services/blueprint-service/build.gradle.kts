dependencies {
    implementation(libs.koin.ktor)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hibernateValidator)
    implementation(libs.ktor.server.feature.resources)
    implementation(libs.ktx.serialization.hocon)
    implementation(projects.http.httpClient)
    implementation(projects.http.httpShared)
    implementation(projects.services.idService)
    implementation(projects.services.databaseService)
    implementation(projects.services.instanceService)
    implementation(projects.services.fsService)
}
