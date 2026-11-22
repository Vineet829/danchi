export function safeLoginDestination(value: string | null): string {
	if (!value || !value.startsWith("/") || value.startsWith("//")) return "/welcome";
	if (Array.from(value).some(char => char === "\\" || char.charCodeAt(0) <= 32)) return "/welcome";
	const target = new URL(value, "https://danchi.invalid");
	return target.origin === "https://danchi.invalid" && !["/login", "/register"].includes(target.pathname)
		? `${target.pathname}${target.search}${target.hash}` : "/welcome";
}

export function loginDestination(): string {
	return safeLoginDestination(typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("next"));
}
