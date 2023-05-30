package com.example.screenlesscats

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.screenlesscats.adapters.CatAdapter
import com.example.screenlesscats.data.Cat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase


/**
 * This fragment is used to show all the cats the user has
 *
 */
class CatsFragment : Fragment(R.layout.fragment_cats) {

    private lateinit var cats: ArrayList<Cat>

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var spinner: CircularProgressIndicator
    private lateinit var toInfoBtn: FloatingActionButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Get Spinner, Button
        spinner = view.findViewById(R.id.spinner)

        toInfoBtn = view.findViewById(R.id.floating_action_button)

        toInfoBtn.setOnClickListener {
            val intent = Intent(it.context, Info::class.java)
            startActivity(intent)
        }
        //Load all cats into array
        cats = ArrayList()
        loadCats(view)
    }

    /**
     * Gets all cats from the db and loads them into a RecyclerView using createCatList()
     *
     * @param view
     */
    private fun loadCats(view: View) {
        //Get user id
        auth = Firebase.auth
        val uid = auth.uid.toString()

        //Db reference
        database =
            FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(uid).child("cats")

        //Get user cats on real time
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //Add cats into array
                for (cat in dataSnapshot.children) {
                    cats.add(
                        Cat(
                            Integer.parseInt(cat.child("id").value.toString()),
                            cat.child("name").value.toString(),
                            cat.child("rarity").value.toString()
                        )
                    )
                }
                //Load to RecyclerView
                createCatList(view)

                //Hides spinner
                spinner.visibility = View.GONE

            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("BON DIA", "On Cancelled")
            }
        })

    }

    /**
     * Calls the adapter of the Recycler view
     *
     * @param view
     */
    private fun createCatList(view: View) {
        //Get RecyclerView
        val catList = view.findViewById<RecyclerView>(R.id.cat_list)

        //Add cats in rows of 3
        catList.layoutManager = GridLayoutManager(view.context, 3).apply { }

        //Add cats into RecyclerView
        catList.adapter = CatAdapter(cats, view.context)
    }
}
