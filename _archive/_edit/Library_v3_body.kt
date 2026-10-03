
@Composable
private fun skColor(s: Int) = if (s == 0) Brand.Gold else Brand.Skandha[s - 1]

private fun greeting(l: Lang): Pair<String, String> {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> tr(l, "Good night", "शुभ रात्रि", "শুভ রাত্রি") to "ॐ"
        h < 12 -> tr(l, "Good morning", "सुप्रभात", "শুভ সকাল") to "सुप्रभातम्"
        h < 17 -> tr(l, "Good afternoon", "शुभ दोपहर", "শুভ অপরাহ্ন") to "नमो नमः"
        h < 21 -> tr(l, "Good evening", "शुभ संध्या", "শুভ সন্ধ্যা") to "शुभ सन्ध्या"
        else -> tr(l, "Good night", "शुभ रात्रि", "শুভ রাত্রি") to "ॐ"
    }
}

@Composable
private fun SectionRow(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
        if (action != null) Text(action, Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onAction).heightIn(min = 48.dp).padding(horizontal = 8.dp).wrapContentHeight(Alignment.CenterVertically),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
    }
}

@Composable
fun SkandhaCard(n: Int, state: AppState, modifier: Modifier, onClick: () -> Unit) {
    val s = state.strings
    val sk = SampleData.skandha(n)
    val hue = skColor(n)
    val read = state.finished.count { it.startsWith("$n.") }
    Box(modifier.clip(RoundedCornerShape(20.dp)).clickable(onClickLabel = "${s.skandha} $n", role = Role.Button, onClick = onClick)) {
        ArtBackdrop(skArt(n), skGradient(hue), Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(localDigits(n.toString(), state.uiLang), fontFamily = EnglishReading, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(sk.nameSa, fontFamily = NotoDevanagari, fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFFFFF4DC), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(localDigits("$read / ${sk.adhyayaCount} ${s.adhyayas}", state.uiLang), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFF4DC))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0x40FFFFFF))) {
                    if (read > 0) Box(Modifier.fillMaxWidth(read.toFloat() / sk.adhyayaCount).height(4.dp).background(Color(0xFFFFE2A3)))
                }
            }
        }
    }
}

@Composable
private fun StoryCard(e: Episode, state: AppState, onOpen: (Int, Int) -> Unit) {
    val hue = skColor(e.s)
    Box(Modifier.width(200.dp).height(128.dp).clip(RoundedCornerShape(20.dp))
        .clickable(onClickLabel = e.title(state.uiLang), role = Role.Button) { onOpen(e.s, e.a) }) {
        ArtBackdrop(chArt(e.s, e.a), skGradient(hue), Modifier.matchParentSize(), petals = 12)
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(localDigits("${e.s}.${e.a}", state.uiLang), Modifier.clip(CircleShape).background(Color(0x33FFFFFF)).padding(horizontal = 9.dp, vertical = 3.dp),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(e.title(state.uiLang), fontFamily = readingFont(state.uiLang), fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickAction(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Brand.Card).border(1.dp, Brand.Separator, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick).heightIn(min = 76.dp).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(painterResource(icon), null, tint = Brand.Kesari, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink, maxLines = 1)
    }
}

@Composable
fun HomeScreen(
    state: AppState, onResume: () -> Unit, onSearch: () -> Unit,
    onOpenSkandha: (Int) -> Unit = {}, onOpenGranth: () -> Unit = {},
    onOpenChapter: (Int, Int) -> Unit = { _, _ -> }, onSaved: () -> Unit = {}, onGlossary: () -> Unit = {},
) {
    val s = state.strings
    val ui = state.uiLang
    val (hello, sa) = remember(ui) { greeting(ui) }
    val total = remember { SampleData.skandhas.sumOf { it.adhyayaCount } }
    val done = state.finished.size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(size = 46, fontSize = 20)
            Column {
                Text(hello, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                Text(sa, fontFamily = NotoDevanagari, fontSize = 14.sp, color = Brand.Gold)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            // Continue reading
            val skHue = skColor(state.lastSkandha)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClickLabel = s.resumeReading, onClick = onResume),
            ) {
                ArtBackdrop(chArt(state.lastSkandha, state.lastAdhyaya), listOf(Color(0xFF160E38), Color(0xFF3A1A5E), Color(0xFF6B2A55)),
                    Modifier.matchParentSize(), mandala = Color(0x26F5C97A))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dot(lerp(skHue, Color.White, 0.4f), 10)
                        Text(localDigits("${s.skandha} ${state.lastSkandha} · ${s.adhyaya} ${state.lastAdhyaya}", ui),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE9DFF5))
                    }
                    Text(SampleData.adhyayaTitle(state.lastSkandha, state.lastAdhyaya, state.titleLang, s),
                        fontFamily = readingFont(state.titleLang), fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, color = Color.White,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.resumeReading, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF5C97A))
                            Text(localDigits("${s.stoppedAt} ${state.lastVerse}", ui), fontSize = 13.sp, color = Color(0xFFD7CBE6))
                        }
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFF5C97A))
                                .clickable(onClickLabel = s.listen, role = Role.Button) {
                                    state.playVerses(SampleData.versesFor(state.lastSkandha, state.lastAdhyaya), (state.lastVerse - 1).coerceAtLeast(0))
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(painterResource(Ic.PlayArrow), s.listen, tint = Color(0xFF2A1140), modifier = Modifier.size(28.dp)) }
                    }
                }
            }
            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Ic.Search, s.tabSearch, Modifier.weight(1f), onSearch)
                QuickAction(Ic.Bookmark, tr(ui, "Saved", "सहेजे गए", "সংরক্ষিত"), Modifier.weight(1f), onSaved)
                QuickAction(Ic.Scroll, s.glossary, Modifier.weight(1f), onGlossary)
                QuickAction(Ic.Library, s.tabGranth, Modifier.weight(1f), onOpenGranth)
            }
            // Your progress
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.Card).border(1.dp, Brand.Separator, RoundedCornerShape(20.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(tr(ui, "Your reading", "आपका पठन", "আপনার পাঠ"), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                    Text(localDigits("$done / $total", ui), fontFamily = EnglishReading, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Brand.Kesari)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Brand.Fill)) {
                    if (done > 0) Box(Modifier.fillMaxWidth((done.toFloat() / total).coerceIn(0.02f, 1f)).height(8.dp).clip(CircleShape).background(Brand.Kesari))
                }
                Text(
                    if (done == 0) tr(ui, "Finish a chapter and it is counted here.", "कोई अध्याय पूरा करें, वह यहाँ गिना जाएगा।", "একটি অধ্যায় শেষ করলে এখানে গোনা হবে।")
                    else tr(ui, "$done chapters finished. ${total - done} to go.", "$done अध्याय पूरे हुए। ${total - done} शेष हैं।", "$done টি অধ্যায় শেষ। ${total - done} টি বাকি।").let { localDigits(it, ui) },
                    fontSize = 13.sp, color = Brand.Secondary,
                )
            }
            // Well-loved stories
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionRow(tr(ui, "Well-loved stories", "प्रिय कथाएँ", "প্রিয় কথা"))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(Episodes.all, key = { "${it.s}.${it.a}" }) { StoryCard(it, state, onOpenChapter) }
                }
            }
            // Skandhas
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionRow(s.tabGranth, s.all, onOpenGranth)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items((1..12).toList(), key = { it }) { n -> SkandhaCard(n, state, Modifier.width(150.dp).height(124.dp)) { onOpenSkandha(n) } }
                }
            }
            // Shloka of the day
            val v = SampleData.shlokaOfTheDay
            if (state.showDaily) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).border(1.dp, Brand.Separator, RoundedCornerShape(22.dp))) {
                Mandala(Brand.Gold.copy(alpha = 0.14f), Modifier.matchParentSize())
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(s.shlokaOfDay, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                        Text(localDigits(v.ref, ui), fontSize = 14.sp, color = Brand.Secondary)
                    }
                    if (state.showSanskrit) ShlokaText(v, state.script, Brand.Sindoor, 19f, center = true)
                    val trl = if (state.readLang == Lang.SA) state.alongsideLayers().first() else state.readLang
                    if (v.hasText(trl)) Text(v.translation(trl), fontFamily = readingFont(trl), fontSize = 16.sp, lineHeight = 25.sp, color = Brand.Ink)
                    Row(
                        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(Brand.KesariTint).clickable { state.playVerses(listOf(v)) }.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(painterResource(Ic.PlayArrow), null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                        Text(s.listen, color = Brand.Kesari, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        VSpace(160)
    }
}

@Composable
fun GranthScreen(state: AppState, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomRoom),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LargeTitle(s.tabGranth, s.granthSub, horizontalPadding = 4.dp)
                Text("॥ श्रीमद्भागवत महापुराण ॥", fontFamily = NotoDevanagari, fontSize = 20.sp, color = Brand.Sindoor, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
        }
        item(span = { GridItemSpan(2) }) {
            val m = SampleData.skandha(0)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable { onOpen(0) }) {
                ArtBackdrop(skArt(0), listOf(Color(0xFF5A3C06), Color(0xFF9A6A12)), Modifier.matchParentSize(), petals = 12, mandala = Color(0x30FFE9B0))
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                        Text("मा", fontFamily = NotoDevanagari, fontSize = 20.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(s.mahatmya, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(localDigits("${m.title(state.titleLang)} · ${m.adhyayaCount} ${s.adhyayas}", ui), fontSize = 13.sp, color = Color(0xFFFFF1D0))
                    }
                    Icon(painterResource(Ic.KeyboardArrowRight), null, tint = Color.White)
                }
            }
        }
        items(SampleData.skandhas.filter { it.num > 0 }, key = { it.num }) { sk ->
            SkandhaCard(sk.num, state, Modifier.fillMaxWidth().height(156.dp)) { onOpen(sk.num) }
        }
    }
}

@Composable
fun AdhyayasScreen(state: AppState, skandha: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val s = state.strings
    val ui = state.uiLang
    val sk = SampleData.skandha(skandha)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomRoom)) {
        item { NavBar(s.tabGranth, onBack) }
        item {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))) {
                ArtBackdrop(skArt(skandha), if (skandha == 0) listOf(Color(0xFF5A3C06), Color(0xFF9A6A12)) else skGradient(skColor(skandha)), Modifier.matchParentSize(), mandala = Color(0x33FFFFFF))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(localDigits("${sk.adhyayaCount} ${s.adhyayas}", ui), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFF4DC))
                    Text(if (skandha == 0) s.mahatmya else localDigits("${s.skandha} $skandha", ui), fontFamily = EnglishReading, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(sk.nameSa, fontFamily = NotoDevanagari, fontSize = 19.sp, color = Color.White)
                    Text(sk.title(state.titleLang), fontSize = 16.sp, color = Color(0xFFFFF4DC))
                }
            }
        }
