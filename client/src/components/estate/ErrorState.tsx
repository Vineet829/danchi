"use client";

import { CloudOff, RotateCw } from "lucide-react";

type ErrorStateProps = {
	what: string;
	onRetry?: () => void;
	retrying?: boolean;
};

export default function ErrorState({ what, onRetry, retrying = false }: ErrorStateProps) {
	return (
		<div className="es-empty es-empty--error" role="alert">
			<span className="es-empty-icon" aria-hidden="true">
				<CloudOff strokeWidth={1.5} />
			</span>
			<p className="es-empty-title">We couldn&apos;t load {what}</p>
			<p className="es-empty-text">Check your connection and try again. If it keeps happening, the service may be down.</p>
			{onRetry && (
				<button type="button" className="es-btn es-btn--outline" onClick={onRetry} disabled={retrying}>
					{retrying ? <span className="es-spinner" aria-hidden="true" /> : <RotateCw aria-hidden="true" />}
					{retrying ? "Trying again..." : "Try again"}
				</button>
			)}
		</div>
	);
}
