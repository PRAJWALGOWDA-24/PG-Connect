# PG Connect – PG & Hostel Finder

A full-stack web application that helps students and working professionals discover PG accommodations and hostels based on location, budget, gender, and amenities.

PG Connect provides location-based search, interactive maps, PG reviews, and bulk Excel import/export functionality through a centralized platform.

## Screenshots

<!-- Add your application screenshots here -->

### Home Page
<img width="1783" height="1017" alt="image" src="https://github.com/user-attachments/assets/8764f0a0-d427-42bf-9fc9-bacfd7fce713" />



<img width="1571" height="1007" alt="image" src="https://github.com/user-attachments/assets/58e93662-5ab5-450a-9f8b-66bc4553d629" />

<img width="1385" height="817" alt="image" src="https://github.com/user-attachments/assets/ba93f440-f832-4958-8aa2-aa1801745cd6" />


<img width="1275" height="546" alt="image" src="https://github.com/user-attachments/assets/75d503b8-fd5e-4e9f-885b-4302d6c2462b" />


<img width="1201" height="957" alt="image" src="https://github.com/user-attachments/assets/1e017125-b3d2-44df-b3ae-7a789d5f8dd4" />

<img width="1891" height="1132" alt="image" src="https://github.com/user-attachments/assets/eaef565a-cd68-4e54-9536-5707102320d0" />



## Features

- **PG Discovery:** Browse available PG and hostel listings.
- **Advanced Search:** Filter PGs by gender, rent, area, and amenities.
- **Location-Based Search:** Find nearby PGs using latitude, longitude, and search radius.
- **Interactive Map:** Visualize PG locations using Leaflet.js and OpenStreetMap.
- **PG Management:** Add, view, update, and delete PG listings.
- **Reviews and Ratings:** Submit reviews and view PG ratings.
- **Excel Import:** Upload multiple PG listings at once using an Excel `.xlsx` file.
- **Excel Export:** Download PG listings and filtered results as Excel files.
- **Analytics:** View insights about PG listings, average rent, ratings, and amenities.

## Technology Stack

| Component | Technologies |
|---|---|
| Backend | Java, Spring Boot |
| Database | MongoDB |
| Database Integration | Spring Data MongoDB |
| Frontend | HTML, CSS, JavaScript |
| Maps | Leaflet.js, OpenStreetMap |
| Excel Processing | Apache POI |
| Build Tool | Maven |
| API Testing | Postman |
| Database Tool | MongoDB Compass |

## System Architecture

```text
User
  |
  v
Frontend (HTML, CSS, JavaScript)
  |
  v
Spring Boot REST APIs
  |
  v
Controller Layer
  |
  v
Service / Repository Layer
  |
  v
MongoDB Database
```

## MongoDB Implementation

PG Connect uses MongoDB as its NoSQL database.

- Stores PG details as documents.
- Uses arrays for amenities and sharing options.
- Stores location information using GeoJSON.
- Uses geospatial queries to search nearby PGs.
- Manages PG reviews and ratings.

## Excel Import and Export

The application uses Apache POI to process Excel files.

**Import:**
- Upload an `.xlsx` file containing multiple PG records.
- Validate required columns and individual records.
- Skip invalid rows and report validation errors.
- Save valid records in MongoDB.

**Export:**
- Export PG listings into Excel format.
- Download filtered accommodation records.

## Getting Started

### Prerequisites

- Java JDK 24 or compatible configured JDK
- Maven
- MongoDB Community Server
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/PRAJWALGOWDA-24/PG-Connect.git
```

### 2. Navigate to the Project

```bash
cd PG-Connect
```

### 3. Configure MongoDB

Make sure MongoDB is running locally.

Configure your MongoDB connection in:

`src/main/resources/application.properties`

Example:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/pgconnect
server.port=8099
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

### 5. Open the Application

Visit:

```text
http://localhost:8099    
```
***based on your port number

## Future Enhancements

- User authentication and role-based access.
- PG owner registration and listing management.
- Image uploads for PG properties.
- Advanced sorting and recommendation features.
- Deployment with a cloud-hosted MongoDB database.

## Author

**Prajwal Gowda T S**

Information Science and Engineering

GitHub: [PRAJWALGOWDA-24](https://github.com/PRAJWALGOWDA-24)

---

*PG Connect – Making PG and Hostel Discovery Easier.*
