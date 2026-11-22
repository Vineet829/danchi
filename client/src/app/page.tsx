import type { Metadata } from "next";
import HomeContent from "@/components/home/HomeContent";

export const metadata: Metadata = {
	title: "Danchi | Community app for apartment buildings",
	description:
		"Danchi is a community app for apartment buildings. Tenants report repairs and share posts with their neighbours.",
};

export default function HomePage() {
	return <HomeContent />;
}
