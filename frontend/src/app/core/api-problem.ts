import { HttpErrorResponse } from '@angular/common/http';

/** A field error in a `VALIDATION_FAILED` problem (docs/openapi.yaml, FieldError). */
export interface FieldError {
  field: string;
  code: string;
  message: string;
}

/** An error response from the API (docs/openapi.yaml, Problem). */
export interface ApiProblem {
  status: number;
  code?: string;
  detail?: string;
  errors?: FieldError[];
}

/** Reads the problem details from a failed request, if the API sent them. */
export function toApiProblem(error: unknown): ApiProblem {
  if (error instanceof HttpErrorResponse) {
    const body = typeof error.error === 'object' && error.error !== null ? error.error : {};
    return { status: error.status, ...body };
  }
  return { status: 0 };
}

/** The message for each invalid field, by field name. */
export function fieldMessages(problem: ApiProblem): Record<string, string> {
  const messages: Record<string, string> = {};
  for (const error of problem.errors ?? []) {
    messages[error.field] ??= error.message;
  }
  return messages;
}
