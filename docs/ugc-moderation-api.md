# Sprint 5 UGC Moderation Backend

## Summary

This change adds backend infrastructure for Google Play UGC compliance:

- `reported_messages` stores abuse reports for anonymous messages received by a registered user.
- `blocked_sessions` stores anonymous session IDs blocked by a registered user.
- `users.eula_accepted_at` records when a registered user accepts the EULA.
- Anonymous WebSocket messages are rejected before persistence or Redis publish when the target user has blocked the sender session.
- Existing chat deletion behavior remains available through `DELETE /api/chat/{anonSessionId}` and still deletes both message directions for that user/session pair.

Blocking does not automatically delete existing chat history. That preserves report evidence and keeps chat deletion as an explicit user action.

Reports snapshot the reported message's content, nickname, and timestamp at report time (`reported_content`, `reported_nickname`, `reported_message_timestamp`). The `message_id` link is kept for traceability but is nullable and set to `NULL` (`ON DELETE SET NULL`) if the underlying chat message is later deleted — the report itself is never lost when a user deletes their chat history.

## Authentication

All endpoints below require:

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

The JWT subject is treated as the authenticated user's email.

## POST /api/moderation/report

Reports an anonymous message that was sent to the authenticated user.

Request:

```json
{
  "messageId": "5b916ac7-4b4e-44a6-9a9d-52b37c63cb73",
  "reason": "HARASSMENT"
}
```

Success response:

```http
201 Created
```

```json
{
  "success": true,
  "message": "Message reported successfully.",
  "reportId": "82250040-3f2c-4bce-9192-0a8aa6f7e916",
  "status": "PENDING"
}
```

Validation and authorization:

- `messageId` is required and must be a valid UUID.
- `reason` is required.
- The message must exist.
- The message must be an anonymous message sent to the authenticated user.
- Reports are created with `status = PENDING`.

## POST /api/moderation/block

Blocks an anonymous session from sending future WebSocket messages to the authenticated user.

Request:

```json
{
  "anonymousSessionId": "anon-session-123"
}
```

Success response:

```http
201 Created
```

```json
{
  "success": true,
  "message": "Anonymous session blocked successfully.",
  "blockId": "2c6c7b8e-38b0-4064-b83c-6718ebef643a",
  "blockedSessionId": "anon-session-123"
}
```

Behavior:

- `anonymousSessionId` is required.
- The `(user_id, blocked_session_id)` pair is unique.
- Repeated block requests for the same pair return the existing block record.
- Future anonymous WebSocket sends to that user are rejected before message save.

## POST /api/users/accept-eula

Records EULA acceptance for the authenticated user.

Request body: none.

Success response:

```http
200 OK
```

```json
{
  "success": true,
  "message": "EULA accepted successfully.",
  "eulaAcceptedAt": "2026-06-23T01:32:00.000Z"
}
```

## WebSocket Block Contract

When an anonymous sender posts an `ANON_TO_USER` WebSocket payload, the backend checks:

```java
blockedSessionRepository.existsByUserIdAndBlockedSessionId(targetUserId, senderSessionId)
```

If blocked:

- the message is not saved to `chat_message`;
- the message is not published to Redis;
- the sender session receives:

```json
{
  "success": false,
  "message": "You have been blocked by this user."
}
```
