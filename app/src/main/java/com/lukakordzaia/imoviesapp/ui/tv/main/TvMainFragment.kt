package com.lukakordzaia.imoviesapp.ui.tv.main

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.lukakordzaia.imoviesapp.R
import com.lukakordzaia.imoviesapp.datamodels.GenreList
import com.lukakordzaia.imoviesapp.datamodels.TitleList
import com.lukakordzaia.imoviesapp.datamodels.WatchedTitleData
import com.lukakordzaia.imoviesapp.ui.phone.genres.GenresViewModel
import com.lukakordzaia.imoviesapp.ui.phone.home.HomeViewModel
import com.lukakordzaia.imoviesapp.ui.tv.details.TvDetailsActivity
import com.lukakordzaia.imoviesapp.ui.tv.search.TvSearchActivity
import com.lukakordzaia.imoviesapp.ui.tv.tvvideoplayer.TvVideoPlayerActivity

class TvMainFragment : BrowseSupportFragment() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var genresViewModel: GenresViewModel
    private lateinit var rowsAdapter: ArrayObjectAdapter
    lateinit var defaultBackground: Drawable
    lateinit var metrics: DisplayMetrics
    lateinit var backgroundManager: BackgroundManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        genresViewModel = ViewModelProvider(this).get(GenresViewModel::class.java)
        prepareBackgroundManager()
        setupUIElements()
        setupEventListeners()

        val listRowPresenter = ListRowPresenter().apply {
            shadowEnabled = false
            selectEffectEnabled = false
        }
        rowsAdapter = ArrayObjectAdapter(listRowPresenter)

        val firstHeaderItem = ListRow(HeaderItem(0, "განაგრძეთ ყურება"), ArrayObjectAdapter())
        val secondHeaderItem = ListRow(HeaderItem(1, "ტოპ ფილები"), ArrayObjectAdapter())
        val thirdHeaderItem = ListRow(HeaderItem(2, "ტოპ სერიალები"), ArrayObjectAdapter())
        val fourthHeaderItem = ListRow(HeaderItem(3, "ჟანრები"), ArrayObjectAdapter())
        val initListRows = listOf<ListRow>(firstHeaderItem, secondHeaderItem, thirdHeaderItem, fourthHeaderItem)
        rowsAdapter.add(initListRows)

        homeViewModel.getWatchedFromDb(requireContext()).observe(viewLifecycleOwner, {
            if (!it.isNullOrEmpty()) {
                homeViewModel.getWatchedTitles(it)
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            homeViewModel.getTopMovies(1)
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            homeViewModel.getTopTvShows(1)
        }, 1500)

        Handler(Looper.getMainLooper()).postDelayed({
            genresViewModel.getAllGenres()
        }, 2000)

        setHeaderPresenterSelector(object : PresenterSelector() {
            override fun getPresenter(item: Any?): Presenter {
                return TvHeaderItemPresenter()
            }
        })

        homeViewModel.watchedList.observe(viewLifecycleOwner, { watched ->
            watchedListRowsAdapter(watched)
        })

        homeViewModel.movieList.observe(viewLifecycleOwner, { movies ->
            topMoviesRowsAdapter(movies)
        })

        homeViewModel.tvShowList.observe(viewLifecycleOwner, { tvShows ->
            topTvShowsRowsAdapter(tvShows)
        })

        genresViewModel.allGenresList.observe(viewLifecycleOwner, { genres ->
            genresRowsAdapter(genres)
        })
    }

    private fun watchedListRowsAdapter(watchedList: List<WatchedTitleData>) {
        val listRowAdapter = ArrayObjectAdapter(TvWatchedCardPresenter()).apply {
            watchedList.forEach {
                add(it)
            }
        }

        val watchedHeaderItem = HeaderItem(0, "განაგრძეთ ყურება")

        if (rowsAdapter.size() > 2) {
            rowsAdapter.replace(0, ListRow(watchedHeaderItem, listRowAdapter))
        } else {
            rowsAdapter.add(ListRow(watchedHeaderItem, listRowAdapter))
        }

    }

    private fun topMoviesRowsAdapter(movies: List<TitleList.Data>) {
        val listRowAdapter = ArrayObjectAdapter(TvCardPresenter()).apply {
            movies.forEach {
                add(it)
            }
        }

        HeaderItem(1, "ტოპ ფილმები").also { header ->
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
    }

    private fun topTvShowsRowsAdapter(tvShows: List<TitleList.Data>) {
        val listRowAdapter = ArrayObjectAdapter(TvCardPresenter()).apply {
            tvShows.forEach {
                add(it)
            }
        }

        HeaderItem(2, "ტოპ სერიალები").also { header ->
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
    }

    private fun genresRowsAdapter(genreList: List<GenreList.Data>) {
        val listRowAdapter = ArrayObjectAdapter(TvGenresPresenter()).apply {
            genreList.forEach {
                add(it)
            }
        }

        HeaderItem(3, "ჟანრის მიხედვით").also { header ->
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
    }

    private fun settingsRowsAdapter() {
        val listRowAdapter = ArrayObjectAdapter(TvGenresPresenter()).apply {
            add("ნახვების ისტორიის წაშლა")
        }

        HeaderItem(4, "პარამეტრები").also { header ->
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }
    }

    private fun prepareBackgroundManager() {
        backgroundManager = BackgroundManager.getInstance(activity).apply {
            attach(activity?.window)
        }
        defaultBackground = resources.getDrawable(R.drawable.main_background)
        metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
    }

    private fun setupUIElements() {
        badgeDrawable = resources.getDrawable(R.drawable.imovies_logo)
        title = "IMOVIES"
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.green_dark)
        searchAffordanceColor = context?.let { ContextCompat.getColor(it, R.color.black) }!!
        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            startActivity(Intent(context, TvSearchActivity::class.java))
        }

        onItemViewClickedListener = ItemViewClickedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
                itemViewHolder: Presenter.ViewHolder,
                item: Any,
                rowViewHolder: RowPresenter.ViewHolder,
                row: Row
        ) {
            if (item is TitleList.Data) {
                val intent = Intent(context, TvDetailsActivity::class.java)
                intent.putExtra("titleId", item.id)
                intent.putExtra("isTvShow", item.isTvShow)
                activity?.startActivity(intent)
            } else if (item is WatchedTitleData) {
                val trailerUrl: String? = null
                val intent = Intent(context, TvVideoPlayerActivity::class.java)
                intent.putExtra("titleId", item.id)
                intent.putExtra("isTvShow", item.isTvShow)
                intent.putExtra("chosenLanguage", item.language)
                intent.putExtra("chosenSeason", item.season)
                intent.putExtra("chosenEpisode", item.episode)
                intent.putExtra("watchedTime", item.watchedTime)
                intent.putExtra("trailerUrl", trailerUrl)
                activity?.startActivity(intent)
            }
        }
    }
}