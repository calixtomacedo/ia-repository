import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import { AuthResponse, DecodedToken, LoginRequest, RefreshTokenRequest, RegisterRequest } from '../models/auth.model';
import { MessageResponse, UserResponse } from '../models/user.model';
import { ConfigService } from './config.service';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly configService = inject(ConfigService);
  private readonly notificationService = inject(NotificationService);

  private readonly ACCESS_TOKEN_KEY = 'atom_access_token';
  private readonly REFRESH_TOKEN_KEY = 'atom_refresh_token';
  private readonly TOKEN_TYPE_KEY = 'atom_token_type';
  private readonly EXPIRES_AT_KEY = 'atom_expires_at';

  // Reactive state signals
  readonly accessToken = signal<string | null>(this.getStoredAccessToken());
  readonly refreshToken = signal<string | null>(this.getStoredRefreshToken());

  readonly decodedToken = computed<DecodedToken | null>(() => {
    const token = this.accessToken();
    return token ? this.decodeJwt(token) : null;
  });

  readonly currentUser = computed(() => {
    const decoded = this.decodedToken();
    if (!decoded) return null;
    return {
      id: decoded.userId,
      email: decoded.sub || '',
      name: decoded.name || decoded.sub || 'Usuário',
      role: decoded.role || 'ROLE_USER'
    };
  });

  readonly isAuthenticated = computed<boolean>(() => {
    const token = this.accessToken();
    if (!token) return false;
    return !this.isTokenExpired(token);
  });

  readonly isAdmin = computed<boolean>(() => {
    const user = this.currentUser();
    if (!user) return false;
    return user.role === 'ROLE_ADMIN' || user.role === 'ADMIN';
  });

  private getStoredAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  private getStoredRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    const url = `${this.configService.apiUrl()}/auth/login`;
    return this.http.post<AuthResponse>(url, credentials).pipe(
      tap(response => {
        this.saveAuthData(response);
        this.notificationService.success('Login realizado com sucesso!', 'Bem-vindo');
      }),
      catchError(error => {
        return throwError(() => error);
      })
    );
  }

  register(userData: RegisterRequest): Observable<UserResponse> {
    const url = `${this.configService.apiUrl()}/auth/register`;
    return this.http.post<UserResponse>(url, userData).pipe(
      tap(() => {
        this.notificationService.success('Usuário cadastrado com sucesso!', 'Cadastro');
      }),
      catchError(error => {
        return throwError(() => error);
      })
    );
  }

  refreshCurrentToken(): Observable<AuthResponse> {
    const refresh = this.refreshToken();
    if (!refresh) {
      this.clearSession();
      return throwError(() => new Error('Refresh token inexistente'));
    }

    const url = `${this.configService.apiUrl()}/auth/refresh-token`;
    const payload: RefreshTokenRequest = { refreshToken: refresh };

    return this.http.post<AuthResponse>(url, payload).pipe(
      tap(response => {
        this.saveAuthData(response);
        this.notificationService.info('Sessão renovada com sucesso!', 'Token Atualizado');
      }),
      catchError(error => {
        this.clearSession();
        return throwError(() => error);
      })
    );
  }

  logout(): Observable<MessageResponse | null> {
    const token = this.accessToken();
    const url = `${this.configService.apiUrl()}/auth/logout`;

    // Attempt backend logout to register in logout_audit and blacklist token
    if (token) {
      return this.http.post<MessageResponse>(url, {}).pipe(
        tap(() => {
          this.clearSession();
          this.notificationService.info('Você foi desconectado com segurança.', 'Logout');
          this.router.navigate(['/login']);
        }),
        catchError(() => {
          // Even if backend fails, clear local session
          this.clearSession();
          this.router.navigate(['/login']);
          return of(null);
        })
      );
    } else {
      this.clearSession();
      this.router.navigate(['/login']);
      return of(null);
    }
  }

  saveAuthData(response: AuthResponse): void {
    if (response.accessToken) {
      localStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
      this.accessToken.set(response.accessToken);
    }

    if (response.refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
      this.refreshToken.set(response.refreshToken);
    }

    if (response.tokenType) {
      localStorage.setItem(this.TOKEN_TYPE_KEY, response.tokenType);
    }

    if (response.expiresIn) {
      const expiresAt = Date.now() + response.expiresIn * 1000;
      localStorage.setItem(this.EXPIRES_AT_KEY, expiresAt.toString());
    }
  }

  clearSession(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_TYPE_KEY);
    localStorage.removeItem(this.EXPIRES_AT_KEY);

    this.accessToken.set(null);
    this.refreshToken.set(null);
  }

  decodeJwt(token: string): DecodedToken | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = parts[1];
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  }

  isTokenExpired(token: string): boolean {
    const decoded = this.decodeJwt(token);
    if (!decoded || !decoded.exp) return true;
    const expiryDate = new Date(decoded.exp * 1000);
    return expiryDate <= new Date();
  }

  getTokenRemainingSeconds(): number {
    const decoded = this.decodedToken();
    if (!decoded || !decoded.exp) return 0;
    const diff = decoded.exp - Math.floor(Date.now() / 1000);
    return Math.max(0, diff);
  }
}
