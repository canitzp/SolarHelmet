# Solar Helmet

## Support
|    Version    | Modloader | Support-Status |
|:-------------:|:---------:|:--------------:|
|    1.21.8     | NeoForge  |       ✅        |
|    1.21.3     | NeoForge  |       ⚠️       |
| 1.21 / 1.21.1 | NeoForge  |       ⚠️       |
|    1.20.6     | NeoForge  |       ❌        |
|    1.20.4     | NeoForge  |       ❌        |
|    1.20.2     | NeoForge  |       ❌        |
|    1.20.1     |   Forge   |       ❌        |
|    1.19.x     |   Forge   |       ❌        |
|    1.18.x     |   Forge   |       ❌        |
|    1.17.x     |   Forge   |       ❌        |
|    1.16.x     |   Forge   |       ❌        |
|    1.15.x     |   Forge   |       ❌        |

⚠️: bug fix only

## About

This mod adds a little item, the "Solar helmet module", which can be applied to any helmet, that uses the default implementation from Minecraft.

When crafting together with a helmet, the helmet gets a new tooltip text and while you're wearing it in your helmet slot, it produces energy.  
The energy is internally stored and automatically applied to any item in you inventory that can hold FE (Forge Energy).  
The helmet itself can't be used as battery and the only way to extract the energy is by putting a energy consuming one, eg.: a battery, in your inventory.  
The helmet has no transfer limit, so it can be discharged as fast as possible (the real transfer rate depends on the energy consumer).

![creative_tab](https://raw.githubusercontent.com/canitzp/SolarHelmet/master/readme/creative_tab.png)

#### Crafting
|     Version     |                                  How                                  |
|:---------------:|:---------------------------------------------------------------------:|
|    >= 1.21.3    | Right click with the feeder module, while wearing the desired helmet. |
| 1.19.4 - 1.21.1 |         Craft module and helmet together in a smiting table.          |
|    <= 1.19.4    |  Craft module and helmet together in any crafting grid (shapeless).   |

### Solar production
Since version 40.1.0 (1.18.2 backport) and 45.1.0, a new calculation is used for determining a multiplier based on daytime.
For simplicity the here shown graph uses the default base energy value for easier readability:
![solar_production_plot](https://raw.githubusercontent.com/canitzp/SolarHelmet/master/readme/solar_production_plot.png)

x-axis: Minecraft ingame daytime, value between 0 and infinite (every 24000 is one day), every day starts at (daycount*24000)+6000

y-axis: energy production FE/t, if in direct sunlight, and with the default value of 15FE/t max

Formula:
```math
f(x)=0.9*\sin((\tfrac{\pi}{12000})*x)+0.1
```

[![mini_mod](https://canitzp.de/minimod_logo.png)](https://canitzp.de/minimod.html)