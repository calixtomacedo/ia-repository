import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthResponse } from '../../core/models/auth.model';

@Component({
  selector: 'app-tokens',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tokens.component.html',
  styleUrl: './tokens.component.scss'
})
export class TokensComponent implements OnInit, OnDestroy {
  readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  isRefreshing = signal<boolean>(false);
  isLoggingOut = signal<boolean>(false);
  remainingSeconds = signal<number>(0);
  private timerInterval: any = null;

  // Custom Refresh Token test
  customRefreshToken = signal<string>('');
  customTestResponse = signal<AuthResponse | null>(null);
  customTestError = signal<string | null>(null);
  isCustomTesting = signal<boolean>(false);

  ngOnInit(): void {
    this.updateRemainingSeconds();
    this.timerInterval = setInterval(() => {
      this.updateRemainingSeconds();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  private updateRemainingSeconds(): void {
    this.remainingSeconds.set(this.authService.getTokenRemainingSeconds());
  }

  copyToClipboard(text: string | null, label: string): void {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
      this.notificationService.success(`${label} copiado para a área de transferência!`);
    });
  }

  executeRefresh(): void {
    this.isRefreshing.set(true);
    this.authService.refreshCurrentToken().subscribe({
      next: (response) => {
        this.isRefreshing.set(false);
        this.updateRemainingSeconds();
        this.notificationService.success(
          `Token renovado com sucesso! Expira em ${response.expiresIn} segundos.`,
          'Refresh Token OK'
        );
      },
      error: (err) => {
        this.isRefreshing.set(false);
        const msg = err.error?.message || 'Falha ao renovar token.';
        this.notificationService.error(msg, 'Erro no Refresh');
      }
    });
  }

  executeLogout(): void {
    this.isLoggingOut.set(true);
    this.authService.logout().subscribe({
      next: () => {
        this.isLoggingOut.set(false);
      },
      error: () => {
        this.isLoggingOut.set(false);
      }
    });
  }

  formatSeconds(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs < 10 ? '0' : ''}${secs}s`;
  }
}
