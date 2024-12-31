package com.sample.city.springboot.repository;

import com.sample.city.springboot.model.Counter;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CounterRepository extends MongoRepository<Counter, String> {}
