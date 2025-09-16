package com.animerec.app.ui.watchlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.animerec.app.R
import com.animerec.app.data.Resource
import com.animerec.app.models.AnimeContent
import com.animerec.app.models.ContentType
import com.google.android.material.tabs.TabLayout

/**
 * Fragment for displaying user's watchlist (plan to watch/read).
 */
class WatchlistFragment : Fragment(), WatchlistAdapter.OnItemClickListener {
    
    private val TAG = "WatchlistFragment"
    private lateinit var viewModel: WatchlistViewModel
    
    // UI components
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyStateTextView: TextView
    
    private lateinit var adapter: WatchlistAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[WatchlistViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_watchlist, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        
        // Set up the tab layout
        setupTabLayout()
        
        // Set up recycler view
        adapter = WatchlistAdapter(requireContext(), this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WatchlistFragment.adapter
            setHasFixedSize(true)
        }
        
        // Observe watchlist data
        viewModel.watchlistItems.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                    emptyStateTextView.visibility = View.GONE
                }
                is Resource.Success -> {
                    showLoading(false)
                    
                    if (resource.data.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        adapter.submitList(resource.data)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showEmptyState(true, resource.message)
                    Log.e(TAG, "Error loading watchlist: ${resource.message}")
                }
            }
        }
        
        // Load initial data
        loadWatchlist(ContentType.ANIME)
    }
    
    private fun setupTabLayout() {
        // Add tabs for different content types
        tabLayout.addTab(tabLayout.newTab().setText("Anime"))
        tabLayout.addTab(tabLayout.newTab().setText("Manga"))
        tabLayout.addTab(tabLayout.newTab().setText("Novels"))
        
        // Handle tab selection
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> loadWatchlist(ContentType.ANIME)
                    1 -> loadWatchlist(ContentType.MANGA)
                    2 -> loadWatchlist(ContentType.NOVEL)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun loadWatchlist(contentType: ContentType) {
        viewModel.loadWatchlist(contentType)
    }
    
    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun showEmptyState(isEmpty: Boolean, errorMessage: String? = null) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyStateTextView.visibility = View.VISIBLE
            
            emptyStateTextView.text = errorMessage ?: 
                "No items in your watchlist. Swipe right on recommendations to add them!"
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateTextView.visibility = View.GONE
        }
    }
    
    override fun onItemClick(content: AnimeContent) {
        // Navigate to details screen
        val bundle = Bundle().apply {
            putInt("contentId", content.id)
            putString("contentType", content.type.name)
        }
        findNavController().navigate(R.id.action_watchlistFragment_to_detailsFragment, bundle)
    }
    
    override fun onStatusChange(content: AnimeContent, newStatus: String) {
        viewModel.updateStatus(content, newStatus)
    }
}