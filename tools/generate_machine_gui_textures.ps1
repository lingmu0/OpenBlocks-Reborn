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

$textureSize = 256

# The large repeatable centre is wider and taller than every current machine
# panel, preventing GuiGraphics from submitting thousands of 1px repeat tiles.
$panel = New-Texture $textureSize $textureSize
Fill-TextureRect $panel 0 3 $textureSize ($textureSize - 3) $black
Fill-TextureRect $panel 1 3 ($textureSize - 1) ($textureSize - 3) $light
Fill-TextureRect $panel 3 3 ($textureSize - 3) ($textureSize - 3) $surface
Fill-TextureRect $panel ($textureSize - 3) 3 ($textureSize - 1) ($textureSize - 3) $shadow
Fill-TextureRect $panel 2 0 ($textureSize - 3) 1 $black
Fill-TextureRect $panel 1 1 ($textureSize - 3) 2 $black
Fill-TextureRect $panel 2 1 ($textureSize - 4) 2 $light
Fill-TextureRect $panel 0 2 ($textureSize - 1) 3 $black
Fill-TextureRect $panel 1 2 ($textureSize - 3) 3 $light
Fill-TextureRect $panel ($textureSize - 3) 2 ($textureSize - 2) 3 $surface
Fill-TextureRect $panel 1 3 4 4 $light
Fill-TextureRect $panel ($textureSize - 4) ($textureSize - 4) `
    ($textureSize - 3) ($textureSize - 3) $shadow
Fill-TextureRect $panel 1 ($textureSize - 3) $textureSize ($textureSize - 2) $black
Fill-TextureRect $panel 2 ($textureSize - 3) 3 ($textureSize - 2) $surface
Fill-TextureRect $panel 3 ($textureSize - 3) ($textureSize - 1) ($textureSize - 2) $shadow
Fill-TextureRect $panel 2 ($textureSize - 2) ($textureSize - 1) ($textureSize - 1) $black
Fill-TextureRect $panel 3 ($textureSize - 2) ($textureSize - 2) ($textureSize - 1) $shadow
Fill-TextureRect $panel 3 ($textureSize - 1) ($textureSize - 2) $textureSize $black
Save-Texture $panel 'panel.png'

$recessed = New-Texture $textureSize $textureSize
Fill-TextureRect $recessed 0 0 $textureSize $textureSize $dark
Fill-TextureRect $recessed 1 1 $textureSize $textureSize $light
Fill-TextureRect $recessed 2 2 ($textureSize - 1) ($textureSize - 1) $recess
Save-Texture $recessed 'recessed.png'

$bevel = New-Texture $textureSize $textureSize
Fill-TextureRect $bevel 0 0 ($textureSize - 1) 1 $light
Fill-TextureRect $bevel 0 0 1 ($textureSize - 1) $light
Fill-TextureRect $bevel 0 ($textureSize - 1) $textureSize $textureSize $dark
Fill-TextureRect $bevel ($textureSize - 1) 0 $textureSize $textureSize $dark
Fill-TextureRect $bevel 1 ($textureSize - 2) ($textureSize - 1) ($textureSize - 1) $shadow
Fill-TextureRect $bevel ($textureSize - 2) 1 ($textureSize - 1) ($textureSize - 1) $shadow
Save-Texture $bevel 'bevel_frame.png'

Write-Output "Generated machine GUI textures in $outputRoot"
