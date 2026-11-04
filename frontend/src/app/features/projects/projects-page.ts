import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';

import { Project } from '../../core/projects/project';
import { ProjectsService } from '../../core/projects/projects.service';
import { ProjectCard } from './project-card';
import { ProjectFormDialog } from './project-form-dialog';

type ListState = { status: 'loading' } | { status: 'error' } | { status: 'ready'; projects: Project[] };

/** Projects screen: list, search, create (US-02, US-12, EC-11). */
@Component({
  selector: 'app-projects-page',
  imports: [RouterLink, ProjectCard, ProjectFormDialog],
  templateUrl: './projects-page.html',
})
export class ProjectsPage implements OnInit {
  private readonly projects = inject(ProjectsService);
  private readonly destroyRef = inject(DestroyRef);

  /** Each value asks the API for the list again: a new search, or a reload after a change. */
  private readonly searches = new Subject<string>();

  protected readonly search = signal('');
  protected readonly state = signal<ListState>({ status: 'loading' });
  protected readonly creating = signal(false);
  protected readonly list = computed(() => {
    const state = this.state();
    return state.status === 'ready' ? state.projects : [];
  });

  ngOnInit(): void {
    this.searches
      .pipe(
        // Search after the user stops typing (docs/ui/design-decisions.md).
        debounceTime(300),
        startWith(''),
        map((text) => text.trim()),
        distinctUntilChanged(),
        switchMap((text) => {
          this.state.set({ status: 'loading' });
          return this.projects.list(text).pipe(
            map((projects): ListState => ({ status: 'ready', projects })),
            catchError(() => of<ListState>({ status: 'error' })),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((state) => this.state.set(state));
  }

  protected onSearch(text: string): void {
    this.search.set(text);
    this.searches.next(text);
  }

  protected clearSearch(): void {
    this.onSearch('');
  }

  protected reload(): void {
    this.state.set({ status: 'loading' });
    this.projects.list(this.search()).subscribe({
      next: (projects) => this.state.set({ status: 'ready', projects }),
      error: () => this.state.set({ status: 'error' }),
    });
  }

  /** The new project appears in the list, with the API's data (US-02). */
  protected onCreated(): void {
    this.creating.set(false);
    this.reload();
  }
}
