package com.bicy.whitenoise.onlinemusic

import android.util.Log
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.MusicInfoOnline
import com.bicy.whitenoise.onlinemusic.model.SourceModelsPart.Sources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 内置 SDK 搜索引擎
 * 参考 lx-music-desktop 的 musicSdk 实现
 * 直接调用各平台 API 进行搜索
 */
object OnlineSearchEngine {

    private const val TAG = "OnlineSearchEngine"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 搜索音乐
     * @param source 音源平台 (kw/kg/tx/wy/mg)
     * @param keyword 搜索关键词
     * @param page 页码
     * @param limit 每页数量
     */
    suspend fun search(
        source: String,
        keyword: String,
        page: Int = 1,
        limit: Int = 30
    ): List<MusicInfoOnline> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext emptyList()
        
        try {
            val results = when (source) {
                Sources.KW -> searchKuwo(keyword, page, limit)
                Sources.KG -> searchKugou(keyword, page, limit)
                Sources.TX -> searchQQ(keyword, page, limit)
                Sources.WY -> searchNetease(keyword, page, limit)
                Sources.MG -> searchMigu(keyword, page, limit)
                else -> {
                    Log.w(TAG, "不支持的音源: $source")
                    emptyList()
                }
            }
            Log.d(TAG, "源 $source 搜索到 ${results.size} 条结果")
            results
        } catch (e: Exception) {
            Log.e(TAG, "搜索 $source 失败", e)
            emptyList()
        }
    }

    /**
     * 酷我音乐搜索
     */
    private fun searchKuwo(keyword: String, page: Int, limit: Int): List<MusicInfoOnline> {
        val url = "http://search.kuwo.cn/r.s?client=kt&all=${URLEncoder.encode(keyword, "UTF-8")}" +
            "&pn=${page - 1}&rn=$limit&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1" +
            "&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012&encoding=utf8" +
            "&rformat=json&vermerge=1&mobi=1&issubtitle=1"

        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        return parseKuwoResult(body)
    }

    private fun parseKuwoResult(body: String): List<MusicInfoOnline> {
        val results = mutableListOf<MusicInfoOnline>()
        try {
            val json = JSONObject(body)
            val list = json.optJSONArray("abslist") ?: return emptyList()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val musicId = item.optString("MUSICRID", "").replace("MUSIC_", "")
                val songmid = item.optString("DC_TARGETID", musicId)  // 优先使用 DC_TARGETID
                
                results.add(MusicInfoOnline(
                    id = musicId,
                    name = item.optString("SONGNAME", ""),
                    singer = item.optString("ARTIST", ""),
                    source = Sources.KW,
                    interval = formatDuration(item.optInt("DURATION", 0)),
                    songmid = songmid,  // 酷我 songmid
                    songId = musicId,
                    albumName = item.optString("ALBUM", ""),
                    albumId = item.optString("ALBUMID", ""),
                    picUrl = null,
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析酷我搜索结果失败", e)
        }
        return results
    }

    /**
     * 酷狗音乐搜索
     */
    private fun searchKugou(keyword: String, page: Int, limit: Int): List<MusicInfoOnline> {
        val url = "https://mobileservice.kugou.com/api/v3/search/msong?version=9108" +
            "&keyword=${URLEncoder.encode(keyword, "UTF-8")}&page=$page&pagesize=$limit" +
            "&showtype=1"

        val response = client.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        return parseKugouResult(body)
    }

    private fun parseKugouResult(body: String): List<MusicInfoOnline> {
        val results = mutableListOf<MusicInfoOnline>()
        try {
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: return emptyList()
            val list = data.optJSONArray("info") ?: return emptyList()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val hash = item.optString("hash", "")
                
                results.add(MusicInfoOnline(
                    id = hash,
                    name = item.optString("songname", ""),
                    singer = item.optString("singername", ""),
                    source = Sources.KG,
                    interval = formatDuration(item.optInt("duration", 0)),
                    songmid = item.optString("songmid", hash),  // 酷狗 songmid
                    hash = hash,  // 酷狗专用 hash
                    songId = hash,  // 酷狗的 songId 就是 hash
                    albumName = item.optString("album_name", ""),
                    albumId = "",
                    picUrl = null,
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析酷狗搜索结果失败", e)
        }
        return results
    }

    /**
     * QQ 音乐搜索
     */
    private fun searchQQ(keyword: String, page: Int, limit: Int): List<MusicInfoOnline> {
        // QQ 音乐搜索需要签名，暂时使用简化版 API
        val url = "https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp" +
            "?format=json&n=$limit&w=${URLEncoder.encode(keyword, "UTF-8")}" +
            "&p=$page&aggr=1&lossless=0&cr=1"

        val response = client.newCall(Request.Builder()
            .url(url)
            .header("Referer", "https://y.qq.com")
            .header("User-Agent", "Mozilla/5.0")
            .build()).execute()
        
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        return parseQQResult(body)
    }

    private fun parseQQResult(body: String): List<MusicInfoOnline> {
        val results = mutableListOf<MusicInfoOnline>()
        try {
            val json = JSONObject(body)
            val data = json.optJSONObject("data")?.optJSONObject("song") ?: return emptyList()
            val list = data.optJSONArray("list") ?: return emptyList()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val songmid = item.optString("songmid", "")
                val albummid = item.optString("albummid", "")
                
                results.add(MusicInfoOnline(
                    id = songmid,
                    name = item.optString("songname", ""),
                    singer = parseSingers(item.optJSONArray("singer")),
                    source = Sources.TX,
                    interval = formatDuration(item.optInt("interval", 0)),
                    songmid = songmid,  // 腾讯 songmid
                    strMediaMid = item.optString("strMediaMid", songmid),  // 腾讯专用
                    songId = item.optString("songid", ""),  // 腾讯 songId
                    albumMid = albummid,
                    albumName = item.optString("albumname", ""),
                    albumId = albummid,
                    picUrl = if (albummid.isNotEmpty()) 
                        "https://y.gtimg.cn/music/photo_new/T002R300x300M000${albummid}.jpg" 
                    else null,
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析QQ搜索结果失败", e)
        }
        return results
    }

    /**
     * 网易云音乐搜索
     */
    private fun searchNetease(keyword: String, page: Int, limit: Int): List<MusicInfoOnline> {
        // 使用简化版搜索 API
        val url = "https://music.163.com/api/search/get/web?s=${URLEncoder.encode(keyword, "UTF-8")}" +
            "&type=1&offset=${(page - 1) * limit}&limit=$limit"

        val response = client.newCall(Request.Builder()
            .url(url)
            .header("Referer", "https://music.163.com")
            .header("User-Agent", "Mozilla/5.0")
            .header("Cookie", "appver=1.5.0.75771")
            .build()).execute()
        
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        return parseNeteaseResult(body)
    }

    private fun parseNeteaseResult(body: String): List<MusicInfoOnline> {
        val results = mutableListOf<MusicInfoOnline>()
        try {
            val json = JSONObject(body)
            val result = json.optJSONObject("result") ?: return emptyList()
            val list = result.optJSONArray("songs") ?: return emptyList()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val artists = item.optJSONArray("artists")
                val album = item.optJSONObject("album")
                val songId = item.optString("id", "")
                
                results.add(MusicInfoOnline(
                    id = songId,
                    name = item.optString("name", ""),
                    singer = parseSingers(artists),
                    source = Sources.WY,
                    interval = formatDuration(item.optInt("duration", 0) / 1000),
                    songmid = songId,  // 网易云 songmid 就是歌曲 ID
                    songId = songId,
                    albumName = album?.optString("name", "") ?: "",
                    albumId = album?.optString("id", "") ?: "",
                    picUrl = album?.optString("picUrl", ""),
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析网易云搜索结果失败", e)
        }
        return results
    }

    /**
     * 咪咕音乐搜索
     */
    private fun searchMigu(keyword: String, page: Int, limit: Int): List<MusicInfoOnline> {
        val url = "https://m.music.migu.cn/migu/remoting/scr_search_tag?keyword=" +
            "${URLEncoder.encode(keyword, "UTF-8")}&type=2&pgc=$page&rows=$limit"

        val response = client.newCall(Request.Builder()
            .url(url)
            .header("Referer", "https://m.music.migu.cn")
            .build()).execute()
        
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        return parseMiguResult(body)
    }

    private fun parseMiguResult(body: String): List<MusicInfoOnline> {
        val results = mutableListOf<MusicInfoOnline>()
        try {
            val json = JSONObject(body)
            val list = json.optJSONArray("songs") ?: return emptyList()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val songId = item.optString("id", "")
                
                results.add(MusicInfoOnline(
                    id = songId,
                    name = item.optString("name", ""),
                    singer = item.optString("singer", ""),
                    source = Sources.MG,
                    interval = formatDuration(item.optInt("length", 0)),
                    songmid = item.optString("songId", songId),  // 咪咕 songmid
                    albumMid = item.optString("albumId", ""),  // 咪咕 albumMid
                    songId = songId,
                    albumName = item.optString("album", ""),
                    albumId = item.optString("albumId", ""),
                    picUrl = item.optString("cover", ""),
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析咪咕搜索结果失败", e)
        }
        return results
    }

    // ==================== 辅助方法 ====================

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val min = seconds / 60
        val sec = seconds % 60
        return "%02d:%02d".format(min, sec)
    }

    private fun parseSingers(artists: org.json.JSONArray?): String {
        if (artists == null) return ""
        val singers = mutableListOf<String>()
        for (i in 0 until artists.length()) {
            val artist = artists.getJSONObject(i)
            singers.add(artist.optString("name", ""))
        }
        return singers.joinToString("、")
    }
}