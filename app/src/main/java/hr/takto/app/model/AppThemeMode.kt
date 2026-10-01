package hr.takto.app.model

enum class AppThemeMode(val persistedValue: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromPersisted(value: String?): AppThemeMode =
            entries.firstOrNull { it.persistedValue == value } ?: DARK
    }
}
