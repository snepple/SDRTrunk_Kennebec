## Goal
Ensure all user interface configuration panels and tables automatically adapt and scale to prevent cropping on lower-resolution monitors or constrained window heights.

# Natural Scrolling & Viewport Layout Bounds

SDRTrunk Kennebec introduces a scroll-lock elimination system that prevents viewport cropping issues where forms would previously expand beyond the screen edge.

## UI Component Layouts

| Component | Behavior |
| --- | --- |
| **Adaptive Sidebars** | The main category list (e.g., in User Preferences) is now shrinkable and automatically shows standard scrollbars if the window height is constrained. |
| **ScrollPane Ingestion** | Every single preference editor panel now resides inside a transparent scrolling viewport, guaranteeing that long configuration sheets are fully accessible. |
| **Records Table Elasticity** | Tables like the recordings catalog (`AudioRecordingsPanel`) scale naturally to fill the bottom viewport and expand dynamically when window dimensions are adjusted. |

## Step-by-Step

1. Open **User Preferences** or **Audio Recordings**.
2. Resize the main application window to a smaller size.
3. Observe that the sidebars and panels will automatically inject standard scrollbars instead of cutting off configuration options.
