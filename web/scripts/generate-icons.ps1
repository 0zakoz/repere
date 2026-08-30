param([string]$OutputDirectory = (Join-Path $PSScriptRoot "..\assets"))

Add-Type -AssemblyName System.Drawing

function New-RepereIcon([int]$Size, [string]$Path) {
    $bitmap = [System.Drawing.Bitmap]::new($Size, $Size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml("#171218"))

    $pink = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#FF9FC8"))
    $yellow = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#FFD878"))
    $blue = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#8DC8FA"))
    $dark = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml("#422333"), [Math]::Max(3, $Size * 0.035))

    $graphics.FillEllipse($pink, $Size * 0.10, $Size * 0.10, $Size * 0.80, $Size * 0.80)
    $graphics.FillRectangle($yellow, $Size * 0.24, $Size * 0.39, $Size * 0.52, $Size * 0.22)
    $graphics.FillRectangle($blue, $Size * 0.14, $Size * 0.30, $Size * 0.15, $Size * 0.40)
    $graphics.FillRectangle($blue, $Size * 0.71, $Size * 0.30, $Size * 0.15, $Size * 0.40)
    $graphics.DrawLine($dark, $Size * 0.43, $Size * 0.53, $Size * 0.50, $Size * 0.60)
    $graphics.DrawLine($dark, $Size * 0.50, $Size * 0.60, $Size * 0.64, $Size * 0.43)

    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $dark.Dispose()
    $pink.Dispose()
    $yellow.Dispose()
    $blue.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
New-RepereIcon 180 (Join-Path $OutputDirectory "icon-180.png")
New-RepereIcon 512 (Join-Path $OutputDirectory "icon-512.png")
