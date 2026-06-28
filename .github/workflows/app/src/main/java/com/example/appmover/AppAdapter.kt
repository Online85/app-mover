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
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.bind(app, onItemClick)
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.appIcon)
        private val name: TextView = itemView.findViewById(R.id.appName)
        private val status: TextView = itemView.findViewById(R.id.appStatus)

        fun bind(app: MainActivity.AppInfo, clickListener: (MainActivity.AppInfo) -> Unit) {
            icon.setImageDrawable(app.icon)
            name.text = app.appName
            status.text = if (app.isMoveable) "✅ Можно перенести" else "❌ Нельзя"
            status.setTextColor(
                if (app.isMoveable) android.graphics.Color.GREEN else android.graphics.Color.RED
            )
            itemView.setOnClickListener { clickListener(app) }
        }
    }
}
