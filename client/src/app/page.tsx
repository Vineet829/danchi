import type { Metadata } from "next";
import HomeContent from "@/components/home/HomeContent";

export const metadata: Metadata = {
	title: "Danchi — Good neighbours. Better living.",
	description:
		"A little closer to home. Report maintenance issues, connect with your neighbours, and keep your apartment community together with Danchi.",
};

export default function HomePage() {
	return <HomeContent />;
}
