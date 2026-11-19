package com.danchi.auth.dto;

import com.danchi.profile.Gender;
import com.danchi.profile.Occupation;
import com.danchi.profile.Profile;
import com.danchi.user.User;
import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        boolean isStaff,
        boolean isSuperuser,
        boolean isGuest,
        Instant guestExpiresAt,
        String email,
        String firstName,
        String lastName,
        String username,
        String fullName,
        String slug,
        Gender gender,
        Occupation occupation,
        String phoneNumber,
        String country,
        String city,
        int reputation,
        Instant dateJoined) {

    public static CurrentUserResponse from(User user, Profile profile) {
        return new CurrentUserResponse(
                user.getId(),
                user.isStaff(),
                user.isSuperuser(),
                user.isGuest(),
                user.getGuestExpiresAt(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getFullName(),
                profile.getSlug(),
                profile.getGender(),
                profile.getOccupation(),
                profile.getPhoneNumber(),
                profile.getCountryOfOrigin(),
                profile.getCityOfOrigin(),
                profile.getReputation(),
                user.getDateJoined());
    }
}
