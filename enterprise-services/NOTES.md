# Enterprise services (Stage 4)
- `RegistrationBean` (stateless, local view, container-managed transaction) and `AppMetrics` (singleton + timer).
- Deploy with the web module in one WAR/EAR on a Jakarta EE 10 runtime (WildFly, Payara, TomEE) and keep the server log as deployment evidence.
- Entity beans are not used: they were removed from Jakarta EE; JDBC repositories (Stage 5) or Jakarta Persistence replace them.
- JavaBean DTOs: records in `scasp.core.Model` are immutable value types; use plain getter/setter classes only where a framework requires them.
