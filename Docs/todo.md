TO DO:

- provide a plan for a list system
- provide plan for generic items such as pots and holes, so u can choose from a list,
rather then recreate new every time.
- find a few devices to start testing on
- add change area name
- add delete area
- add delete GrowZone
- Test and fix rotation issues for canvas: `focusedZoneId`
- Second managed device at minSdk for older phone testing
- **Area name cap (50 chars) is enforced only in the UI text field.** Move the rule to
  model/ and check it in the ViewModel too, so future paths (import, sync) can't bypass it.
- Later, own branch: Gradle 9.8 bump;
- learn about leakCanary and how to use it, , debugImplementation.
- gain a better understanding how to write this type of code, that uses a garbage collector, things must have no referencing to be removed from heap
- Find a way to automatically test clean up
