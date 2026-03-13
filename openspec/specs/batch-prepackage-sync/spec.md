## ADDED Requirements

### Requirement: Batch push SHALL deduplicate within the same batch, optimizing file, and work order
The system SHALL treat `batchNum + optimizingFileName + workId` as the business identity for batch push processing. When `/api/v1/third-party/batch/push` receives a record whose batch number, optimizing file, and work order all already exist, it MUST reuse the existing batch, optimizing file, and work order records instead of inserting duplicates.

#### Scenario: Repeat push for the same batch, optimizing file, and work order
- **WHEN** the batch push interface receives a work order whose `batchNum`, `optimizingFileName`, and `workId` already exist together
- **THEN** the system MUST NOT insert a new batch, optimizing file, or work order record
- **THEN** the system MUST reset the existing work order prepackage status to `NOT_PULLED`
- **THEN** the system MUST clear retry-related failure information on that work order

### Requirement: Batch push SHALL support incremental push of new optimizing files and work orders within the same batch
The system SHALL allow the same batch number to be pushed multiple times when the incoming data introduces a new optimizing file or a new work order under an existing optimizing file.

#### Scenario: Push a new optimizing file into an existing batch
- **WHEN** the batch push interface receives an existing `batchNum` with an optimizing file name that does not yet exist under that batch
- **THEN** the system MUST reuse the existing batch record
- **THEN** the system MUST insert the new optimizing file
- **THEN** the system MUST insert all new work orders carried by that optimizing file

#### Scenario: Push a new work order into an existing optimizing file
- **WHEN** the batch push interface receives an existing `batchNum` and existing `optimizingFileName` with a `workId` that does not yet exist under that optimizing file
- **THEN** the system MUST reuse the existing batch and optimizing file records
- **THEN** the system MUST insert the new work order

### Requirement: Work order repull SHALL only reset status
The `/{workId}/repull` interface SHALL mark the target work order as pending for re-pull and MUST NOT immediately call the third-party prepackage service.

#### Scenario: Reset a work order for repull
- **WHEN** a user calls the work order repull interface for an existing work order
- **THEN** the system MUST set that work order prepackage status to `NOT_PULLED`
- **THEN** the system MUST reset retry count and clear stored failure message
- **THEN** the system MUST NOT create a new batch, optimizing file, or work order record

### Requirement: Batch repull SHALL reset all work orders under the batch
The system SHALL provide an interface that resets every work order belonging to the specified batch to `NOT_PULLED` without immediately calling the third-party service.

#### Scenario: Reset all work orders in a batch
- **WHEN** a user calls the batch repull interface with an existing batch number
- **THEN** the system MUST reset every work order under that batch to `NOT_PULLED`
- **THEN** the system MUST reset retry count and clear stored failure message for those work orders
- **THEN** the system MUST return the number of work orders reset

### Requirement: Scheduled prepackage pull SHALL always overwrite existing prepackage hierarchy data
The scheduled pull process MUST save third-party prepackage data using overwrite semantics for every work order pull, regardless of whether the work order was previously pulled.

#### Scenario: Scheduled pull refreshes an already pulled work order
- **WHEN** the scheduler processes a work order that already has prepackage hierarchy data
- **THEN** the system MUST replace the existing prepackage order, box, package, and active part hierarchy for that work order record with the latest third-party result
- **THEN** the system MUST preserve reportable history that depends on logically deleted part records

#### Scenario: Scheduled pull refreshes a reset work order
- **WHEN** the scheduler processes a work order whose status was reset to `NOT_PULLED`
- **THEN** the system MUST fetch the latest third-party prepackage data
- **THEN** the system MUST save the result using overwrite semantics
- **THEN** the system MUST set the work order status according to the pull result

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
