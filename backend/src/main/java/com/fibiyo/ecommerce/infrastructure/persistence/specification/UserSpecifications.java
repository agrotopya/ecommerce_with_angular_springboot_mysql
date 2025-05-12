package com.fibiyo.ecommerce.infrastructure.persistence.specification;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils; // String kontrolü için
import java.util.ArrayList;
import java.util.List;



public class UserSpecifications {
    public static Specification<User> isActive(Boolean isActive) {
        return (root, query, cb) -> {
             if (isActive == null) return cb.conjunction();
             return cb.equal(root.get("isActive"), isActive);
         };
    }
   
    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> {
             if (role == null) return cb.conjunction();
             return cb.equal(root.get("role"), role);
         };
    }
   
    public static Specification<User> searchByTerm(String searchTerm) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(searchTerm)) { // StringUtils daha güvenli
                return cb.conjunction();
            }
            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            // Username, email, firstName, lastName içinde ara --  Hangi alanlarda arama yapılacağı
            Predicate usernameMatch = cb.like(cb.lower(root.get("username")), likePattern);
            Predicate emailMatch = cb.like(cb.lower(root.get("email")), likePattern);
            Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), likePattern);
            Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), likePattern);
   

                        // Bu alanları OR ile birleştir

            return cb.or(usernameMatch, emailMatch, firstNameMatch, lastNameMatch);
        };
    }
}