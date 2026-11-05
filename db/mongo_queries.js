// Run in mongosh AFTER the Spring Boot app has started once (it creates indexes and seeds data):
//   mongosh
//   load("db/mongo_queries.js")
use("pgconnect");

// ---- Indexes (created automatically by Spring; this just shows them) ----
printjson(db.pgs.getIndexes());

// ---- 1. PGs within 2 km of a point, rent <= 9000, nearest first ----
db.pgs.find({
  rent: { $lte: 9000 },
  location: { $near: { $geometry: { type: "Point", coordinates: [77.5963, 13.1007] }, $maxDistance: 2000 } }
}).limit(5);

// ---- 2. Distance included in the result ($geoNear must be the first stage) ----
db.pgs.aggregate([
  { $geoNear: { near: { type: "Point", coordinates: [77.5963, 13.1007] },
                distanceField: "distMeters", maxDistance: 3000, spherical: true } },
  { $project: { name: 1, rent: 1, distMeters: 1 } },
  { $limit: 5 }
]);

// ---- 3. Filter: girls PG with wifi and food under 8000 ----
db.pgs.find({ gender: "female", rent: { $lte: 8000 }, amenities: { $all: ["wifi", "food"] } });

// ---- 4. Text search ----
db.pgs.find({ $text: { $search: "Koramangala" } }).limit(5);

// ---- 5. Analytics ----
db.pgs.aggregate([
  { $group: { _id: "$area", avgRent: { $avg: "$rent" }, total: { $sum: 1 } } },
  { $sort: { avgRent: -1 } }
]);

db.pgs.aggregate([
  { $match: { "reviews.0": { $exists: true } } },
  { $project: { name: 1, area: 1, avgRating: 1, reviewCount: { $size: "$reviews" } } },
  { $sort: { avgRating: -1, reviewCount: -1 } }, { $limit: 10 }
]);

db.pgs.aggregate([
  { $unwind: "$amenities" },
  { $group: { _id: "$amenities", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
]);

// ---- 6. explain(): the viva demo ----
// WITH the 2dsphere index (look for stage "GEO_NEAR_2DSPHERE" and small totalDocsExamined):
db.pgs.find({ location: { $near: { $geometry: { type: "Point", coordinates: [77.5963, 13.1007] }, $maxDistance: 2000 } } })
      .explain("executionStats");

// For a before/after comparison, use a non-geo query on an un-indexed field, then add an index:
db.pgs.find({ phone: /^9812/ }).explain("executionStats");   // COLLSCAN
db.pgs.createIndex({ phone: 1 });
db.pgs.find({ phone: /^9812/ }).explain("executionStats");   // IXSCAN
db.pgs.dropIndex({ phone: 1 });
