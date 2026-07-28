plugins {
    id("komposto.library")
    id("komposto.publish")
}

android {
    namespace = "com.trendyol.design.legacy"
}

dependencies {
    api(projects.theme)

    implementation(libs.androidxCore)
    implementation(libs.androidxAppCompat)

    implementation(platform(libs.androidxComposeBOM))
    implementation(libs.androidxComposeFoundation)
    implementation(libs.androidxComposeUiUi)
    implementation(libs.androidxComposeUiUtil)
}
