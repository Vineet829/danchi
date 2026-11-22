import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import FormShell, { FormSteps } from "@/components/estate/FormShell";
import CreatePostForm from "@/components/forms/add-post/CreatePostForm";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Create a post | Danchi",
	description: "Authenticated users can ask any question or post some content for all users to see",
};

const STEPS = [
	{ title: "Write a clear title", text: "People see it first in the list of posts." },
	{ title: "Add the details", text: "Say where and when, if it matters." },
	{ title: "Add tags", text: "One or two tags are enough." },
];

function AddPostPage() {
	return (
		<FormShell
			eyebrow="Community"
			title={
				<>
					Write a <em>post</em>
				</>
			}
			description="Ask a question or share a notice with your neighbours."
			back={{ href: "/welcome", label: "Back to community" }}
			aside={<FormSteps steps={STEPS} label="Writing a good post" />}
		>
			<CreatePostForm />
		</FormShell>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><AddPostPage /></ProtectedRoute>;
}
