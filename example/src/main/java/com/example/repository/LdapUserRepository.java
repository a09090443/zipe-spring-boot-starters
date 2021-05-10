package com.example.repository;

import com.example.model.LdapUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Gary.Tsai
 */
@Repository
public interface LdapUserRepository extends JpaRepository<LdapUser, String> {

	LdapUser findByUserId(String userId);
}
