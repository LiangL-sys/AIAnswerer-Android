package com.hwb.aianswerer

import android.content.Context
import androidx.activity.ComponentActivity
import com.hwb.aianswerer.utils.LanguageUtil

/**
 * 基础 Activity
 * 统一处理语言配置，所有 Activity 应继承此类
 */
abstract class BaseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtil.attachBaseContext(newBase))
    }
}
