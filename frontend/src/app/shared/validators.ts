import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Length of the text without leading and trailing spaces, in characters (code points), like
 * the API: an emoji counts once, not twice as in `String.length`.
 */
export function trimmedLength(text: string): number {
  return [...text.trim()].length;
}

/**
 * Rejects text made only of spaces (as `required`), or shorter than `min` once trimmed
 * (as `minlength`), like the API (EC-03).
 */
export function trimmedMinLength(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const length = trimmedLength(String(control.value ?? ''));
    if (length === 0) {
      return { required: true };
    }
    return length < min ? { minlength: true } : null;
  };
}

/** Rejects text longer than `max` once trimmed (as `maxlength`), like the API. */
export function trimmedMaxLength(max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    trimmedLength(String(control.value ?? '')) > max ? { maxlength: true } : null;
}
