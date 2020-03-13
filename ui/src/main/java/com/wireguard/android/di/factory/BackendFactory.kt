/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.di.factory

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import java.io.File

object BackendFactory {
    fun getBackend(context: Context, rootShell: RootShell, toolsInstaller: ToolsInstaller): Backend {
        var ret: Backend? = null
        if (File("/sys/module/wireguard").exists()) {
            try {
                rootShell.start()
                ret = WgQuickBackend(context, rootShell, toolsInstaller)
            } catch (_: Exception) {
            }
        }
        if (ret == null) {
            ret = GoBackend(context)
        }
        return ret
    }
}
