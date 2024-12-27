package com.sample.city.springboot.service;

import com.sample.city.springboot.model.User;
import com.sample.city.springboot.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private  UserRepository userRepository;

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
