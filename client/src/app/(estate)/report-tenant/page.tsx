import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import FormShell, { FormContext } from "@/components/estate/FormShell";
import CreateReportForm from "@/components/forms/report-tenant/CreateReportForm";
import { ShieldCheck } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Report a tenant | Danchi",
	description: "Tenants can report their fellow tenants in cases of misconduct or misbehaviour",
};

function ReportTenantPage() {
	return (
		<FormShell
			eyebrow="Get help"
			panelSize="large"
			title={
				<>
					Report a <em>tenant</em>
				</>
			}
			description="Tell the management about a problem with a neighbour."
			back={{ href: "/profile", label: "Back to profile" }}
			aside={
				<FormContext icon={<ShieldCheck strokeWidth={1.75} />} label="Anonymous">
					Only the management can see who sent a report.
				</FormContext>
			}
		>
			<CreateReportForm />
		</FormShell>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><ReportTenantPage /></ProtectedRoute>;
}
