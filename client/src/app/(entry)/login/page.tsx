import type { Metadata } from "next";
import AuthExperience from "@/components/auth/AuthExperience";

export const metadata: Metadata = { title: "Log in — Danchi", description: "Welcome home. Log in to your Danchi community." };

export default function LoginPage() {
	return <AuthExperience mode="login" />;
}
