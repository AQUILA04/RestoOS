- source_spec: `/workspace/_bmad-output/implementation-artifacts/spec-protect-routes-require-auth-tenant.md`
  summary: Add an acceptance e2e that anonymous visits to `/pos`, `/kds`, and `/admin/*` redirect to login and hide operational shell nav.
  evidence: Current e2e coverage only seeds authenticated/station sessions; anonymous denial is covered by unit guards but not end-to-end.
