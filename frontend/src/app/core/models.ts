// TypeScript mirrors of the Java DTOs. If the API changes, the compiler tells us what broke.

export type ApplicationStatus = 'APPROVED' | 'REFERRED' | 'DECLINED';
export type EmploymentStatus =
  'FULL_TIME' | 'PART_TIME' | 'SELF_EMPLOYED' | 'STUDENT' | 'RETIRED' | 'UNEMPLOYED';
export type ProductType = 'PERSONAL_LOAN' | 'CAR_LOAN' | 'LINE_OF_CREDIT';
export type Severity = 'PASS' | 'REFER' | 'DECLINE';

export interface Reason {
  code: string;
  severity: Severity;
  description: string;
}

export interface ApplicantFinancials {
  annualIncome: number;
  monthlyDebtPayments: number;
  creditScore: number;
  requestedAmount: number;
  termMonths: number;
  employmentStatus: EmploymentStatus;
  productType: ProductType;
}

export interface CreateApplicationRequest extends ApplicantFinancials {
  applicantName: string;
  email: string;
}

export interface CreditApplication extends ApplicantFinancials {
  id: number;
  referenceNumber: string;
  applicantName: string;
  emailMasked: string;
  status: ApplicationStatus;
  riskScore: number;
  dtiRatio: number;
  apr: number;
  monthlyPayment: number;
  approvedAmount: number | null;
  reasons: Reason[];
  decidedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface DecisionPreview {
  outcome: ApplicationStatus;
  riskScore: number;
  dtiRatio: number;
  apr: number;
  monthlyPayment: number;
  approvedAmount: number | null;
  reasons: Reason[];
  rulesVersion: string;
}

export interface AuditEntry {
  id: number;
  action: 'AUTO_DECISION' | 'MANUAL_REVIEW';
  fromStatus: ApplicationStatus | null;
  toStatus: ApplicationStatus;
  actor: string;
  note: string | null;
  rulesVersion: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Stats {
  total: number;
  approved: number;
  referred: number;
  declined: number;
  approvalRatePercent: number;
  averageCreditScore: number;
}

export interface ReviewRequest {
  approve: boolean;
  underwriter: string;
  note: string;
}

/** Shape of the RFC 9457 problem+json errors the API returns. */
export interface Problem {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export const EMPLOYMENT_OPTIONS: { value: EmploymentStatus; label: string }[] = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'SELF_EMPLOYED', label: 'Self-employed' },
  { value: 'STUDENT', label: 'Student' },
  { value: 'RETIRED', label: 'Retired' },
  { value: 'UNEMPLOYED', label: 'Unemployed' },
];

export const PRODUCT_OPTIONS: { value: ProductType; label: string }[] = [
  { value: 'PERSONAL_LOAN', label: 'Personal loan' },
  { value: 'CAR_LOAN', label: 'Car loan' },
  { value: 'LINE_OF_CREDIT', label: 'Line of credit' },
];
