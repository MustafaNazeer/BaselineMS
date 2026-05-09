package com.mustafanazeer.baselinems.util

import android.os.Build

object DeviceInfo {
    val summary: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
}
