package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.UserProfileUpdateRequest;
import com.fibiyo.ecommerce.application.dto.UserResponse;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T14:16:27+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse userResponse = new UserResponse();

        userResponse.setActive( user.isActive() );
        userResponse.setAuthProvider( user.getAuthProvider() );
        userResponse.setAvatar( user.getAvatar() );
        userResponse.setCreatedAt( user.getCreatedAt() );
        userResponse.setEmail( user.getEmail() );
        userResponse.setFirstName( user.getFirstName() );
        userResponse.setId( user.getId() );
        userResponse.setLastName( user.getLastName() );
        userResponse.setLoyaltyPoints( user.getLoyaltyPoints() );
        userResponse.setSubscriptionExpiryDate( user.getSubscriptionExpiryDate() );
        userResponse.setSubscriptionType( user.getSubscriptionType() );
        userResponse.setUpdatedAt( user.getUpdatedAt() );
        userResponse.setUsername( user.getUsername() );

        userResponse.setRoles( user.getRole() != null ? List.of("ROLE_" + user.getRole().name()) : Collections.emptyList() );
        userResponse.setImageGenQuota( user.getRole() == com.fibiyo.ecommerce.domain.enums.Role.SELLER ? user.getImageGenQuota() : null );

        return userResponse;
    }

    @Override
    public List<UserResponse> toUserResponseList(List<User> users) {
        if ( users == null ) {
            return null;
        }

        List<UserResponse> list = new ArrayList<UserResponse>( users.size() );
        for ( User user : users ) {
            list.add( toUserResponse( user ) );
        }

        return list;
    }

    @Override
    public void updateUserFromProfileRequest(UserProfileUpdateRequest request, User user) {
        if ( request == null ) {
            return;
        }

        if ( request.getFirstName() != null ) {
            user.setFirstName( request.getFirstName() );
        }
        if ( request.getLastName() != null ) {
            user.setLastName( request.getLastName() );
        }
    }
}
