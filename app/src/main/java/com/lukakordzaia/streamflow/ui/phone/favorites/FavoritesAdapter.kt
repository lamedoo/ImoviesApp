package com.lukakordzaia.streamflow.ui.phone.favorites

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.lukakordzaia.streamflow.R
import com.lukakordzaia.streamflow.datamodels.TitleList
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.rv_favorite_item.view.*

class FavoritesAdapter(
    private val context: Context,
    private val onTitleClick: (titleId: Int) -> Unit,
//    private val onInfoClick: (titleId: Int) -> Unit,
    private val onMoreMenuClick: (titleId: Int, view: View) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
    private var list: List<TitleList.Data> = ArrayList()

    fun setFavoritesTitleList(list: List<TitleList.Data>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.rv_favorite_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listModel = list[position]

        holder.favoriteTitleRoot.setOnClickListener {
            onTitleClick(listModel.id)
        }

        Picasso.get().load(listModel.posters?.data?.x240).placeholder(R.drawable.movie_image_placeholder).error(R.drawable.movie_image_placeholder).into(holder.favoriteTitlePosterImageView)

//        holder.dbTitleInfo.setOnClickListener {
//            onInfoClick(listModel.id)
//        }
//
        holder.favoriteTitleMore.setOnClickListener {
            onMoreMenuClick(listModel.id, holder.favoriteTitleMore)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val favoriteTitleRoot: ConstraintLayout = view.rv_favorite_item_root
        val favoriteTitlePosterImageView: ImageView = view.rv_favorite_item_poster
        val favoriteTitleMore: ImageView = view.rv_favorite_item_more
        val favoriteTitleInfo: ImageView = view.rv_favorite_item_info
    }
}