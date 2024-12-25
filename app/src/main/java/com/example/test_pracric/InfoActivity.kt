package com.example.test_pracric

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test_pracric.user.Developer
import com.example.test_pracric.user.DeveloperAdapter

class InfoActivity : AppCompatActivity() {

    private lateinit var developersRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        val backArrow: ImageView = findViewById(R.id.back_arrow)
        backArrow.setOnClickListener {
            // Возвращаемся на HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Закрываем текущую активность
        }
        developersRecyclerView = findViewById(R.id.developersRecyclerView)
        developersRecyclerView.layoutManager = LinearLayoutManager(this)

        // Список разработчиков
        val developers = listOf(
            Developer("llaysy", R.drawable.developer_llaysy),
            Developer("Ярцев Дмитрий", R.drawable.developer_dmitry),
            Developer("Бакижанов Сардор", R.drawable.developer_sardor),
            Developer("Ефименко Андрей", R.drawable.developer_andrey),
            Developer("Царенко Никита", R.drawable.developer_nikita)
        )

        val adapter = DeveloperAdapter(developers)
        developersRecyclerView.adapter = adapter
    }
}