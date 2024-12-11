package com.example.test_pracric

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class PaymentMethodActivity : AppCompatActivity() {

    private lateinit var paymentMethodTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_method)

        paymentMethodTextView = findViewById(R.id.payment_method_text)

        // Получите данные о способе оплаты
        val paymentMethod = intent.getStringExtra("PAYMENT_METHOD")
        paymentMethodTextView.text = paymentMethod ?: "Способ оплаты не указан"
    }
}