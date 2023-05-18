package com.example.screenlesscats

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.screenlesscats.block.AppBlockerService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random


class Home : AppCompatActivity() {

    private lateinit var bottomNavigationBar: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        checkNewCat()

        if(!isAccessServiceEnabled(this)){
         Snackbar.make(findViewById<View>(android.R.id.content), "Accessibility perms needed. Some functionalities will now work otherwise", Snackbar.LENGTH_LONG)
             .setAction("Settings"){
                 requestAppAccessibilitySettings()
             }
             .show()
        }

        bottomNavigationBar = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        topAppBar = findViewById(R.id.top_app_bar)

        auth = FirebaseAuth.getInstance()

        val homeFragment = HomeFragment()
        val timeFragment = TimeManagementFragment()
        val catsFragment = CatsFragment()

        setCurrentFragment(homeFragment)

        bottomNavigationBar.selectedItemId = R.id.item_home

        bottomNavigationBar.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.item_time -> {
                    setCurrentFragment(timeFragment)
                    topAppBar.title = "Time management"
                    true
                }
                R.id.item_home -> {
                    setCurrentFragment(homeFragment)
                    topAppBar.title = "Home"
                    true
                }
                R.id.item_cats -> {
                    setCurrentFragment(catsFragment)
                    topAppBar.title = "Cats"
                    true
                }
                else -> false
            }
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about_us_option -> {
                    // Handle edit text press
                    true
                }
                R.id.sign_out_option -> {
                    signOut()
                    true
                }
                R.id.give_accessibility_permissions -> {
                    requestAppAccessibilitySettings()
                    true
                }
                else -> false
            }
        }

    }

    private fun setCurrentFragment(fragment: Fragment)=
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
        }
    /**
     * Signs out the user
     */
    private fun signOut() {
        auth.signOut()
        // Starts main screen
        val intent= Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    fun startBlockService() {
        val serviceClass = AppBlockerService::class.java

        if (!isServiceRunning(serviceClass)) {
            Log.d("BLOCK SERVICE", "TRYING TO START SERVICE")
            val intent = Intent(applicationContext, serviceClass)
            startService(intent)
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val runningServices = activityManager?.getRunningServices(Integer.MAX_VALUE)

        for (service in runningServices ?: emptyList()) {
            if (serviceClass.name == service.service.className) {
                Log.d("BLOCK SERVICE", "SERVICE ALREADY RUNNING")
                return true
            }
        }

        return false
    }

    private fun requestAppAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    private fun isAccessServiceEnabled(context: Context): Boolean {
        val prefString =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString.contains("${context.packageName}/${context.packageName}.block.AppBlockerService")
    }

    private fun checkNewCat(){
        val calendar = Calendar.getInstance()

        val currentDate = ""+calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE)

        val auth = Firebase.auth
        val uid = auth.uid.toString()

        val ref = FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app").getReference(uid)

        var maxId : Long = 1
        ref.get().addOnSuccessListener {
            val ds = Integer.parseInt(it.child("user_data").child("days_streaks").value.toString())

            maxId = it.child("user_data").child("limits").childrenCount
            val limit = it.child("user_data").child("limits").child(maxId.toString())

            if(limit.child("Date_limit_ended").value.toString() == ""){
                Log.d("CREISI", "Last limit not ended yet")
                Log.d("CREISI", "Date of today: $currentDate")
                Log.d("CREISI", "Date of limit: "+it.child("user_data").child("Date_limit_defined").value.toString())

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // Format of the dates
                val date1 = dateFormat.parse(currentDate)
                val date2 = dateFormat.parse(limit.child("Date_limit_defined").value.toString())

                val dif = date1.time - date2.time
                val seconds = dif / 1000
                if (seconds > 60 * ds+1){
                    ref.child("user_data").child("days_streaks").setValue(ds+1)
                    val newCat = getRewardCatInfo()
                    ref.child("cats").child(newCat["name"].toString()).setValue(newCat)

                }

            }
            val newDefinedTime : HashMap<String, Any> = HashMap()
            newDefinedTime["Defined_screen_time"] = 0
        }.addOnCanceledListener {
            Log.d("BON DIA", "On Cancelled")
        }


    }

    private fun getRewardCatInfo(): HashMap<String, Any>{
        val rarities = arrayOf("mythic", "legendary", "epic", "very_rare", "rare", "common")
        var r : String = ""
        val prob = arrayOf(0.005, 0.01, 0.05, 0.115, 0.22, 0.6)
        val randomNumber = Random.nextDouble()
        for ( i in prob.indices){
            if(randomNumber <= prob[i]){
                r = rarities[i]
            }
        }
        if(r == "") r = rarities.last()
        val length = countResources(r+'_', "drawable")

        val catID = Random.nextInt(0, length)

        val cat = HashMap<String, Any>()

        cat["id"] = catID
        cat["name"] = r+'_'+catID
        cat["rarity"] = r

        return cat
    }
    private fun countResources(prefix: String, type: String): Int {
        var id: Long = -1
        var count = -1
        while (id != 0L) {
            count++
            id = resources.getIdentifier(
                prefix + (count + 1),
                type, packageName
            ).toLong()
        }
        return count
    }

}