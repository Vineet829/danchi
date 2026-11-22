import { format, formatDistanceToNow, isValid, parseISO } from "date-fns";

function toDate(value?: string | null) {
	if (!value) return null;
	const parsed = parseISO(value);
	if (isValid(parsed)) return parsed;
	const fallback = new Date(value);
	return isValid(fallback) ? fallback : null;
}

export function timeAgo(value?: string | null) {
	const date = toDate(value);
	return date ? formatDistanceToNow(date, { addSuffix: true }) : "";
}

export function shortDate(value?: string | null) {
	const date = toDate(value);
	return date ? format(date, "d MMM yyyy") : "";
}

export function longDate(value?: string | null) {
	const date = toDate(value);
	return date ? format(date, "d MMMM yyyy") : "";
}

export function monthYear(value?: string | null) {
	const date = toDate(value);
	return date ? format(date, "MMMM yyyy") : "";
}

export function plural(count: number | undefined, one: string, many = `${one}s`) {
	const total = count ?? 0;
	return `${total} ${total === 1 ? one : many}`;
}

export function voteScore({ upvotes = 0, downvotes = 0 }: { upvotes?: number; downvotes?: number }) {
	return upvotes - downvotes;
}

export function tagSlug(name: string) {
	return name
		.normalize("NFKD")
		.replace(/\p{M}/gu, "")
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/(^-|-$)/g, "");
}

const UPPERCASE = new Set(["hvac"]);

export function label(value?: string | null) {
	if (!value) return "";
	if (UPPERCASE.has(value.toLowerCase())) return value.toUpperCase();
	const text = value.replace(/_/g, " ");
	return text.charAt(0).toUpperCase() + text.slice(1);
}

export function joined(parts: Array<string | number | null | undefined>, separator = ", ") {
	return parts.filter((part) => part !== null && part !== undefined && `${part}`.trim() !== "").join(separator);
}
