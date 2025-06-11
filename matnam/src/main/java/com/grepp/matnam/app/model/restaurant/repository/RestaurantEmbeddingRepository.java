package com.grepp.matnam.app.model.restaurant.repository;

import com.grepp.matnam.app.model.restaurant.document.RestaurantEmbedding;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantEmbeddingRepository
extends MongoRepository<RestaurantEmbedding, String> {

}
