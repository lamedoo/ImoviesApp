package com.lukakordzaia.medootv.ui.phone.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.lukakordzaia.medootv.R
import com.lukakordzaia.medootv.datamodels.StudioList
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.rv_studio_item.view.*

class StudiosAdapter(private val context: Context, private val onGenreClick: (genreId: Int) -> Unit) : RecyclerView.Adapter<StudiosAdapter.ViewHolder>() {
    private var list: List<StudioList.Data> = ArrayList()

    fun setStudioList(list: List<StudioList.Data>) {
        this.list = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.rv_studio_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val studioModel = list[position]

        Picasso.get().load(studioModel.poster).into(holder.studioPosterImageView)
        holder.studioPosterImageView.setOnClickListener {
            onGenreClick(studioModel.id)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val studioPosterImageView: ImageView = view.rv_studio_poster
    }
}