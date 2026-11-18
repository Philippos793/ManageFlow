# ManageFlow Frontend

React 19 + Vite single-page application for ManageFlow. See the [root README](../README.md) for the full project overview, backend setup, and Docker instructions.

## Setup

```bat
copy .env.example .env
npm install
npm run dev
```

`VITE_API_URL` in `.env` must point to the backend, e.g. `http://localhost:8080`.

## Scripts

| Command | Purpose |
| --- | --- |
| `npm run dev` | Start the Vite dev server on `http://localhost:5173`. |
| `npm test` | Run the Vitest + React Testing Library suite. |
| `npm run lint` | Run ESLint. |
| `npm run build` | Create a production build in `dist/`. |

## Structure

- `src/pages` — route-level pages (employees, departments, tasks, work hours, login, onboarding).
- `src/components` — feature components and modals; `src/components/ui` holds shared UI primitives.
- `src/services` — API clients that attach the JWT and normalize errors.
- `src/hooks` — reusable hooks.
- `src/utils` — error helpers.

## Production image

The `Dockerfile` builds the app and serves it with Nginx. `nginx.conf` adds SPA routing and security headers, including a Content-Security-Policy whose `connect-src` is limited to the configured `VITE_API_URL`.
