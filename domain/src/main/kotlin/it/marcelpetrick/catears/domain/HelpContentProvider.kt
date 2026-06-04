// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

const val REPO_URL = "https://github.com/marcelpetrick/androidCatEars"
const val FEEDBACK_URL = "https://github.com/marcelpetrick/androidCatEars/issues"

fun helpContent(language: HelpLanguage): HelpContent = when (language) {
    HelpLanguage.ENGLISH -> englishContent()
    HelpLanguage.GERMAN -> germanContent()
    HelpLanguage.CROATIAN -> croatianContent()
    HelpLanguage.MANDARIN -> mandarinContent()
}

private fun englishContent() = HelpContent(
    appPitch = "A real-time AR cat-ear camera for Android. Procedurally drawn ears follow your face.",
    authorLine = "by Marcel Petrick",
    repoLabel = "Source code on GitHub",
    feedbackLabel = "Open a GitHub Issue — feedback welcome!",
    feedbackInvitation = "This is a fun open-source exercise — input is very welcome! " +
        "Ideas, bug reports, or just a hello all count.",
    featuresTitle = "Features",
    features = listOf(
        "10 procedural ear styles — Classic, Sharp Feline, Rounded, Lynx Tufted, Dense Fluffy, " +
            "Canine Floppy, Canine Perky, Rabbit, Fox and Bear",
        "Smooth real-time animation following your head movement",
        "Expression-reactive: ears perk up when smiling or wide-eyed; one droops on a wink",
        "Tracks up to 4 faces simultaneously",
        "Party Mode gives every face its own stable ear style and tint — re-roll the group for a fresh vibe",
        "6 colour tints: Natural, Rose, Gold, Mint, Sky, Lavender",
        "Photo capture with ears baked in — share instantly",
        "Five-second video clips with animated ears baked in — tiny, shareable chaos",
    ),
    howToUseTitle = "How to use",
    howToUseSteps = listOf(
        "Grant camera permission when prompted",
        "Face the lens — cat ears appear above your head automatically",
        "Tap the style button (bottom-right) to cycle through ear styles",
        "Tap the colour button to change the ear tint",
        "Use Party Mode for group selfies; tap re-roll to reshuffle everyone's look",
        "Tap camera-switch to flip between front and rear lens",
        "Tap the shutter to capture a photo; tap Share to share it",
        "Tap the video button for a 5-second clip; share it when the save banner appears",
        "Smile or open your eyes wide to animate the ears!",
    ),
    closeLabel = "Close",
)

private fun germanContent() = HelpContent(
    appPitch = "Eine Echtzeit-AR-Katzenohren-Kamera für Android. Prozedural gezeichnete Ohren folgen deinem Gesicht.",
    authorLine = "von Marcel Petrick",
    repoLabel = "Quellcode auf GitHub",
    feedbackLabel = "GitHub Issue öffnen — Feedback willkommen!",
    feedbackInvitation = "Spaßiges Open-Source-Projekt — Rückmeldungen sehr willkommen! " +
        "Ideen, Fehlerberichte oder einfach Hallo.",
    featuresTitle = "Funktionen",
    features = listOf(
        "10 prozedurale Ohrenstile — Klassisch, Scharf Felinartig, Gerundet, Luchs, Flauschig, " +
            "Hund Hängend, Hund Aufrecht, Hase, Fuchs und Bär",
        "Flüssige Echtzeit-Animation, die dem Kopf folgt",
        "Ausdrucksreaktiv: Ohren richten sich beim Lächeln auf; ein Ohr hängt beim Zwinkern",
        "Verfolgt bis zu 4 Gesichter gleichzeitig",
        "Party Mode gibt jedem Gesicht einen stabilen Ohrenstil und Farbton — neu würfeln für frische Gruppen-Vibes",
        "6 Farbtöne: Natürlich, Rose, Gold, Minze, Himmel, Lavendel",
        "Fotoaufnahme mit eingebauten Ohren — sofort teilbar",
        "Fünf-Sekunden-Videoclips mit animierten Ohren — klein, teilbar und herrlich albern",
    ),
    howToUseTitle = "So geht's",
    howToUseSteps = listOf(
        "Kameraberechtigung erteilen, wenn aufgefordert",
        "Ins Objektiv schauen — Ohren erscheinen automatisch über dem Kopf",
        "Stil-Schaltfläche (unten rechts) zum Wechseln der Ohrenstile tippen",
        "Farb-Schaltfläche zum Ändern des Farbtons tippen",
        "Party Mode für Gruppen-Selfies nutzen; neu würfeln mischt alle Looks neu",
        "Kamera-Wechsel-Schaltfläche zum Umschalten tippen",
        "Auslöser antippen zum Aufnehmen; Teilen-Schaltfläche zum Teilen",
        "Video-Schaltfläche antippen für einen 5-Sekunden-Clip; teilen, sobald er gespeichert ist",
        "Lächeln oder Augen weit aufmachen, um die Ohren zu animieren!",
    ),
    closeLabel = "Schließen",
)

private fun croatianContent() = HelpContent(
    appPitch = "Android kamera s AR mačjim ušima u stvarnom vremenu. Proceduralno nacrtane uši prate tvoje lice.",
    authorLine = "autor Marcel Petrick",
    repoLabel = "Izvorni kod na GitHubu",
    feedbackLabel = "Otvori GitHub Issue — povratne informacije su dobrodošle!",
    feedbackInvitation = "Ovo je zabavan open-source projekt — svaka povratna informacija dobro dođe! " +
        "Ideje, prijave grešaka ili samo pozdrav, sve se računa.",
    featuresTitle = "Značajke",
    features = listOf(
        "10 proceduralnih stilova ušiju — Klasične, Oštre mačje, Zaobljene, Risove, Gusto čupave, " +
            "Pseće spuštene, Pseće uspravne, Zečje, Lisičje i Medvjeđe",
        "Glatka animacija u stvarnom vremenu koja prati pokrete glave",
        "Reagira na izraze: uši se podignu kad se smiješ ili širom otvoriš oči; jedno se spusti na namig",
        "Prati do 4 lica istovremeno",
        "Party Mode svakom licu daje stabilan stil ušiju i nijansu — re-roll promiješa ekipu za novi vibe",
        "6 nijansi boje: Prirodna, Ružičasta, Zlatna, Menta, Nebeska, Lavanda",
        "Snimanje fotografije s već ugrađenim ušima — odmah spremno za dijeljenje",
        "Videoisječci od 5 sekundi s animiranim ušima — mala, djeljiva doza kaosa",
    ),
    howToUseTitle = "Kako koristiti",
    howToUseSteps = listOf(
        "Dopusti pristup kameri kad se pojavi upit",
        "Okreni se prema objektivu — mačje uši automatski se pojave iznad glave",
        "Dodirni gumb za stil (dolje desno) za promjenu stilova ušiju",
        "Dodirni gumb za boju za promjenu nijanse ušiju",
        "Uključi Party Mode za grupne selfije; re-roll svima promiješa izgled",
        "Dodirni gumb za promjenu kamere za prebacivanje između prednje i stražnje leće",
        "Dodirni okidač za fotografiju; dodirni Share za dijeljenje",
        "Dodirni gumb za video za isječak od 5 sekundi; podijeli ga kad se pojavi poruka o spremanju",
        "Nasmiješi se ili širom otvori oči da uši ožive!",
    ),
    closeLabel = "Zatvori",
)

private fun mandarinContent() = HelpContent(
    appPitch = "一款Android实时增强现实猫耳相机。程序绘制的耳朵会跟随您的面部移动。",
    authorLine = "作者：Marcel Petrick",
    repoLabel = "GitHub源代码",
    feedbackLabel = "提交GitHub Issue——欢迎反馈！",
    feedbackInvitation = "这是一个有趣的开源项目——非常欢迎您的意见！欢迎提交想法、错误报告，或者只是打个招呼。",
    featuresTitle = "功能特性",
    features = listOf(
        "10种程序生成的耳朵样式——经典、尖锐猫型、圆润、猞猁、浓密蓬松、垂耷犬型、竖立犬型、兔子、狐狸和熊",
        "流畅的实时动画，跟随头部运动",
        "表情互动：微笑或睁大眼睛时耳朵竖起；眨眼时单耳下垂",
        "同时追踪最多4张人脸",
        "派对模式会给每张脸固定分配耳朵样式和颜色——点重新随机，整组立刻换个氛围",
        "6种颜色：自然、玫瑰、金色、薄荷、天蓝、薰衣草",
        "拍摄带耳朵的照片——可即时分享",
        "5秒视频短片会把动画耳朵一起录进去——短小、好分享、很有戏",
    ),
    howToUseTitle = "使用方法",
    howToUseSteps = listOf(
        "出现提示时授予摄像头权限",
        "对准镜头——猫耳朵自动出现在头顶",
        "点击右下角的样式按钮循环切换耳朵样式",
        "点击颜色按钮更改耳朵色调",
        "为多人自拍打开派对模式；点击重新随机，给每个人换个造型",
        "点击切换相机按钮，在前后摄像头间切换",
        "点击快门拍照；用分享按钮分享",
        "点击视频按钮录制5秒短片；保存提示出现后即可分享",
        "微笑或睁大眼睛，让耳朵动起来！",
    ),
    closeLabel = "关闭",
)
