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
    appPitch = "A real-time AR cat-ear camera for Android. Photorealistic ears lock onto your face and come alive.",
    authorLine = "by Marcel Petrick",
    repoLabel = "Source code on GitHub",
    feedbackLabel = "Open a GitHub Issue — feedback welcome!",
    feedbackInvitation = "This is a fun open-source exercise — input is very welcome! " +
        "Ideas, bug reports, or just a hello all count.",
    featuresTitle = "Features",
    features = listOf(
        "6 photorealistic styles — glossy Classic, wild Sharp Feline, soft Rounded, Lynx, Fluffy and Fox",
        "Smooth real-time motion: head tilts, turns and tiny perspective shifts make the ears feel attached",
        "Expression-reactive magic: smile, go wide-eyed, wink — the ears perk, pop and squash with you",
        "Tracks up to 4 faces at once, so group selfies turn into a tiny personality quiz",
        "Party Mode gives every face its own stable ear style — re-roll the group for a fresh vibe",
        "Photo capture bakes the ears into the shot — ready to share the second it saves",
        "Five-second video clips record the animated ears directly into the MP4 — short, silly, instantly sendable",
    ),
    howToUseTitle = "How to use",
    howToUseSteps = listOf(
        "Grant camera permission when prompted",
        "Face the lens — the ears snap into place above your head automatically",
        "Tap the style button (bottom-right) to find your ear personality",
        "Use Party Mode for group selfies; tap re-roll until everyone gets a favorite look",
        "Tap camera-switch to flip between front and rear lens",
        "Tap the shutter to capture a photo; tap Share to share it",
        "Tap the video button for a 5-second clip; share it when the save banner appears",
        "Smile or open your eyes wide to animate the ears!",
    ),
    closeLabel = "Close",
)

private fun germanContent() = HelpContent(
    appPitch = "Eine Echtzeit-AR-Katzenohren-Kamera für Android. " +
        "Fotorealistische Ohren heften sich an dein Gesicht und werden lebendig.",
    authorLine = "von Marcel Petrick",
    repoLabel = "Quellcode auf GitHub",
    feedbackLabel = "GitHub Issue öffnen — Feedback willkommen!",
    feedbackInvitation = "Spaßiges Open-Source-Projekt — Rückmeldungen sehr willkommen! " +
        "Ideen, Fehlerberichte oder einfach Hallo.",
    featuresTitle = "Funktionen",
    features = listOf(
        "6 fotorealistische Stile — glänzend Klassisch, wild Scharf, weich Gerundet, Luchs, Flauschig und Fuchs",
        "Flüssige Echtzeit-Bewegung: Kopfneigung, Drehung und Perspektive lassen die Ohren richtig befestigt wirken",
        "Ausdruckszauber: lächeln, große Augen, zwinkern — die Ohren richten sich auf, springen und stauchen mit",
        "Verfolgt bis zu 4 Gesichter gleichzeitig, damit Gruppen-Selfies zum kleinen Persönlichkeitstest werden",
        "Party Mode gibt jedem Gesicht einen stabilen Ohrenstil — neu würfeln für frische Gruppen-Vibes",
        "Fotoaufnahme backt die Ohren direkt ins Bild — sofort bereit zum Teilen",
        "Fünf-Sekunden-Videoclips nehmen die animierten Ohren direkt ins MP4 auf — kurz, albern, sofort verschickbar",
    ),
    howToUseTitle = "So geht's",
    howToUseSteps = listOf(
        "Kameraberechtigung erteilen, wenn aufgefordert",
        "Ins Objektiv schauen — die Ohren springen automatisch an die richtige Stelle",
        "Stil-Schaltfläche (unten rechts) antippen und deinen Ohren-Charakter finden",
        "Party Mode für Gruppen-Selfies nutzen; neu würfeln, bis alle ihren Lieblingslook haben",
        "Kamera-Wechsel-Schaltfläche zum Umschalten tippen",
        "Auslöser antippen zum Aufnehmen; Teilen-Schaltfläche zum Teilen",
        "Video-Schaltfläche antippen für einen 5-Sekunden-Clip; teilen, sobald er gespeichert ist",
        "Lächeln oder Augen weit aufmachen, um die Ohren zu animieren!",
    ),
    closeLabel = "Schließen",
)

private fun croatianContent() = HelpContent(
    appPitch = "Android kamera s AR mačjim ušima u stvarnom vremenu. Fotorealistične uši sjednu na tvoje lice i ožive.",
    authorLine = "autor Marcel Petrick",
    repoLabel = "Izvorni kod na GitHubu",
    feedbackLabel = "Otvori GitHub Issue — povratne informacije su dobrodošle!",
    feedbackInvitation = "Ovo je zabavan open-source projekt — svaka povratna informacija dobro dođe! " +
        "Ideje, prijave grešaka ili samo pozdrav, sve se računa.",
    featuresTitle = "Značajke",
    features = listOf(
        "6 fotorealističnih stilova — sjajne Klasične, divlje Oštre, mekane Zaobljene, Risove, Čupave i Lisičje",
        "Glatko kretanje u stvarnom vremenu: nagib glave, okret i perspektiva čine da uši djeluju pričvršćeno",
        "Čarolija izraza: osmijeh, širom otvorene oči, namig — uši se podignu, poskoče i spljošte s tobom",
        "Prati do 4 lica istovremeno, pa grupni selfie postaje mali test osobnosti",
        "Party Mode svakom licu daje stabilan stil ušiju — re-roll promiješa ekipu za novi vibe",
        "Fotografija sprema uši izravno u kadar — spremna za dijeljenje čim se spremi",
        "Videoisječci od 5 sekundi snimaju animirane uši ravno u MP4 — kratko, smiješno, odmah za slanje",
    ),
    howToUseTitle = "Kako koristiti",
    howToUseSteps = listOf(
        "Dopusti pristup kameri kad se pojavi upit",
        "Okreni se prema objektivu — uši automatski sjednu iznad glave",
        "Dodirni gumb za stil (dolje desno) i pronađi svoj karakter ušiju",
        "Uključi Party Mode za grupne selfije; re-roll dok svatko ne dobije omiljeni izgled",
        "Dodirni gumb za promjenu kamere za prebacivanje između prednje i stražnje leće",
        "Dodirni okidač za fotografiju; dodirni Share za dijeljenje",
        "Dodirni gumb za video za isječak od 5 sekundi; podijeli ga kad se pojavi poruka o spremanju",
        "Nasmiješi se ili širom otvori oči da uši ožive!",
    ),
    closeLabel = "Zatvori",
)

private fun mandarinContent() = HelpContent(
    appPitch = "一款Android实时增强现实猫耳相机。写实猫耳会贴住您的脸，并跟着表情一起活起来。",
    authorLine = "作者：Marcel Petrick",
    repoLabel = "GitHub源代码",
    feedbackLabel = "提交GitHub Issue——欢迎反馈！",
    feedbackInvitation = "这是一个有趣的开源项目——非常欢迎您的意见！欢迎提交想法、错误报告，或者只是打个招呼。",
    featuresTitle = "功能特性",
    features = listOf(
        "6种写实耳朵样式——亮泽经典、野性尖锐猫型、柔和圆润、猞猁、浓密蓬松和狐狸",
        "流畅的实时运动：点头、转头和透视变化都会让耳朵像真的贴在头上一样",
        "表情魔法：微笑、睁大眼睛、眨眼——耳朵会竖起、弹动、压扁，跟着您一起演",
        "同时追踪最多4张人脸，让多人自拍变成一场小小性格测试",
        "派对模式会给每张脸固定分配耳朵样式——点重新随机，整组立刻换个氛围",
        "拍照时把耳朵直接烘进画面——保存后马上就能分享",
        "5秒视频会把动画耳朵直接录进MP4——短、好笑、马上能发",
    ),
    howToUseTitle = "使用方法",
    howToUseSteps = listOf(
        "出现提示时授予摄像头权限",
        "对准镜头——耳朵会自动贴到头顶的正确位置",
        "点击右下角的样式按钮，找到属于您的耳朵性格",
        "为多人自拍打开派对模式；反复点击重新随机，直到每个人都有喜欢的造型",
        "点击切换相机按钮，在前后摄像头间切换",
        "点击快门拍照；用分享按钮分享",
        "点击视频按钮录制5秒短片；保存提示出现后即可分享",
        "微笑或睁大眼睛，让耳朵动起来！",
    ),
    closeLabel = "关闭",
)
