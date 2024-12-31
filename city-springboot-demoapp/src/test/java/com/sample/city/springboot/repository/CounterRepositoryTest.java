package com.sample.city.springboot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sample.city.springboot.model.Counter;
import com.sample.city.springboot.service.CounterService;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.IntStream;

@SpringBootTest
@ActiveProfiles("local")
public class CounterRepositoryTest {

    @Autowired CounterRepository counterRepository;
    @Autowired CounterService counterService;

    @Test
    public void counterTest() {

        var counter = Instancio.create(Counter.class).withCount(1);
        counterRepository.save(counter);
        System.out.println("Before incremnent : " + counter.count());

        IntStream.range(0, 100) // Generate a range of integers from 0 to 99
                .parallel() // Enable parallel processing
                .forEach(
                        i -> {
                            counterService.incrementCounter(counter.id());
                        });

        var counterFromDb = counterRepository.findById(counter.id());
        assertThat(counterFromDb).isNotEmpty();
        assertThat(counterFromDb.get().count()).isEqualTo(101L);

        //        counterRepository.delete(counter);
    }
}
