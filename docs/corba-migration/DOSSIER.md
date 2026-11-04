# CORBA interoperability dossier (Stage 9)

## 1. CORBA parts in one call
Client -> **stub** -> **ORB** (marshals, IIOP over TCP) -> server ORB -> **skeleton** -> **servant**.
The client finds the object through an **object reference (IOR)**, usually obtained from the **Naming Service**.
The contract is written in **IDL**; the IDL compiler generates stubs and skeletons.

## 2. IDL (equivalent to two RMI operations)
```idl
module scasp {
  struct CourseSummary { string code; string title; long capacity; long registered; long waiting; };
  typedef sequence<CourseSummary> CourseSummaryList;
  interface Admin {
    CourseSummaryList courseReport();
    long totalRegistrations();
  };
};
```
Mapping: `CourseSummary` <-> `AdminService.CourseSummary`; `long` <-> `int`; `sequence` <-> `List`; `string` <-> `String`.

## 3. Comparison
| | CORBA | RMI | HTTP (Servlets) |
|---|---|---|---|
| Languages | Many (IDL) | Java only | Any |
| Coupling | Tight, IDL contract | Tight, shared Java interface | Loose, URL + data format |
| Deployment | ORB + naming service | JVM + registry | Web server, firewall friendly |
| Security | Needs ORB security add-ons | Needs SSL socket factories | TLS, sessions, standard tooling |
| Maintenance | Legacy; Java modules removed in JDK 11 (JEP 320) | Stable but Java-only | Best tooling and skills |

## 4. Migration pattern: adapter / facade
Keep the legacy CORBA client unchanged. Put an adapter (a servant) in front of the existing `AdminService`:
legacy client -> CORBA adapter -> `AcademicService`/RMI facade -> core. New clients use HTTP. When no CORBA
client remains, delete the adapter. A third-party ORB (e.g. JacORB) is needed to run the adapter.

## 5. Risks
- Compatibility: no CORBA in the JDK, so an external ORB version must be pinned.
- Data mapping: IDL `long` is 32-bit, strings are ASCII-by-default unless `wstring` is used; null is not allowed in structs.
- Transactions: CORBA Object Transaction Service has no equivalent here; keep each adapter call one local transaction.
- Operations: IOR/naming-service failures are hard to diagnose; add health checks and logging.
