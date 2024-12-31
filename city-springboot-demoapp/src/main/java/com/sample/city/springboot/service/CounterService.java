package com.sample.city.springboot.service;

import com.sample.city.springboot.model.Counter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CounterService {

    @Autowired private MongoTemplate mongoTemplate;

    public long incrementCounter(String id) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().inc("count", 1);

        Counter counter = mongoTemplate.findAndModify(query, update, Counter.class);

        return counter.count();
    }
}
