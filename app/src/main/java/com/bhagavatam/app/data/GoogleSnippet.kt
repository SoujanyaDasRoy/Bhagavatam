package com.bhagavatam.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONTokener
import kotlin.coroutines.resume

/**
 * The first one or two lines of the meaning on Google's results page for a word, read from a page loaded out of sight.
 * It depends on how the page is laid out, so it can come back empty; the card then says so and offers the full results.
 * Must be called on the main thread (WebView).
 */
object GoogleSnippet {
    private val cache = HashMap<String, String>()

    // Sentence-like lines only (they contain a full stop or danda); page titles, URLs and menu text are skipped.
    private const val JS = """
(function(){
  var r=document.getElementById('rso')||document.getElementById('search')||document.body;
  var skip=/^https?:|›|Show more|AI Overview|People also ask|^Images|^Videos|^News|Sign in/i;
  var t=(r.innerText||'').split('\n').map(function(s){return s.trim();})
    .filter(function(s){return s.length>=28 && !skip.test(s) && /[\u0964.]/.test(s) && s.indexOf(' | ')<0;});
  return t.slice(0,2).join(' ').slice(0,240);
})();
"""

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun lookup(ctx: Context, word: String, lang: Lang): String {
        val key = "${lang.code}:$word"
        cache[key]?.let { return it }
        val text = suspendCancellableCoroutine { cont ->
            val h = Handler(Looper.getMainLooper())
            val wv = WebView(ctx)
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            var done = false
            fun finish(s: String) {
                if (done) return
                done = true
                h.removeCallbacksAndMessages(null)
                wv.stopLoading(); wv.destroy()
                if (cont.isActive) cont.resume(s)
            }
            fun read() = wv.evaluateJavascript(JS) { raw ->
                val s = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull().orEmpty().trim()
                if (s.isNotEmpty()) finish(s)
            }
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    // The answer box fills in late, so look a few times.
                    for (d in longArrayOf(300, 1200, 2500, 4000)) h.postDelayed({ if (!done) read() }, d)
                }
            }
            h.postDelayed({ finish("") }, 9000)
            cont.invokeOnCancellation { h.post { if (!done) { done = true; h.removeCallbacksAndMessages(null); wv.destroy() } } }
            wv.loadUrl("https://www.google.com/search?hl=en&q=" + Uri.encode("$word meaning"))
        }
        if (text.isNotEmpty()) cache[key] = text
        return text
    }
}
