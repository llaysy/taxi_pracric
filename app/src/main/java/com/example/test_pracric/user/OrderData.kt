package com.example.test_pracric.user

data class OrderData(
    val fromAddress: String = "",
    val toAddress: String = "",
    val price: String = "",
    val status: String = "pending",
    val userId: String = "",
    val userRating: Float = 5.00f,
    val paymentMethod: String = ""
)