package com.sample.city.springboot.repository;

import com.sample.city.springboot.model.User;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {}
