using System;
using System.Collections.Generic;
using System.IO;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Interactivity;
using Avalonia.Media;
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

    private AudioFileReader? _reader;
    private WaveOutEvent? _output;
    private readonly DispatcherTimer _timer = new() { Interval = TimeSpan.FromMilliseconds(100) };
    private readonly List<TextBlock> _lyricBlocks = new();
    private readonly Dictionary<string, (StackPanel Panel, Border Border)> _avatars = new();
    private int _currentLine = -1;
    private bool _dragging;

    public MainWindow()
    {
        InitializeComponent();

        foreach (var line in LyricsData.Lines)
        {
            var tb = new TextBlock
            {
                Text = line.Text,
                TextAlignment = TextAlignment.Center,
                Foreground = FutureBrush,
                FontSize = 18,
                Margin = new Thickness(0, 9),
                Cursor = new Cursor(StandardCursorType.Hand),
            };
            tb.PointerPressed += (_, _) => { SeekTo(line.Time, true); Update(); };
            LyricsPanel.Children.Add(tb);
            _lyricBlocks.Add(tb);
        }

        _avatars["鋒兄"] = (AvatarFeng, BorderFeng);
        _avatars["小塗"] = (AvatarTu, BorderTu);
        _avatars["牙妹"] = (AvatarYa, BorderYa);
        _avatars["魚妹"] = (AvatarYu, BorderYu);

        try
        {
            var path = Path.Combine(AppContext.BaseDirectory, "Assets", "song.mp3");
            _reader = new AudioFileReader(path);
            _output = new WaveOutEvent();
            _output.Init(_reader);
            _output.PlaybackStopped += OnPlaybackStopped;
            SeekSlider.Maximum = _reader.TotalTime.TotalSeconds;
        }
        catch (Exception)
        {
            TimeLabel.Text = "找不到 Assets/song.mp3";
            PlayBtn.IsEnabled = false;
        }

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
        for (var i = 0; i < LyricsData.Lines.Length; i++)
        {
            if (t >= LyricsData.Lines[i].Time) c = i;
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
            }

            var line = c >= 0 ? LyricsData.Lines[c].Text : "";
            foreach (var kv in _avatars)
            {
                var active = line.Contains(kv.Key);
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
}
