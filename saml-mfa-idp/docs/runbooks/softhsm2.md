# SoftHSM2 initialization runbook

Phase 1 documents the commands. Phase 3 automates them in lab/CI. **Never put real PINs in git.** Examples use placeholders.

## Install (Debian/Ubuntu)

```bash
sudo apt-get update
sudo apt-get install -y softhsm2 opensc p11-kit
```

Library path is typically `/usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so` or `/usr/lib64/libsofthsm2.so`.

## Token storage

```bash
sudo mkdir -p /var/lib/oih-idp/tokens
sudo chown oih-idp:oih-idp /var/lib/oih-idp/tokens
sudo chmod 700 /var/lib/oih-idp/tokens
```

`/etc/softhsm/softhsm2.conf` (or `SOFTHSM2_CONF`):

```
directories.tokendir = /var/lib/oih-idp/tokens
objectstore.backend = file
log.level = INFO
slots.removable = false
```

## Initialize token

```bash
softhsm2-util --init-token --free \
  --label "oih-idp" \
  --so-pin "$OIH_HSM_SO_PIN" \
  --pin "$OIH_HSM_PIN"
softhsm2-util --show-slots
```

## SunPKCS11 configuration file

`/etc/oih-idp/pkcs11.cfg` (no PIN here):

```
name = OihIdp
library = /usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so
slotListIndex = 0
attributes(*, CKO_PRIVATE_KEY, *) = {
  CKA_TOKEN = true
  CKA_PRIVATE = true
  CKA_SENSITIVE = true
  CKA_EXTRACTABLE = false
  CKA_SIGN = true
  CKA_DECRYPT = false
}
```

## Generate RSA-3072 inside the token

Using OpenSC (`pkcs11-tool`) so generation happens in the HSM:

```bash
pkcs11-tool --module /usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so \
  --login --pin "$OIH_HSM_PIN" \
  --keypairgen --key-type rsa:3072 \
  --label "idp-signing-active" \
  --id 01 \
  --usage-sign
```

Confirm objects:

```bash
pkcs11-tool --module /usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so \
  --login --pin "$OIH_HSM_PIN" --list-objects
```

## Bind an X.509 certificate to that public key

The certificate **must** contain the public key that belongs to CKA_ID `01`.

Lab (Java keytool against PKCS#11 — keytool will use the token key and emit a matching self-signed cert):

```bash
keytool -genkeypair \
  -alias idp-signing-active \
  -keyalg RSA -keysize 3072 \
  -dname "CN=IMT InfraMindTech IdP Signing,O=InfraMindTech,C=OM" \
  -validity 825 \
  -storetype PKCS11 \
  -providerClass sun.security.pkcs11.SunPKCS11 \
  -providerArg /etc/oih-idp/pkcs11.cfg \
  -keystore NONE \
  -storepass "$OIH_HSM_PIN"
```

Use **either** `pkcs11-tool --keypairgen` **or** `keytool -genkeypair`, not both for the same CKA_ID. Prefer one documented path in Phase 3 (keytool is simpler for Java cert objects; pkcs11-tool is clearer about attributes). Phase 3 must pick one and prove the cert modulus matches the token public key.

Export the certificate (public only):

```bash
keytool -exportcert -rfc \
  -alias idp-signing-active \
  -storetype PKCS11 \
  -providerClass sun.security.pkcs11.SunPKCS11 \
  -providerArg /etc/oih-idp/pkcs11.cfg \
  -keystore NONE \
  -storepass "$OIH_HSM_PIN" \
  -file /tmp/idp-signing.pem
```

Production: `keytool -certreq` with the same PKCS#11 keystore, sign CSR with the enterprise CA, import the issued cert **only**.

## Verify signing

```bash
# Sign a digest via pkcs11-tool (mechanism CKM_SHA256_RSA_PKCS)
echo -n "oih-idp-health" > /tmp/nonce.bin
pkcs11-tool --module /usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so \
  --login --pin "$OIH_HSM_PIN" \
  --sign --id 01 --mechanism SHA256-RSA-PKCS \
  --input-file /tmp/nonce.bin --output-file /tmp/nonce.sig
```

Application-level verify uses the exported certificate’s public key.

## Verify non-extractability

Attempt to export the private key must fail:

```bash
pkcs11-tool --module /usr/lib/x86_64-linux-gnu/softhsm/libsofthsm2.so \
  --login --pin "$OIH_HSM_PIN" \
  --read-object --type privkey --id 01 --output-file /tmp/should-not-exist.der
```

Expected: error / empty; application startup must also read `CKA_EXTRACTABLE` and refuse `true`.

## Backup

Backup `/var/lib/oih-idp/tokens`, `softhsm2.conf`, and PINs (secret manager), together with the PostgreSQL backup generation number.

Restore onto a clean host **before** starting the IdP if `signing_keys.cka_id` must still resolve.
