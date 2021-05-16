package com.pengxh.secretkey.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.pengxh.secretkey.R
import com.pengxh.secretkey.utils.Constant
import com.pengxh.secretkey.utils.SQLiteUtil

/**
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @date: 2020/7/1 15:10
 */
class SecretCategoryAdapter(ctx: Context) : BaseAdapter() {

    private var context: Context = ctx
    private var inflater: LayoutInflater
    private var sqLiteUtil: SQLiteUtil

    init {
        inflater = LayoutInflater.from(context)
        sqLiteUtil = SQLiteUtil()
    }

    override fun getCount(): Int = Constant.IMAGES.size

    override fun getItem(position: Int): Any = Constant.IMAGES[position]

    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val itemViewHolder: ItemViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.item_secret_gridview, null)
            itemViewHolder = ItemViewHolder(view)
            view.tag = itemViewHolder
        } else {
            view = convertView
            itemViewHolder = view.tag as ItemViewHolder
        }
        itemViewHolder.secretCover.setImageResource(Constant.IMAGES[position])
        itemViewHolder.secretCategory.text = Constant.CATEGORY[position]
        itemViewHolder.secretCount.text =
            "(${sqLiteUtil.loadCategory(Constant.CATEGORY[position]).size})"
        return view
    }

    class ItemViewHolder(v: View) {
        var secretCover: ImageView = v.findViewById(R.id.secretCover)
        var secretCategory: TextView = v.findViewById(R.id.secretCategory)
        var secretCount: TextView = v.findViewById(R.id.secretCount)
    }
}