package com.example.screenlesscats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.media.Image
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.adapters.CatAdapter
import com.example.screenlesscats.data.Cat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match

class CatsFragment:Fragment(R.layout.fragment_cats) {
    private lateinit var cats : ArrayList<Cat>
    private lateinit var allCats : List<Image>

    private lateinit var database: DatabaseReference
    private lateinit var auth : FirebaseAuth

    private fun loadCats(){
        database = Firebase.database.reference
        auth = Firebase.auth
        CoroutineScope(Dispatchers.IO).launch {
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allCats = snapshot.child(auth.uid).child("cats")
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            }
        }


    }
    private fun createCatList(view: View){
        val catList = view.findViewById<RecyclerView>(R.id.cat_list)
        catList.layoutManager = GridLayoutManager(view.context, 3)
        val cats :
        catList.adapter = CatAdapter(cats)


    }
}
