import { useUserProfile } from "@/hooks/useUseProfile";

export function useDisplayName() {
	const { profile } = useUserProfile();

	return (username?: string) => {
		if (username && profile?.username === username && profile.full_name) return profile.full_name;
		return username ?? "";
	};
}
