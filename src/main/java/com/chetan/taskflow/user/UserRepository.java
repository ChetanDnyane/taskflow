package com.chetan.taskflow.user;

// <editor-fold defaultstate="collapsed" desc="Account lookup and persistence">
/*
 * Spring Data generates this interface implementation and inherited CRUD operations for User IDs.
 * findByEmail returns Optional so callers decide how to handle absence; existsByEmail supports the
 * registration duplicate check. These are exact equality queries, so callers normalize email first.
 * Authentication and task ownership resolution both rely on the same persisted account identity.
 */
// </editor-fold>

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
