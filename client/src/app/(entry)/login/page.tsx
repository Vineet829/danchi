import type { Metadata } from "next";
import AuthExperience from "@/components/auth/AuthExperience";

export const metadata: Metadata = { title: "Log in | Danchi", description: "Log in to Danchi." };

export default function LoginPage() {
	return <AuthExperience mode="login" />;
}
