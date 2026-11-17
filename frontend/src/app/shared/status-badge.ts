import { Component, input } from '@angular/core';
import { ApplicationStatus } from '../core/models';

/** Reusable presentational component: input in, markup out, no services. */
@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge badge-' + status().toLowerCase()">{{ status() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<ApplicationStatus>();
}
