# Generates the cute calico-cat app icon ("MiaoBubu") for web / Android / desktop.
# Drawn in a 1024-unit space, supersampled 4x via GDI+ transform, downsampled with
# high-quality bicubic. Re-run this script to regenerate every icon asset.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$imgDir      = Join-Path $root 'img'
$deskAssets  = Join-Path $root 'desktop\LyricsPlayer\Assets'
$resDir      = Join-Path $root 'android\LyricsPlayer\app\src\main\res'
foreach ($d in @($imgDir, $deskAssets)) { if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d | Out-Null } }

# ---- palette (matches the app's purple gradient + gold accent) ----
$col = {
  param($hex, $a = 255)
  [System.Drawing.Color]::FromArgb($a,
    [convert]::ToInt32($hex.Substring(0,2),16),
    [convert]::ToInt32($hex.Substring(2,2),16),
    [convert]::ToInt32($hex.Substring(4,2),16))
}
$cBgTop   = & $col '2A1747'
$cBgBot   = & $col '171130'
$cGold    = & $col 'FFD75E'
$cFur     = & $col 'FFF8EE'
$cOrange  = & $col 'F6A63C'
$cDark    = & $col '4A4046'
$cPinkEar = & $col 'F7AFC4'
$cPink    = & $col 'F290AE'
$cBlush   = & $col 'FFA8C5'
$cEye     = & $col '3E3340'
$cMouth   = & $col '5A4A5E'
$cWhite   = & $col 'FFFFFF'
$cLav     = & $col 'C9B6E4'

function New-RoundedRect([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
  $p = New-Object System.Drawing.Drawing2D.GraphicsPath
  $d = $r * 2
  $p.AddArc($x, $y, $d, $d, 180, 90)
  $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
  $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
  $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
  $p.CloseFigure()
  $p
}

function Draw-Star4($g, [single]$cx, [single]$cy, [single]$r, [System.Drawing.Brush]$brush) {
  $pts = @()
  for ($i = 0; $i -lt 8; $i++) {
    $a  = (-90 + 45 * $i) * [math]::PI / 180
    $rr = if ($i % 2 -eq 0) { $r } else { $r * 0.34 }
    $pts += New-Object System.Drawing.PointF(
      [single]($cx + $rr * [math]::Cos($a)), [single]($cy + $rr * [math]::Sin($a)))
  }
  $g.FillPolygon($brush, $pts)
}

function Draw-TriRounded($g, [System.Drawing.PointF[]]$pts, [System.Drawing.Brush]$brush) {
  # triangle with rounded corners: cut each corner at t of its edges, join with a cubic
  $t = 0.22
  $n = $pts.Count
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $cursor = $null
  $start = $null
  for ($i = 0; $i -lt $n; $i++) {
    $v = $pts[$i]; $a = $pts[(($i - 1 + $n) % $n)]; $b = $pts[(($i + 1) % $n)]
    $p1 = New-Object System.Drawing.PointF ([single]($v.X + ($a.X - $v.X) * $t)), ([single]($v.Y + ($a.Y - $v.Y) * $t))
    $p2 = New-Object System.Drawing.PointF ([single]($v.X + ($b.X - $v.X) * $t)), ([single]($v.Y + ($b.Y - $v.Y) * $t))
    if ($i -eq 0) { $start = $p1 } else { $path.AddLine($cursor, $p1) }
    $c1 = New-Object System.Drawing.PointF ([single]($p1.X + ($v.X - $p1.X) * 0.62)), ([single]($p1.Y + ($v.Y - $p1.Y) * 0.62))
    $c2 = New-Object System.Drawing.PointF ([single]($p2.X + ($v.X - $p2.X) * 0.62)), ([single]($p2.Y + ($v.Y - $p2.Y) * 0.62))
    $path.AddBezier($p1, $c1, $c2, $p2)
    $cursor = $p2
  }
  $path.CloseFigure()
  $g.FillPath($brush, $path)
}

# Cat drawn in 1024-space, centered around (512, 505); vertical extent ~168..842.
function Draw-Cat($g) {
  # ears (behind head): left = orange, right = dark charcoal (calico signature)
  Draw-TriRounded $g @(
    (New-Object System.Drawing.PointF 275, 425),
    (New-Object System.Drawing.PointF 335, 175),
    (New-Object System.Drawing.PointF 485, 330)) (New-Object System.Drawing.SolidBrush $cOrange)
  Draw-TriRounded $g @(
    (New-Object System.Drawing.PointF 749, 425),
    (New-Object System.Drawing.PointF 689, 175),
    (New-Object System.Drawing.PointF 539, 330)) (New-Object System.Drawing.SolidBrush $cDark)
  Draw-TriRounded $g @(
    (New-Object System.Drawing.PointF 322, 382),
    (New-Object System.Drawing.PointF 352, 242),
    (New-Object System.Drawing.PointF 452, 332)) (New-Object System.Drawing.SolidBrush $cPinkEar)
  Draw-TriRounded $g @(
    (New-Object System.Drawing.PointF 702, 382),
    (New-Object System.Drawing.PointF 672, 242),
    (New-Object System.Drawing.PointF 572, 332)) (New-Object System.Drawing.SolidBrush $cPinkEar)

  # head
  $head = New-Object System.Drawing.Drawing2D.GraphicsPath
  $head.AddEllipse(512 - 275, 590 - 252, 550, 504)
  $g.FillPath((New-Object System.Drawing.SolidBrush $cFur), $head)

  # forehead patches, clipped to the head so they hug the ears
  $g.SetClip($head)
  $prev = $g.Transform.Clone()
  $m = New-Object System.Drawing.Drawing2D.Matrix
  $m.RotateAt(-18, (New-Object System.Drawing.PointF 390, 400))
  $g.MultiplyTransform($m, [System.Drawing.Drawing2D.MatrixOrder]::Prepend)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush $cOrange), 390 - 85, 400 - 60, 170, 120)
  $g.Transform = $prev
  $m = New-Object System.Drawing.Drawing2D.Matrix
  $m.RotateAt(15, (New-Object System.Drawing.PointF 655, 405))
  $g.MultiplyTransform($m, [System.Drawing.Drawing2D.MatrixOrder]::Prepend)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush $cDark), 655 - 75, 405 - 55, 150, 110)
  $g.Transform = $prev
  $g.ResetClip()

  # eyes with double highlights
  $eyeBrush = New-Object System.Drawing.SolidBrush $cEye
  $g.FillEllipse($eyeBrush, 398 - 47, 580 - 51, 94, 102)
  $g.FillEllipse($eyeBrush, 626 - 47, 580 - 51, 94, 102)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush $cWhite), 398 - 32, 580 - 36, 30, 30)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush $cWhite), 626 - 32, 580 - 36, 30, 30)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush ($col.Invoke('FFFFFF', 220))), 398 + 8, 596, 16, 16)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush ($col.Invoke('FFFFFF', 220))), 626 + 8, 596, 16, 16)

  # nose + omega mouth
  $g.FillEllipse((New-Object System.Drawing.SolidBrush $cPink), 512 - 17, 650, 34, 24)
  $pen = New-Object System.Drawing.Pen ($cMouth), 10
  $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
  $mouth = New-Object System.Drawing.Drawing2D.GraphicsPath
  $mouth.AddBezier(466, 676, 488, 700, 502, 698, 512, 684)
  $mouth.AddBezier(512, 684, 522, 698, 536, 700, 558, 676)
  $g.DrawPath($pen, $mouth)

  # blush
  $blush = New-Object System.Drawing.SolidBrush ($col.Invoke('FFA8C5', 115))
  $g.FillEllipse($blush, 318 - 48, 668 - 28, 96, 56)
  $g.FillEllipse($blush, 706 - 48, 668 - 28, 96, 56)
}

function Draw-Note($g) {
  $gold = New-Object System.Drawing.SolidBrush $cGold
  $prev = $g.Transform.Clone()
  $m = New-Object System.Drawing.Drawing2D.Matrix
  $m.RotateAt(-22, (New-Object System.Drawing.PointF 770, 216))
  $g.MultiplyTransform($m, [System.Drawing.Drawing2D.MatrixOrder]::Prepend)
  $g.FillEllipse($gold, 770 - 33, 216 - 25, 66, 50)
  $g.Transform = $prev
  $pen = New-Object System.Drawing.Pen ($cGold), 13
  $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
  $g.DrawLine($pen, 795, 208, 807, 104)
  $flag = New-Object System.Drawing.Drawing2D.GraphicsPath
  $flag.AddBezier(807, 104, 843, 112, 849, 146, 825, 170)
  $g.DrawPath($pen, $flag)
}

# mode: 'square' = rounded-square bg + note + sparkles (web / desktop / legacy android)
#       'adaptive' = cat only, caller positions it (android adaptive foreground)
function Draw-Icon($g, [string]$mode) {
  if ($mode -eq 'square') {
    $bgPath = New-RoundedRect 24 24 976 976 220
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush (
      (New-Object System.Drawing.PointF 0, 24)), (New-Object System.Drawing.PointF 0, 1000), $cBgTop, $cBgBot
    $g.FillPath($brush, $bgPath)

    Draw-Star4 $g 218 190 30 (New-Object System.Drawing.SolidBrush ($col.Invoke('FFD75E', 225)))
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ($col.Invoke('FFD75E', 160))), 254, 250, 14, 14)
    Draw-Star4 $g 856 520 18 (New-Object System.Drawing.SolidBrush ($col.Invoke('C9B6E4', 130)))
    Draw-Star4 $g 168 792 22 (New-Object System.Drawing.SolidBrush ($col.Invoke('FFD75E', 150)))
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ($col.Invoke('C9B6E4', 110))), 872, 772, 12, 12)

    Draw-Note $g
  }
  Draw-Cat $g
}

# render one bitmap at $px; mode + extra transform hook
function New-IconBitmap([int]$px, [string]$mode) {
  $ss = 4
  $big = New-Object System.Drawing.Bitmap ($px * $ss), ($px * $ss)
  $g = [System.Drawing.Graphics]::FromImage($big)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.ScaleTransform($ss * $px / 1024, $ss * $px / 1024)
  Draw-Icon $g $mode
  $g.Dispose()

  $out = New-Object System.Drawing.Bitmap $px, $px
  $g2 = [System.Drawing.Graphics]::FromImage($out)
  $g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g2.DrawImage($big, 0, 0, $px, $px)
  $g2.Dispose()
  $big.Dispose()
  $out
}

# adaptive foreground: cat centered in the 108dp canvas, inside the 66dp safe circle
function New-AdaptiveBitmap([int]$px) {
  $ss = 4
  $big = New-Object System.Drawing.Bitmap ($px * $ss), ($px * $ss)
  $g = [System.Drawing.Graphics]::FromImage($big)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $k = 0.78 * ($px * (66.0 / 108)) / 674 * $ss
  $g.TranslateTransform($px * $ss / 2.0, $px * $ss / 2.0)
  $g.ScaleTransform($k, $k)
  $g.TranslateTransform(-512, -505)
  Draw-Cat $g
  $g.Dispose()
  $out = New-Object System.Drawing.Bitmap $px, $px
  $g2 = [System.Drawing.Graphics]::FromImage($out)
  $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g2.DrawImage($big, 0, 0, $px, $px)
  $g2.Dispose()
  $big.Dispose()
  $out
}

function Save-Png($bmp, [string]$path) { $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png) }

function Get-PngBytes($bmp) {
  $ms = New-Object System.IO.MemoryStream
  $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
  $ms.ToArray()
}

function Save-Ico([string]$path, [int[]]$sizes) {
  $pngs = @(); foreach ($s in $sizes) { $b = New-IconBitmap $s 'square'; $pngs += ,(Get-PngBytes $b); $b.Dispose() }
  $ms = New-Object System.IO.MemoryStream
  $bw = New-Object System.IO.BinaryWriter $ms
  $bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$pngs.Count)
  $offset = 6 + 16 * $pngs.Count
  for ($i = 0; $i -lt $pngs.Count; $i++) {
    $s = $sizes[$i]; $b = if ($s -ge 256) { 0 } else { $s }
    $bw.Write([byte]$b); $bw.Write([byte]$b); $bw.Write([byte]0); $bw.Write([byte]0)
    $bw.Write([uint16]1); $bw.Write([uint16]32)
    $bw.Write([uint32]$pngs[$i].Length); $bw.Write([uint32]$offset)
    $offset += $pngs[$i].Length
  }
  foreach ($p in $pngs) { $data = [byte[]]$p; $bw.Write($data) }
  $bw.Flush(); [System.IO.File]::WriteAllBytes($path, $ms.ToArray()); $bw.Close()
}

# ---- outputs ----
$master = New-IconBitmap 1024 'square'
Save-Png $master (Join-Path $imgDir 'app-icon-1024.png')
foreach ($spec in @(
    @{ p = (Join-Path $imgDir 'app-icon-512.png');  s = 512 },
    @{ p = (Join-Path $imgDir 'app-icon-192.png');  s = 192 },
    @{ p = (Join-Path $imgDir 'favicon-32.png');    s = 32  },
    @{ p = (Join-Path $deskAssets 'app-icon.png');  s = 256 })) {
  $b = New-Object System.Drawing.Bitmap $spec.s, $spec.s
  $g = [System.Drawing.Graphics]::FromImage($b)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($master, 0, 0, $spec.s, $spec.s)
  $g.Dispose()
  Save-Png $b $spec.p; $b.Dispose()
}
$master.Dispose()

Save-Ico (Join-Path $root 'desktop\LyricsPlayer\app.ico') @(256, 48, 32, 16)

$legacy = @{ 'mipmap-mdpi' = 48; 'mipmap-hdpi' = 72; 'mipmap-xhdpi' = 96; 'mipmap-xxhdpi' = 144; 'mipmap-xxxhdpi' = 192 }
$adaptive = @{ 'mipmap-mdpi' = 108; 'mipmap-hdpi' = 162; 'mipmap-xhdpi' = 216; 'mipmap-xxhdpi' = 324; 'mipmap-xxxhdpi' = 432 }
foreach ($dpi in $legacy.Keys) {
  $dir = Join-Path $resDir $dpi
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
  Save-Png (New-IconBitmap $legacy[$dpi] 'square') (Join-Path $dir 'ic_launcher.png')
  Save-Png (New-IconBitmap $legacy[$dpi] 'square') (Join-Path $dir 'ic_launcher_round.png')
  Save-Png (New-AdaptiveBitmap $adaptive[$dpi]) (Join-Path $dir 'ic_launcher_foreground.png')
}
Write-Host 'icons generated OK'
