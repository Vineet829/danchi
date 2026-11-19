import Link from "next/link";

export default function Wordmark({ href = "/" }: { href?: string }) {
	return (
		<Link href={href} className="es-wordmark" aria-label="Danchi home">
			dan<span>chi</span>
			<span className="es-wordmark-dot">.</span>
		</Link>
	);
}
