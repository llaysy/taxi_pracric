package com.example.test_pracric.user

data class UserData(
    val name: String = "",
    val email: String = "",
    val rating: Float = 5.00f, // Устанавливаем значение по умолчанию
    val isDriver: Boolean = false,
    val location: String = "", // Местоположение
    val paymentMethod: String = "" // Способ оплаты
)