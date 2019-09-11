# Free Activity Tracker app.

Intended as a hobby project and personal use and a tracker that keeps
users in control of their own data. Low threshold, no login required

# Roadmap
## Phase 1
#### Home/start Screen
Use FAB to start activity

#### Track Physical activity
Start Activity Tracking tracks:
- [x] duration 
- [x] average speed
- [x] route
- [x] current speed
- [x] distance
	
- [x] Use foreground service to track
- [x] displays ongoing notification with activity metrics 
- [x] Stop button on notification, 
    - [x] stores activity on stop. 
- [x] Pause and Resume buttons on notification

#### OnGoing Activity Screen
updates every seconds
displays
- [ ] duration
- [ ] average speed
- [ ] current speed
- [ ] distance

#### Store activity
use ObjectBox for local storage
option to discard at running activity/cancel
could use popup for that
no summary screen at first, 
stop goes back to start and user can select last activity there

- [x] storing activities after stop

#### display stats
on home screen

#### Display past activities
- on home screen in scroll view or just recent ones
- navigate to full list and details: master detail flow

#### Display route on map

#### Pause/Resume Activity

#### Choose activity type

## Phase 2
Store remote, Free Activity Tracker is supposed to run without login, 
but users may want to backup data. Backing up data and syncing stored data is
intended to use IMAP as the most user centric and centered around owning 
the data. Not server implementation is intended. The user email account
will be used to create a folder where data can be stored.
Other providers may be implemented in the future, but for now IMAP will
be the path taken

#### Store remote using IMAP

## Phase 3
Motivation components may be added.
#### User level, using experience. Just a little WOW-experience here
#### Achievements

## Phase 4 
Reminders may be added. Somewhat personalized. Taking weather and agenda
into account.

## Phase 5
Posting activity and achievements to social media

## Phase 6
Add timestamp information to positions and display on map