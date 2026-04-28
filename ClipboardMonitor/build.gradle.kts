import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

fun localOrGradleProperty(key: String, default: String = ""): String {
    val fromLocal = localProperties.getProperty(key)?.trim().orEmpty()
    if (fromLocal.isNotEmpty()) return fromLocal
    return providers.gradleProperty(key).orElse(default).get()
}

/** 去掉首尾空白与成对引号，避免 local.properties 写成 OPENAI_API_KEY="sk-..." 导致带引号进 BuildConfig */
fun normalizeSecret(raw: String): String =
    raw.trim().removeSurrounding("\"").removeSurrounding("'")

/**
 * 兼容多种 key 名称（团队习惯不同）；优先读 local.properties，再读 Gradle 属性。
 */
fun resolveOpenAiApiKey(): String {
    val fromLocal = normalizeSecret(localProperties.getProperty("OPENAI_API_KEY").orEmpty())
    if (fromLocal.isNotEmpty()) return fromLocal
    return normalizeSecret(providers.gradleProperty("OPENAI_API_KEY").orElse("").get())
}

val openAiApiKey = resolveOpenAiApiKey()
val openAiModel = localOrGradleProperty("OPENAI_MODEL", "gpt-4o-mini")
val openAiBaseUrl = localOrGradleProperty("OPENAI_BASE_URL", "https://api.openai.com").trimEnd('/')
val openAiProxyHost = localOrGradleProperty("OPENAI_PROXY_HOST")
val openAiProxyPort = localOrGradleProperty("OPENAI_PROXY_PORT", "0").toIntOrNull() ?: 0
val aiProvider = localOrGradleProperty("AI_PROVIDER", "openai")
val siliconFlowApiKey = normalizeSecret(
    localOrGradleProperty("SILICONFLOW_API_KEY", openAiApiKey)
)
val siliconFlowModel = localOrGradleProperty("SILICONFLOW_MODEL", "Qwen/Qwen2.5-7B-Instruct")
val siliconFlowBaseUrl = localOrGradleProperty(
    "SILICONFLOW_BASE_URL",
    "https://api.siliconflow.cn"
).trimEnd('/')

android {
    namespace = "com.lzb.clipboardmonitor"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "OPENAI_API_KEY", "\"${openAiApiKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "OPENAI_MODEL", "\"${openAiModel.replace("\"", "\\\"")}\"")
        buildConfigField("String", "OPENAI_BASE_URL", "\"${openAiBaseUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "OPENAI_PROXY_HOST", "\"${openAiProxyHost.replace("\"", "\\\"")}\"")
        buildConfigField("int", "OPENAI_PROXY_PORT", "$openAiProxyPort")
        buildConfigField("String", "AI_PROVIDER", "\"${aiProvider.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SILICONFLOW_API_KEY", "\"${siliconFlowApiKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SILICONFLOW_MODEL", "\"${siliconFlowModel.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SILICONFLOW_BASE_URL", "\"${siliconFlowBaseUrl.replace("\"", "\\\"")}\"")
    }

    buildTypes {
        debug {
            buildConfigField("long", "CLIPBOARD_POLLING_INTERVAL_MS", "500L")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("long", "CLIPBOARD_POLLING_INTERVAL_MS", "1200L")
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kapt {
    correctErrorTypes = true
}
