import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import { TechnicianContext } from "@/components/estate/FormContexts";
import FormShell from "@/components/estate/FormShell";
import CreateRatingForm from "@/components/forms/add-rating/CreateRatingForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Rate a technician | Danchi",
	description:
		"Tenants can rate the technicians, if they're satisfied or dissatisfied with the services rendered to them",
};

function AddRatingPage() {
	return (
		<FormShell
			eyebrow="Technicians"
			title={
				<>
					Rate a <em>technician</em>
				</>
			}
			description="Give one to five stars and say how the job went."
			back={{ href: "/technicians", label: "Back to technicians" }}
			aside={<TechnicianContext />}
		>
			<CreateRatingForm />
		</FormShell>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><AddRatingPage /></ProtectedRoute>;
}
