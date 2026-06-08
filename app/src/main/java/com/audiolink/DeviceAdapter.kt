package com.audiolink

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val onDeviceClick: (DiscoveryManager.Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private val devices = mutableListOf<DiscoveryManager.Device>()

    fun addDevice(device: DiscoveryManager.Device) {
        // Avoid duplicates by IP
        val existing = devices.indexOfFirst { it.ip == device.ip }
        if (existing >= 0) {
            devices[existing] = device
            notifyItemChanged(existing)
        } else {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }

    fun clear() {
        devices.clear()
        notifyDataSetChanged()
    }

    fun getDevice(index: Int) = devices[index]

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvIp: TextView = view.findViewById(R.id.tvDeviceIp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device.name
        holder.tvIp.text = "${device.ip}:${device.port}"
        holder.itemView.setOnClickListener { onDeviceClick(device) }
    }

    override fun getItemCount() = devices.size
}
