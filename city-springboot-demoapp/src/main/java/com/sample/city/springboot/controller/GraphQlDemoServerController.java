package com.sample.city.springboot.controller;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;

import org.springframework.stereotype.Controller;

@GraphQLApi
@Controller
public class GraphQlDemoServerController {

    @GraphQLQuery(name = "getSquare")
    public Integer getSquare(Integer input) {
        return input * input;
    }
}
