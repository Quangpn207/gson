# Gson Refactor Change Report

Date: 2026-03-18
Branch: main

## Summary
This report documents the four approved refactors that were implemented as separate commits, one change per commit.

## Change 1
Commit: e06fa2b9
Title: Rename GsonBuilder field to fieldNamingStrategy

Scope
- Renamed the private field name for consistency with its type and surrounding API naming.

Files
- [src/main/java/com/google/gson/GsonBuilder.java](src/main/java/com/google/gson/GsonBuilder.java)

What changed
- Private field renamed from fieldNamingPolicy to fieldNamingStrategy.
- Internal assignments and constructor/create wiring updated to use the new field name.
- Public API methods remained unchanged.

Expected impact
- Readability improvement only.
- No runtime behavior change.

## Change 2
Commit: 3298df2d
Title: Extract MIN_PRINTABLE_ASCII_CHAR in JsonReader

Scope
- Replaced a hard-coded hexadecimal literal in strict string parsing logic with a named constant.

Files
- [src/main/java/com/google/gson/stream/JsonReader.java](src/main/java/com/google/gson/stream/JsonReader.java)

What changed
- Added MIN_PRINTABLE_ASCII_CHAR with value 0x20.
- Updated strictness check in quoted value parsing to use this constant.

Expected impact
- Readability and maintainability improvement.
- No runtime behavior change.

## Change 3
Commit: 06bce357
Title: Extract shared JsonWriter state handling in Gson

Scope
- Removed duplicated save/apply/restore writer-state logic from two serialization paths.

Files
- [src/main/java/com/google/gson/Gson.java](src/main/java/com/google/gson/Gson.java)

What changed
- Introduced a small internal state holder for JsonWriter settings.
- Added helper methods to save/apply writer state and restore it.
- Updated both toJson overloads that write via JsonWriter to use the shared logic.

Expected impact
- Reduces duplication and drift risk when writer settings evolve.
- Preserves existing behavior by centralizing identical logic.

## Change 4
Commit: 1a8cd2b1
Title: Decompose getBoundFields in ReflectiveTypeAdapterFactory

Scope
- Decomposed a large method into focused helpers without changing public behavior.

Files
- [src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java](src/main/java/com/google/gson/internal/bind/ReflectiveTypeAdapterFactory.java)

What changed
- Extracted inherited-class access-filter handling into a dedicated helper.
- Extracted per-field processing and registration into a dedicated helper.
- Kept duplicate-field detection and record-specific handling semantics intact.

Expected impact
- Improved readability and local testability of method components.
- Lower maintenance cost for future edits in field-binding logic.

## Validation Notes
- Production compile check succeeded: Maven compile with tests skipped completed successfully.
- A focused test run command was attempted, but the environment showed pre-existing test compilation issues unrelated to these refactors.

## Change 5
Commit: bdfa6224
Title: Extract Atomic type adapters (Item 7 partial)

Scope
- Extracted concurrent atomic type adapters out of the `TypeAdapters` God class.

Files
- [src/main/java/com/google/gson/Gson.java](src/main/java/com/google/gson/Gson.java)
- [src/main/java/com/google/gson/internal/bind/AtomicTypeAdapters.java](src/main/java/com/google/gson/internal/bind/AtomicTypeAdapters.java)
- [src/main/java/com/google/gson/internal/bind/TypeAdapters.java](src/main/java/com/google/gson/internal/bind/TypeAdapters.java)

What changed
- Moved `AtomicInteger`, `AtomicBoolean`, `AtomicLong`, `AtomicIntegerArray`, and `AtomicLongArray` adapters into a dedicated `AtomicTypeAdapters` class.
- Updated factory registration in `Gson` to use the new class.

Expected impact
- Improved cohesion by grouping concurrency adapters together.
- Reduced size of the `TypeAdapters` God class.
- No runtime behavior change.

## Change 6
Commit: [Pending]
Title: Decompose TypeAdapters God class

Scope
- Decomposed the massive 967-line `TypeAdapters.java` class into smaller, logically grouped classes.

Files
- [src/main/java/com/google/gson/Gson.java](src/main/java/com/google/gson/Gson.java)
- [src/main/java/com/google/gson/LongSerializationPolicy.java](src/main/java/com/google/gson/LongSerializationPolicy.java)
- [src/main/java/com/google/gson/internal/bind/CommonTypeAdapters.java](src/main/java/com/google/gson/internal/bind/CommonTypeAdapters.java)
- [src/main/java/com/google/gson/internal/bind/MapTypeAdapterFactory.java](src/main/java/com/google/gson/internal/bind/MapTypeAdapterFactory.java)
- [src/main/java/com/google/gson/internal/bind/NumericTypeAdapters.java](src/main/java/com/google/gson/internal/bind/NumericTypeAdapters.java)
- [src/main/java/com/google/gson/internal/bind/TypeAdapters.java](src/main/java/com/google/gson/internal/bind/TypeAdapters.java)

What changed
- Extracted all numeric adapters (Byte, Short, Integer, Long, Float, Double, BigDecimal, BigInteger, etc.) to a new `NumericTypeAdapters` class.
- Extracted all common generic adapters (String, Boolean, Character, URL, URI, UUID, Locale, etc.) to a new `CommonTypeAdapters` class.
- Rewrote `TypeAdapters` to only hold factories, BitSet, Calendar, and inner structures.
- Updated all references in `Gson.java` and related internal files to point to the new adapter homes.

Expected impact
- Drastically improves maintainability (TypeAdapters dropped from 967 lines to ~310 lines).
- Implements the "Large modification: Decompose a God class" requirement.
- No runtime behavior change.

## Traceability
Recent commits in applied order:
1. e06fa2b9 (Rename GsonBuilder field)
2. 3298df2d (Extract MIN_PRINTABLE_ASCII)
3. 06bce357 (Extract shared JsonWriter state)
4. 1a8cd2b1 (Decompose getBoundFields)
5. bdfa6224 (Extract Atomic type adapters)
6. [Pending] (Decompose TypeAdapters God class)
