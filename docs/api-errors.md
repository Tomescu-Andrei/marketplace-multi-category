# Validation and error responses

The API uses Spring's ProblemDetail support and returns `application/problem+json`.
Clients should use HTTP status and `code` for control flow; human-readable messages
may change. Validation errors identify the relevant field without echoing rejected
values. Internal exception details and SQL are logged on the server, not returned.

## Response shape

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "A fixed-price listing requires a price",
  "instance": "/api/v1/listings",
  "code": "INVALID_LISTING",
  "errors": [
    { "field": "price", "message": "A fixed-price listing requires a price" }
  ]
}
```

`instance` is the request path without its query string. `errors` is always an
array and can be empty when the problem cannot be assigned to a specific field.
The contract applies to errors handled by Spring MVC and the API exception handler;
requests rejected before MVC, such as malformed HTTP at the web server, are outside
this handler's scope.

| Status | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Request DTO constraint failure; inspect `errors` |
| 400 | `INVALID_JSON` | Malformed JSON, wrong field type, unknown field, duplicate key, or extra trailing JSON |
| 400 | `INVALID_PARAMETER` | Invalid path ID, pagination value or parameter type |
| 400 | `INVALID_LISTING` | Missing fixed price, unsupported currency, or invalid category attributes |
| 401 | `UNAUTHENTICATED` | Authentication required, expired session or invalid credentials |
| 403 | `INVALID_CSRF_TOKEN` | Missing/invalid CSRF token; fetch a token for this session |
| 403 | `FORBIDDEN` | Operation not permitted |
| 404 | `NOT_FOUND` | Category, listing or route does not exist |
| 405 | `METHOD_NOT_ALLOWED` | Unsupported HTTP method; `Allow` header is preserved |
| 406 | `NOT_ACCEPTABLE` | Response cannot satisfy the requested representation |
| 409 | `DATA_CONFLICT` | Database integrity conflict; no internal constraint names are returned |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Request media type is not supported |
| 500 | `INTERNAL_ERROR` | Unexpected failure; generic public message |
| Other | `HTTP_ERROR` | Other MVC error retaining its HTTP status |

## Input rules

- Category ID must be a positive integer referring to an existing category.
- Title and description are required and nonblank, limited to 200 and 10000 characters.
- Listing type must be the string `FIXED_PRICE` or `AUCTION`; numeric enum ordinals are rejected.
- Prices must be positive, with at most 12 integer digits and 2 decimal places.
  FIXED_PRICE requires a price; AUCTION may omit it. Monetary values use BigDecimal.
- Currency must be an uppercase, supported ISO 4217 code.
- Attributes must match the selected category's codes, required flags and scalar types.
  See [database design](database-design.md) for attribute limits and JSONB trade-offs.
- Listing queries accept `limit` from 1 to 100 and `offset` from 0 to 2147483647.
- Fractional category IDs, unknown request fields (including `sellerId`), duplicate
  JSON object keys and multiple concatenated JSON documents are rejected.

The seller is the authenticated account; clients cannot select a different seller,
status, ID or timestamps in the create request. Valid title/description strings
are stripped of leading/trailing whitespace before persistence.

Reference: [Spring Framework error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html).

M2's security filters also use this error shape. A missing CSRF token may produce
403 before authentication or DTO validation. Unknown routes require authentication
unless explicitly public, so anonymous calls can receive 401 before MVC routing.
See [authentication](authentication.md) for registration and session rules.

Registration requires a valid unique username as well as email. Login's identifier
accepts either one, and invalid credentials use the same generic 401. A duplicate
username or email returns 409 DATA_CONFLICT without disclosing which account exists.
