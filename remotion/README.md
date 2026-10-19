# DimensionWorks opening cinematic

A 90-second, 1920x1080, 30 fps Remotion opening. Run all commands from this directory.

## Timeline

- 00:00-00:15: dimensional anomaly disturbs the sky and land.
- 00:15-00:30: living red heart transforms into the repository normal Gear Heart texture.
- 00:30-00:40: a gear falls from the sky and knocks the player unconscious.
- 00:40-00:58: 30,000 years pass; nature reclaims the world without industrial ruins.
- 00:58-01:15: the player wakes, sees the damaged heart locked in the accessory slot, and discovers its remaining base bonuses.
- 01:15-01:30: DimensionWorks title.

## Run

Install Node.js 20+, Python 3.10+, then run:

    python -m pip install -r requirements.txt
    npm install
    npm run studio

Render with: npm run render. Output: out/dimensionworks-intro.mp4.

## Source assets and mechanics

The normal and damaged 32x32 PNGs are copied from the existing KubeJS textures. The Python asset script preserves the normal texture exact alpha silhouette while replacing all RGB values and internal industrial details with a red organic palette. The final transformed frame uses the original normal texture; the awakening scene uses the existing damaged texture.

The accessory panel is a cinematic reconstruction, not a live Minecraft capture. The displayed base bonuses match the implementation: interaction reach +2, Haste II, and Luck I. Industrial RPM limits and the seven industrial bottlenecks are intentionally not mislabeled as potion effects.

Key motion is frame-driven with useCurrentFrame, interpolate, and restrained spring transitions. CSS is used for static layout, not CSS animations.
