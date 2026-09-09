import com.android.build.api.dsl.ApplicationExtension

dependencies {
    implementation(libs.hiddenapi)
}

configure<ApplicationExtension> {
    namespace = "app.morphe.extension.chmate"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
