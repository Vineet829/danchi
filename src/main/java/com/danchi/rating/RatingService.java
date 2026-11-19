package com.danchi.rating;

import com.danchi.profile.Occupation;
import com.danchi.profile.Profile;
import com.danchi.profile.ProfileRepository;
import com.danchi.rating.dto.RatingCreateRequest;
import com.danchi.user.User;
import com.danchi.user.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RatingService {

    private static final Set<Occupation> TECHNICIANS = EnumSet.of(
            Occupation.CARPENTER,
            Occupation.ELECTRICIAN,
            Occupation.PLUMBER,
            Occupation.HVAC,
            Occupation.MASON,
            Occupation.ROOFER,
            Occupation.PAINTER);

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public RatingService(
            RatingRepository ratingRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Rating create(RatingCreateRequest request, User ratingUser) {
        // A guest's rating would count against a real person's work, as a guest's report doesn't.
        if (ratingUser.isGuest()) {
            throw new AccessDeniedException("Guests can't rate technicians. Sign up to rate their work.");
        }
        User ratedUser = userRepository.findByUsername(request.ratedUserUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with username '" + request.ratedUserUsername() + "' does not exist."));

        if (ratedUser.getPkid().equals(ratingUser.getPkid())) {
            throw new AccessDeniedException("You cannot review yourself.");
        }

        Occupation raterOccupation = occupationOf(ratingUser);
        Occupation ratedOccupation = occupationOf(ratedUser);

        if (raterOccupation == Occupation.TENANT && ratedOccupation == Occupation.TENANT) {
            throw new AccessDeniedException("A tenant cannot review another tenant.");
        }
        if (raterOccupation == Occupation.TENANT && !TECHNICIANS.contains(ratedOccupation)) {
            throw new AccessDeniedException(
                    "A tenant can only review technicians and not other tenants!");
        }
        if (raterOccupation != Occupation.TENANT && ratedOccupation != Occupation.TENANT) {
            throw new AccessDeniedException("A technician cannot review another technician.");
        }
        if (raterOccupation != Occupation.TENANT) {
            throw new AccessDeniedException("Only tenants can review technicians.");
        }

        if (ratingRepository.existsByRatedUserPkidAndRatingUserPkid(
                ratedUser.getPkid(), ratingUser.getPkid())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You have already rated this user.");
        }

        Rating rating = new Rating();
        rating.setRatedUser(ratedUser);
        rating.setRatingUser(ratingUser);
        rating.setRating(request.rating());
        rating.setComment(request.comment() == null ? "" : request.comment());
        return ratingRepository.save(rating);
    }

    private Occupation occupationOf(User user) {
        return profileRepository.findByUserPkid(user.getPkid())
                .map(Profile::getOccupation)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Both users must have a valid occupation."));
    }
}
