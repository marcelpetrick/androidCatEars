// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.domain

const val REPO_URL = "https://github.com/marcelpetrick/androidCatEars"
const val FEEDBACK_URL = "https://github.com/marcelpetrick/androidCatEars/issues"

fun helpContent(language: HelpLanguage): HelpContent = when (language) {
    HelpLanguage.ENGLISH -> englishContent()
    HelpLanguage.GERMAN -> germanContent()
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
        "6 colour tints: Natural, Rose, Gold, Mint, Sky, Lavender",
        "Photo capture with ears baked in — share instantly",
    ),
    howToUseTitle = "How to use",
    howToUseSteps = listOf(
        "Grant camera permission when prompted",
        "Face the lens — cat ears appear above your head automatically",
        "Tap the style button (bottom-right) to cycle through ear styles",
        "Tap the colour button to change the ear tint",
        "Tap camera-switch to flip between front and rear lens",
        "Tap the shutter to capture a photo; tap Share to share it",
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
        "6 Farbtöne: Natürlich, Rose, Gold, Minze, Himmel, Lavendel",
        "Fotoaufnahme mit eingebauten Ohren — sofort teilbar",
    ),
    howToUseTitle = "So geht's",
    howToUseSteps = listOf(
        "Kameraberechtigung erteilen, wenn aufgefordert",
        "Ins Objektiv schauen — Ohren erscheinen automatisch über dem Kopf",
        "Stil-Schaltfläche (unten rechts) zum Wechseln der Ohrenstile tippen",
        "Farb-Schaltfläche zum Ändern des Farbtons tippen",
        "Kamera-Wechsel-Schaltfläche zum Umschalten tippen",
        "Auslöser antippen zum Aufnehmen; Teilen-Schaltfläche zum Teilen",
        "Lächeln oder Augen weit aufmachen, um die Ohren zu animieren!",
    ),
    closeLabel = "Schließen",
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
        "6种颜色：自然、玫瑰、金色、薄荷、天蓝、薰衣草",
        "拍摄带耳朵的照片——可即时分享",
    ),
    howToUseTitle = "使用方法",
    howToUseSteps = listOf(
        "出现提示时授予摄像头权限",
        "对准镜头——猫耳朵自动出现在头顶",
        "点击右下角的样式按钮循环切换耳朵样式",
        "点击颜色按钮更改耳朵色调",
        "点击切换相机按钮，在前后摄像头间切换",
        "点击快门拍照；用分享按钮分享",
        "微笑或睁大眼睛，让耳朵动起来！",
    ),
    closeLabel = "关闭",
)
