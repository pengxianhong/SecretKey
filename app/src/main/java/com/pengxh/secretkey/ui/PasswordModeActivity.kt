package com.pengxh.secretkey.ui

import android.graphics.Color
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.pengxh.app.multilib.base.BaseNormalActivity
import com.pengxh.app.multilib.utils.SaveKeyValues
import com.pengxh.secretkey.R
import com.pengxh.secretkey.utils.StatusBarColorUtil
import kotlinx.android.synthetic.main.activity_password_mode.*
import kotlinx.android.synthetic.main.include_title_white.*

/**
 * @author: Pengxh
 * @email: 290677893@qq.com
 * @description: TODO
 * @date: 2020/7/28 11:04
 */
class PasswordModeActivity : BaseNormalActivity() {

    override fun initLayoutView(): Int = R.layout.activity_password_mode

    override fun initData() {
        StatusBarColorUtil.setColor(this, Color.WHITE)
        ImmersionBar.with(this).statusBarDarkFont(true).init()

        mTitleView.text = "设置解锁方式"
        mTitleRightView.visibility = View.GONE
    }

    override fun initEvent() {
        mTitleLeftView.setOnClickListener { this.finish() }

        val firstPassword = SaveKeyValues.getValue("firstPassword", "") as String
        if (firstPassword != "") {
            numberSwitch.isChecked = true
        }

        //解锁方式切换，事件相互独立，有且只能有一种解锁方式
        numberSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                gestureSwitch.isChecked = false
                fingerprintSwitch.isChecked = false
            }
        }

        gestureSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            //手势解锁打开前需要检查是否有手势解锁方式
            if (isChecked) {
                numberSwitch.isChecked = false
                fingerprintSwitch.isChecked = false
            }
        }

        fingerprintSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            //指纹解锁打开前需要检查是否有手势解锁方式
            if (isChecked) {
                numberSwitch.isChecked = false
                gestureSwitch.isChecked = false
            }
        }
    }
}