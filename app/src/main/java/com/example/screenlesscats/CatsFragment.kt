package com.example.screenlesscats

import android.content.ContentValues.TAG
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

    private fun loadCats(view : View){
        database = Firebase.database.reference
        auth = Firebase.auth
        val uid = auth.uid.toString()
        cats.add(Cat(23,"Janzo", "common"))
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("BON DIA", "Entrada a onDataChange")
                //Get cats child
                val aviableCats = snapshot.child(uid).child("cats")
                //Get all childs of cats
                val children = aviableCats.children
                children.forEach{
                    Log.d("BON DIA", it.child("id").value.toString())
                    Log.d("BON DIA", it.child("name").value.toString())
                    Log.d("BON DIA", it.child("rarity").value.toString())
                    //Add cat info into a cat ArrayList
                    cats.add( Cat(
                        Integer.parseInt(it.child("id").value.toString()),
                        it.child("name").value.toString(),
                        it.child("rarity").value.toString()))
                }
                createCatList(view)
            }


            override fun onCancelled(error: DatabaseError) {
                Log.d("BON DIA", "MAMAMIA" + error.toString())
            }
        })
    }
    private fun createCatList(view: View){
        val catList = view.findViewById<RecyclerView>(R.id.cat_list)
        catList.layoutManager = GridLayoutManager(view.context, 3)
        catList.adapter = CatAdapter(cats)


    }
}
