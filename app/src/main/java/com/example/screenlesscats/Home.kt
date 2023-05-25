package com.example.screenlesscats

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.screenlesscats.block.AppBlockerService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random


const val CHECK_NCAT_TIME = 60
class Home : AppCompatActivity() {

    private lateinit var bottomNavigationBar: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        checkNewCat()

        if(!isAccessServiceEnabled(this)){
         Snackbar.make(findViewById(android.R.id.content), getString(R.string.snackbar_perms_needed), Snackbar.LENGTH_LONG)
             .setAction("Settings"){
                 requestAppAccessibilitySettings()
             }
             .show()
        }

        bottomNavigationBar = findViewById(R.id.bottom_navigation)
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
                    showAboutUs()
                    true
                }
                R.id.how_this_works_option -> {
                    showHowThisWorks()
                    true
                }
                R.id.options_option -> {
                    val intent = Intent(this, Options::class.java)
                    startActivity(intent)
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

    /*
        Method to check if the user has cats to reclaim and collects them if necessary
     */
    private fun checkNewCat() {
        //Get date
        val calendar = Calendar.getInstance()

        val currentDate =
            "" + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(
                Calendar.DAY_OF_MONTH
            ) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE)

        //The user
        val auth = Firebase.auth
        val uid = auth.uid.toString()
        //The user info in db
        val ref =
            FirebaseDatabase.getInstance("https://screenlesscats-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference(uid)

        var maxId: Long = 1
        ref.get().addOnSuccessListener { dataSnapshot ->
            //Day streak
            var ds =
                Integer.parseInt(dataSnapshot.child("user_data").child("days_streaks").value.toString())

            var dedicationValue = dataSnapshot.child("user_data").child("dedication_value").value.toString().toFloat()

            //Id from the last register of the limits
            maxId = dataSnapshot.child("user_data").child("limits").childrenCount
            //Last limit info
            val limit = dataSnapshot.child("user_data").child("limits").child(maxId.toString())

            if (limit.exists()) {
                //Cats earned until this moment
                var catsEarned = Integer.parseInt(limit.child("Cats_earned").value.toString())
                val catsEarnedStart = catsEarned

                if (limit.child("Date_limit_ended").value.toString() == "") {
                    //Convert current date and the date of the start of the limit to Date types
                    val dateFormat =
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault()
                        ) // Format of the dates
                    val date1 = dateFormat.parse(currentDate)
                    val date2 = dateFormat.parse(limit.child("Date_limit_defined").value.toString())

                    //Time passed from one date to another in seconds-------------------------------------------------------
                    val dif = date1.time - date2.time
                    val seconds = dif / 1000

                    //If the user lasted the defined time
                    if (seconds > CHECK_NCAT_TIME * ds + 1) {
                        //Update day streak
                        val scope = CoroutineScope(Dispatchers.IO) // Create a coroutine scope bound to a specific job
                        scope.launch {
                            while (ds.toLong() != (seconds / 60)) {
                                ref.child("user_data").child("days_streaks").setValue(ds + 1)
                                ds += 1
                                if(dedicationValue < 20)
                                    ref.child("user_data").child("dedication_value").setValue(dedicationValue+0.0135)
                            }
                            //Calculate how many cats does he have to reclaim
                            val catsToGet = if (ds == 0) 1 else ds

                            //Reclaim cats and update
                            while (catsEarned < catsToGet) {
                                val newCat = getRewardCatInfo(dedicationValue)
                                ref.child("cats").child(newCat["name"].toString()).setValue(newCat)
                                catsEarned += 1
                            }
                            //Pop up with the info
                            withContext(Dispatchers.Main) {
                                showCatsEarned(catsEarned - catsEarnedStart, ((seconds/86400)).toInt())
                                ref.child("user_data").child("limits").child(maxId.toString())
                                    .child("Cats_earned").setValue(ds)
                            }
                        }
                    }
                }
            }
            val newDefinedTime: HashMap<String, Any> = HashMap()
            newDefinedTime["Defined_screen_time"] = 0
        }.addOnCanceledListener {
            Log.d("BON DIA", "On Cancelled")
        }
    }


    /**
     *
     * @return HashMap with cat info
     */
    private fun getRewardCatInfo(dedicationValue : Float): HashMap<String, Any> {
        var rarities = arrayOf("mythic", "legendary", "epic", "very_rare", "rare", "common")
        var r: String = ""
        val prob = arrayOf(0.005, 0.01, 0.05, 0.115, 0.22, 0.6)
        val v25 = dedicationValue /100*(prob[5] - prob[2])
        val v14 = dedicationValue /100*(prob[4] - prob[1])
        val v03 = dedicationValue /100*(prob[3] - prob[0])
        var realprob = arrayOf(prob[0]+v03, prob[1]+v14, prob[2]+v25, prob[3]-v03, prob[4]-v14, prob[5]-v25)
        //Gets a random rarity with probability
        var randomNumber = Random.nextDouble()
        if(dedicationValue > 50) {
            realprob = realprob.reversed().toTypedArray()
            rarities = rarities.reversed().toTypedArray()
        }
        for (i in realprob.indices) {
            if (randomNumber <= realprob[i]) {
                r = rarities[i]
                break
            }
        }
        if (r == "") r = rarities.last()

        //Get random cat ID from all the cats of that rarity
        val length = countResources(r + '_', "drawable")
        val catID = Random.nextInt(0, length)

        //Create cat
        val cat = HashMap<String, Any>()
        cat["id"] = catID
        cat["name"] = getRandomWordFromRawFile(this, R.raw.cat_names) ?: "Manolo" //Random name from file
        cat["rarity"] = r

        return cat
    }

    /**
     *  Counts how many resources are in the type (ex. drawable) with wanted prefix
     */
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

    /*
        Gets random line from file
     */
    private fun getRandomWordFromRawFile(context: Context, fileId: Int): String? {
        val wordList = mutableListOf<String>()

        try {
            //Open file with id
            val inputStream: InputStream = context.resources.openRawResource(fileId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?

            //Read all lines and add them to a List
            while (reader.readLine().also { line = it } != null) {
                wordList.add(line.orEmpty())
            }

            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (wordList.isEmpty()) {
            return null
        }
        //Get random line
        val randomIndex = Random.nextInt(wordList.size)
        return wordList[randomIndex]
    }

    /*
        Pop up with how many cats the user earned
     */
    private fun showCatsEarned(catsEarned: Int, time: Int) {
        this.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.cat_pop_title))
                .setMessage(resources.getString(R.string.cat_pop_msg, catsEarned, time))
                .setNeutralButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to neutral button press
                }
                .show()

        }
    }

    private fun showAboutUs() {
        this.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.about_us_title))
                .setMessage(resources.getString(R.string.about_us))
                .setNeutralButton(resources.getString(R.string.amazing)) { dialog, which ->
                    // Respond to neutral button press
                }
                .setPositiveButton("Donate us!") { dialog, which ->
                    val uri: Uri =
                        Uri.parse("https://www.paypal.com/paypalme/ferrerjoel")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                .show()

        }
    }

    private fun showHowThisWorks() {
        this.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(resources.getString(R.string.help_title))
                .setMessage(resources.getString(R.string.help))
                .setNeutralButton(resources.getString(R.string.accept)) { dialog, which ->
                    // Respond to neutral button press
                }
                .show()

        }
    }

}