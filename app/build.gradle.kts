plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "ru.profikrol.operator"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.profikrol.operator"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("boolean", "SHOW_RABBIT_INFO_CARDS", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://195.58.153.25:5216/\"")
            buildConfigField("String", "PRODUCTION_API_BASE_URL", "\"http://195.58.153.25:55915/\"")
            buildConfigField("String", "API_FALLBACK_HOST", "\"\"")
            buildConfigField("String", "API_FALLBACK_IP", "\"\"")
            buildConfigField("String", "NOTIFICATIONS_GRPC_HOST", "\"195.58.153.25\"")
            buildConfigField("int", "NOTIFICATIONS_GRPC_PORT", "5216")
            buildConfigField("boolean", "NOTIFICATIONS_GRPC_TLS", "true")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://profikrol.org/\"")
            buildConfigField("String", "PRODUCTION_API_BASE_URL", "\"https://profikrol.org/\"")
            buildConfigField("String", "API_FALLBACK_HOST", "\"profikrol.org\"")
            buildConfigField("String", "API_FALLBACK_IP", "\"89.108.94.132\"")
            buildConfigField("String", "NOTIFICATIONS_GRPC_HOST", "\"profikrol.org\"")
            buildConfigField("int", "NOTIFICATIONS_GRPC_PORT", "7069")
            buildConfigField("boolean", "NOTIFICATIONS_GRPC_TLS", "true")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Notification bidirectional stream
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("kotlin") { option("lite") }
                create("java") { option("lite") }
            }
            plugins {
                create("grpc") { option("lite") }
                create("grpckt")
            }
        }
    }
}
