$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$outputRoot = Join-Path $PSScriptRoot '..\src\main\resources\assets\openblocks_reborn\textures\gui\sprites\machine'
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$black = [System.Drawing.Color]::FromArgb(255, 0, 0, 0)
$dark = [System.Drawing.Color]::FromArgb(255, 55, 55, 55)
$shadow = [System.Drawing.Color]::FromArgb(255, 85, 85, 85)
$light = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)
$surface = [System.Drawing.Color]::FromArgb(255, 198, 198, 198)
$recess = [System.Drawing.Color]::FromArgb(255, 139, 139, 139)

function New-Texture([int]$width, [int]$height) {
    return [System.Drawing.Bitmap]::new(
        $width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Fill-TextureRect(
    [System.Drawing.Bitmap]$bitmap,
    [int]$x1, [int]$y1, [int]$x2, [int]$y2,
    [System.Drawing.Color]$color
) {
    for ($y = $y1; $y -lt $y2; $y++) {
        for ($x = $x1; $x -lt $x2; $x++) {
            $bitmap.SetPixel($x, $y, $color)
        }
    }
}

function Save-Texture([System.Drawing.Bitmap]$bitmap, [string]$name) {
    try {
        $path = Join-Path $outputRoot $name
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

# A 9x9 canonical instance of the previous containerPanel() renderer.
# Four-pixel nine-slice borders preserve every corner and bevel pixel.
$panel = New-Texture 9 9
Fill-TextureRect $panel 0 3 9 6 $black
Fill-TextureRect $panel 1 3 8 6 $light
Fill-TextureRect $panel 3 3 6 6 $surface
Fill-TextureRect $panel 6 3 8 6 $shadow
Fill-TextureRect $panel 2 0 6 1 $black
Fill-TextureRect $panel 1 1 6 2 $black
Fill-TextureRect $panel 2 1 5 2 $light
Fill-TextureRect $panel 0 2 8 3 $black
Fill-TextureRect $panel 1 2 6 3 $light
Fill-TextureRect $panel 6 2 7 3 $surface
Fill-TextureRect $panel 1 3 4 4 $light
Fill-TextureRect $panel 5 5 6 6 $shadow
Fill-TextureRect $panel 1 6 9 7 $black
Fill-TextureRect $panel 2 6 3 7 $surface
Fill-TextureRect $panel 3 6 8 7 $shadow
Fill-TextureRect $panel 2 7 8 8 $black
Fill-TextureRect $panel 3 7 7 8 $shadow
Fill-TextureRect $panel 3 8 7 9 $black
Save-Texture $panel 'panel.png'

# A 5x5 canonical instance of the previous recessed() renderer.
$recessed = New-Texture 5 5
Fill-TextureRect $recessed 0 0 5 5 $dark
Fill-TextureRect $recessed 1 1 5 5 $light
Fill-TextureRect $recessed 2 2 4 4 $recess
Save-Texture $recessed 'recessed.png'

# Transparent scalable frame drawn over the dynamic bevel fill colour.
$bevel = New-Texture 5 5
Fill-TextureRect $bevel 0 0 4 1 $light
Fill-TextureRect $bevel 0 0 1 4 $light
Fill-TextureRect $bevel 0 4 5 5 $dark
Fill-TextureRect $bevel 4 0 5 5 $dark
Fill-TextureRect $bevel 1 3 4 4 $shadow
Fill-TextureRect $bevel 3 1 4 4 $shadow
Save-Texture $bevel 'bevel_frame.png'

Write-Output "Generated machine GUI textures in $outputRoot"
