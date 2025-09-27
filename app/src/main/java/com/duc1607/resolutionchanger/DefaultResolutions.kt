package com.duc1607.resolutionchanger

object DefaultResolutions {
    val all = listOf(
        // Square / baseline
        Resolution(720, 720, "Square baseline (1:1)"),
        Resolution(780, 780, "WILL BREAK SYSTEMUI!!! Square (1:1)"),
        // Portrait aspect variants
        Resolution(720, 960, "3:4 portrait (moderate)"),
        Resolution(720, 1080, "2:3 portrait (standard HD ratio)"),
        Resolution(720, 1440, "1:2 tall (battery saver tall)"),
    )

    val common = listOf(
        Resolution(720, 720, "Square baseline"),
        Resolution(720, 960, "3:4 portrait"),
//        Resolution(720, 1080, "2:3 portrait standard")
    )
}
