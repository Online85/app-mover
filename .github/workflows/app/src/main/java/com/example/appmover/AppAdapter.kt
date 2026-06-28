package com.example.appmover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val onItemClick: (MainActivity.AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    private var items: List<MainActivity.AppInfo> = emptyList()

    fun submitList(list: List<MainActivity.AppInfo>) {
        try {
            items = list
            notifyDataSetChanged()
        } catch (e: Exception) {
            // Игнорируем ошибки адаптера
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        } catch (e: Exception) {
            // Если не можем создать вью - возвращаем пустую
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val app = items[position]
            holder.bind(app, onItemClick)
        } catch (e: Exception) {
            // Игнорируем ошибки при отображении одного элемента
        }
    }

    override fun getItemCount(): Int {
        return try {
            items.size
        } catch (e: Exception) {
            0
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView? = try {
            itemView.findViewById(R.id.appIcon)
        } catch (e: Exception) {
            null
        }
        private val name: TextView? = try {
            itemView.findViewById(R.id.appName)
        } catch (e: Exception) {
            null
        }
        private val status: TextView? = try {
            itemView.findViewById(R.id.appStatus)
        } catch (e: Exception) {
            null
        }

        fun bind(app: MainActivity.AppInfo, clickListener: (MainActivity.AppInfo) -> Unit) {
            try {
                icon?.setImageDrawable(app.icon)
                name?.text = app.appName
                status?.text = if (app.isMoveable) "✅ Можно перенести" else "❌ Нельзя"
                status?.setTextColor(
                    if (app.isMoveable) android.graphics.Color.GREEN else android.graphics.Color.RED
                )
                itemView.setOnClickListener { clickListener(app) }
            } catch (e: Exception) {
                // Игнорируем ошибки привязки
            }
        }
    }
}
