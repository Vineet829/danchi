import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import FormShell, { FormContext } from "@/components/estate/FormShell";
import ApartmentCreateForm from "@/components/forms/apartment/ApartmentCreateForm";
import { Wrench } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Add your apartment | Danchi",
	description: "Authenticated users can add their apartment details",
};

function AddApartmentPage() {
	return (
		<FormShell
			eyebrow="Your space"
			title={
				<>
					Add your <em>apartment</em>
				</>
			}
			description="This shows your neighbours and the technicians where you live."
			back={{ href: "/profile", label: "Back to profile" }}
			aside={
				<FormContext icon={<Wrench strokeWidth={1.75} />} label="Good to know">
					You need an apartment on your profile to report an issue.
				</FormContext>
			}
		>
			<ApartmentCreateForm />
		</FormShell>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><AddApartmentPage /></ProtectedRoute>;
}
