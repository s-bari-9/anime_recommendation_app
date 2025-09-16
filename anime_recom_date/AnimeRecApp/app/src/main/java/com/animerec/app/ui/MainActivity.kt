package com.animerec.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.animerec.app.R
import com.animerec.app.ui.auth.AuthViewModel
import com.animerec.app.ui.auth.LoginFragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main activity that hosts all fragments and manages navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize view model
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Find the navigation host fragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up the app bar configuration with top-level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.profileFragment,
                R.id.watchlistFragment,
                R.id.historyFragment
            )
        )

        // Set up the action bar with the nav controller
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Hide bottom navigation on auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.profileSetupFragment, R.id.preferencesFragment -> {
                    bottomNav.visibility = View.GONE
                    supportActionBar?.hide()
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                    supportActionBar?.show()
                }
            }
        }
        
        // Check intent for OAuth redirect if activity is started with an intent
        handleIntent(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Handle the case where the app is opened with a deep link
     * (for OAuth redirect handling in LoginFragment)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "animerec" && uri.host == "auth") {
                // Find the current fragment
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                
                // Handle auth redirect in login fragment if it's current
                if (currentFragment is LoginFragment) {
                    currentFragment.handleIntent(intent)
                } else {
                    // Navigate to login fragment if needed
                    navController.navigate(R.id.loginFragment)
                }
            }
        }
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Clear Glide memory when the app is in the background
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            Glide.get(this).clearMemory()
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear Glide memory when the system is low on memory
        Glide.get(this).clearMemory()
    }
}