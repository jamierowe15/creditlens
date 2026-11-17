import { Component, OnInit, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CreditApiService } from '../core/credit-api.service';
import { AuditEntry, CreditApplication } from '../core/models';
import { readProblem } from '../core/problem';
import { DecisionCard } from '../shared/decision-card';
import { StatusBadge } from '../shared/status-badge';

@Component({
  selector: 'app-application-detail',
  imports: [RouterLink, CurrencyPipe, DatePipe, ReactiveFormsModule, DecisionCard, StatusBadge],
  template: `
    <p><a routerLink="/">&larr; Back to dashboard</a></p>
    @if (error()) { <p class="alert">{{ error() }}</p> }

    @if (app(); as a) {
      <div class="title-row">
        <h1>{{ a.referenceNumber }}</h1>
        <app-status-badge [status]="a.status" />
      </div>
      <p class="muted">{{ a.applicantName }} &middot; {{ a.emailMasked }} &middot; submitted {{ a.createdAt | date: 'medium' }}</p>

      <div class="grid-2">
        <div>
          <app-decision-card [outcome]="a.status" [riskScore]="a.riskScore" [dtiRatio]="a.dtiRatio" [apr]="a.apr"
            [monthlyPayment]="a.monthlyPayment" [approvedAmount]="a.approvedAmount" [reasons]="a.reasons"
            [label]="'Decided by ' + a.decidedBy" />

          <section class="card">
            <h3>Application details</h3>
            <dl class="details">
              <dt>Product</dt><dd>{{ a.productType }}</dd>
              <dt>Requested</dt><dd>{{ a.requestedAmount | currency: 'CAD' : 'symbol-narrow' }} over {{ a.termMonths }} months</dd>
              <dt>Annual income</dt><dd>{{ a.annualIncome | currency: 'CAD' : 'symbol-narrow' }}</dd>
              <dt>Monthly debt</dt><dd>{{ a.monthlyDebtPayments | currency: 'CAD' : 'symbol-narrow' }}</dd>
              <dt>Credit score</dt><dd>{{ a.creditScore }}</dd>
              <dt>Employment</dt><dd>{{ a.employmentStatus }}</dd>
            </dl>
          </section>
        </div>

        <div>
          @if (a.status === 'REFERRED') {
            <form class="card form review" [formGroup]="reviewForm" (ngSubmit)="review(true)">
              <h3>Underwriter review</h3>
              <p class="muted">The engine referred this application. Record your decision and the reason.</p>
              <label>Underwriter name <input formControlName="underwriter" /></label>
              <label>Decision note <textarea formControlName="note" rows="3" placeholder="e.g. Verified two recent pay stubs"></textarea></label>
              @if (reviewError()) { <p class="alert">{{ reviewError() }}</p> }
              <div class="actions">
                <button type="submit" class="btn btn-approve" [disabled]="reviewForm.invalid || busy()">Approve</button>
                <button type="button" class="btn btn-decline" [disabled]="reviewForm.invalid || busy()" (click)="review(false)">Decline</button>
              </div>
            </form>
          }

          <section class="card">
            <h3>Audit trail</h3>
            <ol class="timeline">
              @for (e of audit(); track e.id) {
                <li>
                  <strong>{{ e.action === 'AUTO_DECISION' ? 'Automated decision' : 'Manual review' }}</strong>
                  <span>{{ e.fromStatus ? e.fromStatus + ' -> ' : '' }}{{ e.toStatus }}</span>
                  <div class="muted small">{{ e.actor }} &middot; {{ e.createdAt | date: 'medium' }}</div>
                  @if (e.note) { <div class="note">{{ e.note }}</div> }
                </li>
              }
            </ol>
          </section>
        </div>
      </div>
    } @else if (!error()) {
      <p class="muted">Loading...</p>
    }
  `,
})
export class ApplicationDetail implements OnInit {
  private readonly api = inject(CreditApiService);

  /** Filled from the route ':id' thanks to withComponentInputBinding() in app.config.ts. */
  readonly id = input.required<string>();

  protected readonly app = signal<CreditApplication | null>(null);
  protected readonly audit = signal<AuditEntry[]>([]);
  protected readonly error = signal('');
  protected readonly reviewError = signal('');
  protected readonly busy = signal(false);

  protected readonly reviewForm = inject(FormBuilder).nonNullable.group({
    underwriter: ['', [Validators.required, Validators.maxLength(100)]],
    note: ['', [Validators.required, Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.load();
  }

  protected review(approve: boolean): void {
    if (this.reviewForm.invalid) return;
    this.busy.set(true);
    this.reviewError.set('');
    this.api.review(Number(this.id()), { approve, ...this.reviewForm.getRawValue() }).subscribe({
      next: updated => { this.app.set(updated); this.busy.set(false); this.loadAudit(); },
      error: err => { this.reviewError.set(readProblem(err).message); this.busy.set(false); },
    });
  }

  private load(): void {
    const id = Number(this.id());
    this.api.get(id).subscribe({
      next: a => this.app.set(a),
      error: err => this.error.set(readProblem(err).message),
    });
    this.loadAudit();
  }

  private loadAudit(): void {
    this.api.audit(Number(this.id())).subscribe({ next: list => this.audit.set(list), error: () => {} });
  }
}
