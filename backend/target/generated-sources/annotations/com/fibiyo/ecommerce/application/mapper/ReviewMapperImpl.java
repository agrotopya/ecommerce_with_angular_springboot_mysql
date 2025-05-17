package com.fibiyo.ecommerce.application.mapper;

import com.fibiyo.ecommerce.application.dto.ReviewRequest;
import com.fibiyo.ecommerce.application.dto.ReviewResponse;
import com.fibiyo.ecommerce.domain.entity.Product;
import com.fibiyo.ecommerce.domain.entity.Review;
import com.fibiyo.ecommerce.domain.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-17T11:59:19+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250508-0718, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewResponse toReviewResponse(Review review) {
        if ( review == null ) {
            return null;
        }

        ReviewResponse reviewResponse = new ReviewResponse();

        reviewResponse.setProductId( reviewProductId( review ) );
        reviewResponse.setProductName( reviewProductName( review ) );
        reviewResponse.setCustomerId( reviewCustomerId( review ) );
        reviewResponse.setCustomerUsername( reviewCustomerUsername( review ) );
        reviewResponse.setApproved( review.isApproved() );
        reviewResponse.setComment( review.getComment() );
        reviewResponse.setCreatedAt( review.getCreatedAt() );
        reviewResponse.setId( review.getId() );
        reviewResponse.setRating( review.getRating() );

        return reviewResponse;
    }

    @Override
    public List<ReviewResponse> toReviewResponseList(List<Review> reviews) {
        if ( reviews == null ) {
            return null;
        }

        List<ReviewResponse> list = new ArrayList<ReviewResponse>( reviews.size() );
        for ( Review review : reviews ) {
            list.add( toReviewResponse( review ) );
        }

        return list;
    }

    @Override
    public Review toReview(ReviewRequest reviewRequest) {
        if ( reviewRequest == null ) {
            return null;
        }

        Review review = new Review();

        review.setComment( reviewRequest.getComment() );
        review.setRating( reviewRequest.getRating() );

        return review;
    }

    private Long reviewProductId(Review review) {
        if ( review == null ) {
            return null;
        }
        Product product = review.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String reviewProductName(Review review) {
        if ( review == null ) {
            return null;
        }
        Product product = review.getProduct();
        if ( product == null ) {
            return null;
        }
        String name = product.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long reviewCustomerId(Review review) {
        if ( review == null ) {
            return null;
        }
        User customer = review.getCustomer();
        if ( customer == null ) {
            return null;
        }
        Long id = customer.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String reviewCustomerUsername(Review review) {
        if ( review == null ) {
            return null;
        }
        User customer = review.getCustomer();
        if ( customer == null ) {
            return null;
        }
        String username = customer.getUsername();
        if ( username == null ) {
            return null;
        }
        return username;
    }
}
