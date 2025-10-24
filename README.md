# 🌍 VoyageMapper

**VoyageMapper** is an Android app (built in Java) that helps you explore the world through [Wikivoyage](https://en.wikivoyage.org/) travel guides.  
Use your **current location** or **search for a place**, view nearby articles, and explore the sights from within each destination — all on an interactive map.

---

## ✨ Features

- 🧭 **Map-based discovery** — View Wikivoyage destinations within 20 km of your current location.  
- 🔍 **Search anywhere** — Find destinations worldwide and see related travel articles.  
- 🗺️ **Map the Sights** — Tap a place to fetch all `{{see}}` listings from Wikivoyage and plot them on the map (with contact info).  
- 📍 **Custom markers** — Black pins for places, green pins for sights, with labeled titles.  
- 📱 **Bottom sheet previews** — Article details, images, and quick links.  
- 🧑‍💻 **Built in Java** — Uses AndroidX, Google Maps SDK, Retrofit, and Glide.

---

## 🛠️ Tech Stack

| Purpose | Library / API |
|----------|----------------|
| Map & location | Google Maps SDK, Play Services Location |
| HTTP / API | Retrofit2 + OkHttp |
| JSON parsing | Gson |
| Image loading | Glide |
| Clustering | Google Maps Utils |
| Material UI | Material Components for Android |

---

## 🏗️ Project Structure

app/
├── java/org/okane/voyagemapper/
│ ├── MainActivity.java
│ ├── MapActivity.java
│ ├── model/
│ │ ├── PlaceItem.java
│ │ ├── SeeListing.java
│ ├── network/
│ │ ├── WikiService.java
│ │ ├── WikiRepository.java
│ │ ├── WikiResponse.java
│ └── ui/
│ ├── PlaceClusterRenderer.java
│ └── MarkerBitmapFactory.java
├── res/
│ ├── layout/
│ ├── drawable/
│ └── values/
└── build.gradle


---

## 🚀 Getting Started

1. **Clone the repo**
   ```bash
   git clone https://github.com/<your-username>/VoyageMapper.git
   cd VoyageMapper

2. Open in Android Studio
   File → Open → Select the project folder

3. Add your Google Maps API key
   In app/src/debug/res/values/google_maps_api.xml, replace
   <string name="google_maps_key" templateMergeStrategy="replace" translatable="false">YOUR_KEY_HERE</string>

4. Run the app on an emulator or device.


Future Enhancements

🏞️ Add support for more listing types ({{do}}, {{eat}}, etc.)

🗂️ Offline caching of fetched Wikivoyage articles

🔖 Bookmark favorite places and sights

🧭 Compass / route directions integration

🌐 Multi-language Wikivoyage support
