# Zindigi Corporate — Mobile App APIs (NEW: Signup & Entity Onboarding)

**For:** Mobile team — add these screens/APIs to the **existing** KYC app.  
**Out of scope here:** personal KYC (`/api/public/app-kyc/*`) — keep your current integration unchanged.

After entity **submit**, call the bridge endpoint below, then continue with your **existing** personal KYC flow using the returned `sessionToken`.

| Item | Value |
|------|--------|
| Local base URL | `http://localhost:8090` |
| Typical server | `http://<host>:8150` (or legacy `:8050`) |
| Auth for these APIs | Portal JWT from verify-otp / email login |
| Postman (this flow) | `postman/Zindigi-Corporate-Signup-Onboarding.postman_collection.json` |

---

## Flow (new only)

```
Sign up → Email OTP → Entity onboarding → Documents → Submit
    → POST /api/onboarding/start-personal-kyc  →  sessionToken
    → [EXISTING personal KYC — do not change]
```

---

## Auth token

| Token | Field | When | Header |
|-------|-------|------|--------|
| **Portal JWT** | `token` | After `verify-otp` or portal `login` | `Authorization: Bearer <token>` |

Errors: `{ "error": "…" }` or `{ "message": "…" }` with HTTP 4xx/5xx.

---

## 1. `GET /api/entity-types` (public)

**Response `200`**

```json
[
  { "code": "SOLE_PROPRIETORSHIP", "label": "Sole Proprietorship" },
  { "code": "SMALL_BUSINESS", "label": "Small business / freelance profession" },
  { "code": "PARTNERSHIP", "label": "Partnership" },
  { "code": "LLP", "label": "Limited Liability Partnership (LLP)" }
]
```

---

## 2. `GET /api/party-types` (public)

**Response `200`**

```json
[
  { "code": "MERCHANT", "label": "Corporate (Master Wallet)" }
]
```

---

## 3. `POST /api/auth/signup` (public)

**Request**

```json
{
  "partyType": "MERCHANT",
  "fullName": "Nazish Daud",
  "businessName": "Nazish Clothing",
  "email": "nazish.daud@zindigi.pk",
  "phone": "03152728200"
}
```

**Response `200`**

```json
{
  "message": "OTP sent to email",
  "email": "nazish.daud@zindigi.pk",
  "partyPublicId": "a1b2c3d4-…",
  "trackingId": "DFS-20261008-183268",
  "partyType": "MERCHANT",
  "devOtpHint": "378996"
}
```

- `devOtpHint` when mail is off / fallback (also in backend `[MAIL-FALLBACK]` logs).
- Same email still in draft → OTP **resent** (`"resent": true`) instead of conflict.

**Error `409`**

```json
{ "error": "Email already registered" }
```

---

## 4. `POST /api/auth/verify-otp` (public)

**Request**

```json
{
  "email": "nazish.daud@zindigi.pk",
  "code": "378996"
}
```

**Response `200`** — save `token` as portal JWT

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9…",
  "partyStatus": "DRAFT",
  "partyType": "MERCHANT",
  "partyPublicId": "a1b2c3d4-…",
  "trackingId": "DFS-20261008-183268",
  "role": "PARTY_USER"
}
```

---

## 5. `POST /api/auth/login` (public)

Use after entity submit (email + temporary password from email/logs). Not for draft signup (use OTP).

**Request**

```json
{
  "email": "nazish.daud@zindigi.pk",
  "password": "FHbZuJ8286a8"
}
```

**Response `200`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9…",
  "role": "PARTY_USER",
  "portalRoles": ["PARTY_ADMIN", "MAKER"],
  "partyStatus": "SUBMITTED",
  "partyType": "MERCHANT",
  "partyPublicId": "a1b2c3d4-…",
  "fullName": "Nazish Daud",
  "firstLogin": true
}
```

Then call `start-personal-kyc` (section 11) to resume personal KYC.

---

## 6. `GET /api/onboarding/me`  
**Auth:** portal JWT

**Response `200`** (abbreviated)

```json
{
  "id": 31,
  "publicId": "a1b2c3d4-…",
  "trackingId": "DFS-20261008-183268",
  "partyType": "MERCHANT",
  "status": "DRAFT",
  "onboardingStep": 1,
  "fullName": "Nazish Daud",
  "businessName": "Nazish Clothing",
  "entityType": "SOLE_PROPRIETORSHIP",
  "partnershipUnregistered": false,
  "applicantIsPartner": true,
  "email": "nazish.daud@zindigi.pk",
  "phone": "03152728200",
  "registeredAddress": null,
  "natureOfBusiness": null,
  "purposeOfAccount": null,
  "termsAccepted": false,
  "canSubmit": false,
  "requiredDocuments": [
    {
      "documentCode": "AUTH_ID_FRONT",
      "documentLabel": "Authorized person ID — front",
      "mandatory": true,
      "uploaded": false
    }
  ],
  "documents": [],
  "associatedPersons": [],
  "partnerAppUsers": [],
  "partnerKycTotal": 0,
  "partnerKycCompleted": 0,
  "tatWorkingDays": 5
}
```

Use `requiredDocuments` for uploads. Enable Submit when `canSubmit === true`.

---

## 7. `PUT /api/onboarding/profile`  
**Auth:** portal JWT

**Request**

```json
{
  "fullName": "Nazish Daud",
  "businessName": "Nazish Clothing",
  "entityType": "SOLE_PROPRIETORSHIP",
  "partnershipUnregistered": false,
  "applicantIsPartner": true,
  "incorporationNumber": "",
  "incorporationDate": "2020-01-15",
  "incorporationCountry": "Pakistan",
  "incorporationAuthority": "SECP",
  "ntnNumber": "",
  "taxCountry": "Pakistan",
  "registeredAddress": "Street 1, Lahore",
  "mailingAddress": "",
  "placeOfBusiness": "",
  "city": "Lahore",
  "country": "Pakistan",
  "phone": "03152728200",
  "natureOfBusiness": "Retail clothing",
  "purposeOfAccount": "Business collections",
  "intendedRelationship": "",
  "termsAccepted": true,
  "onboardingStep": 3
}
```

Rules:
- Sole prop / small business → `applicantIsPartner: true`, `onboardingStep: 3` (docs next).
- Partnership / LLP → `onboardingStep: 2` (partners next).

**Response `200`:** same shape as `GET /onboarding/me`.

---

## 8. Partners (Partnership / LLP only)

### `POST /api/onboarding/associated-persons`  
**Auth:** portal JWT

```json
{
  "roleType": "PARTNER",
  "fullName": "Ali Khan",
  "phone": "03001234567",
  "email": "ali@example.com",
  "authorizedToOperate": true
}
```

**Response `200`:** full party (`associatedPersons` updated).

### `DELETE /api/onboarding/associated-persons/{id}`  
**Auth:** portal JWT → party response.

---

## 9. `POST /api/onboarding/documents`  
**Auth:** portal JWT  
**Content-Type:** `multipart/form-data`

| Part | Type |
|------|------|
| `documentCode` | text (e.g. `AUTH_ID_FRONT`) |
| `file` | file |

**Response `200`:** party with updated `requiredDocuments` / `documents`.

Typical codes:
- Sole / small: `AUTH_ID_FRONT`, `AUTH_ID_BACK`, `AUTH_LIVE_PHOTO` (+ e.g. `NTN_OR_TAX`)
- Partnership: `PARTNERSHIP_DEED`, `PARTNERSHIP_AUTHORITY`, optional `PARTNERSHIP_REG_CERT`
- LLP: `LLP_DEED`, `LLP_SECP_CERT`, `LLP_AUTHORITY`

---

## 10. `POST /api/onboarding/submit`  
**Auth:** portal JWT  
**Body:** empty

**Response `200`:** party with `"status": "SUBMITTED"`, `trackingId`, etc.  
Side effects: personal KYC invites + portal password emailed (or logged if mail fails).

---

## 11. `POST /api/onboarding/start-personal-kyc` ⭐ bridge to existing KYC  
**Auth:** portal JWT  
**Body:** empty

Call after submit (or after portal login when status is `SUBMITTED` / `PENDING_APPROVAL`).

**Response `200`** — hand `sessionToken` to your **existing** personal KYC module:

```json
{
  "sessionToken": "abc123…",
  "appUserId": 12,
  "phone": "03152728200",
  "fullName": "Nazish Daud",
  "email": "nazish.daud@zindigi.pk",
  "status": "INVITED",
  "businessName": "Nazish Clothing",
  "trackingId": "DFS-20261008-183268",
  "canSubmit": false,
  "mustChangePassword": true,
  "mobileVerified": false,
  "videoUploaded": false,
  "requiredDocuments": [],
  "kycFailCount": 0,
  "kycAttemptsRemaining": 3,
  "bankVisitRequired": false
}
```

**Errors**
- `400` — still `DRAFT` (submit first)
- `400` / `403` — no personal KYC invite for this login

---

## Screen map (new only)

| Screen | APIs |
|--------|------|
| Sign up | `POST /api/auth/signup` |
| Verify email OTP | `POST /api/auth/verify-otp` |
| Entity form | `GET /api/entity-types`, `GET/PUT /api/onboarding/*` |
| Partner roster | `POST /api/onboarding/associated-persons` |
| Documents | `POST /api/onboarding/documents` |
| Submit | `POST /api/onboarding/submit` → `POST /api/onboarding/start-personal-kyc` |
| Returning login | `POST /api/auth/login` → `start-personal-kyc` |

---

## Smoke test

1. `POST /api/auth/signup` → copy `devOtpHint`  
2. `POST /api/auth/verify-otp` → save `token`  
3. `PUT /api/onboarding/profile` (sole prop + terms)  
4. Upload docs via `POST /api/onboarding/documents`  
5. `POST /api/onboarding/submit`  
6. `POST /api/onboarding/start-personal-kyc` → pass `sessionToken` into **existing** KYC flow  
```
