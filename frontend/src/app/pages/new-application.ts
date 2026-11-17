import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, debounceTime, filter, map, of, switchMap } from 'rxjs';
import { CreditApiService } from '../core/credit-api.service';
import {
  ApplicantFinancials, DecisionPreview, EMPLOYMENT_OPTIONS, EmploymentStatus, PRODUCT_OPTIONS, ProductType,
} from '../core/models';
import { readProblem } from '../core/problem';
import { DecisionCard } from '../shared/decision-card';

@Component({
  selector: 'app-new-application',
  imports: [ReactiveFormsModule, DecisionCard],
  template: `
    <h1>New credit application</h1>
    <div class="grid-2">
      <form class="card form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <fieldset>
          <legend>Applicant</legend>
          <label>Full name
            <input formControlName="applicantName" autocomplete="name" />
            @if (showError('applicantName')) { <span class="field-error">{{ fieldError('applicantName', 'Name is required') }}</span> }
          </label>
          <label>Email
            <input formControlName="email" type="email" autocomplete="email" />
            @if (showError('email')) { <span class="field-error">{{ fieldError('email', 'Enter a valid email') }}</span> }
          </label>
          <label>Employment
            <select formControlName="employmentStatus">
              @for (o of employmentOptions; track o.value) { <option [value]="o.value">{{ o.label }}</option> }
            </select>
          </label>
        </fieldset>

        <fieldset>
          <legend>Finances</legend>
          <label>Annual income (CAD)
            <input formControlName="annualIncome" type="number" min="0" step="1000" />
            @if (showError('annualIncome')) { <span class="field-error">{{ fieldError('annualIncome', 'Must be 0 or more') }}</span> }
          </label>
          <label>Existing monthly debt payments (CAD)
            <input formControlName="monthlyDebtPayments" type="number" min="0" step="50" />
            @if (showError('monthlyDebtPayments')) { <span class="field-error">{{ fieldError('monthlyDebtPayments', 'Must be 0 or more') }}</span> }
          </label>
          <label>Credit score (300-900)
            <input formControlName="creditScore" type="number" min="300" max="900" />
            @if (showError('creditScore')) { <span class="field-error">{{ fieldError('creditScore', 'Between 300 and 900') }}</span> }
          </label>
        </fieldset>

        <fieldset>
          <legend>Request</legend>
          <label>Product
            <select formControlName="productType">
              @for (o of productOptions; track o.value) { <option [value]="o.value">{{ o.label }}</option> }
            </select>
          </label>
          <label>Amount (CAD 1,000 - 100,000)
            <input formControlName="requestedAmount" type="number" min="1000" max="100000" step="500" />
            @if (showError('requestedAmount')) { <span class="field-error">{{ fieldError('requestedAmount', 'Between 1,000 and 100,000') }}</span> }
          </label>
          <label>Term (months, 6-84)
            <input formControlName="termMonths" type="number" min="6" max="84" step="6" />
            @if (showError('termMonths')) { <span class="field-error">{{ fieldError('termMonths', 'Between 6 and 84') }}</span> }
          </label>
        </fieldset>

        @if (submitError()) { <p class="alert">{{ submitError() }}</p> }
        <button type="submit" class="btn btn-primary" [disabled]="submitting()">
          {{ submitting() ? 'Submitting...' : 'Submit application' }}
        </button>
      </form>

      <aside>
        <h2>Live decision preview</h2>
        <p class="muted">Updates as you type. Nothing is saved until you submit.</p>
        @if (preview(); as p) {
          <app-decision-card [outcome]="p.outcome" [riskScore]="p.riskScore" [dtiRatio]="p.dtiRatio"
            [apr]="p.apr" [monthlyPayment]="p.monthlyPayment" [approvedAmount]="p.approvedAmount"
            [reasons]="p.reasons" />
          <p class="muted small">Rules version {{ p.rulesVersion }}</p>
        } @else {
          <div class="card muted">{{ previewMessage() }}</div>
        }
      </aside>
    </div>
  `,
})
export class NewApplication implements OnInit {
  private readonly api = inject(CreditApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly employmentOptions = EMPLOYMENT_OPTIONS;
  protected readonly productOptions = PRODUCT_OPTIONS;

  // Client-side rules mirror the server's @Min/@Max. The server still re-checks everything.
  protected readonly form = inject(FormBuilder).nonNullable.group({
    applicantName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    annualIncome: [65000, [Validators.required, Validators.min(0)]],
    monthlyDebtPayments: [400, [Validators.required, Validators.min(0)]],
    creditScore: [700, [Validators.required, Validators.min(300), Validators.max(900)]],
    requestedAmount: [10000, [Validators.required, Validators.min(1000), Validators.max(100000)]],
    termMonths: [36, [Validators.required, Validators.min(6), Validators.max(84)]],
    employmentStatus: ['FULL_TIME' as EmploymentStatus, Validators.required],
    productType: ['PERSONAL_LOAN' as ProductType, Validators.required],
  });

  protected readonly preview = signal<DecisionPreview | null>(null);
  protected readonly previewMessage = signal('Fill in the finances to see a decision.');
  protected readonly submitting = signal(false);
  protected readonly submitError = signal('');
  protected readonly serverErrors = signal<Record<string, string>>({});

  private readonly financialFields = [
    'annualIncome', 'monthlyDebtPayments', 'creditScore', 'requestedAmount',
    'termMonths', 'employmentStatus', 'productType',
  ] as const;

  ngOnInit(): void {
    // RxJS pipeline: wait for typing to pause, skip invalid input, cancel stale requests (switchMap).
    this.form.valueChanges.pipe(
      debounceTime(350),
      map(() => this.financials()),
      filter((f): f is ApplicantFinancials => f !== null),
      switchMap(f => this.api.preview(f).pipe(
        catchError(err => { this.previewMessage.set(readProblem(err).message); return of(null); }),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(p => this.preview.set(p));

    // Trigger the first preview with the default values.
    this.form.updateValueAndValidity({ emitEvent: true });
  }

  protected showError(name: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[name];
    return (c.invalid && (c.touched || c.dirty)) || !!this.serverErrors()[name];
  }

  protected fieldError(name: string, fallback: string): string {
    return this.serverErrors()[name] ?? fallback;
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.submitError.set('Please fix the highlighted fields.');
      return;
    }
    this.submitting.set(true);
    this.submitError.set('');
    this.serverErrors.set({});
    this.api.submit(this.form.getRawValue()).subscribe({
      next: app => this.router.navigate(['/applications', app.id]),
      error: err => {
        const problem = readProblem(err);
        this.submitError.set(problem.message);
        this.serverErrors.set(problem.fields);
        this.submitting.set(false);
      },
    });
  }

  private financials(): ApplicantFinancials | null {
    const valid = this.financialFields.every(f => this.form.controls[f].valid);
    if (!valid) {
      this.preview.set(null);
      this.previewMessage.set('Fix the highlighted finance fields to see a decision.');
      return null;
    }
    const v = this.form.getRawValue();
    return {
      annualIncome: v.annualIncome, monthlyDebtPayments: v.monthlyDebtPayments, creditScore: v.creditScore,
      requestedAmount: v.requestedAmount, termMonths: v.termMonths,
      employmentStatus: v.employmentStatus, productType: v.productType,
    };
  }
}
