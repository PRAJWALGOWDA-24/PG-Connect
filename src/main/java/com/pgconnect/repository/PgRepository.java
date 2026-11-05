package com.pgconnect.repository;

import com.pgconnect.model.Pg;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PgRepository extends MongoRepository<Pg, String> { // used to connect backed with DB nd ive simplest
                                                                    // opeartions
}
