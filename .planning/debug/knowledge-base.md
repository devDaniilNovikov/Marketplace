# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## redis-unauth-publish — unsigned Redis USER_UPDATED PUBLISH upserts accounts
- **Date:** 2026-09-16
- **Error patterns:** Redis PUBLISH без аутентификации, USER_UPDATED, accounts upsert, username/email overwrite, unauthenticated keycloak.events.user
- **Root cause(s):** Consume path treats any Redis PUBLISH on keycloak.events.user as a trusted Keycloak SPI event (no HMAC/origin/principal); UserUpdatedHandler then insertIfAbsent + applyProfile. AND Redis is deployed without AUTH/ACL and with host port 6379, so unauthenticated clients can issue that PUBLISH.
- **Fix:** Listener verifies HMAC-SHA256 of canonical JSON (accountId, username, email, firstName, lastName, issuedAt) with constant-time compare and ±30s skew before handle(); SPI signs with KEYCLOAK_EVENTS_MAC_SECRET. Redis requirepass not used as identity.
- **Files changed:** src/main/java/dn/marketplace/account/config/UserUpdatedEventMac.java, src/main/java/dn/marketplace/account/config/UserUpdatedWireEvent.java, src/main/java/dn/marketplace/account/config/UserUpdatedMacProperties.java, src/main/java/dn/marketplace/account/config/UserUpdatedEventAuthenticator.java, src/main/java/dn/marketplace/account/config/UserUpdatedMessageListener.java, src/main/java/dn/marketplace/account/config/UserUpdatedListenerConfig.java, src/main/java/dn/marketplace/account/api/event/UserUpdatedEvent.java, src/main/resources/application.yml, src/test/java/dn/marketplace/support/AbstractIntegrationTest.java, src/test/java/dn/marketplace/account/config/UserUpdatedEventMacTest.java, src/test/java/dn/marketplace/account/config/UserUpdatedEventAuthenticatorTest.java, src/test/java/dn/marketplace/account/config/UserUpdatedMessageListenerTest.java, src/test/java/dn/marketplace/account/config/UserUpdatedMacPropertiesTest.java, keycloak-spi/src/main/java/dn/marketplace/keycloak/UserUpdatedEventMac.java, keycloak-spi/src/main/java/dn/marketplace/keycloak/UserUpdatedEventListener.java, keycloak-spi/src/main/java/dn/marketplace/keycloak/UserUpdatedEventListenerFactory.java, keycloak-spi/src/test/java/dn/marketplace/keycloak/UserUpdatedEventMacTest.java, keycloak-spi/build.gradle.kts, keycloak-spi/README.md, docker-compose.yml
- **Why not caught:** no gate existed for this class — UserUpdatedHandlerTest encoded unsigned upsert as intended; Scenario 2 treated USER_UPDATED risk as message loss (G4), not spoofed origin
- **Recurrence guard:** src/test/java/dn/marketplace/account/config/UserUpdatedMessageListenerTest.java (без_mac / чужая_mac never call handler; valid MAC delivers the event)
---
