import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CreditApiService } from '../core/credit-api.service';
import { ApplicationStatus, CreditApplication, Page, Stats } from '../core/models';
import { readProblem } from '../core/problem';
import { StatusBadge } from '../shared/status-badge';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CurrencyPipe, DatePipe, StatusBadge],
  template: `
    <h1>Credit decisions</h1>

    @if (stats(); as s) {
      <section class="tiles" aria-label="Portfolio summary">
        <div class="tile"><span>Total</span><strong>{{ s.total }}</strong></div>
        <div class="tile tile-approved"><span>Approved</span><strong>{{ s.approved }}</strong></div>
        <div class="tile tile-referred"><span>Referred</span><strong>{{ s.referred }}</strong></div>
        <div class="tile tile-declined"><span>Declined</span><strong>{{ s.declined }}</strong></div>
        <div class="tile"><span>Approval rate</span><strong>{{ s.approvalRatePercent }}%</strong></div>
        <div class="tile"><span>Avg. score</span><strong>{{ s.averageCreditScore }}</strong></div>
      </section>
    }

    <section class="card">
      <div class="toolbar">
        <h2>Applications</h2>
        <div class="filters" role="group" aria-label="Filter by status">
          @for (f of filters; track f.value) {
            <button type="button" [class.active]="filter() === f.value" (click)="setFilter(f.value)">{{ f.label }}</button>
          }
        </div>
      </div>

      @if (error()) {
        <p class="alert">{{ error() }}</p>
      }

      @if (page(); as p) {
        @if (p.content.length === 0) {
          <p class="muted">No applications match this filter.</p>
        } @else {
          <table>
            <thead>
              <tr>
                <th>Reference</th><th>Applicant</th><th>Product</th><th class="num">Amount</th>
                <th class="num">Score</th><th class="num">Risk</th><th>Status</th><th>Submitted</th>
              </tr>
            </thead>
            <tbody>
              @for (a of p.content; track a.id) {
                <tr>
                  <td><a [routerLink]="['/applications', a.id]">{{ a.referenceNumber }}</a></td>
                  <td>{{ a.applicantName }}</td>
                  <td>{{ productLabel(a.productType) }}</td>
                  <td class="num">{{ a.requestedAmount | currency: 'CAD' : 'symbol-narrow' : '1.0-0' }}</td>
                  <td class="num">{{ a.creditScore }}</td>
                  <td class="num">{{ a.riskScore }}</td>
                  <td><app-status-badge [status]="a.status" /></td>
                  <td>{{ a.createdAt | date: 'MMM d, h:mm a' }}</td>
                </tr>
              }
            </tbody>
          </table>
          <div class="pager">
            <button type="button" [disabled]="p.page === 0" (click)="go(p.page - 1)">Previous</button>
            <span>Page {{ p.page + 1 }} of {{ p.totalPages || 1 }} ({{ p.totalElements }} total)</span>
            <button type="button" [disabled]="p.page + 1 >= p.totalPages" (click)="go(p.page + 1)">Next</button>
          </div>
        }
      } @else if (!error()) {
        <p class="muted">Loading...</p>
      }
    </section>
  `,
})
export class Dashboard implements OnInit {
  private readonly api = inject(CreditApiService);

  // Signals hold UI state. Setting a signal tells Angular exactly what to re-render.
  protected readonly stats = signal<Stats | null>(null);
  protected readonly page = signal<Page<CreditApplication> | null>(null);
  protected readonly filter = signal<ApplicationStatus | ''>('');
  protected readonly error = signal('');

  protected readonly filters: { value: ApplicationStatus | ''; label: string }[] = [
    { value: '', label: 'All' },
    { value: 'REFERRED', label: 'Needs review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'DECLINED', label: 'Declined' },
  ];

  ngOnInit(): void {
    this.api.stats().subscribe({ next: s => this.stats.set(s), error: e => this.error.set(readProblem(e).message) });
    this.go(0);
  }

  protected setFilter(value: ApplicationStatus | ''): void {
    this.filter.set(value);
    this.go(0);
  }

  protected go(pageIndex: number): void {
    this.api.list(this.filter(), pageIndex, 10).subscribe({
      next: p => { this.page.set(p); this.error.set(''); },
      error: e => this.error.set(readProblem(e).message),
    });
  }

  protected productLabel(p: string): string {
    return p.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase());
  }
}
