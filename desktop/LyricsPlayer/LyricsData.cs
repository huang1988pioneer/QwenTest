using System.Collections.Generic;

namespace LyricsPlayer;

public record LyricLine(double Time, string Text);

public record Song(
    string Title,
    string Subtitle,
    string AudioFile,
    IReadOnlyList<LyricLine> Lines,
    IReadOnlyDictionary<string, string> AvatarNames,
    IReadOnlyList<string> AllWords,
    IReadOnlyDictionary<string, string> CharTriggers);

public static class LyricsData
{
    public static readonly Song[] Songs =
    {
        new(
            "最瞎結婚理由",
            "鋒兄 × 小塗 × 牙妹 × 魚妹｜今彩五三九牽紅線",
            "song1.mp3",
            new[]
            {
                new LyricLine(20.60, "鋒兄啊你說真的還假的"),
                new LyricLine(26.38, "小塗聽了都快笑翻了"),
                new LyricLine(32.30, "鋒兄說要結婚"),
                new LyricLine(35.10, "理由只有一個"),
                new LyricLine(38.23, "今彩五三九開獎那天"),
                new LyricLine(41.39, "頭獎號碼是牙妹給的"),
                new LyricLine(43.81, "看著獎金直直落"),
                new LyricLine(47.13, "心也跟著被收編"),
                new LyricLine(49.86, "他說這是命中注定"),
                new LyricLine(52.76, "不娶怎麼對得起這一連串的玄"),
                new LyricLine(58.76, "史上最瞎結婚理由"),
                new LyricLine(62.07, "今彩五三九牽紅線牽這麼兇"),
                new LyricLine(65.08, "一個牙妹一個魚妹"),
                new LyricLine(67.88, "號碼一簽兩人都中頭獎圈"),
                new LyricLine(70.65, "你說愛情是運氣還是數學題"),
                new LyricLine(76.47, "笑到流淚也只能說一句"),
                new LyricLine(79.43, "最瞎最瞎卻又有點甜蜜"),
                new LyricLine(88.10, "換到小塗這邊"),
                new LyricLine(90.78, "故事居然同一套"),
                new LyricLine(93.61, "今彩五三九播報畫面"),
                new LyricLine(96.95, "他整個人直接跳"),
                new LyricLine(99.47, "魚妹隨手寫的牌"),
                new LyricLine(102.36, "竟然全中好幾排"),
                new LyricLine(105.76, "他說財神爺都點名了"),
                new LyricLine(108.70, "不跟她走進禮堂實在太不應該"),
                new LyricLine(114.62, "鋒兄牽著牙妹"),
                new LyricLine(117.43, "小塗牽著魚妹"),
                new LyricLine(120.33, "喝喜酒的人一桌一桌"),
                new LyricLine(123.42, "還在笑這兩段緣"),
                new LyricLine(126.09, "最瞎結婚理由"),
                new LyricLine(129.10, "結果都開成頭獎"),
                new LyricLine(131.72, "如果幸福也能這樣瞎忙"),
                new LyricLine(135.01, "那我明天也去買一張"),
            },
            new Dictionary<string, string>
            {
                ["鋒兄"] = "鋒兄",
                ["小塗"] = "小塗",
                ["牙妹"] = "牙妹",
                ["魚妹"] = "魚妹",
            },
            new[] { "結婚", "今彩五三九", "命中注定" },
            new Dictionary<string, string>()),
        new(
            "鋒塗力百年夢",
            "鋒兄 × 塗哥｜資訊 × 文化 × 水電科技",
            "song2.mp3",
            new[]
            {
                new LyricLine(10.46, "鋒兄一個念頭"),
                new LyricLine(11.51, "塗哥一起出手"),
                new LyricLine(12.61, "一張桌 一枝筆"),
                new LyricLine(13.99, "畫出未來藍圖走"),
                new LyricLine(15.36, "資訊亮起燈火"),
                new LyricLine(16.73, "文化寫進生活"),
                new LyricLine(17.86, "水電科技串起來"),
                new LyricLine(18.93, "一條路 三個夢同軌"),
                new LyricLine(20.22, "鋒塗力鋒塗力 一起拚下去"),
                new LyricLine(23.67, "百年企業心擔起"),
                new LyricLine(26.25, "今天種的夢 明天變成你福氣"),
                new LyricLine(29.73, "鋒塗力 鋒塗力 就在你身邊一起前進"),
                new LyricLine(39.37, "螢幕後的溫度 字句裡的態度"),
                new LyricLine(41.51, "每一條線 每一支螺絲"),
                new LyricLine(42.81, "都藏著職人的堅固"),
                new LyricLine(44.27, "資訊讓世界接軌"),
                new LyricLine(45.62, "文化替城市發揮"),
                new LyricLine(46.79, "水電科技打底座"),
                new LyricLine(47.78, "安全舒適住進你家裡"),
                new LyricLine(49.30, "鋒塗力鋒塗力 一起拚下去"),
                new LyricLine(53.01, "百年企業心擔起"),
                new LyricLine(55.60, "今天種的夢 明天變成你福氣"),
                new LyricLine(58.64, "鋒塗力 鋒塗力 就在你身邊一起前進"),
                new LyricLine(67.70, "一個鋒 一個塗"),
                new LyricLine(69.19, "名字寫進版圖"),
                new LyricLine(70.20, "從這里 到全國"),
                new LyricLine(71.75, "一步一步走得穩固"),
                new LyricLine(78.01, "鋒塗力鋒塗力 一起拚下去"),
                new LyricLine(81.82, "百年企業心擔起"),
                new LyricLine(84.69, "今天種的夢 明天變成你福氣"),
                new LyricLine(87.71, "鋒塗力 鋒塗力 大家作伙向著未來前進"),
            },
            new Dictionary<string, string>
            {
                ["鋒兄"] = "鋒兄",
                ["小塗"] = "塗哥",
            },
            new[] { "鋒塗力" },
            new Dictionary<string, string>
            {
                ["鋒兄"] = "鋒",
                ["小塗"] = "塗",
            }),
    };
}
