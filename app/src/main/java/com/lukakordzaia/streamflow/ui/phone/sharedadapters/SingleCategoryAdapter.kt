package com.lukakordzaia.streamflow.ui.phone.sharedadapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.lukakordzaia.streamflow.R
import com.lukakordzaia.streamflow.databinding.RvSingleGenreItemBinding
import com.lukakordzaia.streamflow.network.models.imovies.response.titles.GetTitlesResponse
import com.squareup.picasso.Picasso

class SingleCategoryAdapter(private val context: Context, private val onTitleClick: (id : Int) -> Unit) : RecyclerView.Adapter<SingleCategoryAdapter.ViewHolder>() {
    private var list: List<GetTitlesResponse.Data> = ArrayList()
    private var startPosition = 0

    fun setItems(list: List<GetTitlesResponse.Data>) {
        this.list = list
        startPosition += list.size
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RvSingleGenreItemBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listModel = list[position]

        holder.bind(listModel)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val view: RvSingleGenreItemBinding) : RecyclerView.ViewHolder(view.root) {
        fun bind(model: GetTitlesResponse.Data) {
            if (model.primaryName.isNotEmpty()) {
                view.itemName.text = model.primaryName
            } else {
                view.itemName.text = model.secondaryName
            }

            if (model.isTvShow == true) {
                view.isTvShow.text = "სერიალი"
            } else {
                view.isTvShow.text = "ფილმი"
            }

            Picasso.get().load(model.posters?.data?.x240).placeholder(R.drawable.movie_image_placeholder).error(R.drawable.movie_image_placeholder).into(view.itemPoster)

            view.root.setOnClickListener {
                onTitleClick(model.id)
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
            }
        }
    }
}