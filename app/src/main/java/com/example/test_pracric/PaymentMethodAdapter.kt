package com.example.test_pracric

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class PaymentMethodAdapter(
    private val context: Context,
    private val paymentMethods: Array<String>,
    private val icons: IntArray
) : BaseAdapter() {

    private class ViewHolder(view: View) {
        val textView: TextView = view.findViewById(R.id.payment_method_text) // Используйте правильный идентификатор
        val iconView: ImageView = view.findViewById(R.id.icon_view) // Убедитесь, что этот ID существует в вашем макете
    }

    override fun getCount(): Int = paymentMethods.size

    override fun getItem(position: Int): Any = paymentMethods[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.payment_method_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        holder.textView.text = paymentMethods[position]
        holder.iconView.setImageResource(icons[position])

        return view
    }
}