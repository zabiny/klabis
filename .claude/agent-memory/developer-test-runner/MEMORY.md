# Test Runner Agent Memory

## PermissionController HATEOAS Links Fix (2026-03-03)

### Issue
Test `PermissionControllerTest.shouldIncludeHateoasLinks()` was failing with duplicate self links causing an array instead of object.

### Solution
Separated concerns:
1. Removed self link from `PermissionsResponseModelAssembler` - assembler now only instantiates model
2. Controller now adds both self link WITH affordances and separate "permissions" link

**Files Modified:**
- `/backend/src/main/java/com/klabis/common/users/infrastructure/restapi/PermissionsResponseModelAssembler.java`
- `/backend/src/main/java/com/klabis/common/users/infrastructure/restapi/PermissionController.java`

### Key Lesson
Avoid duplicate link additions with same relationship name - HATEOAS merges them into arrays instead of objects. Assemblers should add standard links, controllers add action-specific affordances.

## Calendar Module Visibility Fixes (2026-03-03)

### Issue
Calendar application layer classes were not public, causing visibility errors when imported by controller and test classes in other packages:
- `CalendarItemDto` (record)
- `CreateCalendarItemCommand` (record)
- `UpdateCalendarItemCommand` (record)
- `InvalidCalendarQueryException` (exception class)
- `CalendarItemReadOnlyException` (exception class)
- `CalendarNotFoundException` (exception class)

### Solution
Made all application layer classes public to allow cross-package access from infrastructure layer:

**Files Modified:**
- `/backend/src/main/java/com/klabis/calendar/application/CalendarItemDto.java` - added `public` to record
- `/backend/src/main/java/com/klabis/calendar/application/CreateCalendarItemCommand.java` - added `public` to record
- `/backend/src/main/java/com/klabis/calendar/application/UpdateCalendarItemCommand.java` - added `public` to record
- `/backend/src/main/java/com/klabis/calendar/application/InvalidCalendarQueryException.java` - added `public` class and constructor
- `/backend/src/main/java/com/klabis/calendar/application/CalendarItemReadOnlyException.java` - added `public` class and constructor
- `/backend/src/main/java/com/klabis/calendar/application/CalendarNotFoundException.java` - added `public` class and constructor
- `/backend/src/test/java/com/klabis/calendar/infrastructure/jdbc/CalendarJdbcRepositoryTest.java` - added missing import for `CalendarRepository`

### Test Results (Latest - 2026-03-03)
Calendar package tests: 108 tests pass (100% success rate, 0 failures)
All 1382 backend tests pass (10 skipped)

## Events Module Import Fixes (2026-03-03)

### Issue
Events module had missing imports causing compilation failures:
- `EventRegistrationController.java` - missing import for `DuplicateRegistrationException`
- `EventControllerTest.java` - missing import for `EventNotFoundException`
- `EventRegistrationControllerTest.java` - missing imports for exception classes

### Root Cause
Architecture refactoring moved exception classes to `application` package but imports weren't updated in controller and test classes.

### Solution
Added missing imports to three files:

**Files Modified:**
- `/backend/src/main/java/com/klabis/events/infrastructure/restapi/EventRegistrationController.java` - added `import com.klabis.events.application.DuplicateRegistrationException;`
- `/backend/src/test/java/com/klabis/events/infrastructure/restapi/EventControllerTest.java` - added `import com.klabis.events.application.EventNotFoundException;`
- `/backend/src/test/java/com/klabis/events/infrastructure/restapi/EventRegistrationControllerTest.java` - added imports for `DuplicateRegistrationException`, `EventNotFoundException`, `RegistrationNotFoundException`

### Test Results
All 1392 backend tests pass: 1382 passed, 10 skipped, 0 failed
