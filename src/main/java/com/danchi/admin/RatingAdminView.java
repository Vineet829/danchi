package com.danchi.admin;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "ratings", layout = AdminLayout.class)
@PageTitle("Ratings | Danchi Admin")
@RolesAllowed({"STAFF", "SUPERUSER"})
public class RatingAdminView extends AdminCrudView {
    public RatingAdminView(
            AdminQueryService queries,
            AdminRecordService writes,
            AdminDeleteService deletes,
            AdminPermissions permissions,
            AdminAuditService audit) {
        super(AdminResource.RATINGS, queries, writes, deletes, permissions, audit);
    }
}
