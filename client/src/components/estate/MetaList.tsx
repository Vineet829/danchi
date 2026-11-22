import type { ReactNode } from "react";

export default function MetaList({ children, className = "" }: { children: ReactNode; className?: string }) {
	return (
		<span className={`es-meta ${className}`.trim()}>
			<span className="es-meta-row">{children}</span>
		</span>
	);
}
