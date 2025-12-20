package org.mlm.browkorftv.webengine.gecko.delegates

import android.util.Log
import org.mlm.browkorftv.webengine.gecko.GeckoWebEngine
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension

class AppContentScriptPortDelegate(val port: WebExtension.Port, val webEngine: GeckoWebEngine): WebExtension.PortDelegate {
    private var processSelectionCallback: ((String, Boolean) -> Unit)? = null

    override fun onPortMessage(message: Any, port: WebExtension.Port) {
        //Log.d(TAG, "onPortMessage: $message")
        try {
            val msgJson = message as JSONObject
            when (msgJson.getString("action")) {
                "selectionProcessed" -> {
                    val data = msgJson.getJSONObject("data")
                    processSelectionCallback?.invoke(
                        data.getString("selectedText"),
                        data.getBoolean("editable")
                    )
                    processSelectionCallback = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onPortMessage", e)
        }
    }

    override fun onDisconnect(port: WebExtension.Port) {
        Log.d(TAG, "onDisconnect")
        webEngine.appContentScriptPortDelegate = null
    }

    fun updateSelection(x: Int, y: Int, width: Int, height: Int) {
        val msg = JSONObject()
        msg.put("action", "updateSelection")
        msg.put("data", JSONObject().put("x", x).put("y", y)
            .put("width", width).put("height", height))
        port.postMessage(msg)
    }

    fun clearSelection() {
        val msg = JSONObject()
        msg.put("action", "clearSelection")
        port.postMessage(msg)
    }

    fun processSelection(callback: (String, Boolean) -> Unit) {
        this.processSelectionCallback = callback
        val msg = JSONObject()
        msg.put("action", "processSelection")
        port.postMessage(msg)
    }

    fun replaceSelection(newText: String) {
        val msg = JSONObject()
        msg.put("action", "replaceSelection")
        msg.put("data", newText)
        port.postMessage(msg)
    }

    companion object {
        val TAG: String = AppContentScriptPortDelegate::class.java.simpleName
    }
}