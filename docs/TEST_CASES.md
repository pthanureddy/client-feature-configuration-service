# Test Cases

| ID | Scenario | Expected result | Automation |
| --- | --- | --- | --- |
| T1 | Create at expected version 0 | version 1, normalized regions | JUnit |
| T2 | Update at expected version 1 | version 2 | JUnit |
| T3 | Update with stale expected version | `ABORTED`/version conflict | JUnit |
| T4 | Repeat identical request ID and facts | original snapshot, replay flag | JUnit |
| T5 | Reuse request ID with different facts | idempotency conflict | JUnit |
| T6 | Invalid tenant or feature identifier | `INVALID_ARGUMENT` | JUnit |
| T7 | Rollout outside 0-100 | `INVALID_ARGUMENT` | JUnit |
| T8 | Read existing configuration through gRPC | typed response | JUnit/in-process gRPC |
| T9 | No credentials | `UNAUTHENTICATED` | JUnit/in-process gRPC |
| T10 | Header tenant differs from payload | `INVALID_ARGUMENT` | JUnit/in-process gRPC |
| T11 | Read missing configuration | `NOT_FOUND` | JUnit |
| T12 | Spanner DDL tenant-first keys and version field | schema contract present | JUnit |
| T13 | Spanner emulator atomic create/update/replay | rows and receipts commit together | manual, not claimed complete |

T13 requires creation of an emulator instance/database, applying the committed DDL, enabling the
Spanner profile, and repeating T1-T5 through gRPC. It remains explicitly manual in this repository.
