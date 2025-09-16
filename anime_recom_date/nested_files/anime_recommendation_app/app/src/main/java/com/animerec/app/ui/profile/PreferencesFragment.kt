package com.animerec.app.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.animerec.app.R
import com.animerec.app.data.Resource
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * Fragment for selecting content preferences
 * This includes content types (anime, manga, novels) and genre preferences
 */
class PreferencesFragment : Fragment() {
    
    private val TAG = "PreferencesFragment"
    private lateinit var viewModel: ProfileViewModel
    
    // UI components
    private lateinit var contentTypeContainer: LinearLayout
    private lateinit var genreChipGroup: ChipGroup
    private lateinit var finishButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesLabel: TextView
    private lateinit var noFavoritesText: TextView
    
    // Adapter for favorites
    private lateinit var favoritesAdapter: FavoritesAdapter
    
    // Track selected items
    private val selectedContentTypes = mutableListOf<String>()
    private val selectedGenres = mutableListOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preferences, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        contentTypeContainer = view.findViewById(R.id.contentTypeContainer)
        genreChipGroup = view.findViewById(R.id.genreChipGroup)
        finishButton = view.findViewById(R.id.finishButton)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView)
        favoritesLabel = view.findViewById(R.id.favoritesLabel)
        noFavoritesText = view.findViewById(R.id.noFavoritesText)
        
        // Set up content type checkboxes
        setupContentTypeCheckboxes()
        
        // Set up genre chips
        setupGenreChips()
        
        // Set up favorites adapter
        setupFavoritesAdapter()
        
        // Load user favorites from MyAnimeList
        loadUserFavorites()
        
        // Observe current selections
        viewModel.selectedContentTypes.observe(viewLifecycleOwner) { types ->
            selectedContentTypes.clear()
            selectedContentTypes.addAll(types)
            updateFinishButtonState()
        }
        
        viewModel.selectedGenres.observe(viewLifecycleOwner) { genres ->
            selectedGenres.clear()
            selectedGenres.addAll(genres)
            updateFinishButtonState()
        }
        
        // Initially disable finish button until valid selections are made
        finishButton.isEnabled = false
        
        // Observe user profile for pre-filling
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val user = resource.data
                
                // Pre-select content types
                if (user.contentPreferences.isNotEmpty()) {
                    // Find and check the appropriate checkboxes
                    for (i in 0 until contentTypeContainer.childCount) {
                        val checkbox = contentTypeContainer.getChildAt(i) as? CheckBox
                        if (checkbox != null) {
                            val value = when (checkbox.text.toString()) {
                                "Anime" -> "anime"
                                "Manga" -> "manga"
                                "Light Novels" -> "novels"
                                else -> ""
                            }
                            
                            if (value.isNotEmpty() && user.contentPreferences.contains(value)) {
                                checkbox.isChecked = true
                            }
                        }
                    }
                }
                
                // Pre-select genres
                if (user.genrePreferences.isNotEmpty()) {
                    // Find and check the appropriate chips
                    for (i in 0 until genreChipGroup.childCount) {
                        val chip = genreChipGroup.getChildAt(i) as? Chip
                        if (chip != null && user.genrePreferences.contains(chip.text.toString())) {
                            chip.isChecked = true
                        }
                    }
                }
                
                // Pre-select favorites
                if (user.favoriteIds.isNotEmpty()) {
                    favoritesAdapter.setSelectedIds(user.favoriteIds)
                }
            }
        }
        
        // Finish button click
        finishButton.setOnClickListener {
            // Save all preferences
            viewModel.completeProfileSetup()
            
            // Navigate to home screen
            findNavController().navigate(R.id.action_preferencesFragment_to_homeFragment)
        }
    }
    
    private fun setupFavoritesAdapter() {
        favoritesAdapter = FavoritesAdapter { content, isSelected ->
            // Handle favorite selection/deselection
            val currentIds = viewModel.selectedFavoriteIds.value ?: emptyList()
            val updatedIds = if (isSelected) {
                currentIds + content.id
            } else {
                currentIds - content.id
            }
            viewModel.updateFavorites(updatedIds)
        }
        
        favoritesRecyclerView.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun loadUserFavorites() {
        // Show/hide the favorites section based on data availability
        viewModel.favorites.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    loadingIndicator.visibility = View.VISIBLE
                    noFavoritesText.visibility = View.GONE
                    favoritesRecyclerView.visibility = View.GONE
                }
                is Resource.Success -> {
                    loadingIndicator.visibility = View.GONE
                    
                    if (resource.data.isNotEmpty()) {
                        favoritesLabel.visibility = View.VISIBLE
                        noFavoritesText.visibility = View.GONE
                        favoritesRecyclerView.visibility = View.VISIBLE
                        
                        // Update adapter with favorites
                        favoritesAdapter.submitList(resource.data)
                        
                        // Pre-select all favorites initially
                        val favoriteIds = resource.data.map { it.id }
                        favoritesAdapter.setSelectedIds(favoriteIds)
                        viewModel.updateFavorites(favoriteIds)
                    } else {
                        favoritesLabel.visibility = View.VISIBLE
                        noFavoritesText.visibility = View.VISIBLE
                        favoritesRecyclerView.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    loadingIndicator.visibility = View.GONE
                    favoritesLabel.visibility = View.VISIBLE
                    noFavoritesText.visibility = View.VISIBLE
                    favoritesRecyclerView.visibility = View.GONE
                    
                    noFavoritesText.text = "Could not load favorites: ${resource.message}"
                    Log.e(TAG, "Error loading favorites: ${resource.message}")
                }
            }
        }
        
        // Try to load favorites if the user has any
        viewModel.loadFavorites()
    }
    
    private fun setupContentTypeCheckboxes() {
        val contentTypes = listOf(
            Pair("Anime", "anime"),
            Pair("Manga", "manga"),
            Pair("Light Novels", "novels")
        )
        
        val selectedTypes = mutableListOf<String>()
        
        for ((displayName, value) in contentTypes) {
            val checkbox = CheckBox(requireContext()).apply {
                text = displayName
                isChecked = selectedContentTypes.contains(value)
                
                // Initial selection if already in list
                if (isChecked) {
                    selectedTypes.add(value)
                }
                
                // Handle check changes
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTypes.add(value)
                    } else {
                        selectedTypes.remove(value)
                    }
                    viewModel.updateContentTypes(selectedTypes)
                }
            }
            
            contentTypeContainer.addView(checkbox)
        }
        
        // Initialize with any pre-selected values
        viewModel.updateContentTypes(selectedTypes)
    }
    
    private fun setupGenreChips() {
        val genres = viewModel.getAvailableGenres()
        val selectedGenresList = mutableListOf<String>()
        
        // Function to create a genre chip
        fun createGenreChip(genre: String): Chip {
            return Chip(requireContext()).apply {
                text = genre
                isCheckable = true
                isChecked = selectedGenres.contains(genre)
                
                // Initial selection if already in list
                if (isChecked) {
                    selectedGenresList.add(genre)
                }
                
                // Custom styling
                chipBackgroundColor = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.chip_background_color
                )
                
                // Handle selection changes
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedGenresList.add(genre)
                    } else {
                        selectedGenresList.remove(genre)
                    }
                    viewModel.updateGenres(selectedGenresList)
                }
            }
        }
        
        // Add chips to the group
        for (genre in genres) {
            val chip = createGenreChip(genre)
            genreChipGroup.addView(chip)
        }
        
        // Initialize with any pre-selected values
        viewModel.updateGenres(selectedGenresList)
    }
    
    private fun updateFinishButtonState() {
        // Enable finish button only if at least one content type and genre are selected
        val isEnabled = selectedContentTypes.isNotEmpty() && selectedGenres.isNotEmpty()
        
        finishButton.isEnabled = isEnabled
        finishButton.alpha = if (isEnabled) 1.0f else 0.5f
        
        // Show toast with requirements if button is clicked while disabled
        if (!isEnabled) {
            finishButton.setOnClickListener {
                val message = when {
                    selectedContentTypes.isEmpty() && selectedGenres.isEmpty() ->
                        "Please select at least one content type and genre"
                    selectedContentTypes.isEmpty() ->
                        "Please select at least one content type"
                    else ->
                        "Please select at least one genre"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            // Reset to normal behavior when valid
            finishButton.setOnClickListener {
                viewModel.completeProfileSetup()
                findNavController().navigate(R.id.action_preferencesFragment_to_homeFragment)
            }
        }
    }
}