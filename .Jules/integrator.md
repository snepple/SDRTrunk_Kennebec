## 2026-05-14 - Weekly Sync (actionpagezello)
**Integrated Commits:**
- `484262e5`: Fix silent Zello keepalive death and channel-offline reconnect (ap-14.9.8)
- `153260e7`: Fix CTCSS tone squelch never opening during voice (remove narrowband check)
- `7790ac28`: Fix RSP power overload error spam after device removal, remove DCS broadband interference check, and upgrade to 10-Band Graphic EQ.

**Conflicts Resolved:**
- Manually applied the 10-Band Graphic EQ changes to `P25P1ConfigurationEditor` to ensure Kennebec's UI changes were preserved.
- Retained the Kennebec versioning format (`K.00.054`) instead of adopting the `actionpagezello` format.
