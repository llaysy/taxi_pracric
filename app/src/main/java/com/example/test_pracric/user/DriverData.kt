package com.example.test_pracric.user

data class DriverData(
    val fullName: String = "",          // Полное имя водителя
    val pts: String = "",               // ПТС (паспорт транспортного средства)
    val sts: String = "",               // СТС (свидетельство о регистрации транспортного средства)
    val email: String = "",             // Электронная почта
    val carNumber: String = "",         // Номер машины
    val phoneNumber: String = "",       // Номер телефона
    val licenseNumber: String = "",
    val isDriver: Boolean = true,
    val rating: Float = 5.0f            // Начальный рейтинг
)