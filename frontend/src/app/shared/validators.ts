import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Rejects text made only of spaces (as `required`), or shorter than `min` once trimmed
 * (as `minlength`), like the API (EC-03).
 */
export function trimmedMinLength(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const length = String(control.value ?? '').trim().length;
    if (length === 0) {
      return { required: true };
    }
    return length < min ? { minlength: true } : null;
  };
}
