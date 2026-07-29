plugins {
    id("komposto.library")
    id("komposto.publish")
}

android {
    namespace = "com.trendyol.design.compat"
}

dependencies {
    api(projects.theme)

    implementation(libs.androidxCore)
    implementation(libs.androidxAppCompat)
    implementation(libs.androidxLifecycleRuntime)

    implementation(platform(libs.androidxComposeBOM))
    implementation(libs.androidxComposeFoundation)
    implementation(libs.androidxComposeUiUi)
    implementation(libs.androidxComposeUiUtil)
}
