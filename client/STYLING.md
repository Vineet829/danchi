# Frontend styling

Styling is Tailwind CSS 3. The shared values (palette, scoped theme variables,
fonts, radii, shadows, animations, breakpoints) are in `tailwind.theme.ts`.
`tailwind.config.ts` extends the Tailwind config with them and registers the
variables for the community shell and its portalled surfaces.

- Use utility classes for one-off styling. Styles repeated across components
  go in the component stylesheets in `src/components/estate/styles/` with
  `@apply`.
- The homepage and auth pages use CSS modules, for local selector scoping,
  plus Tailwind utilities.
- The page content area is a named CSS container (`es-content`). Forms and
  page layouts use container queries on it, so they fit the width they get,
  including when the navigation is visible.
- Prefer the named theme utilities like `text-estate-text`,
  `rounded-estate-sm` and `animate-estate-page-in`. Exact pixel sizes,
  gradients, shorthand resets and zoom maths use arbitrary values. If you
  change a shared value, change it in the theme. Don't start a second palette.
- Runtime values (rating percentages, skeleton widths, animation indexes) are
  passed as CSS variables that Tailwind utilities read. Don't build class
  names from runtime numbers, because Tailwind only picks up complete class
  names.
- The occupation picker is react-select in unstyled mode, styled with Tailwind
  component classes. react-select still handles keyboard navigation and
  positioning. Vendor CSS such as React Toastify's animations stays with its
  package.
