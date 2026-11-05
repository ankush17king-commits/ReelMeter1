# Room, Hilt and WorkManager ship their own consumer rules.
-keep class com.reelmeter.service.ReelAccessibilityService { *; }
-keep class com.reelmeter.service.FocusTimerService { *; }
-keep class com.reelmeter.service.ReelMeterWidgetProvider { *; }
-keep class com.reelmeter.data.local.** { *; }
-keep class com.reelmeter.domain.model.** { *; }
-keep class com.reelmeter.domain.repository.** { *; }
-dontwarn com.google.firebase.**
