TO DO:

- provide a plan for a list system
- provide plan for generic items such as pots and holes, so u can choose from a list,
rather then recreate new every time.
- find a few devices to start testing on
- feel-test handle sizes on a real phone (3b: hit area 48 dp, try 56-64; visual 16-24 dp)
- add change area name
- add delete area
- add delete GrowZone
- Test and fix rotation issues for canvas: `focusedZoneId`
- **Area name cap (50 chars) is enforced only in the UI text field.** Move the rule to
  model/ and check it in the ViewModel too, so future paths (import, sync) can't bypass it.
- Later, own branch: Gradle 9.8 bump;
- learn about leakCanary and how to use it, , debugImplementation.
- gain a better understanding how to write this type of code, that uses a garbage collector, things must have no referencing to be removed from heap
- Find a way to automatically test clean up
- Refactor android tests, collectFailures() used by all and helper functions
- when zoned in to zone, add a visible name showing who we are inside
- Check with real users, is the edit/enter panel at the bottom, when selecting zone visible enough, alternatively it could be added to left side with a bit of color
- In-app help/demo (own phase, after Phase 4 at the earliest):
  skippable first launch welcome + "?" button listing named stages
  (area list, create area, enter area, add GrowZone, nesting vs
  overlap, conditions*, pots/holes card*, custom zones*).
  *only once the feature exists. Help text lives in strings.xml as the
  single source; domain_model.md links to it rather than duplicating.
  Idea: each phase adds its own stage as part of its exit criteria.
  Check GIF vs static screenshots for APK size and upkeep. Each how to is its own small demo.