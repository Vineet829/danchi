package com.danchi.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danchi.apartment.ApartmentRepository;
import com.danchi.issue.dto.IssueStatusUpdateRequest;
import com.danchi.mail.MailService;
import com.danchi.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IssueWriteServiceTest {

    @Test
    void resolvingTwiceSendsOneEmail() {
        var issues = mock(IssueRepository.class);
        var mail = mock(MailService.class);
        var issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setTitle("Tap");
        issue.setStatus(IssueStatus.REPORTED);
        User reporter = new User();
        reporter.setEmail("resident@example.com");
        issue.setReportedBy(reporter);
        User actor = new User();
        actor.setStaff(true);
        when(issues.findByExternalIdForUpdate(issue.getId())).thenReturn(Optional.of(issue));
        var service = new IssueWriteService(issues, mock(ApartmentRepository.class), mail);
        var update = new IssueStatusUpdateRequest(null, null, IssueStatus.RESOLVED);

        service.updateStatus(issue.getId(), update, actor);
        service.updateStatus(issue.getId(), update, actor);

        verify(mail, times(1)).send(eq("resident@example.com"), eq("Issue Resolved"), anyString());
        assertThat(issue.getResolvedOn()).isNotNull();
    }
}
