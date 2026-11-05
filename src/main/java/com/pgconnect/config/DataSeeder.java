package com.pgconnect.config;

import com.pgconnect.model.Pg;
import com.pgconnect.model.Review;
import com.pgconnect.repository.PgRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import java.util.*;

/** Inserts 300 random Bengaluru PGs on first run (only when the collection is empty). */
@Component
public class DataSeeder implements CommandLineRunner {

    private final PgRepository repo;

    public DataSeeder(PgRepository repo) {
        this.repo = repo;
    }

    // area, latitude, longitude
    private static final Object[][] AREAS = {
            {"Yelahanka", 13.1007, 77.5963}, {"Koramangala", 12.9352, 77.6245},
            {"Indiranagar", 12.9784, 77.6408}, {"Whitefield", 12.9698, 77.7500},
            {"HSR Layout", 12.9116, 77.6474}, {"Marathahalli", 12.9591, 77.6974},
            {"Electronic City", 12.8452, 77.6602}, {"BTM Layout", 12.9166, 77.6101},
            {"Jayanagar", 12.9308, 77.5838}, {"Hebbal", 13.0358, 77.5970},
            {"Banashankari", 12.9255, 77.5468}, {"Rajajinagar", 12.9916, 77.5520}
    };
    private static final String[] NAMES = {"Sri Sai", "Green Nest", "Comfort Stay", "Royal", "Sunrise",
            "Lakshmi", "Balaji", "Urban Nest", "Happy Homes", "Elite", "Maple", "Ganesh", "Cozy Corner", "Skyline"};
    private static final String[] AMENITIES = {"wifi", "food", "ac", "laundry", "parking", "gym", "cctv"};
    private static final String[] COMMENTS = {"Clean rooms", "Good food", "Owner is helpful", "Noisy at night",
            "Great location", "Value for money", "WiFi is slow", "Safe and secure", "Spacious rooms"};
    private static final String[] USERS = {"Rahul", "Priya", "Aman", "Sneha", "Karthik", "Divya", "Arjun", "Meera"};

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        Random r = new Random(42);
        List<Pg> list = new ArrayList<>();

        for (int i = 0; i < 300; i++) {
            Object[] a = AREAS[r.nextInt(AREAS.length)];
            double lat = (double) a[1] + (r.nextDouble() - 0.5) * 0.024;   // spread about 1.3 km around the area centre
            double lng = (double) a[2] + (r.nextDouble() - 0.5) * 0.024;

            String gender = new String[]{"male", "female", "unisex"}[r.nextInt(3)];
            String suffix = gender.equals("male") ? " Boys PG" : gender.equals("female") ? " Girls PG" : " Co-Living";

            Pg pg = new Pg();
            pg.setName(NAMES[r.nextInt(NAMES.length)] + suffix);
            pg.setGender(gender);
            pg.setRent(4000 + r.nextInt(9) * 750);
            pg.setSharing(new ArrayList<>(List.of(1, 2, 3).subList(0, 1 + r.nextInt(3))));
            pg.setArea((String) a[0]);
            pg.setAddress((1 + r.nextInt(20)) + "th Cross, " + a[0]);
            pg.setPhone("98" + (10000000 + r.nextInt(89999999)));
            pg.setLocation(new GeoJsonPoint(lng, lat));       // longitude first

            Set<String> am = new LinkedHashSet<>();
            am.add("wifi");
            int target = 2 + r.nextInt(4);
            while (am.size() < target) am.add(AMENITIES[r.nextInt(AMENITIES.length)]);
            pg.setAmenities(new ArrayList<>(am));

            int reviewCount = r.nextInt(6);
            double sum = 0;
            for (int k = 0; k < reviewCount; k++) {
                int rating = 2 + r.nextInt(4);
                sum += rating;
                pg.getReviews().add(new Review(USERS[r.nextInt(USERS.length)], rating, COMMENTS[r.nextInt(COMMENTS.length)]));
            }
            pg.setAvgRating(reviewCount == 0 ? 0 : Math.round(sum / reviewCount * 10.0) / 10.0);
            list.add(pg);
        }
        repo.saveAll(list);
        System.out.println("Seeded " + list.size() + " PGs");
    }
}
