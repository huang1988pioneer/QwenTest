using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Interactivity;
using Avalonia.Media;
using Avalonia.Media.TextFormatting;
using Avalonia.Threading;
using NAudio.Wave;

namespace LyricsPlayer;

public partial class MainWindow : Window
{
    private static readonly IBrush NowBrush = new SolidColorBrush(Color.Parse("#FFFFFF"));
    private static readonly IBrush PastBrush = new SolidColorBrush(Color.Parse("#5D4F80"));
    private static readonly IBrush FutureBrush = new SolidColorBrush(Color.Parse("#8F7FB0"));
    private static readonly IBrush ActiveBorder = new SolidColorBrush(Color.Parse("#FFD75E"));
    private static readonly IBrush IdleBorder = new SolidColorBrush(Color.Parse("#4A3768"));
    private static readonly IBrush TabIdleBackground = new SolidColorBrush(Color.Parse("#2A1C48"));
    private static readonly IBrush TabIdleForeground = new SolidColorBrush(Color.Parse("#C9B6E4"));
    private static readonly IBrush TabActiveForeground = new SolidColorBrush(Color.Parse("#241436"));

    private AudioFileReader? _reader;
    private WaveOutEvent? _output;
    private readonly DispatcherTimer _timer = new() { Interval = TimeSpan.FromMilliseconds(100) };
    private readonly List<TextBlock> _lyricBlocks = new();
    private readonly Dictionary<string, (StackPanel Panel, Border Border)> _avatars = new();
    private Song _song = LyricsData.Songs[0];
    private int _songIndex = -1;
    private int _currentLine = -1;
    private bool _dragging;

    public MainWindow()
    {
        InitializeComponent();

        _avatars["鋒兄"] = (AvatarFeng, BorderFeng);
        _avatars["小塗"] = (AvatarTu, BorderTu);
        _avatars["牙妹"] = (AvatarYa, BorderYa);
        _avatars["魚妹"] = (AvatarYu, BorderYu);
        _avatars["喵布布"] = (AvatarMiao, BorderMiao);

        TabSong1.Click += (_, _) => LoadSong(0);
        TabSong2.Click += (_, _) => LoadSong(1);
        TabSong3.Click += (_, _) => LoadSong(2);
        TabSong4.Click += (_, _) => LoadSong(3);
        TabSong5.Click += (_, _) => LoadSong(4);
        TabSong6.Click += (_, _) => LoadSong(5);
        TabSong7.Click += (_, _) => LoadSong(6);
        TabSong8.Click += (_, _) => LoadSong(7);
        TabSong9.Click += (_, _) => LoadSong(8);

        PlayBtn.Click += (_, _) => TogglePlay();

        SeekSlider.AddHandler(InputElement.PointerPressedEvent, (_, _) => _dragging = true, RoutingStrategies.Tunnel, true);
        SeekSlider.AddHandler(InputElement.PointerReleasedEvent, (_, _) => _dragging = false, RoutingStrategies.Tunnel, true);
        this.AddHandler(InputElement.PointerReleasedEvent, (_, _) => _dragging = false, RoutingStrategies.Bubble, true);
        SeekSlider.ValueChanged += (_, e) => { if (_dragging) SeekTo(e.NewValue, false); };

        _timer.Tick += (_, _) => Update();
        _timer.Start();

        Closing += (_, _) =>
        {
            _timer.Stop();
            _output?.Dispose();
            _reader?.Dispose();
        };

        PropertyChanged += (_, e) =>
        {
            if (e.Property != BoundsProperty) return;
            for (var i = 0; i < _lyricBlocks.Count; i++)
                _lyricBlocks[i].Text = BreakAtSpaces(_song.Lines[i].Text, i == _currentLine);
        };

        LoadSong(0);
    }

    private void LoadSong(int index)
    {
        if (index == _songIndex) return;
        _songIndex = index;
        _song = LyricsData.Songs[index];

        if (_output is not null)
        {
            _output.PlaybackStopped -= OnPlaybackStopped;
            _output.Stop();
            _output.Dispose();
            _output = null;
        }
        _reader?.Dispose();
        _reader = null;
        PlayBtn.Content = "▶";

        TitleText.Text = _song.Title;
        SubtitleText.Text = _song.Subtitle;
        Title = $"{_song.Title}｜動態歌詞";
        StyleTab(TabSong1, index == 0);
        StyleTab(TabSong2, index == 1);
        StyleTab(TabSong3, index == 2);
        StyleTab(TabSong4, index == 3);
        StyleTab(TabSong5, index == 4);
        StyleTab(TabSong6, index == 5);
        StyleTab(TabSong7, index == 6);
        StyleTab(TabSong8, index == 7);
        StyleTab(TabSong9, index == 8);

        LyricsPanel.Children.Clear();
        _lyricBlocks.Clear();
        foreach (var line in _song.Lines)
        {
            var tb = new TextBlock
            {
                Text = BreakAtSpaces(line.Text, false),
                TextAlignment = TextAlignment.Center,
                TextWrapping = TextWrapping.Wrap,
                Foreground = FutureBrush,
                FontSize = 18,
                Margin = new Thickness(0, 9),
                Cursor = new Cursor(StandardCursorType.Hand),
            };
            tb.PointerPressed += (_, _) => { SeekTo(line.Time, true); Update(); };
            LyricsPanel.Children.Add(tb);
            _lyricBlocks.Add(tb);
        }
        _currentLine = -1;
        LyricsScroll.Offset = new Vector(0, 0);

        foreach (var kv in _avatars)
        {
            kv.Value.Panel.Opacity = 0.5;
            kv.Value.Border.BorderBrush = IdleBorder;
        }

        try
        {
            var path = Path.Combine(AppContext.BaseDirectory, "Assets", _song.AudioFile);
            _reader = new AudioFileReader(path);
            _output = new WaveOutEvent();
            _output.Init(_reader);
            _output.PlaybackStopped += OnPlaybackStopped;
            SeekSlider.Maximum = _reader.TotalTime.TotalSeconds;
            SeekSlider.Value = 0;
            PlayBtn.IsEnabled = true;
        }
        catch (Exception)
        {
            TimeLabel.Text = $"找不到 Assets/{_song.AudioFile}";
            SeekSlider.Maximum = 1;
            SeekSlider.Value = 0;
            PlayBtn.IsEnabled = false;
        }
    }

    private static void StyleTab(Button tab, bool active)
    {
        tab.Background = active ? ActiveBorder : TabIdleBackground;
        tab.Foreground = active ? TabActiveForeground : TabIdleForeground;
        tab.BorderBrush = active ? ActiveBorder : IdleBorder;
        tab.FontWeight = active ? FontWeight.Bold : FontWeight.Regular;
    }

    private void TogglePlay()
    {
        if (_output is null) return;
        if (_output.PlaybackState == PlaybackState.Playing)
        {
            _output.Pause();
            PlayBtn.Content = "▶";
        }
        else
        {
            _output.Play();
            PlayBtn.Content = "⏸";
        }
    }

    private void SeekTo(double seconds, bool play)
    {
        if (_reader is null || _output is null) return;
        _reader.CurrentTime = TimeSpan.FromSeconds(seconds);
        if (play && _output.PlaybackState != PlaybackState.Playing)
        {
            _output.Play();
            PlayBtn.Content = "⏸";
        }
    }

    private void OnPlaybackStopped(object? sender, StoppedEventArgs e)
    {
        if (_reader is null || _reader.CurrentTime.TotalSeconds < _reader.TotalTime.TotalSeconds - 0.3) return;
        Dispatcher.UIThread.Post(() =>
        {
            _reader.CurrentTime = TimeSpan.Zero;
            PlayBtn.Content = "▶";
            Update();
        });
    }

    private void Update()
    {
        if (_reader is null) return;
        var t = _reader.CurrentTime.TotalSeconds;

        var c = -1;
        for (var i = 0; i < _song.Lines.Count; i++)
        {
            if (t >= _song.Lines[i].Time) c = i;
            else break;
        }

        if (c != _currentLine)
        {
            _currentLine = c;
            for (var i = 0; i < _lyricBlocks.Count; i++)
            {
                var tb = _lyricBlocks[i];
                if (i == c)
                {
                    tb.Foreground = NowBrush;
                    tb.FontSize = 24;
                    tb.FontWeight = FontWeight.Bold;
                }
                else
                {
                    tb.Foreground = i < c ? PastBrush : FutureBrush;
                    tb.FontSize = 18;
                    tb.FontWeight = FontWeight.Regular;
                }
                tb.Text = BreakAtSpaces(_song.Lines[i].Text, i == c);
            }

            var line = c >= 0 ? _song.Lines[c].Text : "";
            var allActive = false;
            foreach (var w in _song.AllWords)
            {
                if (line.Contains(w)) { allActive = true; break; }
            }
            foreach (var kv in _avatars)
            {
                var excluded = _song.AvatarExclude is not null
                    && _song.AvatarExclude.TryGetValue(kv.Key, out var excl)
                    && excl.Any(p => line.Contains(p));
                var active = allActive
                    || (!excluded && (
                        (_song.AvatarNames.TryGetValue(kv.Key, out var name) && line.Contains(name))
                        || (_song.CharTriggers.TryGetValue(kv.Key, out var ch) && line.Contains(ch))
                        || (_song.AvatarKeywords is not null
                            && _song.AvatarKeywords.TryGetValue(kv.Key, out var kws)
                            && kws.Any(w => line.Contains(w)))));
                kv.Value.Panel.Opacity = active ? 1 : 0.5;
                kv.Value.Border.BorderBrush = active ? ActiveBorder : IdleBorder;
            }

            if (c >= 0) CenterOn(_lyricBlocks[c]);
        }

        if (!_dragging) SeekSlider.Value = t;
        TimeLabel.Text = $"{Fmt(t)} / {Fmt(_reader.TotalTime.TotalSeconds)}";
    }

    private void CenterOn(TextBlock block)
    {
        var rel = block.TranslatePoint(new Point(0, 0), LyricsScroll);
        if (rel is null) return;
        var contentY = rel.Value.Y + LyricsScroll.Offset.Y;
        var target = contentY - LyricsScroll.Bounds.Height / 2 + block.Bounds.Height / 2;
        LyricsScroll.Offset = new Vector(0, Math.Max(0, target));
    }

    private static string Fmt(double s)
    {
        var sec = Math.Max(0, (int)Math.Floor(s));
        return $"{sec / 60}:{sec % 60:D2}";
    }

    private double LyricsMaxWidth => (LyricsScroll.Bounds.Width > 0 ? LyricsScroll.Bounds.Width : Width) - 40;

    private double Measure(string s, double size, FontWeight weight)
    {
        var tl = new TextLayout(s, new Typeface(FontFamily, FontStyle.Normal, weight), size, NowBrush);
        return tl.Width;
    }

    // 預先測量：放得下保持原文；放不下在空白分隔段內插 U+2060，讓換行只發生在空白處
    private string BreakAtSpaces(string raw, bool active)
    {
        var size = active ? 24d : 18d;
        var weight = active ? FontWeight.Bold : FontWeight.Regular;
        var max = LyricsMaxWidth;
        if (Measure(raw, size, weight) <= max) return raw;
        var sb = new StringBuilder(raw.Length * 2);
        foreach (var part in raw.Split(' '))
        {
            if (sb.Length > 0) sb.Append(' ');
            if (Measure(part, size, weight) > max)
            {
                sb.Append(part);
                continue;
            }
            for (var j = 0; j < part.Length; j++)
            {
                if (j > 0) sb.Append((char)0x2060);
                sb.Append(part[j]);
            }
        }
        return sb.ToString();
    }
}
