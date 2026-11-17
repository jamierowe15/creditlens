import { Component, computed, input } from '@angular/core';
import { CurrencyPipe, DecimalPipe, PercentPipe } from '@angular/common';
import { ApplicationStatus, Reason } from '../core/models';
import { StatusBadge } from './status-badge';

/** Shows a decision. Used for the live preview AND the saved application, so they always match. */
@Component({
  selector: 'app-decision-card',
  imports: [CurrencyPipe, DecimalPipe, PercentPipe, StatusBadge],
  template: `
    <div class="card decision" [class]="'card decision decision-' + outcome().toLowerCase()">
      <div class="decision-head">
        <app-status-badge [status]="outcome()" />
        <span class="muted">{{ heading() }}</span>
      </div>
      <dl class="metrics">
        <div><dt>Risk score</dt><dd>{{ riskScore() }}<small>/100</small></dd></div>
        <div><dt>Debt-to-income</dt><dd [class.warn]="dtiRatio() > 0.4">{{ dtiRatio() | percent: '1.1-1' }}</dd></div>
        <div><dt>APR</dt><dd>{{ apr() | number: '1.2-2' }}%</dd></div>
        <div><dt>Est. payment</dt><dd>{{ monthlyPayment() | currency: 'CAD' : 'symbol-narrow' }}<small>/mo</small></dd></div>
      </dl>
      <div class="risk-bar" role="img" [attr.aria-label]="'Risk ' + riskScore() + ' out of 100'">
        <span [style.width.%]="riskScore()"></span>
      </div>
      @if (approvedAmount() !== null) {
        <p class="approved">Approved amount: <strong>{{ approvedAmount() | currency: 'CAD' : 'symbol-narrow' }}</strong></p>
      }
      @if (reasons().length > 0) {
        <h4>Reasons</h4>
        <ul class="reasons">
          @for (r of reasons(); track r.code) {
            <li [class]="'sev-' + r.severity.toLowerCase()"><code>{{ r.code }}</code> {{ r.description }}</li>
          }
        </ul>
      } @else {
        <p class="muted">All credit rules passed.</p>
      }
    </div>
  `,
})
export class DecisionCard {
  readonly outcome = input.required<ApplicationStatus>();
  readonly riskScore = input.required<number>();
  readonly dtiRatio = input.required<number>();
  readonly apr = input.required<number>();
  readonly monthlyPayment = input.required<number>();
  readonly approvedAmount = input<number | null>(null);
  readonly reasons = input<Reason[]>([]);
  readonly label = input('');

  // computed() recalculates only when the signals it reads change.
  readonly heading = computed(() => {
    if (this.label()) return this.label();
    switch (this.outcome()) {
      case 'APPROVED': return 'Meets all credit policies';
      case 'REFERRED': return 'Needs an underwriter decision';
      default: return 'Does not meet credit policy';
    }
  });
}
