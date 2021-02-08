package com.lukakordzaia.imoviesapp.ui.tv.details.titlefiles

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.BrowseFrameLayout.OnFocusSearchListener
import com.lukakordzaia.imoviesapp.R
import com.lukakordzaia.imoviesapp.datamodels.*
import com.lukakordzaia.imoviesapp.ui.tv.details.TvDetailsViewModel
import com.lukakordzaia.imoviesapp.ui.tv.details.titlefiles.presenters.TvEpisodesPresenter
import com.lukakordzaia.imoviesapp.ui.tv.details.titlefiles.presenters.TvLanguagesPresenter
import com.lukakordzaia.imoviesapp.ui.tv.details.titlefiles.presenters.TvSeasonsPresenter
import com.lukakordzaia.imoviesapp.ui.tv.main.presenters.TvCardPresenter
import com.lukakordzaia.imoviesapp.ui.tv.main.presenters.TvHeaderItemPresenter
import com.lukakordzaia.imoviesapp.ui.tv.tvvideoplayer.TvVideoPlayerActivity
import com.lukakordzaia.imoviesapp.utils.createToast
import kotlinx.android.synthetic.main.tv_title_files_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class TvTitleFilesBrowse : BrowseSupportFragment() {
    private val tvDetailsViewModel by viewModel<TvDetailsViewModel>()
    private lateinit var rowsAdapter: ArrayObjectAdapter
    lateinit var defaultBackground: Drawable
    lateinit var metrics: DisplayMetrics
    private var hasFocus = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        workaroundFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val containerDock = view!!.findViewById<View>(R.id.browse_container_dock) as FrameLayout
        val params = containerDock.layoutParams as MarginLayoutParams
        val resources: Resources = inflater.context.resources
        val newHeaderMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
        val offsetToZero: Int = -resources.getDimensionPixelSize(R.dimen.lb_browse_rows_margin_top)
        params.topMargin = offsetToZero + newHeaderMargin
        containerDock.layoutParams = params
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleId = activity?.intent?.getSerializableExtra("titleId") as Int
        val isTvShow = activity?.intent?.getSerializableExtra("isTvShow") as Boolean

        val listRowPresenter = ListRowPresenter(FocusHighlight.ZOOM_FACTOR_NONE, false).apply {
            shadowEnabled = false
            selectEffectEnabled = false
        }
        rowsAdapter = ArrayObjectAdapter(listRowPresenter)

        setHeaderPresenterSelector(object : PresenterSelector() {
            override fun getPresenter(item: Any?): Presenter {
                return TvHeaderItemPresenter()
            }
        })

        initRowsAdapter()

        tvDetailsViewModel.getSingleTitleData(titleId)
        tvDetailsViewModel.getSingleTitleFiles(titleId)


        tvDetailsViewModel.numOfSeasons.observe(viewLifecycleOwner, {
            val seasonCount = Array(it!!) { i -> (i * 1) + 1 }.toList()
            seasonsRowsAdapter(seasonCount)
        })

        tvDetailsViewModel.episodeNames.observe(viewLifecycleOwner, {
            episodesRowsAdapter(it)
        })

        tvDetailsViewModel.availableLanguages.observe(viewLifecycleOwner, {
            val languages = it.reversed()
            languageRowsAdapter(languages)
        })

        prepareBackgroundManager()
        setupUIElements()
        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private fun initRowsAdapter() {
        val secondHeaderItem = ListRow(HeaderItem(0, ""), ArrayObjectAdapter(TvCardPresenter()))
        val firstHeaderItem = ListRow(HeaderItem(1, "ეპიზოდები"), ArrayObjectAdapter(TvCardPresenter()))
        val thirdIdem = ListRow(HeaderItem(2, "ენა"), ArrayObjectAdapter(TvCardPresenter()))
        val initListRows = mutableListOf(firstHeaderItem, secondHeaderItem, thirdIdem)
        rowsAdapter.addAll(0, initListRows)
    }

    private fun seasonsRowsAdapter(seasonList: List<Int>) {
        val listRowAdapter = ArrayObjectAdapter(TvSeasonsPresenter()).apply {
            seasonList.forEach {
                add(it)
            }
        }
        HeaderItem(0, ""). also {
            rowsAdapter.replace(0, ListRow(it, listRowAdapter))
        }
    }

    private fun episodesRowsAdapter(episodeList: List<TitleEpisodes>) {
        val listRowAdapter = ArrayObjectAdapter(TvEpisodesPresenter(requireContext())).apply {
            episodeList.forEach {
                add(it)
            }
        }
        HeaderItem(1, "ეპიზოდები"). also {
            rowsAdapter.replace(1, ListRow(it, listRowAdapter))
        }
    }

    private fun languageRowsAdapter(languageList: List<String>) {
        val listRowAdapter = ArrayObjectAdapter(TvLanguagesPresenter()).apply {
            languageList.forEach {
                add(it)
            }
        }
        HeaderItem(2, "ენა"). also {
            rowsAdapter.replace(2, ListRow(it, listRowAdapter))
        }
    }

    private fun prepareBackgroundManager() {
        defaultBackground = resources.getDrawable(R.drawable.main_background)
        metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
    }

    private fun setupUIElements() {
        title = ""
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.green_dark)
        searchAffordanceColor = context?.let { ContextCompat.getColor(it, R.color.black) }!!
        adapter = rowsAdapter
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
                itemViewHolder: Presenter.ViewHolder,
                item: Any,
                rowViewHolder: RowPresenter.ViewHolder,
                row: Row
        ) {
            if (item is TitleEpisodes) {
                val titleId = activity?.intent?.getSerializableExtra("titleId") as Int
                val isTvShow = activity?.intent?.getSerializableExtra("isTvShow") as Boolean
                playEpisode(titleId, isTvShow)
            } else if (item is String) {
                requireContext().createToast("შეიცვალა ენა: $item. შეგიძლიათ აირჩიოთ ეპიზოდი")
                tvDetailsViewModel.getTitleLanguageFiles(item)
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
            if (item is Int) {
                val titleId = activity?.intent?.getSerializableExtra("titleId") as Int
                tvDetailsViewModel.getSeasonFiles(titleId, item)
            } else if (item is TitleEpisodes) {
                tvDetailsViewModel.getEpisodeFile(item.episodeNum)
            }
        }
    }

    private fun playEpisode(titleId: Int, isTvShow: Boolean) {
        val trailerUrl: String? = null
        val intent = Intent(context, TvVideoPlayerActivity::class.java)
        intent.putExtra("titleId", titleId)
        intent.putExtra("isTvShow", isTvShow)
        intent.putExtra("chosenLanguage", this.tvDetailsViewModel.chosenLanguage.value)
        intent.putExtra("chosenSeason", this.tvDetailsViewModel.chosenSeason.value)
        intent.putExtra("chosenEpisode", this.tvDetailsViewModel.chosenEpisode.value)
        intent.putExtra("watchedTime", 0L)
        intent.putExtra("trailerUrl", trailerUrl)
        activity?.startActivity(intent)
    }

    private fun workaroundFocus() {
        if (view != null) {
            val viewToFocus = requireActivity().findViewById<View>(R.id.tv_details_go_top)
            val browseFrameLayout: BrowseFrameLayout = requireView().findViewById(androidx.leanback.R.id.browse_frame)
            browseFrameLayout.onFocusSearchListener = OnFocusSearchListener setOnFocusSearchListener@{ focused: View?, direction: Int ->
                if (direction == View.FOCUS_UP) {
                    this.hasFocus = true
                    return@setOnFocusSearchListener viewToFocus
                } else {
                    return@setOnFocusSearchListener null
                }
            }
        }
    }
}