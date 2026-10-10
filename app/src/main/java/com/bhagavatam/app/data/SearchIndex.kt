package com.bhagavatam.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.Collections

/**
 * Result of a search query executed against search.db and Episodes.kt.
 */
data class SearchResult(
    val verseIds: List<Int>,
    val chapters: List<Pair<Int, Int>>,
    val storyChapters: List<Pair<Int, Int>> = emptyList(),
    val correctedKey: String? = null,
    val suggestedWord: String? = null,
    val searchTimeMs: Long = 0
)

/**
 * High-performance offline search index over Bhagavat Mahapuran text.
 * Replicates the reference search engine in content/_search/search_engine.py.
 *
 * Query execution pipeline:
 *  1. Scripture references (e.g. 1.2, 10.14.8) resolve directly to the chapter/verse.
 *  2. Word tokens expand to:
 *     - Loose phonetic key (cross-script sound match)
 *     - Name aliases (Narasimha / Nrisimha / नृसिंह)
 *     - Related words by exact form (alligator / crocodile / मगर / ग्राह)
 *     - Nearest keys 1 edit away if empty and key length >= 4 (typo correction)
 *  3. Terms are ANDed together. Scope="chapter" ANDs at the chapter level.
 *  4. Story chapters matched from Episodes.kt with the same loose key.
 */
object SearchIndex {
    private const val ASSET = "search.db"
    const val MIN_KEY = 3

    private var db: SQLiteDatabase? = null
    private var contentVersion: Int = 0

    // Chapter boundaries: sorted first verse IDs and corresponding (skandha, chapter) pairs
    private val chapterStarts = ArrayList<Int>()
    private val chapterList = ArrayList<Pair<Int, Int>>()

    // Cached story keywords: (skandha, chapter, Set<looseKey>)
    private val storyIndex: List<Triple<Int, Int, Set<String>>> by lazy {
        Episodes.all.map { ep ->
            val texts = listOf(ep.en, ep.hi, ep.bn) + ep.keywords
            val keys = HashSet<String>()
            for (t in texts) {
                val words = t.split(Regex("[\\W\\d_]+"))
                for (w in words) {
                    if (w.isNotBlank()) {
                        val k = LooseKey.keyOf(w)
                        if (k.length >= MIN_KEY) keys.add(k)
                    }
                }
            }
            Triple(ep.s, ep.a, keys)
        }
    }

    val isOpen: Boolean
        get() = db != null && db?.isOpen == true

    /** Delta-varint decoder for compressed posting list blobs. */
    fun decodePosting(blob: ByteArray): List<Int> {
        val out = ArrayList<Int>()
        var acc = 0
        var valAccum = 0
        var shift = 0
        for (b in blob) {
            val ub = b.toInt() and 0xFF
            valAccum = valAccum or ((ub and 0x7F) shl shift)
            if ((ub and 0x80) != 0) {
                shift += 7
            } else {
                acc += valAccum
                out.add(acc)
                valAccum = 0
                shift = 0
            }
        }
        return out
    }

    /** Levenshtein distance <= 1 check (substitution, insertion, or deletion). */
    fun editsLe1(a: String, b: String): Boolean {
        if (a == b) return true
        val la = a.length
        val lb = b.length
        if (Math.abs(la - lb) > 1) return false
        var i = 0
        val minLen = Math.min(la, lb)
        while (i < minLen && a[i] == b[i]) {
            i++
        }
        return if (la == lb) {
            a.substring(i + 1) == b.substring(i + 1)
        } else if (la > lb) {
            a.substring(i + 1) == b.substring(i)
        } else {
            a.substring(i) == b.substring(i + 1)
        }
    }

    /** Find chapter (skandha, chapter) containing verse with ID [vid]. */
    fun chapterOf(vid: Int): Pair<Int, Int> {
        if (chapterStarts.isEmpty()) return 1 to 1
        val idx = Collections.binarySearch(chapterStarts, vid)
        val pos = if (idx >= 0) idx else -idx - 2
        val safePos = pos.coerceIn(0, chapterList.size - 1)
        return chapterList[safePos]
    }

    /** Attach open SQLiteDatabase directly (for testing or custom initialization). */
    @Synchronized
    fun attach(database: SQLiteDatabase, version: Int) {
        db?.close()
        db = database
        contentVersion = version
        loadIndexMetadata()
    }

    /** Initialize from app context assets. */
    @Synchronized
    fun open(ctx: Context, expectedContentVersion: Int? = null) {
        val dir = File(ctx.filesDir, "search").apply { mkdirs() }
        val targetFile = File(dir, "search.db")

        // Copy from assets if missing or version mismatch
        val prefs = ctx.getSharedPreferences("search_index", Context.MODE_PRIVATE)
        val lastCopiedVersion = prefs.getInt("content_version", -1)

        val targetVersion = expectedContentVersion ?: ContentDb.openVersion
        if (!targetFile.exists() || (targetVersion > 0 && lastCopiedVersion != targetVersion)) {
            val tmp = File(targetFile.path + ".tmp")
            val copied = runCatching {
                ctx.assets.open(ASSET).use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                if (targetFile.exists()) targetFile.delete()
                tmp.renameTo(targetFile)
            }.getOrDefault(false)

            if (copied) {
                prefs.edit().putInt("content_version", targetVersion).apply()
            } else if (tmp.exists()) {
                tmp.delete()
            }
        }

        if (!targetFile.exists()) return

        runCatching {
            val database = SQLiteDatabase.openDatabase(targetFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val dbVersion = getDbContentVersion(database)
            if (targetVersion > 0 && dbVersion != targetVersion) {
                // Version mismatch: refuse to use stale index
                database.close()
                return
            }
            attach(database, dbVersion)
        }
    }

    private fun getDbContentVersion(database: SQLiteDatabase): Int {
        return runCatching {
            database.rawQuery("SELECT value FROM meta WHERE key = 'content_version'", null).use { c ->
                if (c.moveToFirst()) c.getString(0).toIntOrNull() ?: 0 else 0
            }
        }.getOrDefault(0)
    }

    private fun loadIndexMetadata() {
        chapterStarts.clear()
        chapterList.clear()
        val d = db ?: return

        runCatching {
            d.rawQuery("SELECT first_id, skandha, chapter FROM chapter_start ORDER BY idx", null).use { c ->
                while (c.moveToNext()) {
                    chapterStarts.add(c.getInt(0))
                    chapterList.add(c.getInt(1) to c.getInt(2))
                }
            }
        }
    }

    private fun fetchLoose(key: String): List<Int> {
        val d = db ?: return emptyList()
        return d.rawQuery("SELECT post FROM loose WHERE key = ?", arrayOf(key)).use { c ->
            if (c.moveToFirst()) decodePosting(c.getBlob(0)) else emptyList()
        }
    }

    private fun fetchExact(form: String): List<Int> {
        val d = db ?: return emptyList()
        return d.rawQuery("SELECT post FROM exact WHERE form = ?", arrayOf(form)).use { c ->
            if (c.moveToFirst()) decodePosting(c.getBlob(0)) else emptyList()
        }
    }

    private fun fetchGroupMembers(term: String, kind: String): List<Pair<String, String>> {
        val d = db ?: return emptyList()
        val out = ArrayList<Pair<String, String>>()
        val groupIds = ArrayList<Int>()
        d.rawQuery("SELECT g FROM grp_by_term WHERE term = ? AND kind = ?", arrayOf(term, kind)).use { c ->
            while (c.moveToNext()) groupIds.add(c.getInt(0))
        }
        for (g in groupIds) {
            d.rawQuery("SELECT kind, term FROM grp WHERE g = ?", arrayOf(g.toString())).use { c ->
                while (c.moveToNext()) {
                    out.add(c.getString(0) to c.getString(1))
                }
            }
        }
        return out
    }

    /**
     * Typo suggestion: find nearest loose keys one edit away from [key], ordered by longer first then df.
     */
    fun suggest(key: String, limit: Int = 3): List<Triple<String, Int, String?>> {
        val d = db ?: return emptyList()
        if (key.length < 3) return emptyList()
        val prefix = key.substring(0, 1) + "%"

        val candidates = ArrayList<Pair<String, Int>>()
        d.rawQuery("SELECT key, df FROM loose WHERE key LIKE ?", arrayOf(prefix)).use { c ->
            while (c.moveToNext()) {
                val k = c.getString(0)
                val df = c.getInt(1)
                if (k != key && Math.abs(k.length - key.length) <= 1 && editsLe1(k, key)) {
                    candidates.add(k to df)
                }
            }
        }

        // A dropped letter is the commonest slip, so longer candidates come first; then higher df
        candidates.sortWith(Comparator { a, b ->
            val priorityA = if (a.first.length > key.length) 0 else if (a.first.length == key.length) 1 else 2
            val priorityB = if (b.first.length > key.length) 0 else if (b.first.length == key.length) 1 else 2
            if (priorityA != priorityB) priorityA.compareTo(priorityB)
            else b.second.compareTo(a.second)
        })

        val result = ArrayList<Triple<String, Int, String?>>()
        for ((k, df) in candidates.take(limit)) {
            var forms: String? = null
            d.rawQuery("SELECT forms FROM loose WHERE key = ?", arrayOf(k)).use { c ->
                if (c.moveToFirst()) forms = c.getString(0)
            }
            result.add(Triple(k, df, forms))
        }
        return result
    }

    /**
     * Look up verse IDs for a single word. Returns Pair(verseIds, correctedKeyOrNull).
     */
    fun lookup(word: String): Pair<Set<Int>, String?> {
        val key = LooseKey.keyOf(word)
        val ex = Normalise.exactOf(word)
        val keys = HashSet<String>().apply { add(key) }
        val forms = HashSet<String>().apply { add(ex) }

        // Expand alias and related groups
        for ((kind, term) in fetchGroupMembers(key, "k") + fetchGroupMembers(ex, "f")) {
            if (kind == "k") keys.add(term) else forms.add(term)
        }

        val ids = HashSet<Int>()
        for (k in keys) {
            if (k.length >= MIN_KEY) {
                ids.addAll(fetchLoose(k))
            }
        }
        for (f in forms) {
            ids.addAll(fetchExact(f))
        }

        // Typo fallback if no matches found
        if (ids.isEmpty() && key.length >= 4) {
            val suggestions = suggest(key, limit = 1)
            if (suggestions.isNotEmpty()) {
                val candidateKey = suggestions[0].first
                val correctedIds = fetchLoose(candidateKey).toSet()
                if (correctedIds.isNotEmpty()) {
                    return correctedIds to candidateKey
                }
            }
        }

        return ids to null
    }

    /**
     * Match stories from Episodes.kt whose title/keywords contain all query terms' loose keys.
     */
    fun storyChapters(query: String): List<Pair<Int, Int>> {
        val words = SearchQuery.parse(raw = query).terms
        val keys = words.map { LooseKey.keyOf(it) }.filter { it.length >= MIN_KEY }.toSet()
        if (keys.isEmpty()) return emptyList()

        return storyIndex
            .filter { (_, _, epKeys) -> keys.all { k -> epKeys.contains(k) } }
            .map { (s, a, _) -> s to a }
    }

    /**
     * Execute a search query, intersecting words and ranking results.
     */
    fun search(rawQuery: String, scope: String = "verse"): SearchResult {
        val t0 = System.currentTimeMillis()
        val parsed = SearchQuery.parse(raw = rawQuery, scope = scope)
        if (parsed.isEmpty) {
            return SearchResult(emptyList(), emptyList(), emptyList())
        }

        // Scriptural reference jump
        if (parsed.reference != null) {
            val ref = parsed.reference
            val matchingChapters = listOf(ref.skandha to ref.chapter)
            return SearchResult(
                verseIds = emptyList(),
                chapters = matchingChapters,
                storyChapters = emptyList(),
                searchTimeMs = System.currentTimeMillis() - t0
            )
        }

        val words = parsed.terms
        if (words.isEmpty()) {
            return SearchResult(emptyList(), emptyList(), emptyList())
        }

        var correctedKey: String? = null
        var suggestedWord: String? = null
        val verseIdSets = ArrayList<Set<Int>>()

        for (w in words) {
            val (vIds, corrKey) = lookup(w)
            if (corrKey != null && correctedKey == null) {
                correctedKey = corrKey
                // Find display word from candidate forms
                db?.rawQuery("SELECT forms FROM loose WHERE key = ?", arrayOf(corrKey))?.use { c ->
                    if (c.moveToFirst()) suggestedWord = c.getString(0)?.split(",")?.firstOrNull()
                }
            }
            if (vIds.isEmpty()) {
                // AND semantics: if any word has 0 matches, intersection is empty
                return SearchResult(
                    verseIds = emptyList(),
                    chapters = emptyList(),
                    storyChapters = storyChapters(rawQuery),
                    correctedKey = correctedKey,
                    suggestedWord = suggestedWord,
                    searchTimeMs = System.currentTimeMillis() - t0
                )
            }
            verseIdSets.add(vIds)
        }

        // Intersect all words
        var intersectedVerseIds = verseIdSets[0]
        for (i in 1 until verseIdSets.size) {
            intersectedVerseIds = intersectedVerseIds.intersect(verseIdSets[i])
        }

        val sortedVerseIds = intersectedVerseIds.sorted()
        val matchingChapters = HashSet<Pair<Int, Int>>()
        for (vid in sortedVerseIds) {
            matchingChapters.add(chapterOf(vid))
        }

        val stories = storyChapters(rawQuery)
        matchingChapters.addAll(stories)

        return SearchResult(
            verseIds = sortedVerseIds,
            chapters = matchingChapters.toList().sortedWith(compareBy({ it.first }, { it.second })),
            storyChapters = stories,
            correctedKey = correctedKey,
            suggestedWord = suggestedWord,
            searchTimeMs = System.currentTimeMillis() - t0
        )
    }
}
