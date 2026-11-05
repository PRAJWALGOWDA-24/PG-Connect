package com.pgconnect.controller;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final MongoTemplate mongo;

    public AnalyticsController(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Average rent and number of PGs per area. */
    @GetMapping("/avg-rent")
    public List<Document> avgRentPerArea() {
        Aggregation agg = newAggregation(
                group("area").avg("rent").as("avgRent").count().as("total"),
                sort(Sort.Direction.DESC, "avgRent")
        );
        return mongo.aggregate(agg, "pgs", Document.class).getMappedResults();
    }

    /** Top 10 PGs by rating (only PGs that have at least one review). */
    @GetMapping("/top-rated")
    public List<Document> topRated() {
        Aggregation agg = newAggregation(
                match(Criteria.where("reviews.0").exists(true)),
                project("name", "area", "avgRating").and("reviews").size().as("reviewCount"),
                sort(Sort.Direction.DESC, "avgRating", "reviewCount"),
                limit(10)
        );
        return mongo.aggregate(agg, "pgs", Document.class).getMappedResults();
    }

    /** How many PGs offer each amenity. */
    @GetMapping("/amenities")
    public List<Document> amenityPopularity() {
        Aggregation agg = newAggregation(
                unwind("amenities"),
                group("amenities").count().as("count"),
                sort(Sort.Direction.DESC, "count")
        );
        return mongo.aggregate(agg, "pgs", Document.class).getMappedResults();
    }
}
