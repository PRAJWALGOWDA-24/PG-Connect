
package com.pgconnect.controller;

import com.pgconnect.model.Pg;
import com.pgconnect.model.PgRequest;
import com.pgconnect.model.Review;
import com.pgconnect.repository.PgRepository;
import com.pgconnect.service.PgExcelService;

import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/pgs")
public class PgController {

    private final PgRepository repo;
    private final MongoTemplate mongo;
    private final PgExcelService excelService;

    public PgController(
            PgRepository repo,
            MongoTemplate mongo,
            PgExcelService excelService) {

        this.repo = repo;
        this.mongo = mongo;
        this.excelService = excelService;
    }

    public record NearbyResult(Pg pg, double distanceKm) {
    }

    // ---------------- SEARCH ----------------

    @GetMapping
    public List<Pg> filter(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minRent,
            @RequestParam(required = false) Integer maxRent,
            @RequestParam(required = false) String amenities,
            @RequestParam(required = false) String area) {

        Query q = buildQuery(gender, minRent, maxRent, amenities, area);
        q.with(Sort.by(Sort.Direction.ASC, "rent")).limit(200);

        return mongo.find(q, Pg.class);
    }

    @GetMapping("/nearby")
    public List<NearbyResult> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "2") double radius,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minRent,
            @RequestParam(required = false) Integer maxRent,
            @RequestParam(required = false) String amenities) {

        Query filter = buildQuery(gender, minRent, maxRent, amenities, null);

        NearQuery near = NearQuery
                .near(new Point(lng, lat), Metrics.KILOMETERS)
                .maxDistance(new Distance(radius, Metrics.KILOMETERS))
                .spherical(true)
                .limit(200)
                .query(filter);

        GeoResults<Pg> results = mongo.geoNear(near, Pg.class);

        List<NearbyResult> output = new ArrayList<>();

        for (GeoResult<Pg> result : results) {
            output.add(new NearbyResult(
                    result.getContent(),
                    Math.round(result.getDistance().getValue() * 100.0) / 100.0));
        }

        return output;
    }

    private Query buildQuery(
            String gender,
            Integer minRent,
            Integer maxRent,
            String amenities,
            String area) {

        Query q = new Query();

        if (gender != null && !gender.isBlank()) {
            q.addCriteria(Criteria.where("gender").is(gender));
        }

        if (minRent != null || maxRent != null) {
            Criteria rent = Criteria.where("rent");

            if (minRent != null)
                rent = rent.gte(minRent);
            if (maxRent != null)
                rent = rent.lte(maxRent);

            q.addCriteria(rent);
        }

        if (amenities != null && !amenities.isBlank()) {
            q.addCriteria(
                    Criteria.where("amenities")
                            .all(Arrays.asList(amenities.split(","))));
        }

        if (area != null && !area.isBlank()) {
            q.addCriteria(
                    Criteria.where("area")
                            .regex(Pattern.quote(area.trim()), "i"));
        }

        return q;
    }

    // ---------------- CRUD ----------------

    @GetMapping("/{id}")
    public Pg get(@PathVariable String id) {

        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "PG not found"));
    }

    @PostMapping
    public Pg create(@RequestBody PgRequest req) {

        Pg pg = new Pg();
        apply(pg, req);

        return repo.save(pg);
    }

    @PutMapping("/{id}")
    public Pg update(
            @PathVariable String id,
            @RequestBody PgRequest req) {

        Pg pg = get(id);
        apply(pg, req);

        return repo.save(pg);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable String id) {

        repo.deleteById(id);
        return Map.of("deleted", id);
    }

    private void apply(Pg pg, PgRequest req) {

        pg.setName(req.getName());
        pg.setGender(req.getGender());
        pg.setRent(req.getRent());
        pg.setSharing(req.getSharing());
        pg.setAmenities(req.getAmenities());
        pg.setArea(req.getArea());
        pg.setAddress(req.getAddress());
        pg.setPhone(req.getPhone());

        pg.setLocation(new GeoJsonPoint(req.getLng(), req.getLat()));
    }

    // ---------------- REVIEWS ----------------

    @PostMapping("/{id}/reviews")
    public Pg addReview(
            @PathVariable String id,
            @RequestBody Review review) {

        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rating must be 1 to 5");
        }

        Pg pg = get(id);

        review.setDate(new Date());
        pg.getReviews().add(review);

        double avg = pg.getReviews()
                .stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);

        pg.setAvgRating(Math.round(avg * 10.0) / 10.0);

        return repo.save(pg);
    }

    // ---------------- BULK EXCEL IMPORT ----------------

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importExcel(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select an Excel file");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(".xlsx")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only Excel .xlsx files are supported");
        }

        PgExcelService.ImportResult result;

        try {
            result = excelService.importExcel(file);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or corrupted Excel file. Please upload a valid .xlsx file.");
        }

        List<Pg> saved = repo.saveAll(result.pgs());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("totalValidRecords", result.pgs().size());
        response.put("successfullyImported", saved.size());
        response.put("failedRecords", result.errors().size());
        response.put("errors", result.errors());

        return response;
    }

    // ---------------- EXCEL EXPORT ----------------

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "5") double radius,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minRent,
            @RequestParam(required = false) Integer maxRent,
            @RequestParam(required = false) String amenities,
            @RequestParam(required = false) String area) throws IOException {

        if ((lat == null) != (lng == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide both latitude and longitude");
        }

        List<Pg> pgs;

        if (lat != null) {

            if (lat < -90 || lat > 90 ||
                    lng < -180 || lng > 180 ||
                    radius <= 0 || radius > 100) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid coordinates or radius");
            }

            Query filter = buildQuery(
                    gender, minRent, maxRent, amenities, area);

            NearQuery near = NearQuery
                    .near(new Point(lng, lat), Metrics.KILOMETERS)
                    .maxDistance(new Distance(radius, Metrics.KILOMETERS))
                    .spherical(true)
                    .limit(10000)
                    .query(filter);

            GeoResults<Pg> results = mongo.geoNear(near, Pg.class);

            pgs = new ArrayList<>();

            for (GeoResult<Pg> result : results) {
                pgs.add(result.getContent());
            }

        } else {

            Query query = buildQuery(
                    gender, minRent, maxRent, amenities, area);

            query.with(Sort.by(Sort.Direction.ASC, "rent"));
            query.limit(10000);

            pgs = mongo.find(query, Pg.class);
        }

        byte[] excel = excelService.exportExcel(pgs);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=PG_Connect_Listings.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
