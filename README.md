# Family_Tracker

📍 Family Tracker - Real-time Location Sharing App
Family Tracker is an advanced Android application designed to help family members stay connected and safe. It provides a secure platform to monitor real-time locations, battery status, and movement speeds of your loved ones within private groups.

✨ Features
Real-time Map Integration: View live locations of all group members on a high-performance Google Map.

Private Group Management: Create your own family circle or join an existing one using unique group IDs.

Live Member Insights: Clicking on a member's marker displays their Battery Level, Current Speed, and Real-time Distance from your location.

Smart Routing: Automatically draws the shortest road route (Polyline) to any selected member using OSRM API.

Instant Battery Alerts: Receive automated notifications when a family member's battery drops below 15%.

Professional Email Invitations: Invite new members to join your group via professional HTML-formatted emails containing a direct APK download link.

Interactive UI: Seamless navigation with a Sidebar (Drawer) and Bottom Navigation bar for quick access to key features.

🛠️ Tech Stack
Language: Java (Android SDK)

Backend: Firebase Realtime Database

Authentication: Firebase Auth (Email/Password)

Maps API: Google Maps SDK for Android

Routing API: OSRM (Open Source Routing Machine)

Networking: Retrofit 2 & GSON (for API communication)

Design: Material Design Components, Vector Graphics

🚀 Installation & Setup
Clone the Repository:

Bash
git clone https://github.com/yourusername/family-gps-tracker.git
Firebase Configuration:

Create a new project in the Firebase Console.

Download the google-services.json file and place it in the app/ directory.

Enable Email/Password Authentication and Realtime Database.

API Key Integration:

Add your Google Maps API Key in the AndroidManifest.xml file.

Update your Email API Key in ManageMembersActivity.java.

Build & Run:

Open the project in Android Studio.

Click Sync Project with Gradle Files.

Run the app on a physical device for accurate GPS testing.

🛡️ Permissions Required
ACCESS_FINE_LOCATION: For precise real-time tracking.

POST_NOTIFICATIONS: To receive low battery and group alerts.

FOREGROUND_SERVICE: To keep tracking active even when the app is in the background.

INTERNET: To sync data with Firebase and fetch map tiles.

👨‍💻 Developed By
Anshul Dave Android Developer | Java | Firebase Specialist 📧 Email: ahdave1573@gmail.com
