---
status: resolved
trigger: "Redis PUBLISH без аутентификации создаёт/меняет accounts (finding 1)"
created: 2026-09-16T17:37:14Z
updated: 2026-09-16T18:11:00Z
---

## Current Focus
<!-- OVERWRITE on each update - always reflects NOW -->

hypothesis: Redis USER_UPDATED listener accepts any PUBLISH payload with no origin/auth/signature check, then UserUpdatedHandler insertIfAbsent + applyProfile mutates accounts; Redis itself has no AUTH/ACL so host clients can PUBLISH
test: HMAC listener + SPI sign applied; guardrail signals recorded; human confirmed
expecting: CONFIRMED — unsigned/foreign JSON dropped; signed SPI still syncs
next_action: Archived to .planning/debug/resolved/redis-unauth-publish.md
bug_class: bohrbug
known_pattern_candidate: none — MemPalace absent; .planning/debug/knowledge-base.md absent
oracle_type: specified
guardrail_verdict: accepted
reasoning_checkpoint:
  hypothesis: "Unsigned USER_UPDATED JSON on Redis Pub/Sub is treated as authoritative because the listener has no publisher authentication and Redis has no AUTH/ACL, so any client that can TCP to 6379 can insertIfAbsent + applyProfile."
  confirming_evidence:
    - "UserUpdatedListenerConfig.userUpdatedMessageListener deserializes message.getBody() to UserUpdatedEvent and calls handle() with no MAC, JWT, or origin check"
    - "UserUpdatedHandler.insertIfAbsent then applyProfile; blank username is the only guard"
    - "UserUpdatedEvent has accountId/username/email/firstName/lastName only — no signature field"
    - "SPI toJson + jedis.publish unsigned JSON; JedisPool(host, port) with no password"
    - "docker-compose redis: redis-server --appendonly yes, ports 6379:6379, no --requirepass/ACL; application.yml spring.data.redis has host/port only"
    - "SecurityFilterChain JWT applies to HTTP only; Redis MessageListener is outside it"
  falsification_test: "A consume-path HMAC/ACL/principal check, Redis requirepass, or handler that refused unknown publishers would disprove this"
  fix_rationale: "Not applied (find_root_cause_only). A fix must authenticate the publisher (payload MAC or Redis ACL that only SPI may PUBLISH) and stop treating unsigned JSON as SSOT."
  blind_spots: "No live redis-cli PUBLISH against a running monolith; a runtime Redis with undocumented AUTH would differ from compose/yml, but declared config has none"
  candidate_causes:
    - "code: listener trusts any JSON; handler upserts accounts without origin check; SPI publishes unsigned payload"
    - "config: Redis image started without --requirepass/ACL; Spring and Jedis connect with host/port only; event contract has no auth fields"
    - "environment: compose publishes 6379 to the host, so unauthenticated PUBLISH does not even require the Docker network"
    - "data: the accepted message shape is unsigned profile fields; any UUID+username JSON is a valid mutating command"
  and_gate: "yes — anonymous host PUBLISH only mutates accounts when Redis accepts unauthenticated clients AND the consume path treats the payload as authoritative. Redis AUTH alone would not meet 'only signed SPI' (shared password is not SPI identity); HMAC/origin check alone would ignore unsigned PUBLISH even on open Redis."
tdd_checkpoint: null

## Symptoms
<!-- Written during gathering, then immutable -->

expected: Только подписанный/доверенный SPI Keycloak может создавать и менять accounts
actual: Любой PUBLISH на канал создаёт или перезаписывает строку account (username/email)
errors: Логи не смотрел — опираюсь на код и find-bugs
reproduction: По коду UserUpdatedHandler + Redis listener, без живого PUBLISH
started: Не знаю точно, когда появилось

## Eliminated
<!-- APPEND only - prevents re-investigating after /clear -->

- hypothesis: RedisConfig polymorphic typeValidator authenticates Pub/Sub publishers or blocks unsigned USER_UPDATED
  evidence: Type allowlist is only on RedisTemplate GenericJacksonJsonRedisSerializer. Listener uses JsonMapper.readValue(body, UserUpdatedEvent.class) and never goes through that template.
  timestamp: 2026-09-16T17:42:10Z

- hypothesis: Spring Security JWT / JitProvisioningFilter wraps the Redis listener so only Keycloak-authenticated principals can mutate accounts
  evidence: SecurityConfig.securityFilterChain is HttpSecurity only (oauth2ResourceServer JWT). UserUpdatedListenerConfig MessageListener is a Redis callback with no SecurityContext.
  timestamp: 2026-09-16T17:42:10Z

- hypothesis: Handler only updates accounts that already exist from JIT, so PUBLISH cannot create rows
  evidence: UserUpdatedHandler always calls insertIfAbsent before findById; UserUpdatedHandlerTest.register_до_jit_создаёт_строку_тем_же_insert verifies that. Deleted accounts are the only skip after insert.
  timestamp: 2026-09-16T17:42:10Z

- hypothesis: Keycloak SPI signs the payload or authenticates to Redis, so unsigned redis-cli PUBLISH would be rejected at the broker
  evidence: toJson writes five unsigned fields; publish is jedis.publish(channel, json). Factory constructs JedisPool(config, host, port) with no password. Compose Redis has no AUTH.
  timestamp: 2026-09-16T17:42:10Z

- hypothesis: A prod profile or second application.yml enables Redis password / ACL
  evidence: Only src/main/resources/application.yml exists; spring.data.redis has host/port only. Testcontainers Redis is also started without AUTH.
  timestamp: 2026-09-16T17:42:10Z

## Evidence
<!-- APPEND only - facts discovered during investigation -->

- timestamp: 2026-09-16T17:37:14Z
  checked: User-selected /find-bugs finding 1 + symptom answers
  found: Expected trusted SPI only; actual unauthenticated Redis PUBLISH upserts accounts; repro is code review of handler/listener, no live PUBLISH yet
  implication: Investigate consume-path auth (or lack of it) before proposing a fix

- timestamp: 2026-09-16T17:42:10Z
  checked: Phase 0 knowledge base + MemPalace semantic recall
  found: mempalace CLI not installed; .planning/debug/knowledge-base.md does not exist; no prior resolved sessions
  implication: No known-pattern candidate to test first; proceed from code

- timestamp: 2026-09-16T17:42:10Z
  checked: Phase 1.25 SBFL
  found: Skipped — no failing test for this class; UserUpdatedHandlerTest encodes the insecure upsert as intended behavior; no per-test coverage spectrum
  implication: Fault localization remains code review of consume path, not Ochiai ranking

- timestamp: 2026-09-16T17:42:10Z
  checked: Phase 1.5 common-bug-patterns + Phase 1.75 taxonomy
  found: Matches Environment/Config (missing Redis AUTH) and Data Shape/API Contract (unsigned event). Deterministic missing control, not timing. bug_class=bohrbug. Route: read consume path completely (working backwards from account mutation).
  implication: Auth is absent by design of the channel, not a race or flake

- timestamp: 2026-09-16T17:42:10Z
  checked: UserUpdatedListenerConfig.java completely
  found: Subscribes to marketplace.keycloak.events-channel. Listener deserializes body to UserUpdatedEvent and calls userUpdatedHandler.handle. Catch logs RuntimeException and swallows. No HMAC, ACL username, issuer, or SecurityContext. serialExecutor only orders messages.
  implication: Any well-formed JSON on the channel is a mutating command

- timestamp: 2026-09-16T17:42:10Z
  checked: UserUpdatedHandler.java + AccountRepository.insertIfAbsent + AccountEntity.applyProfile
  found: Guard is only blank username. Then INSERT ... ON CONFLICT DO NOTHING, then findById, skip if deleted, else applyProfile overwrites username and email/first/last snapshots. AccountFacade.provision is the HTTP JIT path — not used by the listener.
  implication: New UUID creates a row; known UUID overwrites profile. Attacker-chosen accountId is enough.

- timestamp: 2026-09-16T17:42:10Z
  checked: UserUpdatedEvent.java + keycloak-spi UserUpdatedEventListener.toJson/publish + UserUpdatedEventListenerFactory.init
  found: Contract is five unsigned fields. SPI serializes LinkedHashMap via JsonSerialization and PUBLISHes. JedisPool(host, port) — no password/user. README documents the same unsigned JSON.
  implication: Intended publisher also does not authenticate; the channel is the trust boundary

- timestamp: 2026-09-16T17:42:10Z
  checked: docker-compose.yml redis service + application.yml spring.data.redis + AbstractIntegrationTest Redis container
  found: command redis-server --appendonly yes; ports 6379:6379; healthcheck redis-cli ping (no AUTH). App config host/port only. Test Redis has no password either.
  implication: Unauthenticated PUBLISH is possible from the host, not only from the Docker overlay

- timestamp: 2026-09-16T17:42:10Z
  checked: RedisConfig.java vs listener; SecurityConfig.java; SCENARIOS.md scenario 2
  found: RedisConfig typeValidator is RedisTemplate-only (gadget allowlist), not Pub/Sub auth. JWT resource server is HTTP. Scenario 2 specifies fire-and-forget unsigned USER_UPDATED; risk called out is loss, not spoofing. Mitigation G4 is reconciliation, not origin auth.
  implication: Trust-the-channel is an explicit design gap, not an accidental missed if-statement in one method

- timestamp: 2026-09-16T17:42:10Z
  checked: UserUpdatedHandlerTest.java
  found: Tests assert upsert/apply for a constructed UserUpdatedEvent with no publisher credentials. No listener-config test asserts rejection of unsigned/foreign PUBLISH.
  implication: No gate exists for this class; regression would not fail today's suite

- timestamp: 2026-09-16T18:05:00Z
  checked: Fix applied per specialist constraints (HMAC listener, SPI sign, defensive tests)
  found: UserUpdatedMessageListener rejects missing/foreign MAC and issuedAt outside 30s skew before UserUpdatedHandler.handle. SPI wireJson signs the same canonical JSON with KEYCLOAK_EVENTS_MAC_SECRET. UserUpdatedHandler unchanged.
  implication: Unsigned JSON on the channel is no longer a mutating command

- timestamp: 2026-09-16T18:05:00Z
  checked: Fix-acceptance guardrail
  found: "target_test pass (UserUpdatedMessageListenerTest 4/4). mutation_check skipped (no Stryker/PIT); manual mutant removing authenticator.accept was killed by без_mac and чужая_mac. no_op_deletion pass (added verify+sign, not deletion-only). adjacent_tests pass (ArchitectureTest, MarketplaceApplicationTests, account.*, UserUpdatedHandlerTest, SPI UserUpdatedEventMacTest). revert_and_reconfirm pass: without accept() unsigned/foreign invoked handler; with accept() they do not."
  implication: Guardrail accepted; awaiting human verification

- timestamp: 2026-09-16T18:11:00Z
  checked: Human verification checkpoint
  found: User response confirmed fixed
  implication: Archive session; do not re-investigate

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: Consume path treats any Redis PUBLISH on keycloak.events.user as a trusted Keycloak SPI event (no HMAC/origin/principal); UserUpdatedHandler then insertIfAbsent + applyProfile. AND Redis is deployed without AUTH/ACL and with host port 6379, so unauthenticated clients can issue that PUBLISH.
fix: Listener verifies HMAC-SHA256 of canonical JSON (accountId, username, email, firstName, lastName, issuedAt) with constant-time compare and ±30s skew before handle(); SPI signs with KEYCLOAK_EVENTS_MAC_SECRET. Redis requirepass not used as identity.
verification:
  target_test: { result: pass }
  mutation_check: { result: skipped, reason_if_skipped: "Stryker/PIT not configured", mutant_killed: true }
  no_op_deletion: { result: pass, deletion_justified_by_rca: false }
  adjacent_tests: { result: pass, suites_run: ["dn.marketplace.account.config.*", "dn.marketplace.account.service.UserUpdatedHandlerTest", "dn.marketplace.account.*", "dn.marketplace.architecture.ArchitectureTest", "dn.marketplace.MarketplaceApplicationTests", "dn.marketplace.keycloak.UserUpdatedEventMacTest"] }
  revert_and_reconfirm: { result: pass, bug_returned_on_revert: true, fixed_on_reapply: true }
  guardrail_verdict: accepted
oracle_type: specified
files_changed:
  - src/main/java/dn/marketplace/account/config/UserUpdatedEventMac.java
  - src/main/java/dn/marketplace/account/config/UserUpdatedWireEvent.java
  - src/main/java/dn/marketplace/account/config/UserUpdatedMacProperties.java
  - src/main/java/dn/marketplace/account/config/UserUpdatedEventAuthenticator.java
  - src/main/java/dn/marketplace/account/config/UserUpdatedMessageListener.java
  - src/main/java/dn/marketplace/account/config/UserUpdatedListenerConfig.java
  - src/main/java/dn/marketplace/account/api/event/UserUpdatedEvent.java
  - src/main/resources/application.yml
  - src/test/java/dn/marketplace/support/AbstractIntegrationTest.java
  - src/test/java/dn/marketplace/account/config/UserUpdatedEventMacTest.java
  - src/test/java/dn/marketplace/account/config/UserUpdatedEventAuthenticatorTest.java
  - src/test/java/dn/marketplace/account/config/UserUpdatedMessageListenerTest.java
  - src/test/java/dn/marketplace/account/config/UserUpdatedMacPropertiesTest.java
  - keycloak-spi/src/main/java/dn/marketplace/keycloak/UserUpdatedEventMac.java
  - keycloak-spi/src/main/java/dn/marketplace/keycloak/UserUpdatedEventListener.java
  - keycloak-spi/src/main/java/dn/marketplace/keycloak/UserUpdatedEventListenerFactory.java
  - keycloak-spi/src/test/java/dn/marketplace/keycloak/UserUpdatedEventMacTest.java
  - keycloak-spi/build.gradle.kts
  - keycloak-spi/README.md
  - docker-compose.yml

## Specialist Review

skill: engineering:debug (not installed in this runtime; general Java/Spring review applied)
specialist_hint: general
verdict: SUGGEST_CHANGE
timestamp: 2026-09-16T17:43:30Z

response: |
  SUGGEST_CHANGE — direction is correct (authenticate the publisher; do not treat unsigned JSON as SSOT; Redis password is not SPI identity; HTTP JWT and RedisTemplate type allowlist are the wrong layer). Tighten before any fix:

  1. Prefer payload MAC/signature as the identity check (shared secret between Keycloak SPI and the listener). Redis ACL/`--requirepass` and un-publishing host 6379 are defense in depth, not a substitute — a shared Redis password is still not Keycloak origin (AND-gate already recorded this).
  2. MAC the canonical JSON (or a stable field list) with a timestamp (and short skew window) so a captured PUBLISH cannot be replayed forever. Constant-time compare; reject missing/invalid MAC before `insertIfAbsent`.
  3. Verify in the Redis listener/adapter, not in `UserUpdatedHandler` / `AccountFacade` — HTTP JIT must stay on JWT. SPI `toJson`/`JedisPool` must sign with the same secret; secret via env/config, not source.
  4. Add a test that unsigned/foreign `PUBLISH` JSON is dropped (today `UserUpdatedHandlerTest` encodes the insecure upsert as intended — no gate exists).
  5. Do not “fix” this by only adding `requirepass` or by binding Spring Security onto the listener.

## Prevention

why_not_caught: no gate existed for this class — UserUpdatedHandlerTest encoded unsigned upsert as intended; Scenario 2 treated USER_UPDATED risk as message loss (G4), not spoofed origin
recurrence_guard: src/test/java/dn/marketplace/account/config/UserUpdatedMessageListenerTest.java — missing MAC and foreign MAC never call UserUpdatedHandler; valid MAC delivers the domain event

blameless_5_whys: |
  code: The listener deserialized any JSON to UserUpdatedEvent and called handle() because the consume path had no publisher identity check; the SPI serialized five unsigned fields because the wire contract had no MAC/issuedAt.
  config: Redis and Spring were configured with host/port only (no requirepass/ACL, no events MAC secret), so the broker and the app treated connectivity as trust.
  environment: Compose published 6379 to the host, so a TCP client outside the Docker network could PUBLISH without joining the overlay.
  data: The accepted message shape was unsigned profile fields; any UUID+username JSON was a valid mutating command.
  and_gate: Anonymous PUBLISH mutated accounts only when Redis accepted unauthenticated clients AND the listener treated the payload as authoritative. A Redis password is not SPI identity; HMAC on the listener is.
  why_possible: HTTP JWT never wrapped Redis callbacks, so the missing control was not a missed if in SecurityConfig — it was an unauthenticated side channel.
