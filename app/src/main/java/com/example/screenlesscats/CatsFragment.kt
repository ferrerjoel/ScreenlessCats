package com.example.screenlesscats

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.adapters.CatAdapter
import com.example.screenlesscats.data.Cat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match

class CatsFragment:Fragment(R.layout.fragment_cats) {
    private lateinit var cats : ArrayList<Cat>

    private lateinit var database: DatabaseReference
    private lateinit var auth : FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cats = ArrayList<Cat>()
        loadCats(view)
    }

    private fun loadCats(view : View) {
        auth = Firebase.auth
        val uid = auth.uid.toString()

        database = FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app").getReference(uid)
        Log.d("DATABASE_REF", database.toString())

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for(cat in dataSnapshot.child(uid).child("cats").children){
                    Log.d("BON DIA", cat.child("id").value.toString())
                    Log.d("BON DIA", cat.child("name").value.toString())
                    Log.d("BON DIA", cat.child("rarity").value.toString())

                    cats.add(Cat(
                        Integer.parseInt(cat.child("id").value.toString()),
                        cat.child("name").value.toString(),
                        cat.child("rarity").value.toString()
                    ))
                }
                createCatList(view)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("BON DIA", "On Cancelled")
            }
        })

    }
    private fun createCatList(view: View){
        val catList = view.findViewById<RecyclerView>(R.id.cat_list)
        catList.layoutManager = GridLayoutManager(view.context, 3)
        catList.adapter = CatAdapter(cats)


    }
}
