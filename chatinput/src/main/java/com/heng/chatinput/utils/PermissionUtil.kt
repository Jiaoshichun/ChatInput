package com.heng.chatinput.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.TextUtils
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.Permission
import com.yanzhenjie.permission.Rationale
import com.yanzhenjie.permission.RequestExecutor

/**
 * 权限申请工具类
 * 用的这个类
 *  https://github.com/yanzhenjie/AndPermission
 */
object PermissionUtil {
    interface CallBack {
        fun onSuccess()
        fun onFail()
    }

    abstract class SimpleCallBack : CallBack {
        override fun onFail() {}
    }

    fun requestPermission(activity: Activity, vararg permission: String, callBack: CallBack? = null) {
        AndPermission.with(activity)
            .runtime()
            .permission(*permission)
            .rationale(RuntimeRationale())//如果用户拒绝过一次时下次申请，弹出提示
            .onGranted {
                callBack?.onSuccess()
            }
            .onDenied { permissions ->
                callBack?.onFail()
                if(hasPermisssion(activity,*permission))return@onDenied
                if (AndPermission.hasAlwaysDeniedPermission(activity, permissions)) {
                    //如果用户总是禁止权限，弹出提示 让用户去设置界面开启
                    showSettingDialog(activity, permissions)
                }
            }
            .start()
    }


    private class RuntimeRationale : Rationale<List<String>> {

        override fun showRationale(context: Context, permissions: List<String>, executor: RequestExecutor) {
            val permissionNames = Permission.transformText(context, permissions)
            val message = String.format("需要以下权限才可继续使用\n%s", TextUtils.join("\n", permissionNames))
            if (context !is Activity) return
            AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("继续") { dialog, which -> executor.execute() }
                .setNegativeButton("取消") { dialog, which -> executor.cancel() }
                .show()
        }

    }

    fun hasPermisssion(context: Context, vararg permission: String): Boolean {
        return AndPermission.hasPermissions(context, permission)
    }

    /**
     * Display setting dialog.
     */
    private fun showSettingDialog(activity: Activity, permissions: List<String>) {
        val permissionNames = Permission.transformText(activity, permissions)
        val message = String.format("以下权限被禁止，请去设置页开启对应权限\n%s", TextUtils.join("\n", permissionNames))
        AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle("提示")
            .setMessage(message)
            .setPositiveButton("去设置") { dialog, which -> setPermission(activity) }
            .setNegativeButton("取消") { dialog, which -> }
            .show()
    }

    /**
     * 去应用设置界面
     */
    private fun setPermission(activity: Activity) {
        AndPermission.with(activity)
            .runtime()
            .setting()
            .start(101)
    }
}