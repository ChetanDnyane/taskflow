package com.chetan.taskflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

// The implementation created by JPA for this class provides us the below free methods
//    save(user);
//    findById(id);
//    findAll();
//    existsById(id);
//    deleteById(id);
//    count();

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
