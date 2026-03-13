## ADDED Requirements

### Requirement: Board persistence SHALL store rotate and process code
The system SHALL persist `rotate` and `processCode` as part-level attributes whenever third-party prepackage data creates or refreshes board records.

#### Scenario: Initial pull stores new board fields
- **WHEN** the scheduler or manual refresh flow saves third-party prepackage data for a work order whose board payload contains `rotate` and `processCode`
- **THEN** the system MUST write both fields into the corresponding board records in `mes_part`
- **THEN** the stored values MUST match the upstream payload values without local recomputation

#### Scenario: Overwrite refresh updates existing board fields
- **WHEN** overwrite save reuses an existing board record matched by `partCode`
- **THEN** the system MUST refresh `rotate` and `processCode` on the revived board record to the latest upstream values
- **THEN** logically deleted historical board records that are not revived MUST remain excluded from query results

### Requirement: Board query APIs SHALL return rotate and process code
Any API response that returns board entities SHALL include `rotate` and `processCode` for each active board record.

#### Scenario: Batch hierarchy query returns board fields
- **WHEN** a client calls `/api/v1/production/part/{partCode}/work-order-and-batch`
- **THEN** every board node returned in the hierarchy MUST include `rotate` and `processCode`

#### Scenario: Package hierarchy query returns board fields
- **WHEN** a client calls `/api/v1/production/part/{partCode}/package`
- **THEN** every board node returned in the package hierarchy MUST include `rotate` and `processCode`

#### Scenario: Part detail query returns board fields
- **WHEN** a client calls `/api/v1/production/part/{partCode}/detail` for an active board
- **THEN** the returned board detail MUST include `rotate` and `processCode`
- **THEN** requests for logically deleted boards MUST continue to return not found instead of historical data
