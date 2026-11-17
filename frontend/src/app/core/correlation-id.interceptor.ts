import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Functional interceptor: stamps every API call with an X-Correlation-Id.
 * The backend logs it on every line, so a failed click in the browser can be traced in the logs.
 */
export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }
  const id = crypto.randomUUID();
  return next(req.clone({ setHeaders: { 'X-Correlation-Id': id } }));
};
