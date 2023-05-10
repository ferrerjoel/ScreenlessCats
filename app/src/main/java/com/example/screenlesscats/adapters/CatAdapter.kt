package com.example.screenlesscats.adapters

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.R
import com.example.screenlesscats.data.Cat

class CatAdapter(private val cats: List<Cat>) : RecyclerView.Adapter<CatAdapter.CatViewHolder>() {

    lateinit var view : View

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cat_list_element, parent, false)
        return CatViewHolder(view)
    }

    override fun onBindViewHolder(holder: CatViewHolder, position: Int) {
        val cat = cats[position]
        val imageID = holder.itemView.context.resources.getIdentifier("drawable/${cat.catRarity}_${cat.catId}", null, holder.itemView.context.packageName)
        holder.catName.text = cat.catName
        holder.catImage.setImageResource(imageID)
    }

    override fun getItemCount(): Int {
        return cats.size
    }

    class CatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val catImage: ImageView = itemView.findViewById(R.id.cat_image)
        val catName: TextView = itemView.findViewById(R.id.cat_name)
    }
}
