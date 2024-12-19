package com.example.test_pracric.doc

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_pracric.R

class DocumentsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents)

        // Получите ссылки на ваши LinearLayout
        val rightsLayout: LinearLayout = findViewById(R.id.rightsLayout)
        val vehiclePassportLayout: LinearLayout = findViewById(R.id.vehiclePassportLayout)
        val registrationCertificateLayout: LinearLayout = findViewById(R.id.registrationCertificateLayout)

        // Установите обработчики кликов
        rightsLayout.setOnClickListener {
            val intent = Intent(this, RightsActivity::class.java) // Укажите вашу активность
            startActivity(intent)
        }

        vehiclePassportLayout.setOnClickListener {
            val intent = Intent(this, VehiclePassportActivity::class.java) // Укажите вашу активность
            startActivity(intent)
        }

        registrationCertificateLayout.setOnClickListener {
            val intent = Intent(this, RegistrationCertificateActivity::class.java) // Укажите вашу активность
            startActivity(intent)
        }

        // Обработчик для кнопки "Назад"
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Закрыть текущую активность
        }
    }
}