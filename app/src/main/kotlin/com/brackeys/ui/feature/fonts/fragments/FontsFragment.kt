/*
 * Copyright 2020 Brackeys IDE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brackeys.ui.feature.fonts.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.brackeys.ui.R
import com.brackeys.ui.databinding.FragmentFontsBinding
import com.brackeys.ui.domain.model.font.FontModel
import com.brackeys.ui.feature.base.fragments.BaseFragment
import com.brackeys.ui.feature.fonts.adapters.FontAdapter
import com.brackeys.ui.feature.fonts.viewmodel.FontsViewModel
import com.jakewharton.rxbinding3.appcompat.queryTextChangeEvents
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class FontsFragment : BaseFragment(R.layout.fragment_fonts), FontAdapter.FontInteractor {

    private val viewModel: FontsViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var binding: FragmentFontsBinding
    private lateinit var adapter: FontAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFontsBinding.bind(view)
        observeViewModel()

        navController = findNavController()
        val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.recyclerView.addItemDecoration(itemDecoration)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = FontAdapter(this)
            .also { adapter = it }

        binding.actionAdd.setOnClickListener {
            val destination = FontsFragmentDirections.toExternalFontFragment()
            navController.navigate(destination)
        }

        viewModel.fetchFonts()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_fonts, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        if (viewModel.searchQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(viewModel.searchQuery, false)
        }

        searchView
            .queryTextChangeEvents()
            .skipInitialValue()
            .debounce(200, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                viewModel.searchQuery = it.queryText.toString()
                viewModel.fetchFonts()
            }
            .disposeOnFragmentDestroyView()
    }

    override fun selectFont(fontModel: FontModel) {
        viewModel.selectFont(fontModel)
    }

    override fun removeFont(fontModel: FontModel) {
        if (fontModel.isExternal) {
            viewModel.removeFont(fontModel)
        }
    }

    private fun observeViewModel() {
        viewModel.fontsEvent.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.emptyView.isVisible = it.isEmpty()
        }
        viewModel.selectEvent.observe(viewLifecycleOwner) {
            showToast(text = getString(R.string.message_selected, it))
        }
        viewModel.removeEvent.observe(viewLifecycleOwner) {
            showToast(text = getString(R.string.message_font_removed, it))
        }
    }
}