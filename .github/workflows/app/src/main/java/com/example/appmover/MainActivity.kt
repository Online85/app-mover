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

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
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

            // Показываем статус
            statusTextView.text = "⏳ Инициализация..."
            statusTextView.visibility = android.view.View.VISIBLE

            // Запускаем проверку разрешений
            checkAndRequestPermissions()

        } catch (e: Exception) {
            // Если ошибка в onCreate - показываем её
            showError("Ошибка при запуске: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    private fun showError(message: String) {
        try {
            errorTextView.text = "❌ $message"
            errorTextView.visibility = android.view.View.VISIBLE
            statusTextView.visibility = android.view.View.GONE
        } catch (e: Exception) {
            // Если даже errorTextView не работает - показываем Toast
            Toast.makeText(this, "Критическая ошибка: $message", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        try {
            val permissionsNeeded = mutableListOf<String>()

            // Для Android 6+ (API 23+) запрашиваем разрешения
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
            }

            if (permissionsNeeded.isNotEmpty()) {
                statusTextView.text = "⏳ Запрашиваем разрешения..."
                statusTextView.visibility = android.view.View.VISIBLE
                ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                statusTextView.text = "✅ Разрешения есть, загружаем приложения..."
                statusTextView.visibility = android.view.View.VISIBLE
                loadInstalledApps()
            }

        } catch (e: Exception) {
            showError("Ошибка при проверке разрешений: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            if (requestCode == PERMISSION_REQUEST_CODE) {
                var allGranted = true
                for (i in permissions.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        showError("Разрешение ${permissions[i]} не получено")
                    }
                }

                if (allGranted) {
                    statusTextView.text = "✅ Все разрешения получены"
                    statusTextView.visibility = android.view.View.VISIBLE
                    loadInstalledApps()
                } else {
                    showError("Некоторые разрешения не получены. Перейдите в настройки и разрешите их вручную.")
                }
            }
        } catch (e: Exception) {
            showError("Ошибка при обработке разрешений: ${e.message}")
        }
    }

    private fun loadInstalledApps() {
        try {
            statusTextView.text = "⏳ Загрузка списка приложений..."
            statusTextView.visibility = android.view.View.VISIBLE

            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            appList.clear()
            var count = 0

            for (app in apps) {
                try {
                    // Пропускаем системные приложения
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

            statusTextView.text = "📱 Загружено приложений: $count"
            statusTextView.visibility = android.view.View.VISIBLE
            errorTextView.visibility = android.view.View.GONE

        } catch (e: Exception) {
            showError("Ошибка загрузки приложений: ${e.message}\n\n" +
                    "Попробуйте:\n" +
                    "1. Перезапустить приложение\n" +
                    "2. Включить все разрешения в настройках\n" +
                    "3. Включить режим разработчика")
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
