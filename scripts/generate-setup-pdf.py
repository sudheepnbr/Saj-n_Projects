#!/usr/bin/env python3
"""Generate docs/OIH-IdP-Setup-Instructions.pdf from the production setup guide."""

from __future__ import annotations

from pathlib import Path

from fpdf import FPDF

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "OIH-IdP-Setup-Instructions.pdf"


class Guide(FPDF):
    def header(self) -> None:
        if self.page_no() == 1:
            return
        self.set_font("Helvetica", "I", 9)
        self.set_text_color(80, 80, 80)
        self.cell(0, 8, "OIH Local Identity Provider | Setup and production", align="L")
        self.cell(0, 8, str(self.page_no()), align="R", new_x="LMARGIN", new_y="NEXT")
        self.set_draw_color(15, 76, 92)
        self.line(20, 16, 190, 16)
        self.ln(4)

    def footer(self) -> None:
        self.set_y(-15)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, "Confidential operations copy | branch cursor/saml-mfa-idp-phases-3-7-04b9", align="C")


def ascii(text: str) -> str:
    return (
        text.replace("\u2192", "->")
        .replace("\u2014", "-")
        .replace("\u2013", "-")
        .replace("\u2018", "'")
        .replace("\u2019", "'")
        .replace("\u201c", '"')
        .replace("\u201d", '"')
        .replace("\u2022", "-")
        .replace("\u00a0", " ")
        .replace("\u00b7", "|")
        .replace("\u2260", "!=")
    )


def reset_x(pdf: Guide) -> None:
    pdf.set_x(pdf.l_margin)


def h1(pdf: Guide, text: str) -> None:
    reset_x(pdf)
    pdf.set_font("Helvetica", "B", 16)
    pdf.set_text_color(15, 76, 92)
    pdf.multi_cell(0, 9, ascii(text))
    pdf.ln(2)


def h2(pdf: Guide, text: str) -> None:
    pdf.ln(3)
    reset_x(pdf)
    pdf.set_font("Helvetica", "B", 12)
    pdf.set_text_color(15, 76, 92)
    pdf.multi_cell(0, 7, ascii(text))
    pdf.ln(1)


def body(pdf: Guide, text: str) -> None:
    reset_x(pdf)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(28, 25, 23)
    pdf.multi_cell(0, 5.2, ascii(text))
    pdf.ln(1.5)


def bullet(pdf: Guide, text: str) -> None:
    reset_x(pdf)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(28, 25, 23)
    pdf.multi_cell(0, 5.2, ascii(f"  -  {text}"))


def code(pdf: Guide, text: str) -> None:
    reset_x(pdf)
    pdf.set_fill_color(244, 241, 234)
    pdf.set_font("Courier", "", 8)
    pdf.set_text_color(28, 25, 23)
    pdf.multi_cell(0, 4.4, ascii(text), fill=True)
    pdf.ln(2)


def table(pdf: Guide, rows: list[tuple[str, str]], col1: str, col2: str) -> None:
    left_m = pdf.l_margin
    usable = pdf.w - pdf.l_margin - pdf.r_margin
    w1, w2 = 70, usable - 70
    pdf.set_x(left_m)
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_fill_color(15, 76, 92)
    pdf.set_text_color(255, 255, 255)
    pdf.cell(w1, 7, ascii(col1), fill=True)
    pdf.cell(w2, 7, ascii(col2), fill=True, new_x="LMARGIN", new_y="NEXT")
    fill = False
    for left, right in rows:
        if pdf.get_y() > 270:
            pdf.add_page()
        pdf.set_x(left_m)
        pdf.set_font("Helvetica", "", 9)
        pdf.set_text_color(28, 25, 23)
        if fill:
            pdf.set_fill_color(244, 241, 234)
        else:
            pdf.set_fill_color(255, 255, 255)
        y = pdf.get_y()
        pdf.multi_cell(w1, 5.5, ascii(left), fill=True)
        h1 = pdf.get_y() - y
        pdf.set_xy(left_m + w1, y)
        pdf.multi_cell(w2, 5.5, ascii(right), fill=True)
        h2 = pdf.get_y() - y
        pdf.set_xy(left_m, y + max(h1, h2))
        fill = not fill
    pdf.set_x(left_m)
    pdf.ln(2)


def main() -> None:
    pdf = Guide(format="A4")
    pdf.set_auto_page_break(auto=True, margin=20)
    pdf.set_margins(20, 20, 20)
    pdf.add_page()

    pdf.set_fill_color(15, 76, 92)
    pdf.rect(0, 0, 210, 48, "F")
    pdf.set_text_color(255, 255, 255)
    pdf.set_font("Helvetica", "B", 22)
    pdf.set_xy(20, 14)
    pdf.cell(0, 10, "OIH Local Identity Provider")
    pdf.set_xy(20, 26)
    pdf.set_font("Helvetica", "", 13)
    pdf.cell(0, 8, "Setup instructions  ·  lab and production")
    pdf.set_xy(20, 36)
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 6, "SAML 2.0 | TOTP MFA | PKCS#11 | PostgreSQL")
    pdf.ln(22)

    pdf.set_text_color(28, 25, 23)
    body(
        pdf,
        "This guide ships with the full-stack ZIP. It covers a local lab start, "
        "first-run administration, registering SAML service providers, and a "
        "production cutover that the application will refuse unless HTTPS, PKCS#11, "
        "and unique secrets are present.",
    )
    body(
        pdf,
        "This product authenticates locally managed users only. It does not broker "
        "LDAP, Active Directory, Entra ID, social login, or OIDC.",
    )

    h2(pdf, "1. What is in the package")
    table(
        pdf,
        [
            ("START.md / run.sh", "Fast local Docker start"),
            ("PRODUCTION.md", "Markdown copy of this guide"),
            ("docker-compose.yml", "Lab: Postgres + IdP, software token"),
            ("docker-compose.prod.yml", "Production: PKCS#11 + Caddy TLS"),
            ("deploy/", "Env template, secret generator, Caddyfile"),
            ("saml-mfa-idp/", "Application source and Dockerfiles"),
        ],
        "Path",
        "Purpose",
    )

    h2(pdf, "2. Lab / local (not production)")
    body(pdf, "Need Docker Desktop, or Java 21+, Maven 3.9+, and PostgreSQL 16+.")
    code(
        pdf,
        "chmod +x run.sh\n./run.sh\n\n# Open http://localhost:8080/setup\n# First-run token: oih-local-setup",
    )
    body(
        pdf,
        "Create the first administrator with a password of at least 12 characters "
        "and at least three of: uppercase, lowercase, digit, symbol.",
    )
    table(
        pdf,
        [
            ("http://localhost:8080/login", "Sign in"),
            ("http://localhost:8080/account", "Profile, MFA, IdP-initiated SSO"),
            ("http://localhost:8080/admin", "Users, service providers, keys"),
            ("http://localhost:8080/saml2/idp/metadata", "Public SAML metadata"),
        ],
        "URL",
        "Use",
    )
    body(
        pdf,
        "Local Docker uses the software signing token and well-known secrets "
        "(oih-local-setup, oih_dev_only, all-zero TOTP wrap key). Do not put this "
        "profile on the internet.",
    )
    h2(pdf, "Without Docker")
    code(
        pdf,
        "CREATE USER oih WITH PASSWORD 'oih_dev_only';\n"
        "CREATE DATABASE oih_idp OWNER oih;\n\n"
        "chmod +x saml-mfa-idp/scripts/run-dev.sh\n"
        "./saml-mfa-idp/scripts/run-dev.sh",
    )

    h2(pdf, "3. Daily use after first-run")
    for item in [
        "Sign in at /login.",
        "Admin → Users to create people (USER, OPERATOR, ADMIN, AUDITOR).",
        "Admin → Service providers: entity ID, ACS HTTP-POST URL (exact allow-list), optional SLO.",
        "The app appears on /account with Sign in (IdP-initiated SSO).",
        "Give each SP {base-url}/saml2/idp/metadata.",
        "Optional: Enroll authenticator (RFC 6238 TOTP). After enrollment every sign-in asks for a code.",
    ]:
        bullet(pdf, item)
    body(
        pdf,
        "Unregistered issuers and ACS URLs that are not allow-listed never receive "
        "an assertion. MFA-pending sessions cannot issue assertions.",
    )

    pdf.add_page()
    h1(pdf, "Production")
    h2(pdf, "4. Architecture")
    code(
        pdf,
        "Users / SPs  ->  Caddy (TLS 1.2+)  ->  IdP (Spring Boot)  ->  PostgreSQL\n"
        "                                       |-- PKCS#11 (SoftHSM2 or network HSM)",
    )
    body(
        pdf,
        "Terminate HTTPS at the proxy. OIH_ENTITY_ID and OIH_BASE_URL must be the "
        "public https:// URLs. SoftHSM is single-node; active/active requires a "
        "network HSM. Do not put SoftHSM tokens on NFS.",
    )
    body(pdf, "SPRING_PROFILES_ACTIVE=prod refuses to start when:")
    for item in [
        "H2 or in-memory JDBC is used",
        "The database password is blank, 'password', or oih_dev_only",
        "OIH_SETUP_TOKEN is missing or change-me",
        "idp.crypto.token is not pkcs11, or OIH_HSM_PIN is missing",
        "OIH_TOTP_WRAP_KEY is missing",
        "Entity ID or base URL is not HTTPS",
    ]:
        bullet(pdf, item)

    h2(pdf, "5. Generate secrets (never commit)")
    code(
        pdf,
        "chmod +x deploy/generate-secrets.sh\n"
        "./deploy/generate-secrets.sh > .env.prod\n"
        "# then set OIH_PUBLIC_HOST, OIH_ENTITY_ID, OIH_BASE_URL to your DNS name",
    )
    table(
        pdf,
        [
            ("OIH_SETUP_TOKEN", "openssl rand -base64 24  (used once at /setup)"),
            ("OIH_TOTP_WRAP_KEY", "openssl rand -base64 32  (wraps TOTP secrets)"),
            ("OIH_HSM_PIN / SO PIN", "Unique PKCS#11 PINs"),
            ("POSTGRES_PASSWORD", "Same value as SPRING_DATASOURCE_PASSWORD"),
        ],
        "Variable",
        "How to make it",
    )

    h2(pdf, "6. Production with Docker (recommended)")
    body(
        pdf,
        "Need a Linux host, Docker Compose, a DNS record for OIH_PUBLIC_HOST, "
        "and ports 80/443 open so Caddy can obtain a certificate.",
    )
    code(
        pdf,
        "cp deploy/env.prod.example .env.prod\n"
        "# edit secrets and hostnames\n\n"
        "docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d\n"
        "docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f idp",
    )
    body(
        pdf,
        "Open https://idp.example.com/setup and enter YOUR OIH_SETUP_TOKEN "
        "(not oih-local-setup). Create the first administrator, register SPs, "
        "and download metadata. Health: /actuator/health (includes PKCS#11 sign self-test). "
        "Volumes oih-idp-pg and oih-idp-hsm must be backed up together.",
    )

    h2(pdf, "7. Production without Docker")
    body(
        pdf,
        "Install Java 21+ (25 LTS is the architecture target), PostgreSQL 16+, "
        "SoftHSM2, and Caddy or nginx. Initialize the token, Maven-package the app, "
        "and run with SPRING_PROFILES_ACTIVE=prod plus the environment variables "
        "listed in deploy/env.prod.example. Put the proxy in front and forward "
        "X-Forwarded-Proto and X-Forwarded-Host. On first start the application "
        "generates a non-extractable RSA-3072 signing key inside the token.",
    )
    code(
        pdf,
        "cd saml-mfa-idp && mvn -DskipTests package\n"
        "export SPRING_PROFILES_ACTIVE=prod\n"
        "java -jar target/oih-idp-0.3.0-SNAPSHOT.jar",
    )

    pdf.add_page()
    h2(pdf, "8. Connect a service provider")
    for item in [
        "On the IdP: Admin → Service providers → entity ID, ACS POST, optional SLO.",
        "Copy IdP metadata from /saml2/idp/metadata.",
        "On the SP set SSO https://idp.example.com/saml2/idp/sso and SLO .../slo.",
        "The SP ACS URL must match the IdP allow-list exactly.",
        "Test SP-initiated SSO, IdP-initiated Sign in from /account, then logout.",
    ]:
        bullet(pdf, item)

    h2(pdf, "9. Backup and restore")
    body(pdf, "Back up the same generation together:")
    for item in [
        "pg_dump -Fc of oih_idp",
        "SoftHSM token directory (Compose volume oih-idp-hsm)",
        "Secret values from the secret manager (PINs and wrap key)",
    ]:
        bullet(pdf, item)
    body(
        pdf,
        "Restore into a scratch database, start the app against it, and confirm "
        "/login and /saml2/idp/metadata before declaring success.",
    )

    h2(pdf, "10. Go-live checklist")
    for item in [
        "Unique secrets; no oih-local-setup, oih_dev_only, or all-zero wrap key",
        "DNS and HTTPS working; metadata URLs are https://",
        "First admin created; TOTP enrolled",
        "At least one real SP SSO and SLO tested",
        "Backup/restore drill completed",
        "Actuator except health not on the public internet",
        "PostgreSQL not published on 0.0.0.0",
        "SoftHSM: one IdP node, or a network HSM for HA",
    ]:
        bullet(pdf, item)

    h2(pdf, "11. Source")
    body(
        pdf,
        "https://github.com/sudheepnbr/Saj-n_Projects/tree/cursor/saml-mfa-idp-phases-3-7-04b9",
    )
    body(pdf, "Pull request: https://github.com/sudheepnbr/Saj-n_Projects/pull/2")
    body(pdf, "Further ops: saml-mfa-idp/docs/runbooks/operations.md and softhsm2.md")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
