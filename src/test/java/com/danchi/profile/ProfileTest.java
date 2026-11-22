package com.danchi.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.danchi.user.User;
import org.junit.jupiter.api.Test;

class ProfileTest {

    @Test
    void reportsNeverBanStaff() {
        Profile profile = new Profile();
        profile.setReportCount(5);
        User resident = new User();
        User staff = new User();
        staff.setStaff(true);
        User superuser = new User();
        superuser.setSuperuser(true);
        assertThat(profile.isBanned(resident)).isTrue();
        assertThat(profile.isBanned(staff)).isFalse();
        assertThat(profile.isBanned(superuser)).isFalse();
        profile.setReportCount(4);
        assertThat(profile.isBanned(resident)).isFalse();
    }
}
