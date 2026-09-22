# Implementation Plan - Display Events from Ticketmaster

This plan outlines the steps to create a new page `showEvents.xml` that displays a list of events fetched from the Ticketmaster API, matching the provided screenshot design.

## User Review Required

> [!IMPORTANT]
> The Ticketmaster API does not natively provide "interested" or "RSVP" counts as shown in the screenshot. I will implement these fields in the UI with placeholder/simulated data unless a different data source is provided.

> [!NOTE]
> The current project structure uses `setContentView` to switch between pages in `MainActivity`. I will continue this pattern for consistency, although Fragments or Navigation Component would be more idiomatic for larger apps.

## Proposed Changes

### [Data Model]

#### [NEW] [Event.kt](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/java/com/example/sisonkeevents/Event.kt)
Create a data class to represent an event.

### [Layouts]

#### [NEW] [item_event.xml](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/res/layout/item_event.xml)
Design the layout for a single list item:
- Icon (Calendar)
- Title
- Date & Location (with icons)
- Interested & RSVP info (with heart icon)

#### [NEW] [showEvents.xml](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/res/layout/showEvents.xml)
A layout containing a `RecyclerView` to display the list of events.

### [Logic]

#### [NEW] [EventAdapter.kt](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/java/com/example/sisonkeevents/EventAdapter.kt)
RecyclerView adapter to bind `Event` objects to `item_event.xml`.

#### [MODIFY] [ticketMaster.kt](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/java/com/example/sisonkeevents/ticketMaster.kt)
Add a parser to convert the JSON string response into a list of `Event` objects.

#### [MODIFY] [MainActivity.kt](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/java/com/example/sisonkeevents/MainActivity.kt)
- Add logic to handle clicks on categories in `explorepage.xml`.
- Implement `showEventsPage(categoryId: String)` to:
    - Set the content view to `showEvents.xml`.
    - Fetch data asynchronously using `ticketMaster`.
    - Update the `RecyclerView` on the UI thread.

#### [MODIFY] [explorepage.xml](file:///D:/IDE files/Android development/SisonkeEvents/app/src/main/res/layout/explorepage.xml)
Add IDs to the category layouts to allow setting click listeners.

## Verification Plan

### Automated Tests
- Not applicable for UI layout and network fetch logic in this context, but I will verify the code compiles.

### Manual Verification
- Deploy the app.
- Navigate to the Explore page.
- Click a category (e.g., Music).
- Verify the list of events is displayed and matches the design in the screenshot.
