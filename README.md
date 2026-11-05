# PG Connect (Spring Boot + MongoDB + HTML/CSS/JS)

## Install
1. JDK 17 (already installed)
2. MongoDB Community Server (run as a service, port 27017) and MongoDB Compass
3. Maven (see below) or use the Maven wrapper
4. VS Code extensions: Extension Pack for Java, Spring Boot Extension Pack

## Run
1. Make sure MongoDB is running (Windows: Services > MongoDB Server > Running)
2. In this folder: `mvn spring-boot:run`  (or `mvnw.cmd spring-boot:run` if you added the wrapper)
3. Open http://localhost:8080 in the browser
4. First start inserts 300 sample PGs and creates all indexes

## API
- GET    /api/pgs?gender=&minRent=&maxRent=&amenities=wifi,food&area=
- GET    /api/pgs/nearby?lat=&lng=&radius=2&gender=&maxRent=&amenities=
- GET    /api/pgs/{id}
- POST   /api/pgs            (body: name, gender, rent, sharing[], amenities[], area, address, phone, lat, lng)
- PUT    /api/pgs/{id}
- DELETE /api/pgs/{id}
- POST   /api/pgs/{id}/reviews   (body: userName, rating, comment)
- GET    /api/analytics/avg-rent | /top-rated | /amenities
