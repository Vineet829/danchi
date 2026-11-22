import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import FormShell, { FormContext } from "@/components/estate/FormShell";
import EditProfileForm from "@/components/forms/profile/EditProfileForm";
import { UsersRound } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Edit profile | Danchi",
	description: "Signed in users can edit their profile information",
};

function EditProfilePage() {
	return (
		<FormShell
			eyebrow="Your space"
			title={
				<>
					Edit your <em>profile</em>
				</>
			}
			description="Change your photo and personal details."
			back={{ href: "/profile", label: "Back to profile" }}
			aside={
				<FormContext icon={<UsersRound strokeWidth={1.75} />} label="Who sees this">
					Anyone signed in can see these details.
				</FormContext>
			}
		>
			<EditProfileForm />
		</FormShell>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><EditProfilePage /></ProtectedRoute>;
}
