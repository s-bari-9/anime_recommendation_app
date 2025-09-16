package com.animerec.app.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.animerec.app.AnimeRecApp
import com.animerec.app.R
import com.animerec.app.data.Resource
import com.animerec.app.models.User
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Fragment for user profile display and management.
 */
class ProfileFragment : Fragment() {
    
    private val TAG = "ProfileFragment"
    private lateinit var viewModel: ProfileViewModel
    
    // UI components
    private lateinit var profilePictureImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userDetailsTextView: TextView
    private lateinit var contentTypesChipGroup: ChipGroup
    private lateinit var genresChipGroup: ChipGroup
    private lateinit var statsTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var editPreferencesButton: Button
    private lateinit var logoutButton: Button
    private lateinit var loadingIndicator: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        profilePictureImageView = view.findViewById(R.id.profilePictureImageView)
        userNameTextView = view.findViewById(R.id.userNameTextView)
        userDetailsTextView = view.findViewById(R.id.userDetailsTextView)
        contentTypesChipGroup = view.findViewById(R.id.contentTypesChipGroup)
        genresChipGroup = view.findViewById(R.id.genresChipGroup)
        statsTextView = view.findViewById(R.id.statsTextView)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        editPreferencesButton = view.findViewById(R.id.editPreferencesButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        
        // Set up button clicks
        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_profileSetupFragment)
        }
        
        editPreferencesButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_preferencesFragment)
        }
        
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        
        // Observe user profile data
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    updateUIWithUserData(resource.data)
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error loading profile: ${resource.message}")
                }
            }
        }
        
        // Observe anime stats
        viewModel.animeStats.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val stats = resource.data
                statsTextView.text = buildString {
                    append("Anime Stats:\n")
                    append("• Watching: ${stats.watching}\n")
                    append("• Completed: ${stats.completed}\n")
                    append("• On Hold: ${stats.onHold}\n")
                    append("• Dropped: ${stats.dropped}\n")
                    append("• Plan to Watch: ${stats.planToWatch}\n")
                    append("• Total: ${stats.total}\n")
                    append("• Mean Score: ${String.format("%.1f", stats.meanScore)}")
                }
            }
        }
        
        // Load data
        viewModel.loadUserProfile()
    }
    
    /**
     * Update UI with user data.
     */
    private fun updateUIWithUserData(user: User) {
        // Set user name
        userNameTextView.text = user.name
        
        // Set user details
        val detailsBuilder = StringBuilder()
        if (user.age != null) {
            detailsBuilder.append("Age: ${user.age}")
        }
        if (!user.gender.isNullOrEmpty()) {
            if (detailsBuilder.isNotEmpty()) detailsBuilder.append(" • ")
            detailsBuilder.append("Gender: ${user.gender}")
        }
        if (!user.location.isNullOrEmpty()) {
            if (detailsBuilder.isNotEmpty()) detailsBuilder.append(" • ")
            detailsBuilder.append("Location: ${user.location}")
        }
        
        userDetailsTextView.text = detailsBuilder.toString()
        userDetailsTextView.visibility = if (detailsBuilder.isEmpty()) View.GONE else View.VISIBLE
        
        // Load profile picture if available
        if (!user.profilePictureUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.profilePictureUrl)
                .circleCrop()
                .into(profilePictureImageView)
        }
        
        // Set content type chips
        contentTypesChipGroup.removeAllViews()
        for (contentType in user.contentPreferences) {
            val chip = Chip(requireContext()).apply {
                text = when (contentType) {
                    "anime" -> "Anime"
                    "manga" -> "Manga"
                    "novels" -> "Light Novels"
                    else -> contentType.capitalize()
                }
                isCheckable = false
            }
            contentTypesChipGroup.addView(chip)
        }
        
        // Set genre chips
        genresChipGroup.removeAllViews()
        for (genre in user.genrePreferences) {
            val chip = Chip(requireContext()).apply {
                text = genre
                isCheckable = false
            }
            genresChipGroup.addView(chip)
        }
        
        // Load anime stats
        viewModel.loadAnimeStats(user.id)
    }
    
    /**
     * Show loading indicator.
     */
    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        profilePictureImageView.visibility = if (isLoading) View.GONE else View.VISIBLE
        userNameTextView.visibility = if (isLoading) View.GONE else View.VISIBLE
        userDetailsTextView.visibility = if (isLoading) View.GONE else View.VISIBLE
        contentTypesChipGroup.visibility = if (isLoading) View.GONE else View.VISIBLE
        genresChipGroup.visibility = if (isLoading) View.GONE else View.VISIBLE
        statsTextView.visibility = if (isLoading) View.GONE else View.VISIBLE
        editProfileButton.isEnabled = !isLoading
        editPreferencesButton.isEnabled = !isLoading
        logoutButton.isEnabled = !isLoading
    }
    
    /**
     * Show logout confirmation dialog.
     */
    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You will need to login again.")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Logout the user.
     */
    private fun logout() {
        val authManager = (requireActivity().application as AnimeRecApp).authManager
        authManager.logout()
        
        // Navigate to login screen
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }
}