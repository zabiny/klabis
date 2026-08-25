package com.klabis.oris;

import java.time.LocalDate;

record OrisEventSummary(int id, String name, LocalDate date, String location, String organizer) {
}
