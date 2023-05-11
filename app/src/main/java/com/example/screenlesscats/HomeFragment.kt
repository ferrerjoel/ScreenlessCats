package com.example.screenlesscats

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.screenlesscats.data.Cat

// TODO: Rename parameter arguments, choose names that match

class HomeFragment:Fragment(R.layout.fragment_home) {
    private lateinit var catImage : ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catImage = view.findViewById(R.id.cat_home)
        loadCat()
    }

    fun loadCat(){
        val cat = Cat(23,"Janzo", "common")
        val imageID = requireContext().resources.getIdentifier("drawable/${cat.catRarity}_${cat.catId}", null, requireContext().packageName)
        catImage.setImageResource(imageID)

    }
}
