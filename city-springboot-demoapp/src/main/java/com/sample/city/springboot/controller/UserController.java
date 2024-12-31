package com.sample.city.springboot.controller;

import com.sample.city.springboot.model.User;
import com.sample.city.springboot.service.UserService;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;

import lombok.extern.slf4j.Slf4j;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@GraphQLApi
@Controller
@Slf4j
public class UserController {

    @Autowired private UserService userService;

    @GraphQLQuery(name = "saveUser")
    public User saveUser(User user) throws Exception {
        log.debug("saveUser() request received with user '{}'", user);
        Thread.sleep(3000);
        // here you will observe same thread getting hold for request to complete
        return userService.saveUser(user);
    }

    @GraphQLQuery(name = "saveUserT")
    public Mono<User> saveUserT(User user) {
        log.debug("saveUserT() request received with user '{}'", user);
        return Mono.just(userService.saveUser(user)).map(u -> u);
    }

    @GraphQLQuery(name = "saveUserR")
    public Mono<User> saveUserReactive(User user) {
        log.debug("saveUserR() request received with user '{}'", user);

        return Mono.just(userService.saveUser(user))
                .delayElement(
                        Duration.ofSeconds(
                                3)) // because of this you will see different threads in logs and
                // subscription happening immediately while doOnNext getting
                // called after 3 seconds delay with different threads
                .doOnSubscribe(subscription -> log.info("Subscribed to Mono for : {}", user))
                .doOnNext(u -> log.info("User emitted: {}", u))
                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()))
                .doOnTerminate(() -> log.info("Mono processing terminated for : {}", user));
    }

    @GraphQLQuery(name = "saveUserFlux")
    public Publisher<String> saveUserFlux(User user) {
        log.debug("saveUserR() request received with user '{}'", user);

        return Flux.interval(Duration.ofSeconds(1)) // Simulate emitting values every second
                .map(i -> "Event #" + i)
                .takeUntil(s -> s.endsWith("5"))
                //                .delayElement(
                //                        Duration.ofSeconds(
                //                                3)) // because of this you will see different
                // threads in logs and
                // subscription happening immediately while doOnNext getting
                // called after 3 seconds delay with different threads
                .doOnSubscribe(subscription -> log.info("Subscribed to Mono for : {}", user))
                .doOnNext(u -> log.info("User emitted: {}", u))
                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()))
                .doOnTerminate(() -> log.info("Mono processing terminated for : {}", user));

        /*
        This will emit response once the flux is completed
        i.e {
              "data": {
                "saveUserFlux": [
                  "Event #0",
                  "Event #1",
                  "Event #2",
                  "Event #3",
                  "Event #4",
                  "Event #5"
                ]
              }
            }
         */
    }

    @GraphQLQuery(name = "saveUserRC")
    public Mono<User> saveUserReactiveCallable(User user) {
        log.debug("saveUserRC() request received with user '{}'", user);

        return Mono.fromCallable(
                        () -> {
                            log.info("Callable for saving execution started");
                            Thread.sleep(3000);
                            return userService.saveUser(user);
                        })
                .subscribeOn(Schedulers.boundedElastic())

                // because of this you will see that now even the save is getting called
                // asynchronously
                // instead of request handling thread calling itself
                .doOnSubscribe(subscription -> log.info("Subscribed to Mono for : {}", user))
                .doOnNext(u -> log.info("User emitted: {}", u))
                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()))
                .doOnTerminate(() -> log.info("Mono processing terminated for : {}", user));
    }

    @GraphQLSubscription(name = "getEvents")
    public Flux<String> getEvents() {
        log.debug("getEvents() request received");
        return Flux.interval(Duration.ofSeconds(1)) // Simulate emitting values every second
                .map(i -> "Event #" + i);
    }
}
