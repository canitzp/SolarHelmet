**41.0.1**

- Fix forge breaking changes within Forge 41.0.94

**41.0.0**

- Update to 1.19

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