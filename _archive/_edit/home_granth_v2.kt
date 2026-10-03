@Composable
fun HomeScreen(state: AppState, onResume: () -> Unit, onSearch: () -> Unit, onOpenSkandha: (Int) -> Unit = {}) {
    val s = state.strings
    val ui = state.uiLang
    val c = LocalAppColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTile(size = 46, fontSize = 20)
            Column {
                Text("Bhagavatam", fontFamily = Cardo, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                Text("श्रीमद्भागवत महापुराण", fontFamily = NotoDevanagari, fontSize = 14.sp, color = Brand.Gold)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Continue reading
            val skHue = skColor(state.lastSkandha)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brand.Card)
                    .border(1.dp, Brand.Separator, RoundedCornerShape(24.dp))
                    .clickable(onClickLabel = s.resumeReading, onClick = onResume).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(skHue, 10)
                    Text(localDigits("${s.skandha} ${state.lastSkandha} · ${s.adhyaya} ${state.lastAdhyaya}", ui), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Brand.Secondary)
                }
                Text(
                    SampleData.adhyayaTitle(state.lastSkandha, state.lastAdhyaya, state.titleLang, s),
                    fontFamily = readingFont(state.titleLang), fontSize = 24.sp, lineHeight = 34.sp, color = Brand.Ink,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(s.resumeReading, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Brand.Kesari)
                        Text(localDigits("${s.stoppedAt} ${state.lastVerse}", ui), fontSize = 14.sp, color = Brand.Secondary)
                    }
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Brand.Kesari)
                            .clickable(onClickLabel = s.listen, role = Role.Button) {
                                state.playVerses(SampleData.versesFor(state.lastSkandha, state.lastAdhyaya), (state.lastVerse - 1).coerceAtLeast(0))
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.PlayArrow, s.listen, tint = Brand.OnKesari, modifier = Modifier.size(30.dp)) }
                }
            }
            // Go to a verse
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Fill).clickable(onClick = onSearch).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Search, null, tint = Brand.Secondary)
                Text(s.goToHint, color = Brand.Secondary, fontSize = 15.sp)
            }
            // Skandhas at a glance
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.tabGranth, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Brand.Gold, modifier = Modifier.padding(horizontal = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items((1..12).toList()) { n ->
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(skFill(skColor(n)))
                                .clickable(onClickLabel = "${s.skandha} $n", role = Role.Button) { onOpenSkandha(n) },
                            contentAlignment = Alignment.Center,
                        ) { Text(localDigits("$n", ui), fontFamily = Cardo, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
            // Shloka of the day
            val v = SampleData.shlokaOfTheDay
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).border(1.dp, Brand.Separator, RoundedCornerShape(22.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(s.shlokaOfDay, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Brand.Gold)
                    Text(localDigits(v.ref, ui), fontSize = 14.sp, color = Brand.Secondary)
                }
                if (state.showSanskrit) ShlokaText(v, state.script, Brand.Sindoor, 19f, center = true)
                val tr = if (state.readLang == com.bhagavatam.app.data.Lang.SA) state.alongsideLayers().first() else state.readLang
                if (v.hasText(tr)) Text(v.translation(tr), fontFamily = readingFont(tr), fontSize = 16.sp, lineHeight = 25.sp, color = Brand.Ink)
                Row(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape).background(Brand.KesariTint).clickable { state.playVerses(listOf(v)) }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Brand.Kesari, modifier = Modifier.size(20.dp))
                    Text(s.listen, color = Brand.Kesari, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        VSpace(160)
    }
}

/** Flat Skandha colour: dark enough for white text in light mode, deeper still in dark mode. */
@Composable
private fun skFill(hue: Color): Color = lerp(hue, Color.Black, if (LocalAppColors.current.isDark) 0.5f else 0.25f)

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
        item(span = { GridItemSpan(2) }) { LargeTitle(s.tabGranth, s.granthSub, horizontalPadding = 4.dp) }
        item(span = { GridItemSpan(2) }) {
            val m = SampleData.skandha(0)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.Card).border(1.dp, Brand.Separator, RoundedCornerShape(20.dp))
                    .clickable { onOpen(0) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(skFill(Brand.Gold)), contentAlignment = Alignment.Center) {
                    Text("मा", fontFamily = NotoDevanagari, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(s.mahatmya, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brand.Ink)
                    Text(localDigits("${m.title(state.titleLang)} · ${m.adhyayaCount} ${s.adhyayas}", ui), fontSize = 14.sp, color = Brand.Secondary)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.Chevron)
            }
        }
        items(SampleData.skandhas.filter { it.num > 0 }) { sk ->
            val readCount = state.finished.count { it.startsWith("${sk.num}.") }
            Column(
                Modifier.fillMaxWidth().height(148.dp).clip(RoundedCornerShape(20.dp)).background(skFill(skColor(sk.num)))
                    .clickable { onOpen(sk.num) }.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(localDigits(sk.num.toString(), ui), fontFamily = Cardo, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(sk.nameSa, fontFamily = NotoDevanagari, fontSize = 15.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = 0.92f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(localDigits("$readCount / ${sk.adhyayaCount} ${s.adhyayas}", ui), fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f))) {
                        if (readCount > 0) Box(Modifier.fillMaxWidth(readCount.toFloat() / sk.adhyayaCount).height(4.dp).background(Color.White))
                    }
                }
            }
        }
    }
}

