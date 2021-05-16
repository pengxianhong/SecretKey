package com.pengxh.secretkey.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cn.bertsir.zbar.utils.QRUtils
import com.google.android.material.snackbar.Snackbar
import com.pengxh.app.multilib.utils.DensityUtil
import com.pengxh.secretkey.BaseActivity
import com.pengxh.secretkey.R
import com.pengxh.secretkey.adapter.SearchSecretAdapter
import com.pengxh.secretkey.bean.SecretBean
import com.pengxh.secretkey.utils.SQLiteUtil
import com.pengxh.secretkey.utils.ToastHelper
import com.pengxh.secretkey.widgets.ShareDialog
import kotlinx.android.synthetic.main.activity_search.*

/**
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @date: 2020/8/5 10:15
 */
class SearchEventActivity : BaseActivity() {

    private val context = this
    private lateinit var sqLiteUtil: SQLiteUtil
    private lateinit var clipboardManager: ClipboardManager

    override fun initLayoutView(): Int = R.layout.activity_search

    override fun setupTopBarLayout() {
        topLayout.setTitle("搜索结果").setTextColor(ContextCompat.getColor(this, R.color.white))
        topLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainThemeColor))
    }

    override fun initData() {
        sqLiteUtil = SQLiteUtil()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val snackBar = Snackbar.make(linearLayout, "长按账号和密码复制哦~", Snackbar.LENGTH_LONG)
        snackBar.setAction("知道了") { snackBar.dismiss() }
        snackBar.show()
    }

    override fun initEvent() {
        val keyWords = intent.getStringExtra("mode")
        val beanList = sqLiteUtil.loadAccountSecret(keyWords!!)
        initUI(beanList)
        val adapter = SearchSecretAdapter(this, beanList)
        searchRecyclerView.layoutManager = LinearLayoutManager(this)
        searchRecyclerView.adapter = adapter
        adapter.setOnItemClickListener(object : SearchSecretAdapter.OnChildViewClickListener {

            override fun onAccountLongPressed(index: Int) {
                val secretBean = beanList[index]
                val cipData = ClipData.newPlainText("secretAccount", secretBean.secretAccount)
                clipboardManager.setPrimaryClip(cipData)
                ToastHelper.showToast("账号复制成功", ToastHelper.SUCCESS)
            }

            override fun onPasswordLongPressed(index: Int) {
                val secretBean = beanList[index]
                val cipData = ClipData.newPlainText("secretPassword", secretBean.secretPassword)
                clipboardManager.setPrimaryClip(cipData)
                ToastHelper.showToast("密码复制成功", ToastHelper.SUCCESS)
            }

            override fun onShareViewClicked(index: Int) {
                val secretBean = beanList[index]
                val data = secretBean.secretAccount + "\r\n" + secretBean.secretPassword
                val createCodeBitmap = QRUtils.getInstance().createQRCode(
                    data,
                    DensityUtil.dp2px(context, 300.0f),
                    DensityUtil.dp2px(context, 300.0f)
                )
                ShareDialog.Builder().setContext(context).setDialogTitle("请不要将此二维码随意泄露给他人")
                    .setDialogBitmap(createCodeBitmap).build().show()
            }

            override fun onCopyViewClicked(index: Int) {
                //复制的数据需要精练
                val secretBean = beanList[index]
                val data = secretBean.secretAccount + "\r\n" + secretBean.secretPassword
                val clipboard: ClipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val cipData = ClipData.newPlainText("Secret", data)
                // 将ClipData内容放到系统剪贴板里。
                clipboard.setPrimaryClip(cipData);
                ToastHelper.showToast("密码复制成功", ToastHelper.SUCCESS)
            }
        })
    }

    private fun initUI(list: MutableList<SecretBean>) {
        if (list.size > 0) {
            searchRecyclerView.visibility = View.VISIBLE
            emptyLayout.visibility = View.GONE
        } else {
            emptyLayout.visibility = View.VISIBLE
            searchRecyclerView.visibility = View.GONE
        }
    }
}