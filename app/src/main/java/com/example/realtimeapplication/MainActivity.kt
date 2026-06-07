package com.example.realtimeapplication

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.realtimeapplication.data.repository.AuthRepository
import com.example.realtimeapplication.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        // Removed enableEdgeToEdge() as it can conflict with keyboard resizing in some layouts
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNav.setupWithNavController(navController)
        
        val isLoggedIn = intent.getBooleanExtra("is_logged_in", false)
        val checkProfile = intent.getBooleanExtra("check_profile", false)

        if (checkProfile) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                lifecycleScope.launch {
                    val userData = AuthRepository().getUserData(uid)
                    if (userData == null || userData.username.isEmpty()) {
                        navController.navigate(R.id.registerFragment, null, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build())
                    } else {
                        navController.navigate(R.id.homeFragment, null, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build())
                    }
                }
            }
        } else if (isLoggedIn) {
            navController.navigate(R.id.homeFragment, null, NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build())
        }
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.profileFragment, R.id.searchFragment -> {
                    binding.bottomNavCard.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavCard.visibility = View.GONE
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
