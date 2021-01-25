package com.lukakordzaia
.imoviesapp.ui.phone.searchtitles

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.lukakordzaia.imoviesapp.R
import com.lukakordzaia.imoviesapp.utils.*
import kotlinx.android.synthetic.main.fragment_search_titles.*

class SearchTitlesFragment : Fragment(R.layout.fragment_search_titles) {
    private lateinit var viewModel: SearchTitlesViewModel
    private lateinit var searchTitlesAdapter: SearchTitlesAdapter
    private var page = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(SearchTitlesViewModel::class.java)
        search_title_text.isIconified = false


        searchTitlesAdapter = SearchTitlesAdapter(requireContext()) {
            viewModel.onSingleTitlePressed(it)
        }

        val layoutManager = GridLayoutManager(requireActivity(), 2, GridLayoutManager.VERTICAL, false)

        rv_search_titles.adapter = searchTitlesAdapter
        rv_search_titles.layoutManager = layoutManager

        search_title_text.setOnQueryTextListener(object  : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.getSearchTitles(query, page)
                    rv_search_titles.setVisible()
                    top_search_container.setGone()
                } else {
                    rv_search_titles.setGone()
                    top_search_container.setVisible()
                }
                return true
            }


            override fun onQueryTextChange(query: String?): Boolean {
                if (query.isNullOrBlank()) {
                    rv_search_titles.setGone()
                    top_search_container.setVisible()
                    viewModel.clearSearchResults()
                }
                return true
            }
        })

        viewModel.searchList.observe(viewLifecycleOwner, Observer {
            searchTitlesAdapter.setSearchTitleList(it)
        })

        viewModel.navigateScreen.observe(viewLifecycleOwner, EventObserver {
            navController(it)
        })

        infiniteScroll(search_nested_scroll) {
            fetchMoreResults()
        }

        viewModel.toastMessage.observe(viewLifecycleOwner, EventObserver {
            requireContext().createToast(it)
        })
    }

    private fun fetchMoreResults() {
        page++
        Log.d("currentpage", page.toString())
        viewModel.getSearchTitles(search_title_text.query.toString(), page)
    }
}