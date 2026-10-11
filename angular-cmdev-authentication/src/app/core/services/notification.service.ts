import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'danger' | 'warning' | 'info';
  title?: string;
  message: string;
  timeout?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly toastsSignal = signal<ToastMessage[]>([]);
  public readonly toasts = this.toastsSignal.asReadonly();

  show(message: string, type: 'success' | 'danger' | 'warning' | 'info' = 'info', title?: string, timeout: number = 5000): void {
    const id = Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = { id, type, title, message, timeout };

    this.toastsSignal.update(toasts => [...toasts, newToast]);

    if (timeout > 0) {
      setTimeout(() => {
        this.remove(id);
      }, timeout);
    }
  }

  success(message: string, title: string = 'Sucesso'): void {
    this.show(message, 'success', title);
  }

  error(message: string, title: string = 'Erro'): void {
    this.show(message, 'danger', title, 7000);
  }

  warning(message: string, title: string = 'Atenção'): void {
    this.show(message, 'warning', title);
  }

  info(message: string, title: string = 'Informação'): void {
    this.show(message, 'info', title);
  }

  remove(id: string): void {
    this.toastsSignal.update(toasts => toasts.filter(t => t.id !== id));
  }

  clear(): void {
    this.toastsSignal.set([]);
  }
}
