package com.example.appmover

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private val appList = mutableListOf<AppInfo>()
    private lateinit var errorTextView: TextView
    private lateinit var statusTextView: TextView

    // Код запроса разрешений
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        errorTextView = findViewById(R.id.errorTextView)
        statusTextView = findViewById(R.id.statusTextView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppAdapter { appInfo ->
            openAppSettings(appInfo.packageName)
        }
        recyclerView.adapter = adapter

        // Проверяем и запрашиваем разрешения
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        // Для Android 11+ (API 30+) нужно разрешение QUERY_ALL_PACKAGES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Это разрешение не запрашивается через диалог, оно указывается в манифесте
            // Проверяем, есть ли у нас доступ к списку приложений
            try {
                val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                if (apps.isEmpty()) {
                    // Если список пуст, значит разрешение не дано
                    statusTextView.text = "⚠️ Нет доступа к списку приложений. Перейдите в настройки и разрешите доступ."
                    statusTextView.visibility = android.view.View.VISIBLE
                }
            } catch (e: Exception) {
                statusTextView.text = "⚠️ Ошибка доступа: ${e.message}"
                statusTextView.visibility = android.view.View.VISIBLE
            }
        }

        // Для Android 6-10 (API 23-29) запрашиваем READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            // Для Android 8+ (API 26+) запрашиваем INSTALL_PACKAGES (необязательно, но может помочь)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Это разрешение тоже не запрашивается через диалог, только в манифесте
            }
        }

        // Если есть разрешения, которые нужно запросить через диалог
        if (permissionsNeeded.isNotEmpty()) {
            statusTextView.text = "⏳ Запрашиваем разрешения..."
            statusTextView.visibility = android.view.View.VISIBLE
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Разрешения уже есть или не требуются
            statusTextView.text = "✅ Разрешения получены, загружаем приложения..."
            statusTextView.visibility = android.view.View.VISIBLE
            loadInstalledApps()
        }
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    statusTextView.text = "❌ Разрешение ${permissions[i]} не получено"
                    statusTextView.visibility = android.view.View.VISIBLE
                }
            }

            if (allGranted) {
                statusTextView.text = "✅ Все разрешения получены"
                statusTextView.visibility = android.view.View.VISIBLE
                loadInstalledApps()
            } else {
                // Если разрешения не даны, предлагаем перейти в настройки
                statusTextView.text = "⚠️ Некоторые разрешения не получены. Перейдите в настройки и разрешите их вручную."
                statusTextView.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Разрешите доступ в настройках приложения", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadInstalledApps() {
        try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            appList.clear()
            var count = 0

            for (app in apps) {
                try {
                    // Пропускаем системные приложения (можно убрать, если хотите видеть все)
                    if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

                    val isMoveable = (app.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0 ||
                            pm.getInstallLocation(app.packageName) == PackageManager.INSTALL_LOCATION_AUTO

                    appList.add(
                        AppInfo(
                            packageName = app.packageName,
                            appName = pm.getApplicationLabel(app).toString(),
                            icon = pm.getApplicationIcon(app),
                            isMoveable = isMoveable
                        )
                    )
                    count++
                } catch (e: Exception) {
                    // Пропускаем проблемные приложения
                    continue
                }
            }

            appList.sortByDescending { it.isMoveable }
            adapter.submitList(appList)

            // Показываем статус загрузки
            statusTextView.text = "📱 Загружено приложений: $count"
            statusTextView.visibility = android.view.View.VISIBLE
            errorTextView.visibility = android.view.View.GONE

        } catch (e: Exception) {
            errorTextView.text = "❌ Ошибка загрузки: ${e.message}\n\n" +
                    "Попробуйте:\n" +
                    "1. Перезапустить приложение\n" +
                    "2. Включить все разрешения в настройках\n" +
                    "3. Включить режим разработчика"
            errorTextView.visibility = android.view.View.VISIBLE
            e.printStackTrace()
        }
    }

    private fun openAppSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
        }
    }

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: android.graphics.drawable.Drawable,
        val isMoveable: Boolean
    )
}
