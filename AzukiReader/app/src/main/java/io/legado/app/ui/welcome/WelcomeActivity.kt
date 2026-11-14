package io.legado.app.ui.welcome

import android.app.Activity
import android.os.Bundle
import io.legado.app.ui.main.MainActivity

/**
 * 兼容历史内部入口，直接进入首页，不创建欢迎界面。
 */
class WelcomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(MainActivity.createHomeIntent(this))
        finish()
    }
}
