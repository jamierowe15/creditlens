import { Routes } from '@angular/router';

// loadComponent = lazy loading: each page's code downloads only when the user opens it.
export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/dashboard').then(m => m.Dashboard), title: 'CreditLens | Dashboard' },
  { path: 'apply', loadComponent: () => import('./pages/new-application').then(m => m.NewApplication), title: 'CreditLens | New application' },
  { path: 'applications/:id', loadComponent: () => import('./pages/application-detail').then(m => m.ApplicationDetail), title: 'CreditLens | Application' },
  { path: '**', redirectTo: '' },
];
