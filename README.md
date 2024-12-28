# TrackMyBusWAW

## Overview
**TrackMyBusWAW** is an Android application designed to provide real-time tracking of buses and trams in Warsaw. The app uses the Google Maps API and integrates with the Warsaw Public Transport API to display the current locations of vehicles, helping users plan their journeys more efficiently.

---

## Features
- **Real-Time Bus/Tram Tracking**: View live locations of buses and trams on an interactive map.
- **Line Filtering**: Select a specific bus or tram line to filter displayed vehicles.
- **Location Awareness**: Automatically zooms into your current location.
- **Dynamic Map Updates**: Map refreshes based on zoom level and visible bounds.
- **Customizable Markers**: Displays buses and trams with custom markers, including line numbers.

---

## Technologies Used
- **Android Development**:
  - Minimum SDK: 26
  - Target SDK: 31
- **APIs**:
  - Google Maps API
  - Warsaw Public Transport API
- **Libraries**:
  - Retrofit (API communication)
  - Gson (JSON parsing)
  - Robolectric (Unit testing)
  - Mockito (Mocking dependencies)
- **Development Tools**:
  - Android Studio
  - Java 8

---

## Installation

### Prerequisites
- Android Studio (latest version recommended)
- Google Maps API Key
- Internet connection

### Steps to Install
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/TrackMyBusWAW.git
   ```
2.	Open the project in Android Studio.
3.	Replace the placeholder in AndroidManifest.xml with your own Google Maps API key:
    ```bash
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_GOOGLE_MAPS_API_KEY" />
    ```
4.	Build and run the project on an emulator or physical device.

---

## How to Use
1.	Open the app to view the interactive map.
2.	Use the current location button to center the map on your location.
3.	Tap the menu button to filter vehicles by specific lines.
4.	Select “SHOW ALL BUSES” to reset the filter and view all vehicles.
5.	Zoom in/out to adjust the visible range of vehicles on the map.

---

## Project Structure
`MainActivity:` Core logic for map interaction and API integration.
###	Adapters:
`BusLinesAdapter:` Manages the list of bus lines in the RecyclerView.
###	Models:
`Bus:` Represents a single bus or tram with details like location and line.
`ApiResponse:` Represents the response structure of the Warsaw API.
###	API Service:
`WarsawApiService:` Interface for communicating with the Warsaw Public Transport API.

---

## Testing
The application includes unit tests for key components:
`MainActivityTest:`

Tests for sorting, filtering, and formatting logic.

`WarsawApiServiceTest:`
Verifies API calls and interactions using mocked dependencies.

`ApiResponseTest:`
Tests data handling and response structure.

To run tests:
`./gradlew test`

---

## Contributing
1.	Fork the repository.
2.	Create a new branch for your feature:
`git checkout -b feature-name`
3.	Commit your changes:
`git commit -m "Add your message"`
4.	Push to your branch:
`git push origin feature-name`
5.	Create a pull request.

---

## License

This project is licensed under the MIT License. See the LICENSE file for details.

---

Enjoy using **TrackMyBusWAW! 😊**