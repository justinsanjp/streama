# Streama Extension & SSL API

This document describes the administrative APIs for SSL certificate management, plugin and theme marketplace access, and the extension runtime contracts used by plugins and themes.

## Table of contents
- [SSL certificate management](#ssl-certificate-management)
- [Marketplace](#marketplace)
- [Plugin runtime API](#plugin-runtime-api)
- [Theme API](#theme-api)
- [Security considerations](#security-considerations)

## SSL certificate management
Endpoints under `/ssl` allow administrators to configure Let's Encrypt / certbot integration for Streama.

### `GET /ssl/config`
Returns the persisted SSL configuration, including the current status of the latest issuance or renewal attempt.

### `POST /ssl/request`
Creates or updates the SSL configuration and triggers a dry-run request. The payload accepts:

```json
{
  "domainName": "streama.example.com",
  "email": "admin@example.com",
  "autoRenew": true,
  "staging": false
}
```

* `domainName` and `email` are required.
* When successful the service provisions a placeholder certificate bundle at `config/certs/<domain>.pem` so the server can be wired to that path.

### `POST /ssl/renew`
Marks a renewal attempt and updates the status timestamp. Renewal reuses the stored configuration.

## Marketplace
The marketplace surfaces curated extensions from a bundled manifest (`src/main/resources/marketplace/default-manifest.json`) and
falls back to `docs/marketplace/manifest.json` when present on disk.

### `GET /marketplace/manifest`
Returns the manifest with two arrays: `plugins` and `themes`. Each entry contains `name`, `author`, `version`, `description`, `download_url`, and `screenshots`.

### `GET /marketplace/installed`
Returns locally installed extensions and their enabled/disabled state.

### `POST /marketplace/install`
Installs or updates an extension from the manifest or any compatible payload:

```json
{
  "name": "Darkflix Theme",
  "author": "Lena Beispiel",
  "version": "1.0.3",
  "description": "Dark cinematic UI",
  "downloadUrl": "https://themes.example.com/darkflix-theme.zip",
  "type": "THEME",
  "capabilities": ["ui:palette"],
  "dependencies": [],
  "screenshots": ["https://themes.example.com/screenshots/darkflix1.png"],
  "config": {"primaryColor": "#141414"}
}
```

### `PUT /marketplace/{id}/state`
Toggle an installed extension. Payload: `{ "enabled": true }`.

## Plugin runtime API
Plugins register themselves and declare the capabilities they expose. Capabilities cover user-requested features like paid subscriptions, multi-language, announcements, content-creator dashboards, better user management, SSO, webhooks, and mobile API bridges.

### Supported capability keys
- `billing:stripe` (paid subscription with Stripe)
- `i18n:multilanguage`
- `engagement:announcements`
- `creator:dashboard`
- `creator:monetization` (requires `creator:dashboard` dependency)
- `users:management`
- `auth:social` (Google/Facebook/Discord/Apple/etc.)
- `hooks:webhook`
- `mobile:bridge`

### `POST /api/v1/plugins/register`
Registers a plugin and validates declared dependencies.

```json
{
  "name": "Premium Billing",
  "author": "Example Devs",
  "version": "1.0.0",
  "downloadUrl": "https://plugins.example.com/premium-billing.zip",
  "capabilities": ["billing:stripe", "users:management"],
  "dependencies": ["creator:dashboard"],
  "webhooks": [{"eventType": "subscription.created", "url": "https://example.com/webhooks/streama"}]
}
```

The response contains the persisted extension and a computed `dependencyState` that reports missing capabilities.

### `GET /api/v1/plugins/capabilities`
Returns a merged view of all registered capabilities, grouped by plugin. Use this to dynamically enable UI elements (e.g., paywalls, announcement banners, creator analytics) only when supported.

### `POST /api/v1/plugins/webhooks`
Adds or updates webhook registrations for installed plugins. Payload matches the `webhooks` array in the register call. Webhooks are stored and can later be used by background jobs to call out to external systems.

## Theme API

### `GET /api/v1/themes/active`
Returns the active theme selection and custom palette.

### `POST /api/v1/themes/activate`
Activate a theme (from the marketplace) and store custom UI settings:

```json
{
  "name": "Darkflix Theme",
  "primaryColor": "#141414",
  "accentColor": "#e50914",
  "background": "https://themes.example.com/bg.png",
  "allowCustomLogos": true
}
```

### `GET /api/v1/themes/manifest`
Returns only the `themes` array from the marketplace manifest for quick lookups inside the UI.

### Customization
Additional customizations are persisted under the `theme_customization` setting:

```json
{
  "primaryColor": "#141414",
  "accentColor": "#e50914",
  "background": "#000000",
  "buttonShape": "rounded",
  "typography": "sans-serif"
}
```

## Security considerations
- All endpoints validate required fields and reject unsafe download URLs (only `http`/`https` are allowed).
- Dependency validation ensures monetization plugins are only enabled when their prerequisite `creator:dashboard` capability is present.
- SSL settings are stored without private key material and renewal timestamps are tracked to audit certificate lifecycles.
- Webhook URLs are stored with explicit event scoping to reduce the blast radius of misconfiguration.
