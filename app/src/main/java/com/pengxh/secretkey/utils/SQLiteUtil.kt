package com.pengxh.secretkey.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.pengxh.app.multilib.utils.BroadcastManager
import com.pengxh.secretkey.BaseApplication
import com.pengxh.secretkey.bean.SecretBean
import java.util.*

class SQLiteUtil {

    companion object {
        private const val Tag = "SQLiteUtil"

        val instance: SQLiteUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SQLiteUtil()
        }
    }

    private val db: SQLiteDatabase
    private var context: Context = BaseApplication.instance()
    private val manager: BroadcastManager

    /**
     * 数据库名
     */
    private val databaseName = "SecretKey.db"

    /**
     * 数据库表名
     */
    private val tableName = "SecretNewTable"

    /**
     * 数据库版本
     */
    private val databaseVersion = 2

    init {
        val mSqLiteUtilHelper = SQLiteUtilHelper(context, databaseName, null, databaseVersion)
        db = mSqLiteUtilHelper.writableDatabase
        //通知列表页更新数据
        manager = BroadcastManager.getInstance(context)
    }


    /**
     * 保存账号密码
     * */
    fun saveSecret(
        secretCategory: String,
        secretTitle: String,
        secretAccount: String,
        secretPassword: String,
        secretRemarks: String?
    ) {
        val values = ContentValues()
        values.put("secretCategory", secretCategory)
        values.put("secretTitle", secretTitle)
        values.put("secretAccount", secretAccount)
        values.put("secretPassword", SecretHelper.encode(secretPassword))
        values.put("secretRemarks", secretRemarks)
        if (!isSecretExist(secretTitle, secretAccount)) {
            Log.d(Tag, secretCategory + "，" + secretAccount + "保存密码")
            db.insert(tableName, null, values)
        } else {
            //批量导入可能会有重复的，需要更新数据
            updateSecret(secretTitle, secretAccount, secretCategory, secretPassword, secretRemarks)
        }
        //通知列表页更新数据
        manager.sendBroadcast(Constant.ACTION_UPDATE, "updateData")
    }

    /**
     * 更新账号信息
     * */
    fun updateSecret(
        secretTitle: String,
        secretAccount: String,
        secretCategory: String,
        secretPassword: String,
        secretRemarks: String?
    ) {
        Log.d(Tag, secretAccount + "更新账号信息")
        val values = ContentValues()
        values.put("secretCategory", secretCategory)
        values.put("secretPassword", SecretHelper.encode(secretPassword))
        values.put("secretRemarks", secretRemarks)
        if (isSecretExist(secretTitle, secretAccount)) {
            db.update(
                tableName,
                values,
                "secretTitle = ? and secretAccount = ?",
                arrayOf(secretTitle, secretAccount)
            )
        }
        //通知列表页更新数据
        manager.sendBroadcast(Constant.ACTION_UPDATE, "updateData")
    }

    /**
     * 加载某个分类的数据
     * */
    fun loadCategory(category: String): MutableList<SecretBean> {
        val list: MutableList<SecretBean> = ArrayList()
        val cursor = db.query(
            tableName,
            null,
            "secretCategory = ?",
            arrayOf(category),
            null,
            null,
            "id DESC"
        ) //倒序
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val resultBean = SecretBean()
            resultBean.secretCategory = cursor.getString(cursor.getColumnIndex("secretCategory"))
            resultBean.secretTitle = cursor.getString(cursor.getColumnIndex("secretTitle"))
            resultBean.secretAccount = cursor.getString(cursor.getColumnIndex("secretAccount"))
            resultBean.secretPassword =
                SecretHelper.decode(cursor.getString(cursor.getColumnIndex("secretPassword")))
            resultBean.secretRemarks = cursor.getString(cursor.getColumnIndex("secretRemarks"))
            list.add(resultBean)
            //下一次循环开始
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    /**
     * 删除数据
     * */
    fun deleteSecret(title: String, account: String) {
        db.delete(tableName, "secretTitle = ? and secretAccount = ?", arrayOf(title, account))
        //通知列表页更新数据
        manager.sendBroadcast(Constant.ACTION_UPDATE, "updateData")
    }

    /**
     * 查询账号下所有的数据
     * */
    fun loadAccountSecret(account: String): MutableList<SecretBean> {
        Log.d(Tag, "查询账号: $account" + "所有数据")
        val list: MutableList<SecretBean> = ArrayList()
        val cursor = db.query(
            tableName,
            null,
            "secretAccount = ?",
            arrayOf(account),
            null,
            null,
            null
        )
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val resultBean = SecretBean()
            resultBean.secretCategory = cursor.getString(cursor.getColumnIndex("secretCategory"))
            resultBean.secretTitle = cursor.getString(cursor.getColumnIndex("secretTitle"))
            resultBean.secretAccount = cursor.getString(cursor.getColumnIndex("secretAccount"))
            resultBean.secretPassword =
                SecretHelper.decode(cursor.getString(cursor.getColumnIndex("secretPassword")))
            resultBean.secretRemarks = cursor.getString(cursor.getColumnIndex("secretRemarks"))
            list.add(resultBean)

            //下一次循环开始
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    /**
     * 查询所有的数据
     * */
    fun loadAllSecret(): ArrayList<SecretBean> {
        val list: ArrayList<SecretBean> = ArrayList()
        val cursor = db.query(tableName, null, null, null, null, null, "id DESC")
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val resultBean = SecretBean()
            resultBean.secretCategory = cursor.getString(cursor.getColumnIndex("secretCategory"))
            resultBean.secretTitle = cursor.getString(cursor.getColumnIndex("secretTitle"))
            resultBean.secretAccount = cursor.getString(cursor.getColumnIndex("secretAccount"))
            resultBean.secretPassword =
                SecretHelper.decode(cursor.getString(cursor.getColumnIndex("secretPassword")))
            resultBean.secretRemarks = cursor.getString(cursor.getColumnIndex("secretRemarks"))
            list.add(resultBean)

            //下一次循环开始
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    /**
     * 双重判断，title和account都相同才认为是同一个账号修改密码，否则认为是新增账号密码
     * */
    private fun isSecretExist(title: String, account: String): Boolean {
        var result = false
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                tableName,
                null,
                "secretTitle = ? and secretAccount = ?",
                arrayOf(title, account),
                null,
                null,
                null
            )
            result = null != cursor && cursor.moveToFirst()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (null != cursor && !cursor.isClosed) {
                cursor.close()
            }
        }
        return result
    }
}