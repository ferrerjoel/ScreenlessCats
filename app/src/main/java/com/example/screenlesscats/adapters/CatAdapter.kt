package com.example.screenlesscats.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.R
import com.example.screenlesscats.data.Cat

/**
 * Adapter to populate the recycler view of the cats
 *
 * @property cats List of cats to show
 */
class CatAdapter(private val cats: List<Cat>) : RecyclerView.Adapter<CatAdapter.CatViewHolder>() {

    lateinit var view: View

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.cat_list_element, parent, false)
        return CatViewHolder(view)
    }

    /**
     * Sets the view of the cat retrieving the cat data values
     */
    override fun onBindViewHolder(holder: CatViewHolder, position: Int) {
        val cat = cats[position]
        val imageID = holder.itemView.context.resources.getIdentifier(
            "drawable/${cat.catRarity}_${cat.catId}",
            null,
            holder.itemView.context.packageName
        )
        holder.catName.text = cat.catName
        Log.d("CAT", cat.catRarity)
        when (cat.catRarity) {
            "common" -> holder.catName.setTextColor(2)
            "rare" -> holder.catName.setTextColor(Color.parseColor("#ADD8E6"))
            "very_rare" -> holder.catName.setTextColor(Color.parseColor("#4682B4"))
            "epic" -> holder.catName.setTextColor(Color.parseColor("#d29bfd"))
            "legendary" -> holder.catName.setTextColor(Color.parseColor("#FFEA00"))
            "mythic" -> holder.catName.setTextColor(Color.parseColor("#C70039"))
        }
        holder.catImage.setImageResource(imageID)
    }

    override fun getItemCount(): Int {
        return cats.size
    }

    /**
     * Gets the id of the image and name text view of the cat container
     *
     * @constructor
     *
     * @param itemView View of the container of the cat
     */
    class CatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val catImage: ImageView = itemView.findViewById(R.id.cat_image)
        val catName: TextView = itemView.findViewById(R.id.cat_name)
    }
}
