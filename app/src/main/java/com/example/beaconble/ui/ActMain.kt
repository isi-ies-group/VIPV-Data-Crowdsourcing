package com.example.beaconble.ui


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.beaconble.BeaconReferenceApplication
import com.example.beaconble.R
import com.google.android.material.navigation.NavigationView


class ActMain : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout

    lateinit var navController : NavController
    lateinit var navView: NavigationView

    val app = BeaconReferenceApplication.instance


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        configureToolbar()  // setups .toolbar, .drawerLayout, .actionBarDrawerToggle, .navController, .navView
        configureNavigationDrawer()  // setups .navView

        checkPermissionsAndTransferToViewIfNeeded()
        checkNeedFirstLogin()
    }

    /**
     * Check if this is the first time the user opens the app and transfer to login in that case
     */
    private fun checkNeedFirstLogin() {
        val userMayWantToLogin = getSharedPreferences("main_prefs", MODE_PRIVATE).getBoolean("userMayWantToLogin", true)
        if (userMayWantToLogin) {
            // Navigate to login fragment
            // userMayWantToLogin may be set to false in the login fragment
            supportFragmentManager.findFragmentById(R.id.fragment_main)?.findNavController()?.navigate(R.id.fragLogin)
        }
    }

    private fun checkPermissionsAndTransferToViewIfNeeded() {
        val arePermissionsOk = ActPermissions.Companion.allPermissionsGranted(this)
        if (!arePermissionsOk) {  // If any permission is not granted, go to permissions activity and wait for user to grant permissions
            val getAllPermissionsGranted = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode != RESULT_OK) {
                    // If user did not grant permissions, close the app (this should not happen)
                    Toast.makeText(this, "Permissions are required to continue", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            getAllPermissionsGranted.launch(Intent(this, ActPermissions::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.fragment_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        /** Handle navigation view item clicks here. */
        Log.d(TAG, "onNavigationItemSelected")
        Log.d(TAG, supportActionBar.toString())
        return when (item.itemId) {
            R.id.homeFragment -> {
                // Close the drawer
                drawerLayout.closeDrawers()
                findNavController(R.id.fragment_main).navigate(R.id.homeFragment)
                Log.d(TAG, "Home from drawer")
                true
            }
            R.id.nav_settings -> {  // Settings
                // Close the drawer
                drawerLayout.closeDrawers()
                findNavController(R.id.fragment_main).navigate(R.id.settingsFragment)
                Log.d(TAG, "Settings from drawer")
                true
            }
            R.id.nav_logout -> {  // Logout
                // Close the drawer
                drawerLayout.closeDrawers()
                // Logout
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
                app.apiUserSession.clearThisAndSharedPreferences(sharedPreferences)
                // Update nav view
                configureLoginLogoutMenu()
                true
            }
            R.id.nav_login -> {  // Login
                // Close the drawer
                drawerLayout.closeDrawers()
                findNavController(R.id.fragment_main).navigate(R.id.fragLogin)
                true
            }
            R.id.nav_about -> {  // About
                // Close the drawer
                drawerLayout.closeDrawers()
                // Open about page fragment
                findNavController(R.id.fragment_main).navigate(R.id.aboutFragment)
                true
            }
            R.id.nav_help -> {  // Help
                // TODO("Review")
                openURL("https://github.com/isi-ies-group/VIPV-Data-Crowdsourcing-Client")
                // findNavController(R.id.fragment_main).navigate(R.id.helpFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configureToolbar() {
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        drawerLayout = findViewById<DrawerLayout>(R.id.main_drawer_layout)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.toolbar_open, R.string.toolbar_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.isDrawerIndicatorEnabled = true
        actionBarDrawerToggle.syncState()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_main) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    private fun configureNavigationDrawer() {
        navView = findViewById<NavigationView>(R.id.nav_view_host)
        navView.setNavigationItemSelectedListener(this)

        // Only show login or logout (hides the other one)
        configureLoginLogoutMenu()
    }

    private fun configureLoginLogoutMenu() {
        val isUserLoggedIn = app.apiUserSession.isProbablyValid()
        //navView.menu.getItem(R.id.nav_logout).isVisible = isUserLoggedIn
        //navView.menu.getItem(R.id.nav_login).isVisible = !isUserLoggedIn
        Log.d(TAG, "isUserLoggedIn: $isUserLoggedIn")
        Log.d(TAG, "navView.menu: ${navView.menu}")
        Log.d(TAG, "navView.menu.size: ${navView.menu.size()}")
        for (i in 0 until navView.menu.size()) {
            val item = navView.menu.getItem(i)
            Log.d(TAG, "item: $item")
        }
    }

    fun openURL(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening link: $url; ${e.message}")
            Toast.makeText(this, "Could not open: $url", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }  // companion object
}
