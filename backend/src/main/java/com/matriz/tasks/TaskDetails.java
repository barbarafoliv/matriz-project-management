package com.matriz.tasks;

import java.time.LocalDate;

/**
 * The fields of a task that members create and edit (FR-04).
 */
public record TaskDetails(String title, String description, LocalDate dueDate, Priority priority) {

}
