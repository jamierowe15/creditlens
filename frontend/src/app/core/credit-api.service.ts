import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ApplicantFinancials, ApplicationStatus, AuditEntry, CreateApplicationRequest,
  CreditApplication, DecisionPreview, Page, ReviewRequest, Stats,
} from './models';

/**
 * The only class that knows URLs. Components ask for data; they never build HTTP calls.
 * Relative '/api' URLs go through proxy.conf.json to http://localhost:8080 during development.
 */
@Injectable({ providedIn: 'root' })
export class CreditApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  list(status: ApplicationStatus | '' = '', page = 0, size = 20): Observable<Page<CreditApplication>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<CreditApplication>>(`${this.base}/applications`, { params });
  }

  get(id: number): Observable<CreditApplication> {
    return this.http.get<CreditApplication>(`${this.base}/applications/${id}`);
  }

  audit(id: number): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(`${this.base}/applications/${id}/audit`);
  }

  submit(request: CreateApplicationRequest): Observable<CreditApplication> {
    return this.http.post<CreditApplication>(`${this.base}/applications`, request);
  }

  review(id: number, request: ReviewRequest): Observable<CreditApplication> {
    return this.http.post<CreditApplication>(`${this.base}/applications/${id}/review`, request);
  }

  preview(financials: ApplicantFinancials): Observable<DecisionPreview> {
    return this.http.post<DecisionPreview>(`${this.base}/decisions/preview`, financials);
  }

  stats(): Observable<Stats> {
    return this.http.get<Stats>(`${this.base}/stats`);
  }
}
