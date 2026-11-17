import { HttpErrorResponse } from '@angular/common/http';
import { Problem } from './models';

/** Turn any HTTP error into a short, human message plus optional per-field errors. */
export function readProblem(err: unknown): { message: string; fields: Record<string, string> } {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return { message: 'Cannot reach the API. Is the backend running on port 8080?', fields: {} };
    }
    const p = (err.error ?? {}) as Problem;
    return { message: p.detail ?? p.title ?? `Request failed (${err.status})`, fields: p.errors ?? {} };
  }
  return { message: 'Something went wrong.', fields: {} };
}
