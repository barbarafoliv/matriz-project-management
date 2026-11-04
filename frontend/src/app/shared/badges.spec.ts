import { TestBed } from '@angular/core/testing';

import { PriorityBadge, StatusBadge } from './badges';

describe('badges', () => {
  it('shows the priority with its label and style', () => {
    const fixture = TestBed.createComponent(PriorityBadge);
    fixture.componentRef.setInput('priority', 'HIGH');
    fixture.detectChanges();
    const badge: HTMLElement = fixture.nativeElement.querySelector('span');

    expect(badge.textContent).toBe('High');
    expect(Array.from(badge.classList).sort()).toEqual(['b-high', 'badge']);
  });

  it('shows the status with its label and style', () => {
    const fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('status', 'IN_PROGRESS');
    fixture.detectChanges();
    const badge: HTMLElement = fixture.nativeElement.querySelector('span');

    expect(badge.textContent).toBe('In Progress');
    expect(Array.from(badge.classList).sort()).toEqual(['b-prog', 'badge']);
  });
});
