package com.example.screenlesscats

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

/**
 * Time till the user gets the next cat
 */
const val CHECK_NCAT_TIME = 172800

/**
 * Activity that manages the top bar, the menu bottom bar and all the three fragments
 *
 */
class Home : AppCompatActivity() {

    private lateinit var bottomNavigationBar: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    private lateinit var auth: FirebaseAuth

    /**
     * Initializes all the necessary components and checks accessibility permissions of the user
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        checkNewCat()

        if (!Options.isAccessServiceEnabled(this)) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.snackbar_perms_needed),
                Snackbar.LENGTH_LONG
            )
                .setAction("Settings") {
                    Options.requestAppAccessibilitySettings(this)
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
            when (item.itemId) {
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

    /**
     * Changes the current shown fragment
     *
     * @param fragment Fragment to set
     */
    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }

    /**
     * If the blocking accessibility service is not started for some reason we manually start it
     *
     */
    fun startBlockService() {
        val serviceClass = AppBlockerService::class.java

        if (!isServiceRunning(serviceClass)) {
            Log.d("BLOCK SERVICE", "TRYING TO START SERVICE")
            val intent = Intent(applicationContext, serviceClass)
            startService(intent)
        }
    }

    /**
     * Checks if the accessibility service is running
     *
     * @param serviceClass Service to check
     * @return True if the service is already running
     */
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

    /**
     * Checks if the user has accomplished the necessary conditions to receive a cat or more, if so it receives them
     *
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

        var maxId: Long
        ref.get().addOnSuccessListener { dataSnapshot ->
            //Day streak
            var ds =
                Integer.parseInt(
                    dataSnapshot.child("user_data").child("days_streaks").value.toString()
                )

            var dedicationValue =
                dataSnapshot.child("user_data").child("dedication_value").value.toString().toFloat()

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
                        val scope =
                            CoroutineScope(Dispatchers.IO) // Create a coroutine scope bound to a specific job
                        scope.launch {
                            while (ds.toLong() != (seconds / 60)) {
                                ref.child("user_data").child("days_streaks").setValue(ds + 1)
                                ds += 1
                                if (dedicationValue < 20)
                                    ref.child("user_data").child("dedication_value")
                                        .setValue(dedicationValue + 0.0135)
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
                                showCatsEarned(
                                    catsEarned - catsEarnedStart,
                                    ((seconds / 86400)).toInt()
                                )
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
     * Creates a new cat for the user
     *
     * @param dedicationValue Dedication value of the user
     * @return The information of the cat
     */
    private fun getRewardCatInfo(dedicationValue: Float): HashMap<String, Any> {
        var rarities = arrayOf("mythic", "legendary", "epic", "very_rare", "rare", "common")
        var r = ""
        val prob = arrayOf(0.005, 0.01, 0.05, 0.115, 0.22, 0.6)
        val v25 = dedicationValue / 100 * (prob[5] - prob[2])
        val v14 = dedicationValue / 100 * (prob[4] - prob[1])
        val v03 = dedicationValue / 100 * (prob[3] - prob[0])
        var realprob = arrayOf(
            prob[0] + v03,
            prob[1] + v14,
            prob[2] + v25,
            prob[3] - v03,
            prob[4] - v14,
            prob[5] - v25
        )

        //Gets a random rarity with probability
        val randomNumber = Random.nextDouble()
        if (dedicationValue > 50) {
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
        cat["name"] =
            getRandomWordFromRawFile(this, R.raw.cat_names) ?: "Manolo" //Random name from file
        cat["rarity"] = r

        return cat
    }

    /**
     * Counts how many resources are in the type (ex. drawable) with wanted prefix
     *
     * @param prefix Name of the rarity
     * @param type File type of the resource
     * @return Number of resources found
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

    /**
     * Gets a random word from a raw file separated by line jumps
     *
     * @param context Context
     * @param fileId Id of the file to get
     * @return The random word
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

    /**
     * Shows as a dialog how many cats the user has gotten after the needed time
     *
     * @param catsEarned Cats the user has earned
     * @param time Time has elapsed since the user has received the last cat
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

    /**
     * Shows a dialog showing information about the developers
     *
     */
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

    /**
     * Shows a dialog showing information on how to use the app
     *
     */
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