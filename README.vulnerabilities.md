# Vulnerabilities in Vulnerable.Java
_(Intentionally insecure for scanner testing. Do not deploy.)_

## 1) Hardcoded API key
- **Where:** `src/main/java/org/example/App.java` — `static final String WEATHER_KEY = "..."` (top of class)
- **CWE:** CWE-798
- **OWASP:** A02 – Cryptographic Failures

## 2) TLS verification disabled (trust-all SSLContext)
- **Where:** `App.java` — `trustAllSSLContext()` used by `HttpClient`
- **CWE:** CWE-295
- **OWASP:** A02 – Cryptographic Failures
- **Fix:** Use default certificate validation or certificate pinning.

## 3) SQL injection via concatenation
- **Where:** `App.java`, `WeatherHandler` — `String sql = "INSERT ... '" + zip + "', '" + body + "'"`
- **CWE:** CWE-89
- **OWASP:** A03 – Injection
- **Fix:** Use `PreparedStatement` with `?` parameters.

## 4) Weak cryptography (MD5)
- **Where:** `App.java` — `MessageDigest.getInstance("MD5")`
- **CWE:** CWE-327
- **OWASP:** A02 – Cryptographic Failures
- **Fix:** Prefer SHA-256/512 as appropriate.

## 5) Insecure deserialization
- **Where:** `App.java`, `/deserialize` — `ObjectInputStream.readObject()` on request body
- **CWE:** CWE-502
- **OWASP:** A08 – Software and Data Integrity Failures
- **Fix:** Avoid native Java serialization; use JSON with strict schema.

## 6) Outdated/vulnerable dependencies
- **Where:** `pom.xml` — `sqlite-jdbc 3.7.2`
- **CWE:** CWE-1104
- **OWASP:** A06 – Vulnerable and Outdated Components
- **Fix:** Upgrade to supported versions; enable SCA tooling.
