package com.sideguard.app

import java.io.File

/** Hands the current report + cached APK between activities without serializing. */
object ReportHolder {
    var report: ApkReport? = null
    var apkFile: File? = null
}
