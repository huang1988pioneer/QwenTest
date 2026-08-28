package com.qwentest.lyricsplayer

data class LyricLine(val timeMs: Int, val text: String)

object LyricsData {
    val lines: List<LyricLine> = listOf(
        LyricLine(20600, "鋒兄啊你說真的還假的"),
        LyricLine(26380, "小塗聽了都快笑翻了"),
        LyricLine(32300, "鋒兄說要結婚"),
        LyricLine(35100, "理由只有一個"),
        LyricLine(38230, "今彩五三九開獎那天"),
        LyricLine(41390, "頭獎號碼是牙妹給的"),
        LyricLine(43810, "看著獎金直直落"),
        LyricLine(47130, "心也跟著被收編"),
        LyricLine(49860, "他說這是命中注定"),
        LyricLine(52760, "不娶怎麼對得起這一連串的玄"),
        LyricLine(58760, "史上最瞎結婚理由"),
        LyricLine(62070, "今彩五三九牽紅線牽這麼兇"),
        LyricLine(65080, "一個牙妹一個魚妹"),
        LyricLine(67880, "號碼一簽兩人都中頭獎圈"),
        LyricLine(70650, "你說愛情是運氣還是數學題"),
        LyricLine(76470, "笑到流淚也只能說一句"),
        LyricLine(79430, "最瞎最瞎卻又有點甜蜜"),
        LyricLine(88100, "換到小塗這邊"),
        LyricLine(90780, "故事居然同一套"),
        LyricLine(93610, "今彩五三九播報畫面"),
        LyricLine(96950, "他整個人直接跳"),
        LyricLine(99470, "魚妹隨手寫的牌"),
        LyricLine(102360, "竟然全中好幾排"),
        LyricLine(105760, "他說財神爺都點名了"),
        LyricLine(108700, "不跟她走進禮堂實在太不應該"),
        LyricLine(114620, "鋒兄牽著牙妹"),
        LyricLine(117430, "小塗牽著魚妹"),
        LyricLine(120330, "喝喜酒的人一桌一桌"),
        LyricLine(123420, "還在笑這兩段緣"),
        LyricLine(126090, "最瞎結婚理由"),
        LyricLine(129100, "結果都開成頭獎"),
        LyricLine(131720, "如果幸福也能這樣瞎忙"),
        LyricLine(135010, "那我明天也去買一張"),
    )

    val characterNames: Map<String, String> = mapOf(
        "avatar_feng" to "鋒兄",
        "avatar_tu" to "小塗",
        "avatar_ya" to "牙妹",
        "avatar_yu" to "魚妹",
    )
}
