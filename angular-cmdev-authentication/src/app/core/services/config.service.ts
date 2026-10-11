import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { catchError, map, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private readonly http = inject(HttpClient);
  private readonly storageKey = 'atom_api_url';

  private readonly apiUrlSignal = signal<string>(this.getInitialApiUrl());
  public readonly apiUrl = this.apiUrlSignal.asReadonly();

  private readonly backendHealthySignal = signal<boolean | null>(null);
  public readonly backendHealthy = this.backendHealthySignal.asReadonly();

  private getInitialApiUrl(): string {
    const saved = localStorage.getItem(this.storageKey);
    return saved && saved.trim() !== '' ? saved.trim() : environment.apiUrl;
  }

  setApiUrl(url: string): void {
    const cleaned = url.trim().replace(/\/+$/, '');
    localStorage.setItem(this.storageKey, cleaned);
    this.apiUrlSignal.set(cleaned);
  }

  resetApiUrl(): void {
    localStorage.removeItem(this.storageKey);
    this.apiUrlSignal.set(environment.apiUrl);
  }

  checkHealth() {
    const currentUrl = this.apiUrl();
    // Test backend reachability via OpenAPI endpoint or root
    return this.http.get(`${currentUrl}/v3/api-docs`, { observe: 'response' }).pipe(
      map(res => {
        const isOk = res.status >= 200 && res.status < 400;
        this.backendHealthySignal.set(isOk);
        return isOk;
      }),
      catchError(() => {
        this.backendHealthySignal.set(false);
        return of(false);
      })
    );
  }
}
