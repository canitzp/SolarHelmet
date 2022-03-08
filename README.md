# Solar Helmet

## Support

- 1.18.2: fully supported
- 1.18/1.18.1: unsupported
- 1.17.x: unsupported
- 1.16.x: only bug fixes
- 1.15.x: unsupported

## Changelog
**40.0.0**

- Update to 1.18.2 (Needs at least Forge 40.0.12)

**38.0.0:**

- Update to 1.18
- New versioning numbering, still compatible with semver, but now the major number is equal to the major of the used minecraft forge version

**1.2.2-1.17.1:**

- Use AddReloadListenerEvent instead on adding the recipes on world load. Now the /reload command works properly.
- Fixed the solar helmet ability to only charge the real inventory items, not armor and offhand. Now all can be charged.

**1.2.1:**

- Fixed recipe injection. It should work properly now, I have tested it together with FeederHelmet

**1.2.0:**

- Update to 1.17.1 (compiled with Forge 37.0.9)
- New recipe registering without access transforming. More safe if helmets with the same names (different modnames) appear.
- When a helmet is repaired with the anvil, the solar feature and energy isn't lost anymore

**1.1.0:**

- Update to 1.16.2 (compiled with Forge 33.0.32)

**1.0.0:**

- Initial Release

## Description

This mod adds a little item, the "Solar helmet module", which can be applied to any helmet, that uses the default implementation from Minecraft.

When crafting together with a helmet, the helmet gets a new tooltip text and while you're wearing it in your helmet slot, it produces energy.  
The energy is internally stored and automatically applied to any item in you inventory that can hold FE (Forge Energy).  
The helmet itself can't be used as battery and the only way to extract the energy is by putting a energy consuming one, eg.: a battery, in your inventory.  
The helmet has no transfer limit, so it can be discharged as fast as possible (the real transfer rate depends on the energy consumer).
