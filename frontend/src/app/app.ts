import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a routerLink="/" class="brand" aria-label="CreditLens home">
        <span class="logo" aria-hidden="true">CL</span> CreditLens
        <span class="tag">Credit Decisions</span>
      </a>
      <nav>
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
        <a routerLink="/apply" routerLinkActive="active" class="btn btn-primary">New application</a>
      </nav>
    </header>
    <main class="container">
      <router-outlet />
    </main>
    <footer class="footer">Portfolio demo with fictional data. Not affiliated with any bank.</footer>
  `,
})
export class App {}
