# Initial database and API design

## Schema ownership and generation

Flyway SQL migrations are the schema source of truth. V1 creates the tables,
constraints and indexes; V2 supplies Electronics and Books category definitions.
The default local profile also runs V3 from `db/local`, adding the predefined
M1 seller. Future shared migrations must use version V4 or higher because V3
is reserved for the local seed. Applied migrations must not be edited.

The Maven generation script starts a temporary PostgreSQL 17 container, runs the
shared Flyway migrations and invokes jOOQ's generator against the migrated schema.
The temporary container is stopped in a finally block. Build and test containers
use random host ports and do not depend on the Compose development database.
The code generator and runtime use the same Spring Boot-managed jOOQ version.

## Tables

- `categories`: identity, name, unique slug and optional self-referencing parent.
- `category_attributes`: per-category codes with TEXT, NUMBER or BOOLEAN types and
  a required flag; each category/code pair is unique.
- `users`: seller identity and creation timestamp. No authentication yet.
- `listings`: seller/category foreign keys, descriptive fields, type, money,
  currency, status, attributes and timestamps.

Listing prices use NUMERIC(14,2), mapped to BigDecimal. Database checks prevent
nonpositive prices and fixed-price listings without prices. Timestamps use
TIMESTAMPTZ, mapped to OffsetDateTime. New listings receive ACTIVE status and
creation/update timestamps from PostgreSQL. There is no update API yet; future
update operations must explicitly maintain `updated_at`.

## Category-specific values

The initial implementation stores values in `listings.attributes` as a JSONB
object keyed by category attribute code. A separate `listing_attributes` table
is therefore unnecessary for M1. This keeps reads and inserts simple, avoids
multiple sparse typed value columns, and preserves JSON scalar types.

The trade-off is that PostgreSQL cannot enforce definition/type relationships
through ordinary foreign keys on JSON keys. The service rejects unknown codes,
missing required values and mismatched JSON scalar types before persistence.
Direct SQL writes bypass those checks. Generic database checks still require a
JSON object. Attribute inheritance from parent categories is not implemented;
only the selected category's definitions apply. Richer validation, attribute
filtering/indexes and schema evolution can be introduced when required.

TEXT values must be nonblank and at most 1000 characters. NUMBER values must be
JSON numbers (limited to 100 characters); BOOLEAN values must be JSON booleans.
Optional attributes may be absent or null. Unknown attributes are rejected even
if null. Requests accept at most 100 attributes.

## Layers and transactions

Controllers receive validated request DTOs and return response DTOs. Services
own business rules and transaction boundaries. Repositories use DSLContext and
generated jOOQ tables/records internally; generated records are never returned
directly by REST controllers. Creation validates the category, its attributes and
the predefined seller in a single Spring transaction before inserting a listing.

## Deferred behavior

AUCTION is currently a listing type only. Auctions, bids, images, favorites,
messages, notifications and valuation tables will be added by future migrations
when their behavior is implemented. Authentication, frontend, payments and
Google Cloud deployment are outside this increment. The local seller is an M1
convenience and must be replaced when authentication is implemented.
