import { FormControl } from '@angular/forms';

import { maxCharacters, trimmedLength, trimmedMaxLength, trimmedMinLength } from './validators';

describe('text length validators (EC-03)', () => {
  const emoji = '😀'; // one character, two UTF-16 units

  it('counts characters after trimming, so an emoji counts once', () => {
    expect(trimmedLength('  abc  ')).toBe(3);
    expect(trimmedLength(emoji.repeat(3))).toBe(3);
  });

  it('accepts a title of 3 to 150 characters, emoji included', () => {
    const min = trimmedMinLength(3);
    const max = trimmedMaxLength(150);
    for (const value of ['abc', emoji.repeat(3), ' ' + 'x'.repeat(150) + ' ', emoji.repeat(150)]) {
      const control = new FormControl(value);
      expect(min(control)).toBeNull();
      expect(max(control)).toBeNull();
    }
  });

  it('rejects text shorter than the minimum once trimmed, emoji included', () => {
    const min = trimmedMinLength(3);
    expect(min(new FormControl('  ab  '))).toEqual({ minlength: true });
    expect(min(new FormControl(emoji.repeat(2)))).toEqual({ minlength: true });
  });

  it('rejects empty text or text made only of spaces as required', () => {
    const min = trimmedMinLength(3);
    expect(min(new FormControl(''))).toEqual({ required: true });
    expect(min(new FormControl('    '))).toEqual({ required: true });
    expect(min(new FormControl(null))).toEqual({ required: true });
  });

  it('rejects text longer than the maximum once trimmed, emoji included', () => {
    const max = trimmedMaxLength(150);
    expect(max(new FormControl('x'.repeat(151)))).toEqual({ maxlength: true });
    expect(max(new FormControl(emoji.repeat(151)))).toEqual({ maxlength: true });
  });

  it('counts every character of a password, spaces included, and an emoji once', () => {
    const max = maxCharacters(64);
    expect(max(new FormControl(emoji.repeat(64)))).toBeNull();
    expect(max(new FormControl(emoji.repeat(65)))).toEqual({ maxlength: true });
    expect(max(new FormControl(' '.repeat(65)))).toEqual({ maxlength: true });
  });
});
