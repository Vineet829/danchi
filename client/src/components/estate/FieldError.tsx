import { CircleAlert } from "lucide-react";
import type { ReactNode } from "react";

export default function FieldError({ id, children }: { id?: string; children: ReactNode }) {
	return (
		<p className="es-field-error" id={id} role="alert">
			<CircleAlert aria-hidden="true" />
			<span>{children}</span>
		</p>
	);
}
