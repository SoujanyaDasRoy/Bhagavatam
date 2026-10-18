package com.bhagavatam.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhagavatam.app.data.AnnKind
import com.bhagavatam.app.data.Annotation
import com.bhagavatam.app.data.NotebookEntry
import com.bhagavatam.app.data.SampleData
import com.bhagavatam.app.data.localDigits
import com.bhagavatam.app.data.notebookGroups
import com.bhagavatam.app.data.notebookText
import com.bhagavatam.app.data.tr
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.Radius

/** The marks of the notebook that match [query] (in the quote or the note) and the colour chip [colour] (0 = all, 1 to 4 = that colour). */
fun filterMarks(marks: List<Annotation>, query: String, colour: Int): List<Annotation> {
    val q = query.trim()
    return marks.filter { a ->
        (colour == 0 || (a.colour == MarkColours.getOrNull(colour - 1))) &&
            (q.isEmpty() || a.quote.contains(q, ignoreCase = true) || a.note.contains(q, ignoreCase = true))
    }
}

/** Plain text of [marks] as shared from the notebook, with the reader's colour names and chapter titles. */
fun notebookExport(state: AppState, marks: List<Annotation>): String = notebookText(
    marks.map { NotebookEntry(it.ref, it.kind, it.colour, it.quote, it.note) },
    List(4) { state.markName(it) },
    MarkColours,
) { s, a -> SampleData.adhyayaTitle(s, a, state.titleLangFor(s, a), state.strings) }

/** Search box, colour chips and the share button above the notebook's marks. */
@Composable
fun NotebookControls(state: AppState, query: String, onQuery: (String) -> Unit, colour: Int, onColour: (Int) -> Unit, marks: List<Annotation>) {
    val ui = state.uiLang
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(Radius.group).background(Brand.Card).border(1.dp, Brand.Separator, Radius.group).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true, modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 16.sp, color = Brand.Ink), cursorBrush = SolidColor(Brand.Kesari),
                decorationBox = { inner ->
                    if (query.isEmpty()) androidx.compose.material3.Text(tr(ui, "Search your notes", "अपने नोट खोजें", "আপনার নোট খুঁজুন"), fontSize = 16.sp, color = Brand.Secondary)
                    inner()
                },
            )
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            @Composable
            fun Chip(selected: Boolean, label: String, dot: Color?, onClick: () -> Unit) {
                Row(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape).background(if (selected) Brand.Card else Color.Transparent)
                        .then(if (selected) Modifier.border(1.dp, Brand.Gold, CircleShape) else Modifier)
                        .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (dot != null) Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
                    androidx.compose.material3.Text(label, fontSize = 15.sp, color = Brand.Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Chip(colour == 0, tr(ui, "All", "सभी", "সব"), null) { onColour(0) }
            MarkColours.forEachIndexed { i, col -> Chip(colour == i + 1, state.markName(i), Color(col)) { onColour(i + 1) } }
        }
        if (marks.isNotEmpty()) Box(
            Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, notebookExport(state, marks))
                runCatching { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }.padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            androidx.compose.material3.Text(tr(ui, "Share these notes as text", "ये नोट टेक्स्ट में साझा करें", "এই নোট টেক্সটে শেয়ার করুন"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
        }
    }
}

/** The marks grouped by chapter, each group under its chapter's name; tapping a mark opens its note editor, "Open" goes to the verse. */
fun LazyListScope.notebookMarks(state: AppState, marks: List<Annotation>, onOpen: (Int, Int) -> Unit) {
    val ui = state.uiLang
    val groups = notebookGroups(marks.map { NotebookEntry(it.ref, it.kind, it.colour, it.quote, it.note) })
    groups.forEach { (key, entries) ->
        val p = key.split(".").map { it.toIntOrNull() ?: 0 }
        item(key = "g$key") {
            androidx.compose.material3.Text(
                localDigits(if (p[0] == 0) "Mahatmya ${p[1]}" else "${p[0]}.${p[1]}", ui) + "  " + SampleData.adhyayaTitle(p[0], p[1], state.titleLangFor(p[0], p[1]), state.strings),
                Modifier.padding(horizontal = 20.dp, vertical = 6.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.Gold,
            )
        }
        val anns = marks.filter { a -> a.ref.split(".").take(2).joinToString(".") == key }.sortedBy { it.ref.substringAfterLast('.').toIntOrNull() ?: 0 }
        items(anns, key = { "m${it.id}" }) { a ->
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(Radius.group).background(Brand.Card)
                    .clickable { state.annDraft = com.bhagavatam.app.data.AnnDraft(a, a.ref, a.layer, a.start, a.end, a.quote) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (a.colour == 0L) Brand.Gold else Color(a.colour)))
                    androidx.compose.material3.Text(localDigits(a.ref, ui), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                    Box(Modifier.weight(1f))
                    val s = a.ref.split(".").map { it.toIntOrNull() ?: 0 }
                    Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { state.scrollToVerse = a.ref; onOpen(s[0], s[1]) }.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(tr(ui, "Open chapter", "अध्याय खोलें", "অধ্যায় খুলুন"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                    }
                }
                androidx.compose.material3.Text(a.quote, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 22.sp, color = Brand.Ink, maxLines = 3)
                if (a.note.isNotBlank()) androidx.compose.material3.Text(a.note, fontSize = 15.sp, lineHeight = 20.sp, color = Brand.Secondary)
            }
        }
    }
}
