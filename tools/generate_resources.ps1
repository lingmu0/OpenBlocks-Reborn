param(
    [string]$LegacyProject = (Join-Path $PSScriptRoot '..\..\OpenBlocks')
)

$ErrorActionPreference = 'Stop'
$project = Resolve-Path (Join-Path $PSScriptRoot '..')
$legacyAssets = Join-Path (Resolve-Path $LegacyProject) 'src\main\resources\assets\openblocks'
$assets = Join-Path $project 'src\main\resources\assets\openblocks_reborn'
$data = Join-Path $project 'src\main\resources\data\openblocks_reborn'
$minecraftFluidTags = Join-Path $project 'src\main\resources\data\minecraft\tags\fluid'

$directories = @(
    (Join-Path $assets 'textures'),
    (Join-Path $assets 'sounds'),
    (Join-Path $assets 'models\block'),
    (Join-Path $assets 'models\item'),
    (Join-Path $assets 'blockstates'),
    (Join-Path $data 'loot_table\blocks'),
    (Join-Path $data 'recipe'),
    $minecraftFluidTags
)
$directories | ForEach-Object { New-Item -ItemType Directory -Force -Path $_ | Out-Null }

# Minecraft 1.13+ only stitches the conventional singular block/item folders into
# the main block atlas. The legacy project uses the old plural folder names, so
# copy their contents into the modern locations instead of preserving the folder.
$modernBlockTextures = Join-Path $assets 'textures\block'
$modernItemTextures = Join-Path $assets 'textures\item'
New-Item -ItemType Directory -Force -Path $modernBlockTextures, $modernItemTextures | Out-Null
Copy-Item -Path (Join-Path $legacyAssets 'textures\blocks\*') -Destination $modernBlockTextures -Recurse -Force
Copy-Item -Path (Join-Path $legacyAssets 'textures\items\*') -Destination $modernItemTextures -Recurse -Force
$modernModelTextures = Join-Path $modernBlockTextures 'models'
New-Item -ItemType Directory -Force -Path $modernModelTextures | Out-Null
Copy-Item -Path (Join-Path $legacyAssets 'textures\models\*') -Destination $modernModelTextures -Recurse -Force
Copy-Item -LiteralPath (Join-Path $legacyAssets 'sounds') -Destination $assets -Recurse -Force

$sounds = Get-Content -LiteralPath (Join-Path $legacyAssets 'sounds.json') -Raw
$sounds = $sounds.Replace('openblocks:', 'openblocks_reborn:')
[IO.File]::WriteAllText((Join-Path $assets 'sounds.json'), $sounds, [Text.UTF8Encoding]::new($false))

$blockTextures = [ordered]@{
    ladder='ladder'; guide='guide'; builder_guide='guide_new'; elevator='elevator'; elevator_rotating='elevator_rot'
    heal='heal'; target='target_front'; grave='grave_front'; flag='flag'; tank='tank'; trophy='trophy'; beartrap='beartrap'
    sprinkler='sprinkler'; cannon='cannon'; vacuum_hopper='vacuum_hopper'; sponge='sponge'; big_button='stone'
    big_button_wood='oak_planks'; imaginary='crayon_block'; fan='fan_frame'; xp_bottler='xp_bottler_front'
    village_highlighter='village_highlighter'; path='path'; auto_anvil='auto_anvil'; auto_enchantment_table='auto_enchantment_table'
    xp_drain='xp_drain'; block_breaker='block_breaker_inactive'; block_placer='block_placer_front'; item_dropper='item_dropper_front'
    rope_ladder='rope_ladder'; donation_station='donation_station'; paint_mixer='paint_mixer'; canvas='canvas'; paint_can='paint_can_front'
    canvas_glass='canvas_glass'; projector='projector'; drawing_table='drawing_table_top'; sky='sky_inactive'; xp_shower='xp_shower'
    golden_egg='egg'
}

function Write-Json([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 20
    [IO.File]::WriteAllText($Path, $json + "`n", [Text.UTF8Encoding]::new($false))
}

foreach ($entry in $blockTextures.GetEnumerator()) {
    $id = $entry.Key
    $texture = $entry.Value
    Write-Json (Join-Path $assets "models\block\$id.json") ([ordered]@{
        parent = 'minecraft:block/cube_all'
        textures = [ordered]@{ all = "openblocks_reborn:block/$texture" }
    })
    Write-Json (Join-Path $assets "blockstates\$id.json") ([ordered]@{
        multipart = @([ordered]@{ apply = [ordered]@{ model = "openblocks_reborn:block/$id" } })
    })
    Write-Json (Join-Path $assets "models\item\$id.json") ([ordered]@{ parent = "openblocks_reborn:block/$id" })
    Write-Json (Join-Path $data "loot_table\blocks\$id.json") ([ordered]@{
        type = 'minecraft:block'
        pools = @([ordered]@{
            bonus_rolls = 0.0
            rolls = 1.0
            entries = @([ordered]@{ type='minecraft:item'; name="openblocks_reborn:$id" })
            conditions = @([ordered]@{ condition='minecraft:survives_explosion' })
        })
        random_sequence = "openblocks_reborn:blocks/$id"
    })
}

# Models for dyeable blocks need tint indices; a cube_all parent cannot add them.
$tintedBlocks = @('elevator', 'flag', 'paint_can')
foreach ($id in $tintedBlocks) {
    $texture = $blockTextures[$id]
    $faces = [ordered]@{}
    foreach ($face in @('down', 'up', 'north', 'south', 'west', 'east')) {
        $faces[$face] = [ordered]@{ texture = '#all'; cullface = $face; tintindex = 0 }
    }
    Write-Json (Join-Path $assets "models\block\$id.json") ([ordered]@{
        parent = 'minecraft:block/block'
        textures = [ordered]@{ all = "openblocks_reborn:block/$texture"; particle = "openblocks_reborn:block/$texture" }
        elements = @([ordered]@{
            from = @(0, 0, 0)
            to = @(16, 16, 16)
            faces = $faces
        })
    })
}

# The rotating elevator shares the normal elevator texture on five faces. Only
# its top face carries the directional marker used to choose the destination yaw.
$rotatingElevatorFaces = [ordered]@{}
foreach ($face in @('down', 'north', 'south', 'west', 'east')) {
    $rotatingElevatorFaces[$face] = [ordered]@{ texture='#side'; cullface=$face; tintindex=0 }
}
$rotatingElevatorFaces['up'] = [ordered]@{ texture='#top'; cullface='up'; tintindex=0 }
Write-Json (Join-Path $assets 'models\block\elevator_rotating.json') ([ordered]@{
    parent='minecraft:block/block'
    textures=[ordered]@{
        side='openblocks_reborn:block/elevator'
        top='openblocks_reborn:block/elevator_rot'
        particle='openblocks_reborn:block/elevator'
    }
    elements=@([ordered]@{
        from=@(0,0,0)
        to=@(16,16,16)
        faces=$rotatingElevatorFaces
    })
})

# Preserve the detailed 1.12 cuboid models. They are standard vanilla JSON; only
# namespace and pre-flattening texture paths need updating for 1.21.
function Import-LegacyModel([string]$LegacyName, [string]$ModernName) {
    $source = Join-Path $legacyAssets "models\block\$LegacyName.json"
    $raw = Get-Content -LiteralPath $source -Raw
    $raw = $raw.Replace('"block/block"', '"minecraft:block/block"')
    $raw = $raw.Replace('openblocks:blocks/', 'openblocks_reborn:block/')
    $raw = $raw.Replace('openblocks:items/', 'openblocks_reborn:item/')
    $raw = $raw.Replace('openblocks:models/', 'openblocks_reborn:block/models/')
    [IO.File]::WriteAllText((Join-Path $assets "models\block\$ModernName.json"), $raw, [Text.UTF8Encoding]::new($false))
}

$legacyModels = [ordered]@{
    auto_anvil='auto_anvil'; auto_enchantment_table='auto_enchantment_table'; beartrap='beartrap'
    guide_inner='guide_inner'; guide_marker='guide_marker'
    egg='golden_egg'; grave_ground='grave'; paint_can='paint_can'; paint_mixer='paint_mixer'
    flag_ground='flag'; flag_wall='flag_wall'
    rope_ladder='rope_ladder'; sprinkler_static='sprinkler'; sprinkler_moving='sprinkler_moving'; tank_frame='tank'
    trophy_base='trophy'; vacuum_hopper_body='vacuum_hopper'; village='village_highlighter'
    xp_drain='xp_drain'; xp_shower='xp_shower'; fan_frame='fan_frame'; fan_blades='fan_blades'
    projector_spinner='projector_spinner'; projector_cone='projector_cone'
    target_inactive='target_inactive'; target_active='target_active'
    big_button_inactive='big_button_inactive'; big_button_active='big_button_active'; big_button_inventory='big_button_inventory'
    imaginary_block='imaginary_block'; imaginary_half='imaginary_half'; imaginary_panel='imaginary_panel'; imaginary_stairs='imaginary_stairs'
}
foreach ($entry in $legacyModels.GetEnumerator()) { Import-LegacyModel $entry.Key $entry.Value }

# The old renderer drew these moving parts in a second pass. Combine them into
# one baked model on modern Minecraft so both parts render without overlap or
# transparent-neighbour artifacts.
$fanFramePath = Join-Path $assets 'models\block\fan_frame.json'
$fanFrame = Get-Content -LiteralPath $fanFramePath -Raw | ConvertFrom-Json
$fanBlades = Get-Content -LiteralPath (Join-Path $assets 'models\block\fan_blades.json') -Raw | ConvertFrom-Json
$fanFrame.textures | Add-Member -NotePropertyName blades -NotePropertyValue 'openblocks_reborn:block/fan_blades' -Force
$fanFrame.textures | Add-Member -NotePropertyName particle -NotePropertyValue 'openblocks_reborn:block/fan_frame' -Force
foreach ($element in $fanBlades.elements) {
    # The classic inventory submodel translated the moving blades upward by
    # 0.171875 blocks (2.75 model units). Preserve that transform when the two
    # render passes are merged into one modern baked model.
    $element.from[1] = [double]$element.from[1] + 2.75
    $element.to[1] = [double]$element.to[1] + 2.75
    foreach ($face in $element.faces.PSObject.Properties.Value) {
        if ($face.texture -eq '#all') { $face.texture = '#blades' }
    }
}
$fanFrame.elements = @($fanFrame.elements) + @($fanBlades.elements)
Write-Json $fanFramePath $fanFrame

$sprinklerPath = Join-Path $assets 'models\block\sprinkler.json'
$sprinkler = Get-Content -LiteralPath $sprinklerPath -Raw | ConvertFrom-Json
$sprinklerMoving = Get-Content -LiteralPath (Join-Path $assets 'models\block\sprinkler_moving.json') -Raw | ConvertFrom-Json
$sprinkler.textures | Add-Member -NotePropertyName particle -NotePropertyValue 'openblocks_reborn:block/sprinkler' -Force
foreach ($element in $sprinklerMoving.elements) {
    if ($element.name -eq 'sprayer') {
        $element.from[1] = [double]$element.from[1] - 4
        $element.to[1] = [double]$element.to[1] - 4
    }
}
$sprinkler.elements = @($sprinkler.elements) + @($sprinklerMoving.elements)
Write-Json $sprinklerPath $sprinkler

$bearTrapClosedPath = Join-Path $assets 'models\block\beartrap_closed.json'
$bearTrapClosed = Get-Content -LiteralPath (Join-Path $assets 'models\block\beartrap.json') -Raw | ConvertFrom-Json
foreach ($element in $bearTrapClosed.elements) {
    if ($element.name -like 'right*' -or $element.name -eq 'spike_right') {
        $element | Add-Member -NotePropertyName rotation -NotePropertyValue ([ordered]@{origin=@(8,8,8);axis='z';angle=-45;rescale=$true}) -Force
    } elseif ($element.name -like 'left*' -or $element.name -eq 'spike_left') {
        $element | Add-Member -NotePropertyName rotation -NotePropertyValue ([ordered]@{origin=@(8,8,8);axis='z';angle=45;rescale=$true}) -Force
    }
}
Write-Json $bearTrapClosedPath $bearTrapClosed

# The legacy model was authored 7.5 pixels above the block floor because its
# TESR supplied a translation. Baked models need that translation applied here.
foreach ($bearTrapModelName in @('beartrap', 'beartrap_closed')) {
    $bearTrapModelPath = Join-Path $assets "models\block\$bearTrapModelName.json"
    $bearTrapModel = Get-Content -LiteralPath $bearTrapModelPath -Raw | ConvertFrom-Json
    foreach ($element in $bearTrapModel.elements) {
        $element.from[1] = [double]$element.from[1] - 7.5
        $element.to[1] = [double]$element.to[1] - 7.5
        if ($null -ne $element.rotation -and $null -ne $element.rotation.origin) {
            $element.rotation.origin[1] = [double]$element.rotation.origin[1] - 7.5
        }
    }
    Write-Json $bearTrapModelPath $bearTrapModel
}

# Forge 1.12 supplied these imaginary-block texture variables through blockstate
# defaults. Modern baked models need the bindings directly on every shape model.
foreach ($imaginaryModelName in @('imaginary_block', 'imaginary_half', 'imaginary_panel', 'imaginary_stairs')) {
    $imaginaryModelPath = Join-Path $assets "models\block\$imaginaryModelName.json"
    $imaginaryModel = Get-Content -LiteralPath $imaginaryModelPath -Raw | ConvertFrom-Json
    foreach ($binding in ([ordered]@{
        block='openblocks_reborn:block/crayon_block'
        full_panel='openblocks_reborn:block/crayon_panel'
        half_panel='openblocks_reborn:block/crayon_half_panel'
    }).GetEnumerator()) {
        $imaginaryModel.textures | Add-Member -NotePropertyName $binding.Key -NotePropertyValue $binding.Value -Force
    }
    Write-Json $imaginaryModelPath $imaginaryModel
}

# Building guides combine the old translucent shell with the small luminous core.
Write-Json (Join-Path $assets 'models\block\guide_outer.json') ([ordered]@{
    parent='minecraft:block/cube_column'
    textures=[ordered]@{end='openblocks_reborn:block/guide_top_new';side='openblocks_reborn:block/guide_side_new';particle='openblocks_reborn:block/guide'}
})
Write-Json (Join-Path $assets 'models\block\guide_inner_normal.json') ([ordered]@{
    parent='openblocks_reborn:block/guide_inner';textures=[ordered]@{texture='openblocks_reborn:block/guide_center_normal';particle='openblocks_reborn:block/guide'}
})
Write-Json (Join-Path $assets 'models\block\guide_inner_builder.json') ([ordered]@{
    parent='openblocks_reborn:block/guide_inner';textures=[ordered]@{texture='openblocks_reborn:block/guide_center_ender';particle='openblocks_reborn:block/guide'}
})
Write-Json (Join-Path $assets 'models\block\guide.json') ([ordered]@{parent='openblocks_reborn:block/guide_outer'})
Write-Json (Join-Path $assets 'models\block\builder_guide.json') ([ordered]@{parent='openblocks_reborn:block/guide_outer'})
$guideRotations = [ordered]@{
    down_east  = [ordered]@{x=90;  y=90}
    down_north = [ordered]@{x=90}
    down_south = [ordered]@{x=90;  y=180}
    down_west  = [ordered]@{x=90;  y=270}
    east_up    = [ordered]@{y=90}
    north_up   = [ordered]@{}
    south_up   = [ordered]@{y=180}
    up_east    = [ordered]@{x=270; y=270}
    up_north   = [ordered]@{x=270; y=180}
    up_south   = [ordered]@{x=270}
    up_west    = [ordered]@{x=270; y=90}
    west_up    = [ordered]@{y=270}
}

function New-GuideBlockstate([string]$InnerModel) {
    $parts = @()
    foreach ($entry in $guideRotations.GetEnumerator()) {
        foreach ($model in @('openblocks_reborn:block/guide_outer', $InnerModel)) {
            $apply = [ordered]@{model=$model}
            if ($entry.Value.Contains('x')) { $apply.x = $entry.Value.x }
            if ($entry.Value.Contains('y')) { $apply.y = $entry.Value.y }
            $parts += [ordered]@{
                when = [ordered]@{orientation=$entry.Key}
                apply = $apply
            }
        }
    }
    return [ordered]@{multipart=$parts}
}

Write-Json (Join-Path $assets 'blockstates\guide.json') `
    (New-GuideBlockstate 'openblocks_reborn:block/guide_inner_normal')
Write-Json (Join-Path $assets 'blockstates\builder_guide.json') `
    (New-GuideBlockstate 'openblocks_reborn:block/guide_inner_builder')

# Paint can colour is stored in the block/item component, so every rendered face
# participates in the same block/item tint just like the other dyeable blocks.
$paintCanModelPath = Join-Path $assets 'models\block\paint_can.json'
$paintCanModel = Get-Content -LiteralPath $paintCanModelPath -Raw | ConvertFrom-Json
foreach ($element in $paintCanModel.elements) {
    foreach ($face in $element.faces.PSObject.Properties.Value) {
        if ($face.texture -in @('#spill', '#top')) {
            $face | Add-Member -NotePropertyName tintindex -NotePropertyValue 0 -Force
        }
    }
}
Write-Json $paintCanModelPath $paintCanModel

foreach ($flagModelName in @('flag', 'flag_wall')) {
    $flagModelPath = Join-Path $assets "models\block\$flagModelName.json"
    $flagModel = Get-Content -LiteralPath $flagModelPath -Raw | ConvertFrom-Json
    foreach ($element in $flagModel.elements) {
        foreach ($face in $element.faces.PSObject.Properties.Value) {
            if ($face.texture -eq '#flag') {
                $face | Add-Member -NotePropertyName tintindex -NotePropertyValue 0 -Force
            }
        }
    }
    Write-Json $flagModelPath $flagModel
}

function Write-OrientableModel([string]$Id, [string]$Front, [string]$Side, [string]$Top = $Side) {
    Write-Json (Join-Path $assets "models\block\$Id.json") ([ordered]@{
        parent = 'minecraft:block/orientable'
        textures = [ordered]@{
            front = "openblocks_reborn:block/$Front"
            side = "openblocks_reborn:block/$Side"
            top = "openblocks_reborn:block/$Top"
        }
    })
}

Write-OrientableModel 'item_dropper' 'item_dropper_front' 'item_dropper'
Write-OrientableModel 'xp_bottler' 'xp_bottler_front' 'xp_bottler_sides' 'xp_bottler_top'
Write-OrientableModel 'drawing_table' 'drawing_table_front' 'drawing_table' 'drawing_table_top'

function Write-SixFaceMachineModel([string]$Id, [string]$Front, [string]$Back, [string]$Side) {
    Write-Json (Join-Path $assets "models\block\$Id.json") ([ordered]@{
        parent='minecraft:block/block'
        textures=[ordered]@{
            front="openblocks_reborn:block/$Front"
            back="openblocks_reborn:block/$Back"
            side="openblocks_reborn:block/$Side"
            particle="openblocks_reborn:block/$Side"
        }
        elements=@([ordered]@{
            from=@(0,0,0)
            to=@(16,16,16)
            faces=[ordered]@{
                north=[ordered]@{texture='#front';cullface='north'}
                south=[ordered]@{texture='#back';cullface='south';rotation=180}
                east=[ordered]@{texture='#side';cullface='east';rotation=90}
                west=[ordered]@{texture='#side';cullface='west';rotation=270}
                up=[ordered]@{texture='#side';cullface='up'}
                down=[ordered]@{texture='#side';cullface='down';rotation=180}
            }
        })
    })
}

Write-SixFaceMachineModel 'block_placer' 'block_placer_front' 'block_placer_back' 'block_placer_side'

function Write-BlockBreakerModel([string]$Id, [string]$Front) {
    Write-SixFaceMachineModel $Id $Front 'block_breaker_back' 'block_breaker_side'
}
Write-BlockBreakerModel 'block_breaker' 'block_breaker_inactive'
Write-BlockBreakerModel 'block_breaker_active' 'block_breaker_active'
Write-Json (Join-Path $assets 'models\block\ladder.json') ([ordered]@{
    parent='minecraft:block/ladder'
    textures=[ordered]@{texture='openblocks_reborn:block/ladder';particle='openblocks_reborn:block/ladder'}
})

function New-CubeElement($From, $To, [string]$Texture = '#all') {
    $faces = [ordered]@{}
    foreach ($face in @('down', 'up', 'north', 'south', 'west', 'east')) {
        $faces[$face] = [ordered]@{ texture = $Texture }
    }
    return [ordered]@{ from = $From; to = $To; faces = $faces }
}

# The old projector base was a stone half slab. Its projector.png file is a
# diagnostic colour grid and must not be used as the visible block texture.
Write-Json (Join-Path $assets 'models\block\projector.json') ([ordered]@{
    parent='minecraft:block/block'
    textures=[ordered]@{all='minecraft:block/smooth_stone';particle='minecraft:block/smooth_stone'}
    elements=@((New-CubeElement @(0,0,0) @(16,8,16)))
})

# The legacy cannon used a TESR model. Rebuild its recognizable wooden carriage,
# wheels, breech and raised barrel as baked cuboids so it also works in inventories.
$cannonBarrel = New-CubeElement @(6,5,0) @(10,9,9) '#metal'
$cannonBarrel.rotation = [ordered]@{origin=@(8,7,7);axis='x';angle=22.5;rescale=$true}
$cannonBreech = New-CubeElement @(5,4,6) @(11,10,12) '#metal'
$cannonBreech.rotation = [ordered]@{origin=@(8,7,7);axis='x';angle=22.5;rescale=$true}
$cannonElements = @(
    (New-CubeElement @(2,0,2) @(14,1,14) '#wood'),
    $cannonBreech, $cannonBarrel,
    (New-CubeElement @(5.5,4.5,-1) @(10.5,9.5,1) '#metal')
)
foreach ($wheelX in @(@(2,3), @(13,14))) {
    foreach ($angle in @(-45, 0, 45)) {
        $wheel = New-CubeElement @($wheelX[0],2,4) @($wheelX[1],9,11) '#wood'
        if ($angle -ne 0) {
            $wheel.rotation = [ordered]@{origin=@(8,5.5,7.5);axis='x';angle=$angle;rescale=$true}
        }
        $cannonElements += $wheel
    }
}
Write-Json (Join-Path $assets 'models\block\cannon.json') ([ordered]@{
    parent='minecraft:block/block'
    textures=[ordered]@{metal='openblocks_reborn:block/cannon';wood='minecraft:block/dark_oak_planks';particle='openblocks_reborn:block/cannon'}
    elements=$cannonElements
})

# The moving pieces were merged into the frame and sprinkler baked models above.
$fanVariants = [ordered]@{}
$sprinklerVariants = [ordered]@{}
$fanRotations = [ordered]@{ north=0; east=90; south=180; west=270 }
foreach ($direction in $fanRotations.GetEnumerator()) {
    $fanApply = [ordered]@{model='openblocks_reborn:block/fan_frame'}
    $sprinklerApply = [ordered]@{model='openblocks_reborn:block/sprinkler'}
    if ($direction.Value -ne 0) {
        $fanApply.y = $direction.Value
        $sprinklerApply.y = $direction.Value
    }
    $fanVariants["facing=$($direction.Key)"] = $fanApply
    $sprinklerVariants["facing=$($direction.Key)"] = $sprinklerApply
}
Write-Json (Join-Path $assets 'blockstates\fan.json') ([ordered]@{ variants=$fanVariants })
Write-Json (Join-Path $assets 'blockstates\sprinkler.json') ([ordered]@{ variants=$sprinklerVariants })

function Write-HorizontalBlockstate([string]$Id, [string]$Model = $Id) {
    $variants = [ordered]@{}
    $rotations = [ordered]@{ north=0; east=90; south=180; west=270 }
    foreach ($entry in $rotations.GetEnumerator()) {
        $value = [ordered]@{ model="openblocks_reborn:block/$Model" }
        if ($entry.Value -ne 0) { $value.y = $entry.Value }
        $variants["facing=$($entry.Key)"] = $value
    }
    Write-Json (Join-Path $assets "blockstates\$Id.json") ([ordered]@{ variants=$variants })
}

function Write-HorizontalPoweredBlockstate([string]$Id, [string]$OffModel = $Id, [string]$OnModel = $OffModel) {
    $variants = [ordered]@{}
    $rotations = [ordered]@{ north=0; east=90; south=180; west=270 }
    foreach ($entry in $rotations.GetEnumerator()) {
        foreach ($powered in @('false', 'true')) {
            $model = if ($powered -eq 'true') { $OnModel } else { $OffModel }
            $value = [ordered]@{ model="openblocks_reborn:block/$model" }
            if ($entry.Value -ne 0) { $value.y = $entry.Value }
            $variants["facing=$($entry.Key),powered=$powered"] = $value
        }
    }
    Write-Json (Join-Path $assets "blockstates\$Id.json") ([ordered]@{ variants=$variants })
}

foreach ($id in @('ladder','rope_ladder','grave','fan','xp_bottler','xp_drain','xp_shower','village_highlighter')) {
    if ($id -ne 'fan') { Write-HorizontalBlockstate $id }
}

$flagVariants = [ordered]@{}
foreach ($entry in ([ordered]@{north=0;east=90;south=180;west=270}).GetEnumerator()) {
    foreach ($face in @('floor', 'wall', 'ceiling')) {
        $value=[ordered]@{model="openblocks_reborn:block/$(if ($face -eq 'wall') {'flag_wall'} else {'flag'})"}
        if ($entry.Value -ne 0) { $value.y=$entry.Value }
        $flagVariants["face=$face,facing=$($entry.Key)"]=$value
    }
}
Write-Json (Join-Path $assets 'blockstates\flag.json') ([ordered]@{variants=$flagVariants})

# LiquidBlock exposes LEVEL=0..15 as block-state variants. The fluid renderer
# draws the actual animated surface; this particle-only model keeps every baked
# state valid and prevents a missing-model fallback during resource loading.
Write-Json (Join-Path $assets 'models\block\xp_juice.json') ([ordered]@{
    textures=[ordered]@{particle='openblocks_reborn:block/xp_juice_still'}
})
$xpJuiceVariants = [ordered]@{}
foreach ($level in 0..15) {
    $xpJuiceVariants["level=$level"]=[ordered]@{model='openblocks_reborn:block/xp_juice'}
}
Write-Json (Join-Path $assets 'blockstates\xp_juice.json') ([ordered]@{variants=$xpJuiceVariants})

$bearTrapVariants = [ordered]@{}
foreach ($entry in ([ordered]@{north=0;east=90;south=180;west=270}).GetEnumerator()) {
    foreach ($triggered in @('false', 'true')) {
        $model = if ($triggered -eq 'true') {'beartrap_closed'} else {'beartrap'}
        $value = [ordered]@{model="openblocks_reborn:block/$model"}
        if ($entry.Value -ne 0) { $value.y=$entry.Value }
        $bearTrapVariants["facing=$($entry.Key),triggered=$triggered"]=$value
    }
}
Write-Json (Join-Path $assets 'blockstates\beartrap.json') ([ordered]@{variants=$bearTrapVariants})

# The legacy shower model is authored against a north wall and points south.
$showerVariants = [ordered]@{}
foreach ($entry in ([ordered]@{south=0;west=90;north=180;east=270}).GetEnumerator()) {
    $value=[ordered]@{model='openblocks_reborn:block/xp_shower'}
    if ($entry.Value -ne 0) { $value.y=$entry.Value }
    $showerVariants["facing=$($entry.Key)"]=$value
}
Write-Json (Join-Path $assets 'blockstates\xp_shower.json') ([ordered]@{variants=$showerVariants})

$imaginaryVariants = [ordered]@{}
$imaginaryRotations = [ordered]@{north=0;east=90;south=180;west=270}
foreach ($direction in $imaginaryRotations.GetEnumerator()) {
    foreach ($shape in @('block','half','panel','stairs')) {
        $imaginaryValue = [ordered]@{model="openblocks_reborn:block/imaginary_$shape"}
        if ($direction.Value -ne 0) { $imaginaryValue.y=$direction.Value }
        $imaginaryVariants["facing=$($direction.Key),shape=$shape"]=$imaginaryValue
    }
}
Write-Json (Join-Path $assets 'blockstates\imaginary.json') ([ordered]@{variants=$imaginaryVariants})
Write-Json (Join-Path $assets 'models\block\imaginary.json') ([ordered]@{parent='openblocks_reborn:block/imaginary_block'})

Write-Json (Join-Path $assets 'blockstates\sky.json') ([ordered]@{variants=[ordered]@{
    'inverted=false,powered=false'=[ordered]@{model='openblocks_reborn:block/sky'}
    'inverted=false,powered=true'=[ordered]@{model='openblocks_reborn:block/sky';y=180}
    'inverted=true,powered=false'=[ordered]@{model='openblocks_reborn:block/sky';y=180}
    'inverted=true,powered=true'=[ordered]@{model='openblocks_reborn:block/sky'}
}})
foreach ($id in @('block_placer','item_dropper','cannon','auto_anvil','auto_enchantment_table','donation_station','paint_mixer','drawing_table')) {
    Write-HorizontalPoweredBlockstate $id
}
Write-HorizontalPoweredBlockstate 'block_breaker' 'block_breaker' 'block_breaker_active'

$targetParts = @()
$targetRotations = [ordered]@{ north=0; east=90; south=180; west=270 }
foreach ($direction in $targetRotations.GetEnumerator()) {
    foreach ($deployed in @('false', 'true')) {
        $targetValue = [ordered]@{
            model = if ($deployed -eq 'true') { 'openblocks_reborn:block/target_active' } else { 'openblocks_reborn:block/target_inactive' }
        }
        if ($direction.Value -ne 0) { $targetValue.y = $direction.Value }
        $targetParts += [ordered]@{
            when=[ordered]@{facing=$direction.Key;deployed=$deployed}
            apply=$targetValue
        }
    }
}
Write-Json (Join-Path $assets 'blockstates\target.json') ([ordered]@{ multipart=$targetParts })

# The projector head follows its horizontal facing.
$projectorParts=@()
foreach ($direction in ([ordered]@{north=0;east=90;south=180;west=270}).GetEnumerator()) {
    foreach ($part in @('projector','projector_spinner')) {
        $apply=[ordered]@{model="openblocks_reborn:block/$part"}; if($direction.Value -ne 0){$apply.y=$direction.Value}
        $projectorParts += [ordered]@{when=[ordered]@{facing=$direction.Key};apply=$apply}
    }
    $cone=[ordered]@{model='openblocks_reborn:block/projector_cone'}; if($direction.Value -ne 0){$cone.y=$direction.Value}
    $projectorParts += [ordered]@{when=[ordered]@{facing=$direction.Key;powered='true'};apply=$cone}
}
Write-Json (Join-Path $assets 'blockstates\projector.json') ([ordered]@{multipart=$projectorParts})

foreach ($buttonId in @('big_button','big_button_wood')) {
    $buttonTexture = if ($buttonId -eq 'big_button_wood') { 'minecraft:block/oak_planks' } else { 'minecraft:block/stone' }
    foreach ($stateName in @('inactive','active','inventory')) {
        $buttonModel = Get-Content -LiteralPath (Join-Path $assets "models\block\big_button_$stateName.json") -Raw | ConvertFrom-Json
        $buttonModel | Add-Member -NotePropertyName textures -NotePropertyValue ([ordered]@{
            all=$buttonTexture; particle=$buttonTexture
        }) -Force
        Write-Json (Join-Path $assets "models\block\${buttonId}_$stateName.json") $buttonModel
    }
    $buttonVariants = [ordered]@{}
    $wallY = [ordered]@{ north=0; east=90; south=180; west=270 }
    foreach ($face in @('floor','wall','ceiling')) {
        foreach ($direction in $wallY.GetEnumerator()) {
            foreach ($powered in @('false','true')) {
                $buttonState = if ($powered -eq 'true') { 'active' } else { 'inactive' }
                $buttonValue = [ordered]@{ model="openblocks_reborn:block/${buttonId}_$buttonState" }
                if ($face -eq 'wall') { $buttonValue.x=90; if ($direction.Value -ne 0) { $buttonValue.y=$direction.Value } }
                elseif ($face -eq 'ceiling') { $buttonValue.x=180; $buttonValue.y=(($direction.Value + 180) % 360) }
                elseif ($direction.Value -ne 0) { $buttonValue.y=$direction.Value }
                $buttonVariants["face=$face,facing=$($direction.Key),powered=$powered"]=$buttonValue
            }
        }
    }
    Write-Json (Join-Path $assets "blockstates\$buttonId.json") ([ordered]@{variants=$buttonVariants})
    Write-Json (Join-Path $assets "models\item\$buttonId.json") ([ordered]@{parent="openblocks_reborn:block/${buttonId}_inventory"})
}

$itemTextures = [ordered]@{
    hang_glider='hang_glider'; generic='blank'; generic_unstackable='pointer'; luggage='luggage'; sonic_glasses='sonic_glasses'; pencil_glasses='glasses_pencil'
    crayon_glasses='glasses_crayon'; technicolor_glasses='glasses_technicolor'; serious_glasses='glasses_admin'
    crane_control='manipulator_base'; crane_backpack='crane_backpack'; slimalyzer='slimeoff'; xp_bucket='xp_bucket'
    sleeping_bag='sleeping_bag'; paintbrush='paintbrush'; stencil='stencil'; squeegee='squeegee'; height_map='height_map'
    empty_map='empty_map'; cartographer='assistant_cartographer'; tasty_clay='yum_yum'; golden_eye='golden_eye'; cursor='cursor'
    info_book='info_book'; dev_null='dev_null'; sponge_on_a_stick='sponge_on_a_stick'; pedometer='pedometer_still'
    epic_eraser='epic_eraser'; wrench='wrench'; glyph='blank'; glider_wing='glider_wing'; beam='beam'
    crane_engine='crane_engine'; crane_magnet='crane_magnet'; miracle_magnet='crane_magnet'; line='line'
    map_controller='map_controller'; map_memory='map_memory'; assistant_base='assistant_base'; unprepared_stencil='stencil'
    sketching_pencil='sketching_pencil'; pointer='pointer'
}
foreach ($entry in $itemTextures.GetEnumerator()) {
    Write-Json (Join-Path $assets "models\item\$($entry.Key).json") ([ordered]@{
        parent = 'minecraft:item/generated'
        textures = [ordered]@{ layer0 = "openblocks_reborn:item/$($entry.Value)" }
    })
}
# The precision tools reuse the classic artwork and render smaller in the
# inventory. Other contexts inherit the parent transforms so handheld angles
# remain identical to the working full-size tools.
$smallToolDisplay = [ordered]@{gui=[ordered]@{scale=@(0.7,0.7,0.7)}}
Write-Json (Join-Path $assets 'models\item\small_paintbrush.json') ([ordered]@{
    parent='openblocks_reborn:item/paintbrush'
    display=$smallToolDisplay
})
Write-Json (Join-Path $assets 'models\item\small_squeegee.json') ([ordered]@{
    parent='openblocks_reborn:item/squeegee'
    display=$smallToolDisplay
})
# Prepared stencils use a client renderer so their actual 16x16 cut-out is
# visible in inventories and hands instead of every pattern sharing a flat icon.
Write-Json (Join-Path $assets 'models\item\stencil.json') ([ordered]@{
    parent='minecraft:builtin/entity'
})

# Luggage uses the original cuboid item model; the entity renderer renders this
# same item, giving both inventory and following entity a proper suitcase body.
$luggageBaseRaw = Get-Content -LiteralPath (Join-Path $legacyAssets 'models\item\luggage_base.json') -Raw
$luggageBaseRaw = $luggageBaseRaw.Replace('"block/block"', '"minecraft:block/block"')
$luggageBaseRaw = $luggageBaseRaw.Replace('openblocks:models/', 'openblocks_reborn:block/models/')
[IO.File]::WriteAllText((Join-Path $assets 'models\item\luggage_base.json'), $luggageBaseRaw, [Text.UTF8Encoding]::new($false))
Write-Json (Join-Path $assets 'models\item\luggage.json') ([ordered]@{
    parent='openblocks_reborn:item/luggage_base'
    textures=[ordered]@{body='openblocks_reborn:block/models/luggage_normal_body';leg='openblocks_reborn:block/models/luggage_normal_leg'}
})

$recipes = [ordered]@{
    elevator = [ordered]@{ type='minecraft:crafting_shaped'; category='redstone'; pattern=@('WWW','WEW','WWW'); key=[ordered]@{ W=[ordered]@{tag='minecraft:wool'}; E=[ordered]@{item='minecraft:ender_pearl'} }; result=[ordered]@{ id='openblocks_reborn:elevator'; count=1 } }
    glider_wing = [ordered]@{ type='minecraft:crafting_shaped'; category='equipment'; pattern=@('SSS','SWS'); key=[ordered]@{ S=[ordered]@{item='minecraft:stick'}; W=[ordered]@{tag='minecraft:wool'} }; result=[ordered]@{ id='openblocks_reborn:glider_wing'; count=1 } }
    hang_glider = [ordered]@{ type='minecraft:crafting_shaped'; category='equipment'; pattern=@('WSW'); key=[ordered]@{ W=[ordered]@{item='openblocks_reborn:glider_wing'}; S=[ordered]@{item='minecraft:stick'} }; result=[ordered]@{ id='openblocks_reborn:hang_glider'; count=1 } }
    sponge = [ordered]@{ type='minecraft:crafting_shapeless'; category='building'; ingredients=@([ordered]@{tag='minecraft:wool'},[ordered]@{item='minecraft:slime_ball'}); result=[ordered]@{ id='openblocks_reborn:sponge'; count=1 } }
    wrench = [ordered]@{ type='minecraft:crafting_shaped'; category='tools'; pattern=@(' II','III','II '); key=[ordered]@{ I=[ordered]@{item='minecraft:iron_ingot'} }; result=[ordered]@{ id='openblocks_reborn:wrench'; count=1 } }
    path = [ordered]@{ type='minecraft:crafting_shaped'; category='building'; pattern=@('GGG'); key=[ordered]@{ G=[ordered]@{item='minecraft:gravel'} }; result=[ordered]@{ id='openblocks_reborn:path'; count=6 } }
    block_breaker = [ordered]@{ type='minecraft:crafting_shaped'; category='redstone'; pattern=@('IPI','IRI','III'); key=[ordered]@{ I=[ordered]@{item='minecraft:iron_ingot'}; P=[ordered]@{item='minecraft:iron_pickaxe'}; R=[ordered]@{item='minecraft:redstone'} }; result=[ordered]@{ id='openblocks_reborn:block_breaker'; count=1 } }
    crayon_glasses = [ordered]@{ type='minecraft:crafting_shapeless'; category='tools'; ingredients=@([ordered]@{item='minecraft:paper'},[ordered]@{item='openblocks_reborn:imaginary'}); result=[ordered]@{ id='openblocks_reborn:crayon_glasses'; count=1 } }
    small_paintbrush = [ordered]@{ type='minecraft:crafting_shaped'; category='tools'; pattern=@('w','s'); key=[ordered]@{ w=[ordered]@{item='minecraft:white_wool'}; s=[ordered]@{item='minecraft:stick'} }; result=[ordered]@{ id='openblocks_reborn:small_paintbrush'; count=1 } }
    small_squeegee = [ordered]@{ type='minecraft:crafting_shaped'; category='tools'; pattern=@('s','w'); key=[ordered]@{ s=[ordered]@{item='openblocks_reborn:sponge'}; w=[ordered]@{item='minecraft:stick'} }; result=[ordered]@{ id='openblocks_reborn:small_squeegee'; count=1 } }
}
foreach ($entry in $recipes.GetEnumerator()) {
    Write-Json (Join-Path $data "recipe\$($entry.Key).json") $entry.Value
}

# Convert the original survival recipes. OpenBlocks 1.12 used Forge ore-dict
# ingredients and metadata containers; both need flattening for 1.21.
$genericItems = @('glider_wing','beam','crane_engine','crane_magnet','miracle_magnet','line',
    'map_controller','map_memory','cursor','assistant_base','unprepared_stencil','sketching_pencil')
$oreItems = @{
    blockGlass='minecraft:glass'; blockGlassColorless='minecraft:glass'; blockRedstone='minecraft:redstone_block'
    chestWood='minecraft:chest'; cobblestone='minecraft:cobblestone'; craftingTableWood='minecraft:crafting_table'
    dustGlowstone='minecraft:glowstone_dust'; dustRedstone='minecraft:redstone'; dyeBlack='minecraft:black_dye'
    dyeYellow='minecraft:yellow_dye'; gemDiamond='minecraft:diamond'; gemEmerald='minecraft:emerald'
    gemLapis='minecraft:lapis_lazuli'; ingotGold='minecraft:gold_ingot'; ingotIron='minecraft:iron_ingot'
    nuggetGold='minecraft:gold_nugget'; paneGlass='minecraft:glass_pane'; plankWood='minecraft:oak_planks'
    slimeball='minecraft:slime_ball'; stickWood='minecraft:stick'; stone='minecraft:stone'
}

function Convert-LegacyItem([string]$Id, $Metadata) {
    $dataValue = if ($null -eq $Metadata) { 0 } else { [int]$Metadata }
    if ($Id -eq 'openblocks:generic') { return "openblocks_reborn:$($genericItems[$dataValue])" }
    if ($Id -eq 'openblocks:generic_unstackable') { return 'openblocks_reborn:pointer' }
    if ($Id.StartsWith('openblocks:')) { return $Id.Replace('openblocks:', 'openblocks_reborn:') }
    switch ($Id) {
        'minecraft:wool' { return @('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')[$dataValue] + '_wool' -replace '^', 'minecraft:' }
        'minecraft:carpet' { return @('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')[$dataValue] + '_carpet' -replace '^', 'minecraft:' }
        'minecraft:dye' {
            if ($dataValue -eq 3) { return 'minecraft:cocoa_beans' }
            return @('black','red','green','brown','blue','purple','cyan','light_gray','gray','pink','lime','yellow','light_blue','magenta','orange','white')[$dataValue] + '_dye' -replace '^', 'minecraft:'
        }
        'minecraft:fence' { return 'minecraft:oak_fence' }
        'minecraft:trapdoor' { return 'minecraft:oak_trapdoor' }
        'minecraft:wooden_button' { return 'minecraft:oak_button' }
        'minecraft:stone_slab' { return 'minecraft:stone_slab' }
        default { return $Id }
    }
}

function Convert-LegacyIngredient($Ingredient) {
    if ($Ingredient.type -eq 'forge:ore_dict') {
        $mapped = $oreItems[[string]$Ingredient.ore]
        if (-not $mapped) { throw "Unmapped legacy ore ingredient: $($Ingredient.ore)" }
        return [ordered]@{item=$mapped}
    }
    return [ordered]@{item=(Convert-LegacyItem ([string]$Ingredient.item) $Ingredient.data)}
}

$legacyRecipeDir = Join-Path $legacyAssets 'recipes'
foreach ($legacyRecipeFile in Get-ChildItem -LiteralPath $legacyRecipeDir -Filter '*_0.json') {
    $legacyRecipe = Get-Content -LiteralPath $legacyRecipeFile.FullName -Raw | ConvertFrom-Json
    if ($legacyRecipe.type -notin @('forge:ore_shaped','forge:ore_shapeless')) { continue }
    $id = $legacyRecipeFile.BaseName -replace '_0$', ''
    $modernResult = [ordered]@{id=(Convert-LegacyItem ([string]$legacyRecipe.result.item) $legacyRecipe.result.data)}
    if ($legacyRecipe.result.count -and [int]$legacyRecipe.result.count -gt 1) { $modernResult.count=[int]$legacyRecipe.result.count }
    if ($legacyRecipe.type -eq 'forge:ore_shaped') {
        $modernKey = [ordered]@{}
        foreach ($keyEntry in $legacyRecipe.key.PSObject.Properties) {
            $modernKey[$keyEntry.Name] = Convert-LegacyIngredient $keyEntry.Value
        }
        $modernRecipe = [ordered]@{
            type='minecraft:crafting_shaped'; category='misc'
            group=([string]$legacyRecipe.group).Replace('openblocks:', 'openblocks_reborn:')
            pattern=@($legacyRecipe.pattern); key=$modernKey; result=$modernResult
        }
    } else {
        $modernIngredients = @($legacyRecipe.ingredients | ForEach-Object { Convert-LegacyIngredient $_ })
        $modernRecipe = [ordered]@{
            type='minecraft:crafting_shapeless'; category='misc'
            group=([string]$legacyRecipe.group).Replace('openblocks:', 'openblocks_reborn:')
            ingredients=$modernIngredients; result=$modernResult
        }
    }
    Write-Json (Join-Path $data "recipe\$id.json") $modernRecipe
}

# The sprinkler's virtual, non-flowing fluid reuses vanilla water behavior
# without creating or leaving a real water block.
Write-Json (Join-Path $minecraftFluidTags 'water.json') ([ordered]@{
    replace=$false
    values=@('openblocks_reborn:sprinkler_water')
})

Copy-Item -LiteralPath (Join-Path (Resolve-Path $LegacyProject) 'LICENSE') -Destination (Join-Path $project 'LICENSE-OpenBlocks') -Force
