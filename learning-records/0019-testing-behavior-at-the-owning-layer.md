# Testing behavior at the layer that owns it

On 2026-11-23, the learner questioned whether a separate fewer-periods tool test would only restate the configuration of a mocked loader. For the current tool, which directly returns loader.load(request), this is a sound scope challenge: the general delegation test covers result forwarding, while filing selection and result transformation should be tested in the components implementing them. The separate wrapper case was removed from checkpoint A.

The learner also asked whether invalid ticker tests cover a missing request. Clarify that constructing FinancialStatementRequest(null, 2) fails before the tool executes, whereas getFinancialStatements(null) enters the tool and exercises its separate request guard. Understanding of that distinction has not yet been demonstrated.
