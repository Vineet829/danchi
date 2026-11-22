import ProtectedRoute from "@/components/shared/ProtectedRoutes";
import BookmarkedPostCard from "@/components/cards/BookmarkedPostCard";
import type { Metadata } from "next";

export const metadata: Metadata = {
	title: "Bookmarks | Danchi",
	description: "Authenticated users can see the posts they've bookmarked",
};


function BookmarkedPostsPage() {
	return (
		<>
			<BookmarkedPostCard />
		</>
	);
}

export default function ProtectedPage() {
	return <ProtectedRoute><BookmarkedPostsPage /></ProtectedRoute>;
}
