package com.fulltack.zooManagment.service;

import com.fulltack.zooManagment.Requests.UserRequest;
import com.fulltack.zooManagment.exception.ServiceException;
import com.fulltack.zooManagment.exception.UserNotFoundException;
import com.fulltack.zooManagment.generators.PDFGeneratorService;
import com.fulltack.zooManagment.model.User;
import com.fulltack.zooManagment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.io.ByteArrayInputStream;
import java.util.*;

@Service
@Primary
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository repository;

    @Autowired
    private PDFGeneratorService pdfGeneratorService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MongoTemplate mongoTemplate;

    public User convertToUser(UserRequest userRequest) {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString().split("-")[0]);
        user.setUsername(userRequest.getUsername());
        user.setName(userRequest.getName());
        user.setPhone(userRequest.getPhone());
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword());
        user.setRole(userRequest.getRole());
        return user;
    }

    public List<User> getAllUsers() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            throw new ServiceException("Error Occurred while fetching all Users", e);
        }
    }

    public User getUserByUserId(String userId) {
        try {
            User user = repository.findByUserId(userId);

            if (user == null) {
                throw new UserNotFoundException("User with username " + userId + " not found");
            }
            return user;
        } catch (Exception e) {
            throw new ServiceException("Error occurred while fetching specific user", e);
        }
    }

    public User getUserByUsername(String username) {
        try {
            User user = repository.findByUsername(username);

            if (user == null) {
                throw new UserNotFoundException("User with username " + username + " not found");
            }
            return user;
        } catch (Exception e) {
            throw new ServiceException("Error occurred while fetching specific user", e);
        }
    }

    //Redundant
    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            User userDetail = repository.findByUsername(username);
            return userDetail;
        } catch (Exception e) {
            throw new ServiceException("Error occurred while fetching specific user", e);
        }
    }


    public User addUser(UserRequest userRequest) {
        try {
            User user = convertToUser(userRequest);
            if (!repository.existsByUsername(user.getUsername().trim())) {
                if (user.getUsername() == null || user.getPassword() == null || user.getUserId() == null) {
                    throw new IllegalArgumentException("Username and Password must be valid.");
                }
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return repository.save(user);
        } catch (Exception e) {
            throw new ServiceException("Error occurred while adding a user", e);
        }
    }

    public boolean login(String username, String password) {
        try {
            if (repository.existsByUsername(username)) {
                User user = repository.findByUsername(username);
                boolean a = passwordEncoder.matches(password, user.getPassword());
                return user != null && a;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new ServiceException("Error occurred while login", e);
        }
    }

    public String deleteUserByUserId(String userId) {
        try {
            if (repository.existsByUserId(userId)) {
                repository.deleteByUserId(userId);
                return "User Deleted Successfully";
            } else {
                return "User doesn't exists";
            }
        } catch (Exception e) {
            throw new ServiceException("Error Occurred while Deleting User", e);
        }
    }

    public List<User> searchUser(String userId, String name, String username) {
        try {
            Query query = new Query();
            List<Criteria> criteria = new ArrayList<>();

            if (userId != null && !userId.isEmpty()) {
                criteria.add(Criteria.where("userId").regex(userId, "i"));
            }
            if (name != null && !name.isEmpty()) {
                criteria.add(Criteria.where("name").regex(name, "i"));
            }
            if (username != null && !username.isEmpty()) {
                criteria.add(Criteria.where("username").regex(username, "i"));
            }

            if (!criteria.isEmpty()) {
                query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
            }

            return mongoTemplate.find(query, User.class);
        } catch (Exception e) {
            throw new ServiceException("Error Searching User", e);
        }
    }

    public void updateUserByUserId(String userId, Map<String, Object> updates) {
        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update();
        updates.forEach((key, value) -> {
            if (!key.equals("id") && !key.equals("userId")) {
                update.set(key, value);
            }
        });
        mongoTemplate.findAndModify(query, update, User.class);
    }

    public ByteArrayInputStream generateUserPDF() {
        List<User> users = repository.findAll();
        return pdfGeneratorService.userReport(users);
    }
}