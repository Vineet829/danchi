import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import { IssueContext } from "@/components/estate/FormContexts";
import FormShell from "@/components/estate/FormShell";
import UpdateIssueForm from "@/components/forms/update-issue/UpdateIssueForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Update issue | Danchi",
	description: "Technicians assigned to an issue can update the status of the issue",
};

interface UpdateParamsProps {
	params: {
		id: string;
	};
}

function UpdateIssuePage({ params }: UpdateParamsProps) {
	return (
		<FormShell
			eyebrow="Assigned to you"
			title={
				<>
					Update the <em>issue</em>
				</>
			}
			description="Change the status as the work goes on."
			back={{ href: `/issue/${params.id}`, label: "Back to the issue" }}
			aside={<IssueContext id={params.id} />}
		>
			<UpdateIssueForm params={params} />
		</FormShell>
	);
}

export default async function ProtectedPage({ params }: { params: Promise<UpdateParamsProps["params"]> }) {
	const props = { params: await params };
	return <ProtectedRoute><UpdateIssuePage {...props} /></ProtectedRoute>;
}
