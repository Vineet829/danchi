import type { Metadata } from "next";
import AuthExperience from "@/components/auth/AuthExperience";

export const metadata: Metadata = { title: "Create your account | Danchi", description: "Create an account for your building's Danchi community." };

export default function RegisterPage() {
	return <AuthExperience mode="register" />;
}
