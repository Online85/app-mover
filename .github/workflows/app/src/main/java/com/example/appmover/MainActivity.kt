package com.example.appmover

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter
    private val appList = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppAdapter { appInfo ->
            openAppSettings(appInfo.packageName)
        }
        recyclerView.adapter = adapter

        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        appList.clear()
        for (app in apps) {
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
        }

        appList.sortByDescending { it.isMoveable }
        adapter.submitList(appList)
    }

    private fun openAppSettings(packageName: String) {
        try {
            val intent = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.parse("package:$packageName")
            startActivity(Intent(intent, uri))
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
