package com.animerec.app.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.animerec.app.R
import com.animerec.app.data.Resource
import com.google.android.material.textfield.TextInputLayout

/**
 * Fragment for user profile setup
 * This collects basic user information like name, age, gender, and location
 */
class ProfileSetupFragment : Fragment() {
    
    private val TAG = "ProfileSetupFragment"
    private lateinit var viewModel: ProfileViewModel
    
    // UI components
    private lateinit var nameEditText: EditText
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var ageEditText: EditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var locationEditText: EditText
    private lateinit var nextButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var titleTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_setup, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        nameInputLayout = view.findViewById(R.id.nameInputLayout)
        nameEditText = view.findViewById(R.id.nameEditText)
        ageEditText = view.findViewById(R.id.ageEditText)
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup)
        locationEditText = view.findViewById(R.id.locationEditText)
        nextButton = view.findViewById(R.id.nextButton)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        titleTextView = view.findViewById(R.id.titleTextView)
        
        // Add text change listener for name input to validate in real-time
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotBlank() == true) {
                    nameInputLayout.error = null
                    nextButton.isEnabled = true
                } else {
                    nameInputLayout.error = "Please enter your name"
                    nextButton.isEnabled = false
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Disable next button initially if name is empty
        nextButton.isEnabled = nameEditText.text.isNotBlank()
        
        // Observe user profile data
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    
                    // Pre-fill with existing data if available
                    val user = resource.data
                    if (user.name.isNotBlank()) {
                        nameEditText.setText(user.name)
                    }
                    
                    user.age?.let { ageEditText.setText(it.toString()) }
                    user.location?.let { locationEditText.setText(it) }
                    
                    // Set gender radio button if available
                    user.gender?.let { gender ->
                        when (gender.lowercase()) {
                            "male" -> {
                                view.findViewById<RadioButton>(R.id.maleRadioButton).isChecked = true
                            }
                            "female" -> {
                                view.findViewById<RadioButton>(R.id.femaleRadioButton).isChecked = true
                            }
                            "other" -> {
                                view.findViewById<RadioButton>(R.id.otherRadioButton).isChecked = true
                            }
                        }
                    }
                    
                    // Check if user already completed setup before
                    if (user.isSetupComplete()) {
                        titleTextView.text = "Edit Your Profile"
                        nextButton.text = "Save & Continue"
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error loading profile: ${resource.message}")
                }
            }
        }
        
        // Handle next button click
        nextButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val age = ageEditText.text.toString().trim().toIntOrNull()
            val location = locationEditText.text.toString().trim()
            
            // Validate name (required field)
            if (name.isEmpty()) {
                nameInputLayout.error = "Please enter your name"
                return@setOnClickListener
            }
            
            // Get selected gender
            val gender = when (genderRadioGroup.checkedRadioButtonId) {
                R.id.maleRadioButton -> "Male"
                R.id.femaleRadioButton -> "Female"
                R.id.otherRadioButton -> "Other"
                else -> null
            }
            
            // Save profile information
            viewModel.saveUserProfile(name, age, gender, location)
            
            // Navigate to preferences screen
            findNavController().navigate(R.id.action_profileSetupFragment_to_preferencesFragment)
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        nameEditText.isEnabled = !isLoading
        ageEditText.isEnabled = !isLoading
        genderRadioGroup.isEnabled = !isLoading
        locationEditText.isEnabled = !isLoading
        nextButton.isEnabled = !isLoading
    }
}