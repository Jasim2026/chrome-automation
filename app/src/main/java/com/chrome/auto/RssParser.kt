package com.chrome.auto

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class FeedItem(
    val title: String,
    val link: String,
    val source: String,
    val pubDate: String
)

object RssParser {
    suspend fun fetchFeed(urlString: String): List<FeedItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FeedItem>()
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            inputStream = connection.inputStream

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentTitle = ""
            var currentLink = ""
            var currentSource = ""
            var currentPubDate = ""
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            insideItem = true
                        } else if (insideItem) {
                            when (name?.lowercase()) {
                                "title" -> currentTitle = parser.nextText()
                                "link" -> currentLink = parser.nextText()
                                "source" -> currentSource = parser.nextText()
                                "pubdate" -> currentPubDate = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            result.add(FeedItem(currentTitle, currentLink, currentSource, currentPubDate))
                            insideItem = false
                            currentTitle = ""
                            currentLink = ""
                            currentSource = ""
                            currentPubDate = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
        result
    }
}
