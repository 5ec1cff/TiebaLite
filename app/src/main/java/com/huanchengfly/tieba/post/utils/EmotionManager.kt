package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import java.io.File
import java.lang.IllegalStateException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object EmotionManager {
    private const val EMOTION_CACHE_DIR_NAME = "emotion"
    private const val TAG = "EmotionManager"

    private lateinit var cacheDir: File
    private lateinit var handler: Handler
    private val drawableCache: MutableMap<String, Drawable> = mutableMapOf()
    private val pendingEmotions = mutableMapOf<String, MutableSet<WeakReference<View>>>()

    private val executor by lazy { Executors.newCachedThreadPool() }

    fun Init(context: Context) {
        cacheDir = File(context.cacheDir, EMOTION_CACHE_DIR_NAME)
        cacheDir.mkdirs()
        handler = Handler(Looper.getMainLooper())
    }

    private fun getEmotionFromFile(name: String, ctx: Context): Drawable? {
        try {
            val cacheFile = File(cacheDir, name)
            if (cacheFile.exists()) {
                cacheFile.inputStream().use { s ->
                    return BitmapDrawable(ctx.resources, s).also { drawableCache[name] = it }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "failed to get emotion from local cache", t)
        }
        return null
    }

    private fun fetchEmotionOnline(name: String, context: Context) {
        // dirty hack
        val realName = if (name == "image_emoticon") "image_emoticon1" else name
        val url = URL("http://static.tieba.baidu.com/tb/editor/images/client/$realName.png")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 20000
        }
        try {
            conn.connect()
            if (conn.responseCode == 200) {
                conn.inputStream.use {
                    val drawable = BitmapDrawable(context.resources, it)
                    val bitmap = drawable.bitmap
                    if (bitmap != null) {
                        handler.post {
                            drawableCache[name] = drawable
                            pendingEmotions[name]?.forEach {
                                it.get()?.invalidate()
                            }
                            pendingEmotions.remove(name)
                        }
                        File(cacheDir, name).outputStream().use { os ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                        }
                    } else {
                        throw UnknownError("bitmap is null!")
                    }
                }
            } else {
                throw IllegalStateException("responseCode=${conn.responseCode}")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error occurred while fetching and caching emotion: $name", t)
        }
    }

    fun preloadEmotionsForView(emotions: Set<String>, ctx: Context, view: View) {
        emotions.forEach { emotion ->
            if (!drawableCache.containsKey(emotion)) {
                if (pendingEmotions.containsKey(emotion)) {
                    pendingEmotions[emotion]?.add(WeakReference(view))
                } else {
                    pendingEmotions[emotion] = mutableSetOf(WeakReference(view))
                }
                executor.submit {
                    fetchEmotionOnline(emotion, ctx)
                }
            }
        }
    }

    fun getEmotionDrawable(name: String, ctx: Context): Drawable? {
        return drawableCache[name] ?: getEmotionFromFile(name, ctx)
    }
}