import { ArrowRight, Check, Home, MessageCircle, ShieldCheck, Wrench } from "lucide-react";
import { Instrument_Serif, Manrope } from "next/font/google";
import Image from "next/image";
import Link from "next/link";
import community from "@/../public/assets/images/tenant-community.jpg";
import styles from "./home.module.css";

const sans = Manrope({ subsets: ["latin"], variable: "--home-sans", display: "swap" });
const editorial = Instrument_Serif({
	subsets: ["latin"],
	weight: "400",
	style: ["normal", "italic"],
	variable: "--home-editorial",
	display: "swap",
	adjustFontFallback: false,
	fallback: ["Georgia", "serif"],
});

const features = [
	{
		number: "01",
		icon: Wrench,
		label: "MAINTENANCE",
		title: "Track your repairs",
		description: "See who's working on each one, and rate the technician when the job is done.",
	},
	{
		number: "02",
		icon: MessageCircle,
		label: "COMMUNITY",
		title: "Talk to your neighbours",
		description: "Write posts for the whole building and reply to other tenants.",
	},
	{
		number: "03",
		icon: ShieldCheck,
		label: "YOUR SPACE",
		title: "Your apartment details",
		description: "Add your apartment to your profile, and report a neighbour if there's a problem.",
	},
];

// Photo: malwina nogaj / Unsplash. Commercial use permitted by the Unsplash License.
// https://unsplash.com/photos/modern-apartment-buildings-with-glass-balconies-and-trees-YcoBpYHdqCg
export default function HomeContent() {
	return (
		<div className={`${styles.home} ${sans.variable} ${editorial.variable}`}>
			<a href="#home-main" className={styles.skipLink}>Skip to content</a>
			<div className={styles.container}>
				<header className={styles.header}>
					<Link href="/" className={styles.wordmark} aria-label="Danchi home">
						dan<span>chi</span><span className={styles.wordmarkDot}>.</span>
					</Link>
					<a href="#everyday" className={styles.headerExplore}>Features</a>
					<nav className={styles.navigation} aria-label="Main navigation">
						<Link href="/login" prefetch={false} className={styles.loginButton}>Log in <ArrowRight size={16} aria-hidden="true" /></Link>
					</nav>
				</header>

				<main id="home-main" className={styles.main} tabIndex={-1}>
					<section className={styles.hero} aria-labelledby="home-title">
						<div className={styles.heroCopy}>
							<p className={styles.eyebrow}><span aria-hidden="true" /> FOR APARTMENT BUILDINGS</p>
							<h1 id="home-title">Report a repair<br /><em>or post a notice.</em></h1>
							<p className={styles.introduction}>Made for tenants and building staff.</p>
							<p className={styles.description}>A technician updates each repair as they work, and neighbours can reply to your posts.</p>
							<div className={styles.heroActions}>
								<Link href="/register" prefetch={false} className={styles.primaryButton}>Create an account <ArrowRight size={19} aria-hidden="true" /></Link>
							</div>
							<div className={styles.heroNote}><Home size={17} strokeWidth={1.5} aria-hidden="true" /><span>You can also look around with a guest account.</span></div>
						</div>

						<div className={styles.visual}>
							<div className={styles.photoFrame}>
								<Image src={community} alt="Apartment balconies beside a leafy shared courtyard" fill priority placeholder="blur" sizes="(max-width: 912px) 92vw, (max-width: 1320px) 48vw, 720px" className={styles.photo} />
								<div className={styles.photoShade} />
								<div className={styles.photoCaption}><span>NEW POST</span><span>Water tank cleaning on Sunday</span></div>
							</div>
							<section className={styles.maintenanceCard} aria-labelledby="maintenance-title">
								<div className={styles.cardTop}><span className={styles.cardIcon}><Wrench size={17} strokeWidth={1.7} aria-hidden="true" /></span><span>REPAIR REQUEST</span></div>
								<h2 id="maintenance-title">Bathroom light not working</h2>
								<ol className={styles.progressSteps} aria-label="Maintenance request stages">
									{["Reported", "In progress", "Resolved"].map((label, index) => (
										<li key={label} className={styles.progressStep}>
											<div className={styles.stepTrack}><span className={styles.stepNode}><Check size={13} strokeWidth={2.5} aria-hidden="true" /></span>{index < 2 && <span className={styles.stepConnector} aria-hidden="true"><span /></span>}</div>
											<span className={styles.stepLabel}>{label}</span>
										</li>
									))}
								</ol>
								<p className={styles.cardNote}>You get an email when it&apos;s marked resolved.</p>
							</section>
						</div>
					</section>

					<section id="everyday" className={styles.features} aria-label="Features">
						{features.map(({ number, icon: Icon, label, title, description }) => (
							<article key={number} className={styles.feature}>
								<div className={styles.featureLabel}><Icon size={17} strokeWidth={1.7} aria-hidden="true" /><span>{label}</span><span className={styles.featureNumber}>{number}</span></div>
								<h2>{title}</h2>
								<p>{description}</p>
							</article>
						))}
					</section>
				</main>

				<footer className={styles.footer}>
					<p>© {new Date().getFullYear()} Danchi</p>
					<p>Good things start with good neighbours.</p>
					<Link href="/login" prefetch={false}>Already registered? <span>Log in <ArrowRight size={14} aria-hidden="true" /></span></Link>
				</footer>
			</div>
		</div>
	);
}
