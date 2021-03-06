package com.pengxh.secretkey.adapter

import android.content.Context
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.pengxh.secretkey.R
import com.pengxh.secretkey.bean.SecretSQLiteBean

/**
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @date: 2020/8/3 12:35
 */
class SearchSecretAdapter(ctx: Context, list: MutableList<SecretSQLiteBean>) :
    RecyclerView.Adapter<SearchSecretAdapter.ItemViewHolder>() {

    private var context: Context = ctx
    private var beanList: MutableList<SecretSQLiteBean> = list

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_search_recycleview, parent, false)
        )
    }

    override fun getItemCount(): Int = beanList.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val secretBean = beanList[position]
        holder.secretTitle.text = secretBean.title
        holder.secretAccount.text = secretBean.account
        holder.secretPassword.text = secretBean.password
        holder.secretRemarks.text = secretBean.remarks

        //账号密码长按事件
        holder.secretAccount.setOnLongClickListener {
            itemClickListener!!.onAccountLongPressed(position)
            true
        }
        holder.secretPassword.setOnLongClickListener {
            itemClickListener!!.onPasswordLongPressed(position)
            true
        }

        //点击事件
        holder.shareTextView.setOnClickListener {
            itemClickListener!!.onShareViewClicked(position)
        }
        holder.visibleView.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                holder.secretPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                holder.secretPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            }
        }
    }

    class ItemViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        var secretTitle: TextView = itemView!!.findViewById(R.id.secretTitle)
        var secretAccount: TextView = itemView!!.findViewById(R.id.secretAccount)
        var secretPassword: TextView = itemView!!.findViewById(R.id.secretPassword)
        var secretRemarks: TextView = itemView!!.findViewById(R.id.secretRemarks)

        //以下控件需要绑定点击事件
        var shareTextView: TextView = itemView!!.findViewById(R.id.shareTextView)
        var visibleView: ToggleButton = itemView!!.findViewById(R.id.visibleView)
    }

    interface OnChildViewClickListener {
        fun onAccountLongPressed(index: Int)

        fun onPasswordLongPressed(index: Int)

        fun onShareViewClicked(index: Int)

        fun onCopyViewClicked(index: Int)
    }

    fun setOnItemClickListener(listener: OnChildViewClickListener) {
        this.itemClickListener = listener
    }

    private var itemClickListener: OnChildViewClickListener? = null
}