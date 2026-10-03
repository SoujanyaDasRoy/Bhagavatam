@Composable
fun HomeScreen(state: AppState, onResume: () -> Unit, onSearch: () -> Unit, onOpenSkandha: (Int) -> Unit = {}) {
    val s = state.strings
    val ui = state.uiLang
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(size = 46, fontSize = 20)
            Column {
                Text("Bhagavatam", fontFamily = Literata, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Brand.Ink)
                Text("श्रीमद्भागवत महापुराण", fontFamily = TiroSanskrit, fontSize = 14.sp, color = Brand.Gold)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Continue reading: the one big thing on this screen
            val skHue = skColor(state.lastSkandha)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF160E38), Color(0xFF3A1A5E), Color(0xFF6B2A55))))
                    .clickable(onClickLabel = s.resumeReading, onClick = onResume),
            ) {
                Mandala(Color(0x26F5C97A), Modifier.matchParentSize())
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dot(lerp(skHue, Color.White, 0.4f), 10)
                        Text(
                            localDigits("${s.skandha} ${state.lastSkandha} · ${s.adhyaya} ${state.lastAdhyaya}", ui),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE9DFF5),
                        )
                    }
                    Text(
                        SampleData.adhyayaTitle(state.lastSkandha, state.lastAdhyaya, state.titleLang, s),
                        fontFamily = readingFont(state.titleLang), fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, color = Color.White,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
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
                        ) { Icon(Icons.Rounded.PlayArrow, s.listen, tint = Color(0xFF2A1140), modifier = Modifier.size(30.dp)) }
                    }
                }
            }
            // Go to a verse
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Card).clickable(onClick = onSearch).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Brand.Secondary)
                Text(s.goToHint, color = Brand.Secondary, fontSize = 15.sp)
            }
            // Skandhas at a glance
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.tabGranth, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold, modifier = Modifier.padding(horizontal = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items((1..12).toList()) { n ->
                        val hue = skColor(n)
                        Box(
                            Modifier.size(52.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(deep(hue, 0.5f), deep(hue, 0.25f))))
                                .clickable(onClickLabel = "${s.skandha} $n", role = Role.Button) { onOpenSkandha(n) },
                            contentAlignment = Alignment.Center,
                        ) { Text(localDigits("$n", ui), fontFamily = Literata, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White) }
                    }
                }
            }
            // Shloka of the day
            val v = SampleData.shlokaOfTheDay
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ॐ", fontFamily = TiroSanskrit, fontSize = 26.sp, color = Brand.Gold)
                Row(Modifier.fillMaxWidth()) {
                    Text(s.shlokaOfDay, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.Gold)
                    Text(localDigits(v.ref, ui), fontSize = 13.sp, color = Brand.Secondary)
                }
                if (state.showSanskrit) ShlokaText(v, state.script, Brand.Sindoor, 19f, center = true)
                val tr = if (state.readLang == com.bhagavatam.app.data.Lang.SA) state.alongsideLayers().first() else state.readLang
                if (v.hasText(tr)) Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = 15.sp, lineHeight = 23.sp, color = Brand.Ink)
                Row(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape).background(Brand.KesariTint).clickable { state.playVerses(listOf(v)) }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                    Text(s.listen, color = Brand.Kesari, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
        VSpace(160)
    }
}

private fun deep(c: Color, t: Float) = lerp(c, Color.Black, t)

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
                Text("॥ श्रीमद्भागवत महापुराण ॥", fontFamily = TiroSanskrit, fontSize = 20.sp, color = Brand.Sindoor, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
        }
        item(span = { GridItemSpan(2) }) {
            val m = SampleData.skandha(0)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF5A3C06), Color(0xFF9A6A12))))
                    .clickable { onOpen(0) },
            ) {
                Mandala(Color(0x30FFE9B0), Modifier.matchParentSize(), petals = 12)
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                        Text("मा", fontFamily = TiroSanskrit, fontSize = 22.sp, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(s.mahatmya, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(localDigits("${m.title(state.titleLang)} · ${m.adhyayaCount} ${s.adhyayas}", ui), fontSize = 13.sp, color = Color(0xFFFFF1D0))
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                }
            }
        }
        items(SampleData.skandhas.filter { it.num > 0 }) { sk ->
            val hue = skColor(sk.num)
            val readCount = state.finished.count { it.startsWith("${sk.num}.") }
            Box(
                Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(deep(hue, 0.58f), deep(hue, 0.28f))))
                    .clickable { onOpen(sk.num) },
            ) {
                Mandala(Color(0x30FFFFFF), Modifier.matchParentSize())
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(localDigits(sk.num.toString(), ui), fontFamily = Literata, fontSize = 40.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(sk.nameSa, fontFamily = TiroSanskrit, fontSize = 16.sp, lineHeight = 22.sp, color = Color(0xFFFFF4DC), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(localDigits("$readCount / ${sk.adhyayaCount} ${s.adhyayas}", ui), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFF4DC))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0x40FFFFFF))) {
                            if (readCount > 0) Box(Modifier.fillMaxWidth(readCount.toFloat() / sk.adhyayaCount).height(4.dp).background(Color(0xFFFFE2A3)))
                        }
                    }
                }
            }
        }
    }
}

